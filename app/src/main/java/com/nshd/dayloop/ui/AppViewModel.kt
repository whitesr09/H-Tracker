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
    val weekCheckins: Int = 0,
    val allItems: List<TrackItem> = emptyList(),
    /** Done-count this week (Mon–today) for items that carry a weekly target. */
    val weeklyProgress: Map<String, Int> = emptyMap(),
    val weekReview: WeekReview = WeekReview(),
    val monthLabel: String = "",
    val monthOffset: Int = 0,
    val monthFractions: List<Float> = emptyList(),
    val completions: List<Completion> = emptyList()
)

data class WeekReview(
    val thisWeekPct: Int = 0,
    val bestDayLabel: String = "—",
    val bestDayCount: Int = 0,
    val thisWeekCheckins: Int = 0,
    val lastWeekCheckins: Int = 0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val store = DayLoopStore(application)

    val state: StateFlow<TodayUiState> = combine(store.items, store.completions) { items, comps ->
        buildState(items, comps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    val alarmPrefs: StateFlow<com.nshd.dayloop.data.AlarmPrefs> = store.alarmPrefs
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.nshd.dayloop.data.AlarmPrefs()
        )

    fun setAlarmSound(value: Boolean) {
        viewModelScope.launch {
            store.saveAlarmPrefs(alarmPrefs.value.copy(sound = value))
        }
    }

    fun setAlarmVibrate(value: Boolean) {
        viewModelScope.launch {
            store.saveAlarmPrefs(alarmPrefs.value.copy(vibrate = value))
        }
    }

    fun setDefaultRemindMode(mode: com.nshd.dayloop.data.RemindMode) {
        viewModelScope.launch {
            store.saveAlarmPrefs(alarmPrefs.value.copy(defaultMode = mode))
        }
    }

    val themeDark: StateFlow<Boolean> = store.themeDark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setThemeDark(dark: Boolean) {
        viewModelScope.launch { store.saveThemeDark(dark) }
    }

    fun exportJson(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(store.exportJson()) }
    }

    fun importJson(json: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(store.importJson(json)) }
    }

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
            .sortedWith(
                compareBy(
                    { it.group.ordinal },
                    { it.sortOrder },
                    { it.timeMinutes ?: 9999 },
                    { it.createdAt }
                )
            )
        val todayMap = comps.filter { it.epochDay == todayEpoch }.associateBy { it.itemId }

        // Weekly-target items: done count from Monday to today.
        val mondayEpoch = today.minusDays((dow - 1).toLong()).toEpochDay()
        val weeklyProgress = items.filter { it.weeklyTarget != null }.associate { item ->
            item.id to comps.count { c ->
                c.itemId == item.id && c.epochDay in mondayEpoch..todayEpoch &&
                    (c.status == Completion.DONE || c.status == Completion.TAKEN)
            }
        }

        val fractions = due.map { item ->
            val base = fractionOf(item, todayMap[item.id])
            val target = item.weeklyTarget
            if (target != null && (weeklyProgress[item.id] ?: 0) >= target) {
                maxOf(base, 1f)
            } else {
                base
            }
        }
        val overall = if (fractions.isEmpty()) 0f else fractions.average().toFloat()
        val doneCount = fractions.count { it >= 1f }

        var streak = 0
        var offset = 0
        while (offset < 365) {
            val day = today.minusDays(offset.toLong())
            val dayDue = items.filter { it.dueOn(day.dayOfWeek.value) && it.weeklyTarget == null }
            if (dayDue.isEmpty()) {
                offset++
                continue
            }
            val dayMap = comps.filter { it.epochDay == day.toEpochDay() }.associateBy { it.itemId }
            val complete = dayDue.all { fractionOf(it, dayMap[it.id]) >= 1f }
            // Rest day: everything due was resolved and at least one item was
            // consciously skipped — the streak survives but does not grow.
            val excused = dayDue.all {
                val c = dayMap[it.id]
                c != null && c.status != Completion.LATER
            } && dayDue.any { dayMap[it.id]?.status == Completion.SKIPPED }
            if (complete) {
                streak++
                offset++
            } else if (excused) {
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
            val dayDue = items.filter { it.dueOn(day.dayOfWeek.value) && it.weeklyTarget == null }
            if (dayDue.isEmpty()) {
                -1f
            } else {
                val dayMap = compsByDay[day.toEpochDay()].orEmpty().associateBy { it.itemId }
                dayDue.map { fractionOf(it, dayMap[it.id]) }.average().toFloat()
            }
        }

        // Forgiving strength per habit: average daily fraction over the
        // last 28 days (misses lower it gradually, never wipe it).
        val strengths = items.filter {
            it.type == ItemType.HABIT && !it.archived && it.weeklyTarget == null
        }.map { habit ->
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

        // Week review (Mon–today vs the full week before it).
        val reviewDays = (0 until dow).map { today.minusDays((dow - 1 - it).toLong()) }
        val dayFractions = reviewDays.map { day ->
            val dayDue = items.filter { it.dueOn(day.dayOfWeek.value) && it.weeklyTarget == null }
            if (dayDue.isEmpty()) {
                null
            } else {
                val dayMap = compsByDay[day.toEpochDay()].orEmpty().associateBy { it.itemId }
                dayDue.map { fractionOf(it, dayMap[it.id]) }.average().toFloat()
            }
        }
        val validFractions = dayFractions.filterNotNull()
        val thisWeekPct = if (validFractions.isEmpty()) 0 else (validFractions.average() * 100).toInt()
        val checkinsOn = { day: LocalDate ->
            comps.count {
                it.epochDay == day.toEpochDay() &&
                    (it.status == Completion.DONE || it.status == Completion.TAKEN)
            }
        }
        val bestDay = reviewDays.maxByOrNull { checkinsOn(it) }
        val weekReview = WeekReview(
            thisWeekPct = thisWeekPct,
            bestDayLabel = bestDay?.dayOfWeek
                ?.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) ?: "—",
            bestDayCount = bestDay?.let { checkinsOn(it) } ?: 0,
            thisWeekCheckins = comps.count {
                it.epochDay in mondayEpoch..todayEpoch &&
                    (it.status == Completion.DONE || it.status == Completion.TAKEN)
            },
            lastWeekCheckins = comps.count {
                it.epochDay in (mondayEpoch - 7) until mondayEpoch &&
                    (it.status == Completion.DONE || it.status == Completion.TAKEN)
            }
        )

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
            weekCheckins = weekCheckins,
            allItems = items,
            weeklyProgress = weeklyProgress,
            weekReview = weekReview,
            monthLabel = today.month
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                .uppercase() + " " + today.year,
            monthOffset = today.withDayOfMonth(1).dayOfWeek.value - 1,
            monthFractions = (1..today.lengthOfMonth()).map { dayOfMonth ->
                val day = today.withDayOfMonth(dayOfMonth)
                when {
                    day.isAfter(today) -> -2f
                    else -> {
                        val dayDue = items.filter {
                            it.dueOn(day.dayOfWeek.value) && it.weeklyTarget == null
                        }
                        if (dayDue.isEmpty()) {
                            -1f
                        } else {
                            val dayMap = compsByDay[day.toEpochDay()].orEmpty()
                                .associateBy { it.itemId }
                            dayDue.map { fractionOf(it, dayMap[it.id]) }.average().toFloat()
                        }
                    }
                }
            },
            completions = comps
        )
    }

    /** Conscious skip for today (rest day) — available for every item type. */
    fun skipToday(item: TrackItem) {
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            store.setCompletion(Completion(item.id, today, Completion.SKIPPED, 0))
        }
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

    /** Moves an item one step within its day-group; order persists. */
    fun move(item: TrackItem, direction: Int) {
        val siblings = state.value.allItems
            .filter { it.group == item.group && !it.archived }
            .sortedWith(compareBy({ it.sortOrder }, { it.timeMinutes ?: 9999 }, { it.createdAt }))
        val index = siblings.indexOfFirst { it.id == item.id }
        val target = index + direction
        if (index < 0 || target !in siblings.indices) return
        val reordered = siblings.toMutableList().apply { add(target, removeAt(index)) }
        val originalOrders = siblings.associate { it.id to it.sortOrder }
        val changed = reordered.mapIndexed { i, it -> it.copy(sortOrder = i * 10) }
            .filter { it.sortOrder != originalOrders[it.id] }
        if (changed.isNotEmpty()) {
            viewModelScope.launch { store.updateItems(changed) }
        }
    }

    fun addItem(item: TrackItem) {
        viewModelScope.launch { store.addItem(item) }
    }

    fun updateItem(item: TrackItem) {
        viewModelScope.launch { store.updateItem(item) }
    }

    fun setArchived(item: TrackItem, archived: Boolean) {
        viewModelScope.launch { store.updateItem(item.copy(archived = archived)) }
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
