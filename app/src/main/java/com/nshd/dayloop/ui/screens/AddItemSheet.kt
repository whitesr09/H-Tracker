package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.data.DayGroup
import com.nshd.dayloop.data.HabitKind
import com.nshd.dayloop.data.ItemType
import com.nshd.dayloop.data.RemindMode
import com.nshd.dayloop.data.TrackItem
import com.nshd.dayloop.ui.AppViewModel
import com.nshd.dayloop.ui.theme.AcidGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onAdd: (TrackItem) -> Unit,
    initial: TrackItem? = null,
    defaultMode: RemindMode = RemindMode.NOTIFY,
    onArchive: (TrackItem) -> Unit = {}
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: ItemType.HABIT) }
    var group by remember { mutableStateOf(initial?.group ?: DayGroup.MORNING) }
    var kind by remember { mutableStateOf(initial?.kind ?: HabitKind.YESNO) }
    var target by remember { mutableStateOf(initial?.targetCount?.toString() ?: "8") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var dose by remember { mutableStateOf(initial?.doseLabel ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var pills by remember { mutableStateOf(initial?.pillsLeft?.toString() ?: "") }
    var perDose by remember { mutableStateOf(initial?.pillsPerDose?.toString() ?: "1") }
    var mode by remember {
        mutableStateOf(
            initial?.let {
                when {
                    !it.remind -> RemindMode.OFF
                    it.alarmStyle -> RemindMode.ALARM
                    else -> RemindMode.NOTIFY
                }
            } ?: defaultMode
        )
    }
    var hasTime by remember { mutableStateOf(initial?.timeMinutes != null) }
    var hour by remember { mutableStateOf(initial?.timeMinutes?.let { (it / 60).toString() } ?: "7") }
    var minute by remember { mutableStateOf(initial?.timeMinutes?.let { (it % 60).toString() } ?: "30") }
    val steps = remember {
        androidx.compose.runtime.mutableStateListOf(
            *(initial?.routineSteps ?: emptyList()).toTypedArray()
        )
    }
    var stepName by remember { mutableStateOf("") }
    var stepMins by remember { mutableStateOf("5") }
    val selectedDays = remember {
        androidx.compose.runtime.mutableStateListOf(
            *(initial?.weekdays ?: (1..7).toSet()).sorted().toTypedArray()
        )
    }
    var weeklyTargetText by remember { mutableStateOf(initial?.weeklyTarget?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initial == null) "Add to your loop" else "Edit item",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Drink water, Vitamin D, Morning walk") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "TYPE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("Habit", type == ItemType.HABIT) { type = ItemType.HABIT }
                TypeChip("Medication", type == ItemType.MEDICATION) { type = ItemType.MEDICATION }
                TypeChip("Routine", type == ItemType.ROUTINE) { type = ItemType.ROUTINE }
                TypeChip("Plan", type == ItemType.PLAN) { type = ItemType.PLAN }
            }

            Text(text = "WHEN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("Morning", group == DayGroup.MORNING) { group = DayGroup.MORNING }
                TypeChip("Afternoon", group == DayGroup.AFTERNOON) { group = DayGroup.AFTERNOON }
                TypeChip("Evening", group == DayGroup.EVENING) { group = DayGroup.EVENING }
                TypeChip("Anytime", group == DayGroup.ANYTIME) { group = DayGroup.ANYTIME }
            }

            Text(text = "REPEAT DAYS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                (1..7).forEach { day ->
                    TypeChip(dayLabels[day - 1], selectedDays.contains(day)) {
                        if (selectedDays.contains(day)) {
                            if (selectedDays.size > 1) selectedDays.remove(day)
                        } else {
                            selectedDays.add(day)
                        }
                    }
                }
            }
            Text(
                text = if (selectedDays.size == 7) {
                    "Every day"
                } else {
                    selectedDays.sorted().joinToString(", ") {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[it - 1]
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Switch(
                    checked = hasTime,
                    onCheckedChange = { hasTime = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = AcidGreen,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Text(text = "Set a time", style = MaterialTheme.typography.bodyLarge)
                if (hasTime) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("HH") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(76.dp)
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("MM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(76.dp)
                    )
                }
            }

            if (type == ItemType.HABIT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip("Yes / No", kind == HabitKind.YESNO) { kind = HabitKind.YESNO }
                    TypeChip("Count", kind == HabitKind.COUNT) { kind = HabitKind.COUNT }
                }
                if (kind == HabitKind.COUNT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Target") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("glasses, pages, reps") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = weeklyTargetText,
                    onValueChange = { weeklyTargetText = it.filter { c -> c.isDigit() }.take(1) },
                    label = { Text("Weekly target (optional)") },
                    placeholder = { Text("e.g. 4 = four times a week; blank = daily") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (type == ItemType.ROUTINE) {
                Text(
                    text = "STEPS (FOR RUN MODE)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${step.name} · ${step.durationMinutes} min",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(onClick = { steps.removeAt(index) }) {
                            Text("Remove", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stepName,
                        onValueChange = { stepName = it },
                        label = { Text("Step") },
                        placeholder = { Text("e.g. Brush, Shower, Read") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stepMins,
                        onValueChange = { stepMins = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                }
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (stepName.isNotBlank()) {
                            steps.add(
                                com.nshd.dayloop.data.RoutineStep(
                                    stepName.trim(),
                                    stepMins.toIntOrNull()?.coerceIn(1, 999) ?: 1
                                )
                            )
                            stepName = ""
                        }
                    }
                ) { Text("+ Add step", color = AcidGreen) }
            }

            if (hasTime) {
                Text(
                    text = "REMINDER",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip("Off", mode == RemindMode.OFF) { mode = RemindMode.OFF }
                    TypeChip("Notify", mode == RemindMode.NOTIFY) { mode = RemindMode.NOTIFY }
                    TypeChip("Alarm ⏰", mode == RemindMode.ALARM) { mode = RemindMode.ALARM }
                }
                if (mode == RemindMode.ALARM) {
                    Text(
                        text = "Rings full-screen with sound until you act — Done, snooze 10 min, or dismiss.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (type == ItemType.MEDICATION) {
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Dose") },
                    placeholder = { Text("e.g. 1 tablet, 5 ml, 2 capsules") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pills,
                        onValueChange = { pills = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Pills left") },
                        placeholder = { Text("optional") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(130.dp)
                    )
                    OutlinedTextField(
                        value = perDose,
                        onValueChange = { perDose = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Per dose") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp)
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. after food, with warm water") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 2
            )

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val time = if (hasTime) {
                        val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                        val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        h * 60 + m
                    } else null
                    onAdd(
                        TrackItem(
                            id = initial?.id ?: AppViewModel.newId(),
                            type = type,
                            name = name.trim(),
                            group = group,
                            timeMinutes = time,
                            kind = kind,
                            targetCount = target.toIntOrNull()?.coerceIn(1, 999) ?: 1,
                            unit = unit.trim(),
                            doseLabel = dose.trim(),
                            pillsLeft = if (type == ItemType.MEDICATION) pills.toIntOrNull()?.coerceIn(0, 9999) else null,
                            pillsPerDose = perDose.toIntOrNull()?.coerceIn(1, 99) ?: 1,
                            remind = hasTime && mode != RemindMode.OFF,
                            alarmStyle = mode == RemindMode.ALARM,
                            routineSteps = if (type == ItemType.ROUTINE) steps.toList() else emptyList(),
                            weeklyTarget = if (type == ItemType.HABIT) {
                                weeklyTargetText.toIntOrNull()?.coerceIn(1, 7)
                            } else {
                                null
                            },
                            weekdays = selectedDays.toSet(),
                            sortOrder = initial?.sortOrder ?: 0,
                            note = note.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            archived = initial?.archived ?: false
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AcidGreen, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = if (initial == null) "Add item" else "Save changes",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (initial != null) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onArchive(initial)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        "Archive item (hide it, keep its history)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = AcidGreen,
            selectedLabelColor = Color.Black
        ),
        border = null
    )
}
