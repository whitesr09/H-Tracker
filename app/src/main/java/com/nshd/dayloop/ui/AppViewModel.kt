package com.nshd.dayloop.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nshd.dayloop.data.Completion
import com.nshd.dayloop.data.DayLoopStore
import com.nshd.dayloop.data.HabitKind
import com.nshd.dayloop.data.ItemType
import com.nshd.dayloop.data.TrackItem
import com.nshd.dayloop.notify.ReminderScheduler
import com.nshd.dayloop.widget.DayLoopWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class HabitStrength(
    val itemId: String,
    val name: String,
    val percent: Float
)

data class TodayUiState(
    val dueItems: List<TrackItem> = emptyList(),
    val todayCompletions: Map<String, Completion> = emptyMap(),
    val fraction: Float = 0f,
    val doneCount: Int = 0,
    val streak: Int = 0,
    val nextDose: TrackItem? = null,
    val totalItems: Int = 0,
    val totalCheckins: Int = 0,
    val medAdherence30: Float? = null,
    val lowStock: List<TrackItem> = emptyList(),
    /** Oldest → today, 105 days; -1f marks a day with nothing due. */
    val heatmap: List<Float> = emptyList(),
    val strengths: List<HabitStrength> = emptyList(),
    val weekCheckins: Int = 0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val store = DayLoopStore(application)

    val state: StateFlow<TodayUiState> = combine(store.items, store.completions) { items, comps ->
        buildState(items, comps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    init {
        // Keep reminder alarms in sync with the data: any item or
        // completion change re-arms the next due reminder for each item.
        viewModelScope.launch {
            combine(store.items, store.completions) { items, comps -> items to comps }
                .collect { (items, comps) ->
                    ReminderScheduler.scheduleAll(app, items, comps)
                    DayLoopWidget.requestUpdate(app)
                }
        }
    }

    private fun fractionOf(item: TrackItem, comp: Completion?): Float {
        if (comp == null) return 0f
        return when {
            comp.status == Completion.SKIPPED -> 0f
            comp.status == Completion.LATER -> 0f
            item.type == ItemType.HABIT && item.kind == HabitKind.COUNT ->
                (comp.value.toFloat() / item.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
            comp.status == Completion.DONE || comp.status == Completion.TAKEN -> 1f
            else -> 0f
        }
    }

    private fun buildState(items: List<TrackItem>, comps: List<Completion>): TodayUiState {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val dow = today.dayOfWeek.value

        val due = items.filter { it.dueOn(dow) }
            .sortedWith(compareBy({ it.group.ordinal }, { it.timeMinutes ?: 9999 }, { it.createdAt }))
        val todayMap = comps.filter { it.epochDay == todayEpoch }.associateBy { it.itemId }

        val fractions = due.map { fractionOf(it, todayMap[it.id]) }
        val overall = if (fractions.isEmpty()) 0f else fractions.average().toFloat()
        val doneCount = fractions.count { it >= 1f }

        var streak = 0
        var offset = 0
        while (offset < 365) {
            val day = today.minusDays(offset.toLong())
            val dayDue = items.filter { it.dueOn(day.dayOfWeek.value) }
            if (dayDue.isEmpty()) {
                offset++
                continue
            }
            val dayMap = comps.filter { it.epochDay == day.toEpochDay() }.associateBy { it.itemId }
            val complete = dayDue.all { fractionOf(it, dayMap[it.id]) >= 1f }
            if (complete) {
                streak++
                offset++
            } else {
                if (offset == 0) offset++ else break
            }
        }

        val nextDose = due.filter { it.type == ItemType.MEDICATION }
            .filter { todayMap[it.id]?.status != Completion.TAKEN }
            .minByOrNull { it.timeMinutes ?: 9999 }

        // Medication adherence over the last 30 days: taken vs taken+skipped.
        val medIds = items.filter { it.type == ItemType.MEDICATION }.map { it.id }.toSet()
        val windowStart = todayEpoch - 29
        val medComps = comps.filter {
            it.itemId in medIds && it.epochDay in windowStart..todayEpoch &&
                (it.status == Completion.TAKEN || it.status == Completion.SKIPPED)
        }
        val medAdherence = if (medComps.isEmpty()) null else
            medComps.count { it.status == Completion.TAKEN }.toFloat() / medComps.size.toFloat()

        val lowStock = items.filter { item ->
            val days = item.daysOfPillsLeft()
            item.type == ItemType.MEDICATION && days != null && days <= 7
        }

        // Heatmap: per-day completion fraction for the last 105 days.
        val compsByDay = comps.groupBy { it.epochDay }
        val heatmap = (104 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val dayDue = items.filter { it.dueOn(day.dayOfWeek.value) }
            if (dayDue.isEmpty()) {
                -1f
            } else {
                val dayMap = compsByDay[day.toEpochDay()].orEmpty().associateBy { it.itemId }
                dayDue.map { fractionOf(it, dayMap[it.id]) }.average().toFloat()
            }
        }

        // Forgiving strength per habit: average daily fraction over the
        // last 28 days (misses lower it gradually, never wipe it).
        val strengths = items.filter { it.type == ItemType.HABIT && !it.archived }.map { habit ->
            var sum = 0f
            var days = 0
            for (back in 0..27) {
                val day = today.minusDays(back.toLong())
                if (!habit.dueOn(day.dayOfWeek.value)) continue
                val createdDay = java.time.Instant.ofEpochMilli(habit.createdAt)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                if (day.isBefore(createdDay)) continue
                val comp = compsByDay[day.toEpochDay()].orEmpty()
                    .firstOrNull { it.itemId == habit.id }
                sum += fractionOf(habit, comp)
                days++
            }
            HabitStrength(habit.id, habit.name, if (days == 0) 0f else sum / days)
        }.sortedByDescending { it.percent }

        val weekStart = todayEpoch - 6
        val weekCheckins = comps.count {
            it.epochDay in weekStart..todayEpoch &&
                (it.status == Completion.DONE || it.status == Completion.TAKEN)
        }

        return TodayUiState(
            dueItems = due,
            todayCompletions = todayMap,
            fraction = overall,
            doneCount = doneCount,
            streak = streak,
            nextDose = nextDose,
            totalItems = items.count { !it.archived },
            totalCheckins = comps.count { it.status == Completion.DONE || it.status == Completion.TAKEN },
            medAdherence30 = medAdherence,
            lowStock = lowStock,
            heatmap = heatmap,
            strengths = strengths,
            weekCheckins = weekCheckins
        )
    }

    /** Mark an item done for today without toggling (Run mode finish). */
    fun markDone(item: TrackItem) {
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            val status = if (item.type == ItemType.MEDICATION) Completion.TAKEN else Completion.DONE
            store.setCompletion(Completion(item.id, today, status, 1))
        }
    }

    fun toggle(item: TrackItem) {
        val today = LocalDate.now().toEpochDay()
        val current = state.value.todayCompletions[item.id]
        val isDone = current != null &&
            (current.status == Completion.DONE || current.status == Completion.TAKEN)
        viewModelScope.launch {
            if (isDone) {
                store.removeCompletion(item.id, today)
                adjustPills(item, takingBack = true)
            } else {
                val status = if (item.type == ItemType.MEDICATION) Completion.TAKEN else Completion.DONE
                val value = if (item.type == ItemType.HABIT && item.kind == HabitKind.COUNT) item.targetCount else 1
                store.setCompletion(Completion(item.id, today, status, value))
                adjustPills(item, takingBack = false)
            }
        }
    }

    /** Pill stock moves only for medications with a tracked count. */
    private suspend fun adjustPills(item: TrackItem, takingBack: Boolean) {
        if (item.type != ItemType.MEDICATION) return
        val left = item.pillsLeft ?: return
        val per = item.pillsPerDose.coerceAtLeast(1)
        val updated = if (takingBack) left + per else (left - per).coerceAtLeast(0)
        if (updated != left) store.updateItem(item.copy(pillsLeft = updated))
    }

    fun changeCount(item: TrackItem, delta: Int) {
        val today = LocalDate.now().toEpochDay()
        val current = state.value.todayCompletions[item.id]
        val newValue = ((current?.value ?: 0) + delta).coerceIn(0, item.targetCount)
        viewModelScope.launch {
            if (newValue <= 0) {
                store.removeCompletion(item.id, today)
            } else {
                store.setCompletion(Completion(item.id, today, Completion.DONE, newValue))
            }
        }
    }

    fun markSkipped(item: TrackItem) {
        val today = LocalDate.now().toEpochDay()
        val current = state.value.todayCompletions[item.id]
        viewModelScope.launch {
            if (current?.status == Completion.SKIPPED) {
                store.removeCompletion(item.id, today)
            } else {
                store.setCompletion(Completion(item.id, today, Completion.SKIPPED, 0))
            }
        }
    }

    /** "Later" snoozes the dose reminder by 30 minutes; the dose stays open. */
    fun markLater(item: TrackItem) {
        val today = LocalDate.now().toEpochDay()
        val current = state.value.todayCompletions[item.id]
        viewModelScope.launch {
            if (current?.status == Completion.LATER) {
                store.removeCompletion(item.id, today)
            } else {
                store.setCompletion(Completion(item.id, today, Completion.LATER, 0))
                ReminderScheduler.snooze(app, item)
            }
        }
    }

    fun addItem(item: TrackItem) {
        viewModelScope.launch { store.addItem(item) }
    }

    fun updateItem(item: TrackItem) {
        viewModelScope.launch { store.updateItem(item) }
    }

    fun deleteItem(item: TrackItem) {
        viewModelScope.launch {
            ReminderScheduler.cancel(app, item.id)
            store.deleteItem(item.id)
        }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
