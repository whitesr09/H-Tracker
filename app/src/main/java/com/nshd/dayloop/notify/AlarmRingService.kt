package com.nshd.dayloop.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nshd.dayloop.data.Completion
import com.nshd.dayloop.data.DayLoopStore
import com.nshd.dayloop.data.HabitKind
import com.nshd.dayloop.data.ItemType
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Rings an item's alarm: foreground service that loops the alarm tone,
 * vibrates, and posts the full-screen alarm notification until the user
 * acts (Done / Snooze 10 min / Dismiss) or 5 minutes pass.
 */
class AlarmRingService : Service() {

    companion object {
        const val ACTION_START = "com.nshd.dayloop.alarm.START"
        const val ACTION_DONE = "com.nshd.dayloop.alarm.DONE"
        const val ACTION_SNOOZE = "com.nshd.dayloop.alarm.SNOOZE"
        const val ACTION_DISMISS = "com.nshd.dayloop.alarm.DISMISS"

        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_NAME = "item_name"
        const val EXTRA_DETAIL = "item_detail"

        private const val NOTIF_ID = 4201
        private const val AUTO_STOP_MS = 5 * 60_000L

        fun startIntent(context: Context, itemId: String, name: String, detail: String): Intent =
            Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ITEM_ID, itemId)
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_DETAIL, detail)
            }

        fun actionIntent(context: Context, action: String, itemId: String): Intent =
            Intent(context, AlarmRingService::class.java).apply {
                this.action = action
                putExtra(EXTRA_ITEM_ID, itemId)
            }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var autoStopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        when (intent?.action) {
            ACTION_DONE -> handleDone(itemId)
            ACTION_SNOOZE -> handleSnooze(itemId)
            ACTION_DISMISS -> stopRinging()
            else -> {
                val name = intent?.getStringExtra(EXTRA_NAME) ?: "Task"
                val detail = intent?.getStringExtra(EXTRA_DETAIL).orEmpty()
                ReminderScheduler.ensureChannel(this)
                val notification = buildNotification(itemId, name, detail)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this, NOTIF_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIF_ID, notification)
                }
                scope.launch {
                    val prefs = DayLoopStore(applicationContext).alarmPrefs.first()
                    if (prefs.sound) startRingtone()
                    if (prefs.vibrate) startVibration()
                }
                autoStopJob?.cancel()
                autoStopJob = scope.launch {
                    delay(AUTO_STOP_MS)
                    stopRinging()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopSound()
        scope.cancel()
        super.onDestroy()
    }

    private fun handleDone(itemId: String) {
        stopSound()
        clearNotification()
        scope.launch {
            val store = DayLoopStore(applicationContext)
            val item = store.items.first().firstOrNull { it.id == itemId }
            if (item != null) {
                val today = LocalDate.now().toEpochDay()
                val status =
                    if (item.type == ItemType.MEDICATION) Completion.TAKEN else Completion.DONE
                val value = if (item.type == ItemType.HABIT && item.kind == HabitKind.COUNT) {
                    item.targetCount
                } else {
                    1
                }
                store.setCompletion(Completion(item.id, today, status, value))
                if (item.type == ItemType.MEDICATION) {
                    val left = item.pillsLeft
                    if (left != null) {
                        val per = item.pillsPerDose.coerceAtLeast(1)
                        val updated = (left - per).coerceAtLeast(0)
                        if (updated != left) store.updateItem(item.copy(pillsLeft = updated))
                    }
                }
            }
            stopSelf()
        }
    }

    private fun handleSnooze(itemId: String) {
        stopSound()
        clearNotification()
        scope.launch {
            val store = DayLoopStore(applicationContext)
            val item = store.items.first().firstOrNull { it.id == itemId }
            if (item != null) ReminderScheduler.snooze(applicationContext, item, delayMinutes = 10)
            stopSelf()
        }
    }

    private fun stopRinging() {
        stopSound()
        clearNotification()
        stopSelf()
    }

    private fun clearNotification() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // Not in foreground (action arrived without a ring); nothing to clear.
        }
        getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
    }

    private fun startRingtone() {
        if (ringtone != null) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    isLooping = true
                } catch (e: Exception) {
                    // Some OEM ringtones refuse looping; a single long tone still plays.
                }
            }
            play()
        }
    }

    private fun startVibration() {
        val vib = getSystemService(Vibrator::class.java) ?: return
        vibrator = vib
        if (vib.hasVibrator()) {
            val pattern = longArrayOf(0, 700, 400, 700, 400)
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun stopSound() {
        autoStopJob?.cancel()
        autoStopJob = null
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // Already stopped.
        }
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun buildNotification(itemId: String, name: String, detail: String) =
        NotificationCompat.Builder(this, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for $name")
            .setContentText(detail.ifBlank { "Tap to open" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(alarmActivityIntent(itemId, name, detail, 700))
            .setFullScreenIntent(alarmActivityIntent(itemId, name, detail, 701), true)
            .addAction(0, "Done", serviceActionIntent(ACTION_DONE, itemId, 702))
            .addAction(0, "Snooze 10 min", serviceActionIntent(ACTION_SNOOZE, itemId, 703))
            .addAction(0, "Dismiss", serviceActionIntent(ACTION_DISMISS, itemId, 704))
            .build()

    private fun alarmActivityIntent(
        itemId: String,
        name: String,
        detail: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_DETAIL, detail)
        }
        return PendingIntent.getActivity(
            this,
            itemId.hashCode() + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceActionIntent(action: String, itemId: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            itemId.hashCode() + requestCode,
            actionIntent(this, action, itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
