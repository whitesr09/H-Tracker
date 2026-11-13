package com.nshd.dayloop.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nshd.dayloop.data.Completion
import com.nshd.dayloop.data.DayLoopStore
import com.nshd.dayloop.data.HabitKind
import com.nshd.dayloop.data.ItemType
import com.nshd.dayloop.data.TrackItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class TodayUiState(
    val dueItems: List<TrackItem> = emptyList(),
    val todayCompletions: Map<String, Completion> = emptyMap(),
    val fraction: Float = 0f,
    val doneCount: Int = 0,
    val streak: Int = 0,
    val nextDose: TrackItem? = null,
    val totalItems: Int = 0,
    val totalCheckins: Int = 0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = DayLoopStore(application)

    val state: StateFlow<TodayUiState> = combine(store.items, store.completions) { items, comps ->
        buildState(items, comps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    private fun fractionOf(item: TrackItem, comp: Completion?): Float {
        if (comp == null) return 0f
        return when {
            comp.status == Completion.SKIPPED -> 0f
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

        return TodayUiState(
            dueItems = due,
            todayCompletions = todayMap,
            fraction = overall,
            doneCount = doneCount,
            streak = streak,
            nextDose = nextDose,
            totalItems = items.count { !it.archived },
            totalCheckins = comps.count { it.status == Completion.DONE || it.status == Completion.TAKEN }
        )
    }

    fun toggle(item: TrackItem) {
        val today = LocalDate.now().toEpochDay()
        val current = state.value.todayCompletions[item.id]
        viewModelScope.launch {
            if (current != null && (current.status == Completion.DONE || current.status == Completion.TAKEN)) {
                store.removeCompletion(item.id, today)
            } else {
                val status = if (item.type == ItemType.MEDICATION) Completion.TAKEN else Completion.DONE
                val value = if (item.type == ItemType.HABIT && item.kind == HabitKind.COUNT) item.targetCount else 1
                store.setCompletion(Completion(item.id, today, status, value))
            }
        }
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

    fun addItem(item: TrackItem) {
        viewModelScope.launch { store.addItem(item) }
    }

    fun deleteItem(item: TrackItem) {
        viewModelScope.launch { store.deleteItem(item.id) }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
