package com.nshd.dayloop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "dayloop_store")

class DayLoopStore(private val context: Context) {

    private val itemsKey = stringPreferencesKey("items_json")
    private val completionsKey = stringPreferencesKey("completions_json")
    private val alarmSoundKey = booleanPreferencesKey("alarm_sound")
    private val alarmVibrateKey = booleanPreferencesKey("alarm_vibrate")
    private val alarmDefaultModeKey = stringPreferencesKey("alarm_default_mode")

    val alarmPrefs: Flow<AlarmPrefs> = context.dataStore.data.map { prefs ->
        AlarmPrefs(
            sound = prefs[alarmSoundKey] ?: true,
            vibrate = prefs[alarmVibrateKey] ?: true,
            defaultMode = runCatching {
                RemindMode.valueOf(prefs[alarmDefaultModeKey] ?: "NOTIFY")
            }.getOrDefault(RemindMode.NOTIFY)
        )
    }

    suspend fun saveAlarmPrefs(prefs: AlarmPrefs) {
        context.dataStore.edit { store ->
            store[alarmSoundKey] = prefs.sound
            store[alarmVibrateKey] = prefs.vibrate
            store[alarmDefaultModeKey] = prefs.defaultMode.name
        }
    }

    private val themeDarkKey = booleanPreferencesKey("theme_dark")

    val themeDark: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[themeDarkKey] ?: true
    }

    suspend fun saveThemeDark(dark: Boolean) {
        context.dataStore.edit { it[themeDarkKey] = dark }
    }

    /** Whole-data backup: items + completions as one JSON document. */
    suspend fun exportJson(): String {
        val prefs = context.dataStore.data.first()
        val root = JSONObject()
        root.put("app", "DayLoop")
        root.put("version", 1)
        root.put("items", JSONArray(prefs[itemsKey] ?: "[]"))
        root.put("completions", JSONArray(prefs[completionsKey] ?: "[]"))
        return root.toString(2)
    }

    /** Restores a backup made by [exportJson]; false when the file is invalid. */
    suspend fun importJson(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val items = root.getJSONArray("items")
            val comps = root.getJSONArray("completions")
            // Validate both payloads before touching stored data.
            parseItems(items.toString())
            parseCompletions(comps.toString())
            context.dataStore.edit { prefs ->
                prefs[itemsKey] = items.toString()
                prefs[completionsKey] = comps.toString()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    val items: Flow<List<TrackItem>> = context.dataStore.data.map { prefs ->
        parseItems(prefs[itemsKey] ?: "[]")
    }

    val completions: Flow<List<Completion>> = context.dataStore.data.map { prefs ->
        parseCompletions(prefs[completionsKey] ?: "[]")
    }

    suspend fun addItem(item: TrackItem) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[itemsKey] ?: "[]") + item
            prefs[itemsKey] = itemsToJson(list)
        }
    }

    suspend fun updateItem(item: TrackItem) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[itemsKey] ?: "[]")
                .map { if (it.id == item.id) item else it }
            prefs[itemsKey] = itemsToJson(list)
        }
    }

    suspend fun updateItems(updated: List<TrackItem>) {
        if (updated.isEmpty()) return
        context.dataStore.edit { prefs ->
            val byId = updated.associateBy { it.id }
            val list = parseItems(prefs[itemsKey] ?: "[]").map { byId[it.id] ?: it }
            prefs[itemsKey] = itemsToJson(list)
        }
    }

    suspend fun deleteItem(id: String) {
        context.dataStore.edit { prefs ->
            val list = parseItems(prefs[itemsKey] ?: "[]").filterNot { it.id == id }
            prefs[itemsKey] = itemsToJson(list)
            val comps = parseCompletions(prefs[completionsKey] ?: "[]").filterNot { it.itemId == id }
            prefs[completionsKey] = completionsToJson(comps)
        }
    }

    suspend fun setCompletion(completion: Completion) {
        context.dataStore.edit { prefs ->
            val list = parseCompletions(prefs[completionsKey] ?: "[]")
                .filterNot { it.itemId == completion.itemId && it.epochDay == completion.epochDay } + completion
            prefs[completionsKey] = completionsToJson(list)
        }
    }

    suspend fun removeCompletion(itemId: String, epochDay: Long) {
        context.dataStore.edit { prefs ->
            val list = parseCompletions(prefs[completionsKey] ?: "[]")
                .filterNot { it.itemId == itemId && it.epochDay == epochDay }
            prefs[completionsKey] = completionsToJson(list)
        }
    }

    private fun parseItems(json: String): List<TrackItem> {
        val arr = JSONArray(json)
        val out = mutableListOf<TrackItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                TrackItem(
                    id = o.getString("id"),
                    type = ItemType.valueOf(o.getString("type")),
                    name = o.getString("name"),
                    group = DayGroup.valueOf(o.getString("group")),
                    timeMinutes = if (o.isNull("time")) null else o.getInt("time"),
                    kind = HabitKind.valueOf(o.optString("kind", "YESNO")),
                    targetCount = o.optInt("target", 1),
                    unit = o.optString("unit", ""),
                    doseLabel = o.optString("dose", ""),
                    pillsLeft = if (!o.has("pills") || o.isNull("pills")) null else o.getInt("pills"),
                    pillsPerDose = o.optInt("perDose", 1).coerceAtLeast(1),
                    remind = o.optBoolean("remind", true),
                    alarmStyle = o.optBoolean("alarm", false),
                    weeklyTarget = if (!o.has("wt") || o.isNull("wt")) null else o.getInt("wt"),
                    sortOrder = o.optInt("ord", 0),
                    routineSteps = parseSteps(o.optJSONArray("steps")),
                    weekdays = o.optString("days", "1,2,3,4,5,6,7")
                        .split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet(),
                    createdAt = o.optLong("created", 0L),
                    archived = o.optBoolean("archived", false)
                )
            )
        }
        return out
    }

    private fun itemsToJson(list: List<TrackItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("type", item.type.name)
            o.put("name", item.name)
            o.put("group", item.group.name)
            if (item.timeMinutes == null) o.put("time", JSONObject.NULL) else o.put("time", item.timeMinutes)
            o.put("kind", item.kind.name)
            o.put("target", item.targetCount)
            o.put("unit", item.unit)
            o.put("dose", item.doseLabel)
            if (item.pillsLeft == null) o.put("pills", JSONObject.NULL) else o.put("pills", item.pillsLeft)
            o.put("perDose", item.pillsPerDose)
            o.put("remind", item.remind)
            o.put("alarm", item.alarmStyle)
            if (item.weeklyTarget == null) o.put("wt", JSONObject.NULL) else o.put("wt", item.weeklyTarget)
            o.put("ord", item.sortOrder)
            val stepsArr = JSONArray()
            item.routineSteps.forEach { step ->
                val so = JSONObject()
                so.put("n", step.name)
                so.put("m", step.durationMinutes)
                stepsArr.put(so)
            }
            o.put("steps", stepsArr)
            o.put("days", item.weekdays.sorted().joinToString(","))
            o.put("created", item.createdAt)
            o.put("archived", item.archived)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun parseSteps(arr: JSONArray?): List<RoutineStep> {
        if (arr == null) return emptyList()
        val out = mutableListOf<RoutineStep>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                RoutineStep(
                    name = o.optString("n", ""),
                    durationMinutes = o.optInt("m", 1).coerceAtLeast(1)
                )
            )
        }
        return out.filter { it.name.isNotBlank() }
    }

    private fun parseCompletions(json: String): List<Completion> {
        val arr = JSONArray(json)
        val out = mutableListOf<Completion>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Completion(
                    itemId = o.getString("item"),
                    epochDay = o.getLong("day"),
                    status = o.getString("status"),
                    value = o.optInt("value", 1),
                    atMillis = o.optLong("at", 0L)
                )
            )
        }
        return out
    }

    private fun completionsToJson(list: List<Completion>): String {
        val arr = JSONArray()
        list.forEach { c ->
            val o = JSONObject()
            o.put("item", c.itemId)
            o.put("day", c.epochDay)
            o.put("status", c.status)
            o.put("value", c.value)
            o.put("at", c.atMillis)
            arr.put(o)
        }
        return arr.toString()
    }
}
