package com.nshd.dayloop.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nshd.dayloop.data.Completion
import com.nshd.dayloop.data.TrackItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    const val CHANNEL_ID = "dayloop_reminders"

    const val EXTRA_ITEM_ID = "item_id"
    const val EXTRA_NAME = "item_name"
    const val EXTRA_DETAIL = "item_detail"
    const val EXTRA_SNOOZE = "is_snooze"
    const val EXTRA_ALARM = "is_alarm"

    private const val SNOOZE_OFFSET = 1_000_003

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Medication and timed item reminders"
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Arms one alarm per timed, reminder-enabled item: its next due
     * occurrence that is still in the future and not already completed.
     * Called whenever items or completions change and on every app open,
     * which also re-arms the following days over time.
     */
    fun scheduleAll(context: Context, items: List<TrackItem>, completions: List<Completion>) {
        ensureChannel(context)
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val todayDone = completions
            .filter { it.epochDay == todayEpoch && (it.status == Completion.DONE || it.status == Completion.TAKEN) }
            .map { it.itemId }
            .toSet()

        // Items edited to drop their time or reminder keep no stale alarm.
        items.filter { !it.remind || it.timeMinutes == null || it.archived }
            .forEach { cancel(context, it.id) }

        items.filter { it.remind && it.timeMinutes != null && !it.archived }
            .forEach { item ->
                val trigger = nextTriggerMillis(item, todayDone.contains(item.id))
                if (trigger != null) {
                    scheduleAt(context, requestCodeFor(item.id), trigger, item, snooze = false)
                } else {
                    cancel(context, item.id)
                }
            }
    }

    fun snooze(context: Context, item: TrackItem, delayMinutes: Long = 30) {
        ensureChannel(context)
        val trigger = System.currentTimeMillis() + delayMinutes * 60_000L
        scheduleAt(context, requestCodeFor(item.id) + SNOOZE_OFFSET, trigger, item, snooze = true)
    }

    fun cancel(context: Context, itemId: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        pendingIntent(context, requestCodeFor(itemId), null, snooze = false)
            ?.let { alarm.cancel(it) }
        pendingIntent(context, requestCodeFor(itemId) + SNOOZE_OFFSET, null, snooze = true)
            ?.let { alarm.cancel(it) }
    }

    private fun nextTriggerMillis(item: TrackItem, doneToday: Boolean): Long? {
        val time = item.timeMinutes ?: return null
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        for (offset in 0..7) {
            val date: LocalDate = today.plusDays(offset.toLong())
            if (!item.dueOn(date.dayOfWeek.value)) continue
            if (offset == 0 && doneToday) continue
            val candidate = LocalDateTime.of(date, LocalTime.of(time / 60, time % 60))
            if (candidate.isAfter(now)) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun scheduleAt(
        context: Context,
        requestCode: Int,
        triggerMillis: Long,
        item: TrackItem,
        snooze: Boolean
    ) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = pendingIntent(context, requestCode, item, snooze) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
        }
    }

    private fun pendingIntent(
        context: Context,
        requestCode: Int,
        item: TrackItem?,
        snooze: Boolean
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            if (item != null) {
                putExtra(EXTRA_ITEM_ID, item.id)
                putExtra(EXTRA_NAME, item.name)
                putExtra(
                    EXTRA_DETAIL,
                    when {
                        snooze -> "Snoozed reminder" + (if (item.doseLabel.isNotBlank()) " · ${item.doseLabel}" else "")
                        item.doseLabel.isNotBlank() -> item.doseLabel
                        else -> item.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    }
                )
                putExtra(EXTRA_SNOOZE, snooze)
                putExtra(EXTRA_ALARM, item.alarmStyle && !snooze)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeFor(itemId: String): Int = itemId.hashCode() and Int.MAX_VALUE
}
