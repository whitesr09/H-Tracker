package com.nshd.dayloop.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nshd.dayloop.MainActivity
import com.nshd.dayloop.R
import com.nshd.dayloop.data.Completion
import com.nshd.dayloop.data.DayLoopStore
import com.nshd.dayloop.data.HabitKind
import com.nshd.dayloop.data.ItemType
import com.nshd.dayloop.data.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Home-screen widget: today's loop at a glance — percent done,
 * count, streak-free simple summary and the next medication dose.
 */
class DayLoopWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = DayLoopStore(appContext)
                val items = store.items.first()
                val comps = store.completions.first()
                ids.forEach { id ->
                    manager.updateAppWidget(id, buildViews(appContext, items, comps))
                }
            } catch (e: Exception) {
                ids.forEach { id ->
                    manager.updateAppWidget(id, fallbackViews(appContext))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun fractionOf(item: TrackItem, comp: Completion?): Float {
        if (comp == null) return 0f
        return when {
            comp.status == Completion.SKIPPED || comp.status == Completion.LATER -> 0f
            item.type == ItemType.HABIT && item.kind == HabitKind.COUNT ->
                (comp.value.toFloat() / item.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
            comp.status == Completion.DONE || comp.status == Completion.TAKEN -> 1f
            else -> 0f
        }
    }

    private fun buildViews(
        context: Context,
        items: List<TrackItem>,
        comps: List<Completion>
    ): RemoteViews {
        val today = LocalDate.now()
        val due = items.filter { it.dueOn(today.dayOfWeek.value) }
        val todayMap = comps.filter { it.epochDay == today.toEpochDay() }.associateBy { it.itemId }
        val fractions = due.map { fractionOf(it, todayMap[it.id]) }
        val percent = if (fractions.isEmpty()) 0 else (fractions.average() * 100).toInt()
        val done = fractions.count { it >= 1f }
        val nextDose = due.filter { it.type == ItemType.MEDICATION }
            .filter { todayMap[it.id]?.status != Completion.TAKEN }
            .minByOrNull { it.timeMinutes ?: 9999 }

        val views = RemoteViews(context.packageName, R.layout.widget_dayloop)
        views.setTextViewText(R.id.widget_percent, "$percent%")
        views.setTextViewText(
            R.id.widget_counts,
            if (due.isEmpty()) "Nothing due today" else "$done of ${due.size} done today"
        )
        views.setTextViewText(
            R.id.widget_next,
            nextDose?.let { dose ->
                val time = dose.timeMinutes?.let { m ->
                    val h = m / 60
                    val suffix = if (h < 12) "AM" else "PM"
                    val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
                    "%d:%02d %s".format(h12, m % 60, suffix)
                }
                if (time != null) "Next dose: ${dose.name} · $time" else "Next dose: ${dose.name}"
            } ?: "No doses left today"
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        return views
    }

    private fun fallbackViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_dayloop)
        views.setTextViewText(R.id.widget_percent, "—")
        views.setTextViewText(R.id.widget_counts, "Open DayLoop")
        views.setTextViewText(R.id.widget_next, "")
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        return views
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            4201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /** Ask the system to refresh every placed widget (data changed). */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, DayLoopWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val intent = Intent(context, DayLoopWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
