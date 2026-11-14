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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val alarmPrefs by viewModel.alarmPrefs.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(false) }
    var runTarget by remember { mutableStateOf<TrackItem?>(null) }
    var editTarget by remember { mutableStateOf<TrackItem?>(null) }
    var deleteTarget by remember { mutableStateOf<TrackItem?>(null) }
    var optionsFor by remember { mutableStateOf<TrackItem?>(null) }
    var detailFor by remember { mutableStateOf<TrackItem?>(null) }
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
                    Text(text = "DAYLOOP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Today", style = MaterialTheme.typography.headlineLarge)
                    Text(text = todayLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = { sortMode = !sortMode },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.SwapVert,
                            contentDescription = "Reorder items",
                            tint = if (sortMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { showSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add item", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        if (sortMode) {
            item {
                Text(
                    text = "Reorder mode — move items with ↑ ↓ inside each section, then tap ⇅ again to finish.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(groupItems, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            completion = state.todayCompletions[item.id],
                            weeklyDone = item.weeklyTarget?.let { state.weeklyProgress[item.id] ?: 0 },
                            onToggle = { viewModel.toggle(item) },
                            onCount = { delta -> viewModel.changeCount(item, delta) },
                            onSkip = { viewModel.markSkipped(item) },
                            onLater = { viewModel.markLater(item) },
                            onRun = { runTarget = item },
                            onEdit = { editTarget = item },
                            sortMode = sortMode,
                            onMoveUp = { viewModel.move(item, -1) },
                            onMoveDown = { viewModel.move(item, 1) },
                            onLongPress = { optionsFor = item },
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
            onAdd = { viewModel.addItem(it) },
            defaultMode = alarmPrefs.defaultMode
        )
    }

    editTarget?.let { target ->
        AddItemSheet(
            onDismiss = { editTarget = null },
            onAdd = {
                viewModel.updateItem(it)
                editTarget = null
            },
            initial = target,
            onArchive = { viewModel.setArchived(it, true) }
        )
    }

    runTarget?.let { target ->
        RunModeScreen(
            item = target,
            onFinish = {
                viewModel.markDone(target)
                runTarget = null
            },
            onExit = { runTarget = null }
        )
    }

    optionsFor?.let { target ->
        val comp = state.todayCompletions[target.id]
        val isDone = comp?.status == Completion.DONE || comp?.status == Completion.TAKEN
        androidx.compose.ui.window.Dialog(onDismissRequest = { optionsFor = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = target.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Quick actions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { detailFor = target; optionsFor = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📊  History & stats", color = MaterialTheme.colorScheme.onSurface) }
                if (!isDone && comp?.status != Completion.SKIPPED) {
                    TextButton(
                        onClick = { viewModel.skipToday(target); optionsFor = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("⏭  Skip today (rest day)", color = MaterialTheme.colorScheme.onSurface) }
                }
                TextButton(
                    onClick = { editTarget = target; optionsFor = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✏️  Edit", color = MaterialTheme.colorScheme.onSurface) }
                TextButton(
                    onClick = { viewModel.setArchived(target, true); optionsFor = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗄  Archive", color = MaterialTheme.colorScheme.onSurface) }
                TextButton(
                    onClick = { deleteTarget = target; optionsFor = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑  Delete", color = MaterialTheme.colorScheme.onSurface) }
                TextButton(
                    onClick = { optionsFor = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    detailFor?.let { target ->
        ItemDetailDialog(
            item = target,
            completions = state.completions,
            onDismiss = { detailFor = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Remove \"${target.name}\"?") },
            text = { Text("This removes the item and its history.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(target)
                    deleteTarget = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1.35f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val ringTrack = MaterialTheme.colorScheme.surfaceVariant
            val ringProgress = MaterialTheme.colorScheme.primary
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    drawArc(
                        color = ringTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ringProgress,
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        state.dueItems.isEmpty() -> "Add your first item to close the loop."
                        state.fraction >= 1f -> "Day complete 🎉 Loop closed!"
                        state.fraction >= 0.5f -> "More than halfway — keep going."
                        else -> "Keep going."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.fraction >= 1f && state.dueItems.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniTile(
                icon = Icons.Filled.Whatshot,
                label = "STREAK",
                value = "${state.streak} ${if (state.streak == 1) "day" else "days"}",
                modifier = Modifier.weight(1f)
            )
            MiniTile(
                icon = Icons.Filled.Medication,
                label = "NEXT DOSE",
                value = state.nextDose?.let { dose ->
                    dose.timeMinutes?.let { "${dose.name} · ${formatTime(it)}" } ?: dose.name
                } ?: "None",
                modifier = Modifier.weight(1f)
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = "REFILL SOON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "${first.name} · ${first.pillsLeft} left" +
                (if (lowStock.size > 1) " (+${lowStock.size - 1} more)" else ""),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "About ${first.daysOfPillsLeft()} days at the current dose.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "Nothing here yet", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Tap + to add a habit, a medication, a routine or a plan — then close your loop one tap at a time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: TrackItem,
    completion: Completion?,
    weeklyDone: Int? = null,
    onToggle: () -> Unit,
    onCount: (Int) -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    sortMode: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val done = completion?.status == Completion.DONE || completion?.status == Completion.TAKEN
    val skipped = completion?.status == Completion.SKIPPED
    val circleColor by animateColorAsState(
        targetValue = if (done) MaterialTheme.colorScheme.primary else Color.Transparent,
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = { if (!sortMode) onToggle() },
                onLongClick = if (sortMode) null else onLongPress
            )
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
                .border(2.dp, if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            } else if (skipped) {
                Icon(Icons.Filled.Close, contentDescription = "Skipped", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            } else {
                Icon(typeIcon(item.type), contentDescription = item.type.name, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (skipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subText(item, completion, weeklyDone),
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sortMode) {
            Column {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Move ${item.name} up",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Move ${item.name} down",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
        if (item.type == ItemType.ROUTINE && !done) {
            IconButton(
                onClick = onRun,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = "Run ${item.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Edit ${item.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onLater,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (completion?.status == Completion.LATER) "Snoozed" else "Later",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onSkip,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(if (skipped) "Skipped" else "Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private fun typeIcon(type: ItemType): ImageVector = when (type) {
    ItemType.HABIT -> Icons.Filled.Repeat
    ItemType.MEDICATION -> Icons.Filled.Medication
    ItemType.ROUTINE -> Icons.Filled.PlayCircle
    ItemType.PLAN -> Icons.Filled.EventNote
}

@Composable
private fun ItemDetailDialog(
    item: TrackItem,
    completions: List<Completion>,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val mine = completions.filter { it.itemId == item.id }
    val byDay = mine.associateBy { it.epochDay }
    val totalDone = mine.count {
        it.status == Completion.DONE || it.status == Completion.TAKEN
    }
    var itemStreak = 0
    var offset = 0
    var started = false
    while (offset < 365) {
        val day = today.minusDays(offset.toLong())
        offset++
        if (!item.dueOn(day.dayOfWeek.value)) continue
        val c = byDay[day.toEpochDay()]
        val done = c?.status == Completion.DONE || c?.status == Completion.TAKEN ||
            (item.kind == HabitKind.COUNT && c != null && c.value >= item.targetCount)
        if (done) {
            itemStreak++
            started = true
            continue
        }
        if (c?.status == Completion.SKIPPED) continue
        if (day == today && !started) continue
        break
    }
    val lastDoneDay = mine
        .filter { it.status == Completion.DONE || it.status == Completion.TAKEN }
        .maxOfOrNull { it.epochDay }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = item.name, style = MaterialTheme.typography.titleLarge)
            if (item.note.isNotBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Current streak: $itemStreak days · Total check-ins: $totalDone",
                style = MaterialTheme.typography.bodyLarge
            )
            lastDoneDay?.let {
                Text(
                    text = "Last done: " + LocalDate.ofEpochDay(it)
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "LAST 5 WEEKS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            val days = (34 downTo 0).map { today.minusDays(it.toLong()) }
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        val due = item.dueOn(day.dayOfWeek.value)
                        val c = byDay[day.toEpochDay()]
                        val frac = when {
                            c == null -> 0f
                            c.status == Completion.DONE || c.status == Completion.TAKEN -> 1f
                            item.kind == HabitKind.COUNT ->
                                (c.value.toFloat() / item.targetCount.coerceAtLeast(1))
                                    .coerceIn(0f, 1f)
                            else -> 0f
                        }
                        val skippedDay = c?.status == Completion.SKIPPED
                        val bg = when {
                            skippedDay -> MaterialTheme.colorScheme.surfaceVariant
                            !due -> Color.Transparent
                            frac >= 1f -> MaterialTheme.colorScheme.primary
                            frac > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f + 0.7f * frac)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .then(
                                    if (!due) {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (skippedDay) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "Green = done · ✕ = skipped · grey = missed · hollow = not scheduled",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun subText(item: TrackItem, completion: Completion?, weeklyDone: Int? = null): String {
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
            item.weeklyTarget?.let { target ->
                val done = weeklyDone ?: 0
                parts.add(if (done >= target) "Weekly goal met ✓ $done/$target" else "$done/$target this week")
            }
        }
        ItemType.ROUTINE -> {
            if (completion?.status == Completion.DONE) parts.add("Done")
            if (item.routineSteps.isNotEmpty()) {
                parts.add("${item.routineSteps.size} steps · ${item.routineTotalMinutes()} min")
            } else if (completion?.status != Completion.DONE) {
                parts.add("Tap ▶ to run")
            }
        }
        ItemType.PLAN -> if (completion?.status == Completion.DONE) parts.add("Done") else parts.add("Tap when done")
    }
    if (item.note.isNotBlank()) parts.add(item.note)
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
