package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.ui.AppViewModel

@Composable
fun ProgressScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(text = "DAYLOOP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = "Progress", style = MaterialTheme.typography.headlineLarge)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(Modifier.weight(1f), "CHECK-INS", "${state.totalCheckins}")
                StatTile(Modifier.weight(1f), "STREAK", "${state.streak} days")
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(Modifier.weight(1f), "THIS WEEK", "${state.weekCheckins}")
                StatTile(Modifier.weight(1f), "TODAY", "${state.doneCount}/${state.dueItems.size}")
            }
        }
        item {
            val review = state.weekReview
            val diff = review.thisWeekCheckins - review.lastWeekCheckins
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "WEEK REVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "This week: ${review.thisWeekPct}% complete · ${review.thisWeekCheckins} check-ins",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Best day: ${review.bestDayLabel} (${review.bestDayCount} check-ins)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Last week: ${review.lastWeekCheckins} check-ins" +
                        if (diff > 0) " · ▲ $diff vs last week" else if (diff < 0) " · ▼ ${-diff} vs last week" else " · same as last week",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                text = "BADGES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            val badges = listOf(
                Triple("🌱", "First steps", state.totalCheckins >= 1),
                Triple("⚡", "3-day streak", state.streak >= 3),
                Triple("🔥", "7-day streak", state.streak >= 7),
                Triple("✅", "50 check-ins", state.totalCheckins >= 50),
                Triple("💊", "Med master", (state.medAdherence30 ?: 0f) >= 0.9f),
                Triple("🏆", "Perfect week", state.weekReview.thisWeekPct >= 100)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (emoji, label, earned) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        if (earned) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (earned) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (earned) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = if (earned) "Earned ✓" else "Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (earned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        repeat(3 - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    Modifier.weight(1f),
                    "MED ADHERENCE · 30D",
                    state.medAdherence30?.let { "${(it * 100).toInt()}%" } ?: "—"
                )
                StatTile(Modifier.weight(1f), "REFILL SOON", "${state.lowStock.size}")
            }
        }

        val weeklyItems = state.allItems.filter { it.weeklyTarget != null && !it.archived }
        if (weeklyItems.isNotEmpty()) {
            item {
                Text(
                    text = "WEEKLY TARGETS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            weeklyItems.forEach { targetItem ->
                val target = targetItem.weeklyTarget ?: 1
                val done = state.weeklyProgress[targetItem.id] ?: 0
                item(key = "wt-${targetItem.id}") {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = targetItem.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (done >= target) "Goal met ✓ $done/$target" else "$done/$target this week",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (done.toFloat() / target).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(7.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        if (state.lowStock.isNotEmpty()) {
            item {
                Text(
                    text = "LOW STOCK",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            state.lowStock.forEach { med ->
                item(key = "low-${med.id}") {
                    Text(
                        text = "${med.name} · ${med.pillsLeft} left · about ${med.daysOfPillsLeft()} days",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        item {
            Text(
                text = "LAST 15 WEEKS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Heatmap(state.heatmap)
        }

        item {
            Text(
                text = state.monthLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            MonthGrid(state)
        }

        if (state.strengths.isNotEmpty()) {
            item {
                Text(
                    text = "HABIT STRENGTH · 28 DAYS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            state.strengths.forEach { strength ->
                item(key = "str-${strength.itemId}") {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = strength.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${(strength.percent * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { strength.percent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(7.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Strength forgives: a missed day lowers it a little, never to zero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 15 columns (weeks) × 7 rows (days); heatmap is oldest → today, column-major weeks. */
@Composable
private fun Heatmap(heatmap: List<Float>) {
    if (heatmap.size < 105) {
        Text(text = "Not enough history yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    // heatmap list is chronological; the last day (today) sits in the
    // final week. Pad the front so weeks align Monday-first is complex;
    // simple chronological week columns read fine.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (week in 0 until 15) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (day in 0 until 7) {
                    val value = heatmap[week * 7 + day]
                    val cellColor = when {
                        value < 0f -> MaterialTheme.colorScheme.surface
                        value <= 0f -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.75f * value)
                    }
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cellColor)
                    )
                }
            }
        }
    }
}

/** Current month, Mon-first. Cell shade = that day's completion fraction. */
@Composable
private fun MonthGrid(state: com.nshd.dayloop.ui.TodayUiState) {
    val todayDay = java.time.LocalDate.now().dayOfMonth
    val cells = ArrayList<Float?>(state.monthOffset + state.monthFractions.size)
    repeat(state.monthOffset) { cells.add(null) }
    state.monthFractions.forEach { cells.add(it) }
    while (cells.size % 7 != 0) cells.add(null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Indexed rows (blank = null cell).
        val indexed = cells.withIndex().toList()
        indexed.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { (idx, value) ->
                    val dayNumber = idx - state.monthOffset + 1
                    val isToday = value != null && dayNumber == todayDay
                    val bg = when {
                        value == null -> androidx.compose.ui.graphics.Color.Transparent
                        value == -2f -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        value < 0f -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f + 0.8f * value)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .then(
                                if (isToday) {
                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        if (value != null) {
                            Text(
                                text = "$dayNumber",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (value >= 0.99f) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Greener = more of that day completed. Future days are dimmed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}
