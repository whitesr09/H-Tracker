package com.nshd.dayloop.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nshd.dayloop.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(ReminderScheduler.EXTRA_NAME) ?: return
        val detail = intent.getStringExtra(ReminderScheduler.EXTRA_DETAIL).orEmpty()
        val itemId = intent.getStringExtra(ReminderScheduler.EXTRA_ITEM_ID).orEmpty()
        val snooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)
        val alarmStyle = intent.getBooleanExtra(ReminderScheduler.EXTRA_ALARM, false)

        ReminderScheduler.ensureChannel(context)

        // Alarm-style items ring full-screen via the ring service; if the
        // service cannot start, fall through to a normal notification.
        if (alarmStyle && !snooze) {
            try {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    AlarmRingService.startIntent(context, itemId, name, detail)
                )
                return
            } catch (e: Exception) {
                // Fall through to the notification below.
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (snooze) "Still time for $name" else "Time for $name"
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(detail.ifBlank { "Tap to open DayLoop" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            manager.notify(itemId.hashCode() and Int.MAX_VALUE, notification)
        } catch (e: SecurityException) {
            // Notifications not granted yet; the alarm still fired and the
            // item stays visible on the Today screen.
        }
    }
}
