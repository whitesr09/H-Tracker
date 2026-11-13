package com.nshd.dayloop.data

enum class ItemType { HABIT, MEDICATION, ROUTINE, PLAN }

enum class DayGroup { MORNING, AFTERNOON, EVENING, ANYTIME }

enum class HabitKind { YESNO, COUNT }

data class TrackItem(
    val id: String,
    val type: ItemType,
    val name: String,
    val group: DayGroup,
    val timeMinutes: Int? = null,
    val kind: HabitKind = HabitKind.YESNO,
    val targetCount: Int = 1,
    val unit: String = "",
    val doseLabel: String = "",
    val weekdays: Set<Int> = (1..7).toSet(),
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
) {
    fun dueOn(dayOfWeek: Int): Boolean = !archived && weekdays.contains(dayOfWeek)
}

data class Completion(
    val itemId: String,
    val epochDay: Long,
    val status: String,
    val value: Int = 1,
    val atMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val DONE = "DONE"
        const val TAKEN = "TAKEN"
        const val SKIPPED = "SKIPPED"
    }
}
