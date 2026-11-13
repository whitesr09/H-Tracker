package com.nshd.dayloop.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.data.Completion
import com.nshd.dayloop.data.DayGroup
import com.nshd.dayloop.data.HabitKind
import com.nshd.dayloop.data.ItemType
import com.nshd.dayloop.data.TrackItem
import com.nshd.dayloop.ui.AppViewModel
import com.nshd.dayloop.ui.TodayUiState
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.MutedText
import com.nshd.dayloop.ui.theme.TileBorder
import com.nshd.dayloop.ui.theme.TileDark
import com.nshd.dayloop.ui.theme.TileRaised
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TrackItem?>(null) }
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "DAYLOOP", style = MaterialTheme.typography.labelLarge, color = AcidGreen)
                    Text(text = "Today", style = MaterialTheme.typography.headlineLarge)
                    Text(text = todayLabel, style = MaterialTheme.typography.bodyMedium, color = MutedText)
                }
                IconButton(
                    onClick = { showSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AcidGreen)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add item", tint = Color.Black, modifier = Modifier.size(26.dp))
                }
            }
        }

        item {
            HeroRow(state)
        }

        if (state.lowStock.isNotEmpty()) {
            item {
                RefillTile(state.lowStock)
            }
        }

        if (state.dueItems.isEmpty()) {
            item {
                EmptyTile()
            }
        } else {
            DayGroup.entries.forEach { group ->
                val groupItems = state.dueItems.filter { it.group == group }
                if (groupItems.isNotEmpty()) {
                    item(key = "head-${group.name}") {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MutedText,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(groupItems, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            completion = state.todayCompletions[item.id],
                            onToggle = { viewModel.toggle(item) },
                            onCount = { delta -> viewModel.changeCount(item, delta) },
                            onSkip = { viewModel.markSkipped(item) },
                            onLater = { viewModel.markLater(item) },
                            onLongPress = { deleteTarget = item },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showSheet) {
        AddItemSheet(
            onDismiss = { showSheet = false },
            onAdd = { viewModel.addItem(it) }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = TileDark,
            title = { Text("Remove \"${target.name}\"?") },
            text = { Text("This removes the item and its history.", color = MutedText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(target)
                    deleteTarget = null
                }) { Text("Remove", color = AcidGreen) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep", color = MutedText) }
            }
        )
    }
}

@Composable
private fun HeroRow(state: TodayUiState) {
    val animated by animateFloatAsState(
        targetValue = state.fraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ring"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1.35f)
                .clip(RoundedCornerShape(24.dp))
                .background(TileDark)
                .border(1.dp, TileBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    drawArc(
                        color = TileRaised,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = AcidGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(animated * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = "Today's loop", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${state.doneCount} of ${state.dueItems.size} done",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AcidGreen
                )
                Text(
                    text = if (state.dueItems.isEmpty()) "Add your first item to close the loop." else "Keep going.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniTile(
                icon = Icons.Filled.Whatshot,
                label = "STREAK",
                value = "${state.streak} ${if (state.streak == 1) "day" else "days"}"
            )
            MiniTile(
                icon = Icons.Filled.Medication,
                label = "NEXT DOSE",
                value = state.nextDose?.let { dose ->
                    dose.timeMinutes?.let { "${dose.name} · ${formatTime(it)}" } ?: dose.name
                } ?: "None"
            )
        }
    }
}

@Composable
private fun RefillTile(lowStock: List<TrackItem>) {
    val first = lowStock.first()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TileDark)
            .border(1.dp, AcidGreen, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = "REFILL SOON", style = MaterialTheme.typography.labelLarge, color = AcidGreen)
        Text(
            text = "${first.name} · ${first.pillsLeft} left" +
                (if (lowStock.size > 1) " (+${lowStock.size - 1} more)" else ""),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "About ${first.daysOfPillsLeft()} days at the current dose.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
    }
}

@Composable
private fun MiniTile(icon: ImageVector, label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = AcidGreen, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MutedText)
        }
        Text(text = value, style = MaterialTheme.typography.titleMedium, maxLines = 2)
    }
}

@Composable
private fun EmptyTile() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "Nothing here yet", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Tap + to add a habit, a medication, a routine or a plan — then close your loop one tap at a time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: TrackItem,
    completion: Completion?,
    onToggle: () -> Unit,
    onCount: (Int) -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = completion?.status == Completion.DONE || completion?.status == Completion.TAKEN
    val skipped = completion?.status == Completion.SKIPPED
    val circleColor by animateColorAsState(
        targetValue = if (done) AcidGreen else Color.Transparent,
        animationSpec = tween(300),
        label = "circle"
    )
    val scale by animateFloatAsState(
        targetValue = if (done) 1f else 0.9f,
        animationSpec = tween(300),
        label = "checkScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(20.dp))
            .combinedClickable(onClick = { onToggle() }, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(circleColor)
                .border(2.dp, if (done) AcidGreen else TileBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(20.dp))
            } else if (skipped) {
                Icon(Icons.Filled.Close, contentDescription = "Skipped", tint = MutedText, modifier = Modifier.size(18.dp))
            } else {
                Icon(typeIcon(item.type), contentDescription = item.type.name, tint = MutedText, modifier = Modifier.size(18.dp))
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (skipped) MutedText else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subText(item, completion),
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) AcidGreen else MutedText
            )
        }

        if (item.type == ItemType.HABIT && item.kind == HabitKind.COUNT) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StepperButton("−") { onCount(-1) }
                Text(
                    text = "${completion?.value ?: 0}/${item.targetCount}",
                    style = MaterialTheme.typography.titleMedium
                )
                StepperButton("+") { onCount(1) }
            }
        }

        if (item.type == ItemType.MEDICATION && !done) {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = onLater,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (completion?.status == Completion.LATER) "Snoozed" else "Later",
                        color = AcidGreen
                    )
                }
                TextButton(
                    onClick = onSkip,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(if (skipped) "Skipped" else "Skip", color = MutedText)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(TileRaised)
            .border(1.dp, TileBorder, CircleShape)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = AcidGreen)
    }
}

private fun typeIcon(type: ItemType): ImageVector = when (type) {
    ItemType.HABIT -> Icons.Filled.Repeat
    ItemType.MEDICATION -> Icons.Filled.Medication
    ItemType.ROUTINE -> Icons.Filled.PlayCircle
    ItemType.PLAN -> Icons.Filled.EventNote
}

private fun subText(item: TrackItem, completion: Completion?): String {
    val parts = mutableListOf<String>()
    item.timeMinutes?.let { parts.add(formatTime(it)) }
    when (item.type) {
        ItemType.MEDICATION -> {
            if (item.doseLabel.isNotBlank()) parts.add(item.doseLabel)
            when (completion?.status) {
                Completion.TAKEN -> parts.add("Taken ${formatMillis(completion.atMillis)}")
                Completion.SKIPPED -> parts.add("Skipped")
                Completion.LATER -> parts.add("Later · reminder in 30 min")
            }
            item.pillsLeft?.let { parts.add("$it left") }
        }
        ItemType.HABIT -> {
            if (item.kind == HabitKind.COUNT) {
                val unit = if (item.unit.isNotBlank()) " ${item.unit}" else ""
                parts.add("${completion?.value ?: 0} of ${item.targetCount}$unit")
            } else if (completion?.status == Completion.DONE) {
                parts.add("Done ${formatMillis(completion.atMillis)}")
            }
        }
        ItemType.ROUTINE -> if (completion?.status == Completion.DONE) parts.add("Done") else parts.add("Tap to complete")
        ItemType.PLAN -> if (completion?.status == Completion.DONE) parts.add("Done") else parts.add("Tap when done")
    }
    return parts.joinToString(" · ").ifBlank { "Anytime" }
}

fun formatTime(minutes: Int): String {
    val h24 = minutes / 60
    val m = minutes % 60
    val suffix = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return "%d:%02d %s".format(h12, m, suffix)
}

private fun formatMillis(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("h:mm a"))
}
