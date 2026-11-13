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
import com.nshd.dayloop.data.TrackItem
import com.nshd.dayloop.ui.AppViewModel
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.OffWhite
import com.nshd.dayloop.ui.theme.TileDark
import com.nshd.dayloop.ui.theme.TileRaised

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(onDismiss: () -> Unit, onAdd: (TrackItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ItemType.HABIT) }
    var group by remember { mutableStateOf(DayGroup.MORNING) }
    var kind by remember { mutableStateOf(HabitKind.YESNO) }
    var target by remember { mutableStateOf("8") }
    var unit by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var pills by remember { mutableStateOf("") }
    var perDose by remember { mutableStateOf("1") }
    var remind by remember { mutableStateOf(true) }
    var hasTime by remember { mutableStateOf(false) }
    var hour by remember { mutableStateOf("7") }
    var minute by remember { mutableStateOf("30") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TileDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Add to your loop", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Drink water, Vitamin D, Morning walk") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "TYPE", style = MaterialTheme.typography.labelLarge, color = com.nshd.dayloop.ui.theme.MutedText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("Habit", type == ItemType.HABIT) { type = ItemType.HABIT }
                TypeChip("Medication", type == ItemType.MEDICATION) { type = ItemType.MEDICATION }
                TypeChip("Routine", type == ItemType.ROUTINE) { type = ItemType.ROUTINE }
                TypeChip("Plan", type == ItemType.PLAN) { type = ItemType.PLAN }
            }

            Text(text = "WHEN", style = MaterialTheme.typography.labelLarge, color = com.nshd.dayloop.ui.theme.MutedText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("Morning", group == DayGroup.MORNING) { group = DayGroup.MORNING }
                TypeChip("Afternoon", group == DayGroup.AFTERNOON) { group = DayGroup.AFTERNOON }
                TypeChip("Evening", group == DayGroup.EVENING) { group = DayGroup.EVENING }
                TypeChip("Anytime", group == DayGroup.ANYTIME) { group = DayGroup.ANYTIME }
            }

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
                        uncheckedThumbColor = OffWhite,
                        uncheckedTrackColor = TileRaised
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
            }

            if (hasTime) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Switch(
                        checked = remind,
                        onCheckedChange = { remind = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AcidGreen,
                            uncheckedThumbColor = OffWhite,
                            uncheckedTrackColor = TileRaised
                        )
                    )
                    Text(text = "Remind me at that time", style = MaterialTheme.typography.bodyLarge)
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
                            id = AppViewModel.newId(),
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
                            remind = hasTime && remind
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AcidGreen, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Text(text = "Add item", style = MaterialTheme.typography.titleMedium)
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
            containerColor = TileRaised,
            labelColor = OffWhite,
            selectedContainerColor = AcidGreen,
            selectedLabelColor = Color.Black
        ),
        border = null
    )
}
