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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_DONE) {
            val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
            val pending = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val store = DayLoopStore(appContext)
                    val items = store.items.first()
                    val comps = store.completions.first()
                    val today = LocalDate.now()
                    val item = items.firstOrNull { it.id == itemId }
                    val existing = comps.firstOrNull {
                        it.itemId == itemId && it.epochDay == today.toEpochDay()
                    }
                    val alreadyDone = existing?.status == Completion.DONE ||
                        existing?.status == Completion.TAKEN
                    if (item != null && !alreadyDone) {
                        val status = if (item.type == ItemType.MEDICATION) {
                            Completion.TAKEN
                        } else {
                            Completion.DONE
                        }
                        val value = if (item.type == ItemType.HABIT &&
                            item.kind == HabitKind.COUNT
                        ) {
                            item.targetCount
                        } else {
                            1
                        }
                        store.setCompletion(Completion(item.id, today.toEpochDay(), status, value))
                        if (item.type == ItemType.MEDICATION) {
                            val left = item.pillsLeft
                            if (left != null) {
                                val updated = (left - item.pillsPerDose.coerceAtLeast(1))
                                    .coerceAtLeast(0)
                                if (updated != left) {
                                    store.updateItem(item.copy(pillsLeft = updated))
                                }
                            }
                        }
                    }
                    val manager = AppWidgetManager.getInstance(appContext)
                    val component = ComponentName(appContext, DayLoopWidget::class.java)
                    val ids = manager.getAppWidgetIds(component)
                    val freshItems = store.items.first()
                    val freshComps = store.completions.first()
                    ids.forEach { id ->
                        manager.updateAppWidget(id, buildViews(appContext, freshItems, freshComps))
                    }
                } catch (e: Exception) {
                    // Widget stays as-is; the app itself is unaffected.
                } finally {
                    pending.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

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
        val nextUp = due.filter { fractionOf(it, todayMap[it.id]) < 1f }
            .minByOrNull { it.timeMinutes ?: 9999 }
        views.setTextViewText(
            R.id.widget_nextitem,
            nextUp?.let { "Next: ${it.name}" } ?: if (due.isEmpty()) "" else "All done"
        )
        if (nextUp != null) {
            views.setViewVisibility(R.id.widget_done_btn, android.view.View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_done_btn,
                markDoneIntent(context, nextUp.id)
            )
        } else {
            views.setViewVisibility(R.id.widget_done_btn, android.view.View.GONE)
        }
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        return views
    }

    private fun fallbackViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_dayloop)
        views.setTextViewText(R.id.widget_percent, "—")
        views.setTextViewText(R.id.widget_counts, "Open DayLoop")
        views.setTextViewText(R.id.widget_next, "")
        views.setTextViewText(R.id.widget_nextitem, "")
        views.setViewVisibility(R.id.widget_done_btn, android.view.View.GONE)
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        return views
    }

    private fun markDoneIntent(context: Context, itemId: String): PendingIntent {
        val intent = Intent(context, DayLoopWidget::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getBroadcast(
            context,
            itemId.hashCode() + 5150,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        const val ACTION_MARK_DONE = "com.nshd.dayloop.widget.MARK_DONE"
        const val EXTRA_ITEM_ID = "item_id"

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
