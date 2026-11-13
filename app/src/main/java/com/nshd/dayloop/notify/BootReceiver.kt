package com.nshd.dayloop.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nshd.dayloop.data.DayLoopStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Re-arms reminder alarms after the phone restarts. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = DayLoopStore(appContext)
                val items = store.items.first()
                val completions = store.completions.first()
                ReminderScheduler.scheduleAll(appContext, items, completions)
            } catch (e: Exception) {
                // Alarms also re-arm on the next app open.
            } finally {
                pending.finish()
            }
        }
    }
}
