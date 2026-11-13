package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.MutedText
import com.nshd.dayloop.ui.theme.TileBorder
import com.nshd.dayloop.ui.theme.TileDark
import com.nshd.dayloop.ui.theme.TileRaised

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
            Text(text = "DAYLOOP", style = MaterialTheme.typography.labelLarge, color = AcidGreen)
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

        if (state.lowStock.isNotEmpty()) {
            item {
                Text(
                    text = "LOW STOCK",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedText,
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
                color = MutedText,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Heatmap(state.heatmap)
        }

        if (state.strengths.isNotEmpty()) {
            item {
                Text(
                    text = "HABIT STRENGTH · 28 DAYS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedText,
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
                                color = AcidGreen
                            )
                        }
                        LinearProgressIndicator(
                            progress = { strength.percent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(7.dp)),
                            color = AcidGreen,
                            trackColor = TileRaised
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Strength forgives: a missed day lowers it a little, never to zero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
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
        Text(text = "Not enough history yet.", style = MaterialTheme.typography.bodyMedium, color = MutedText)
        return
    }
    // heatmap list is chronological; the last day (today) sits in the
    // final week. Pad the front so weeks align Monday-first is complex;
    // simple chronological week columns read fine.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(20.dp))
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
                        value < 0f -> TileDark
                        value <= 0f -> TileRaised
                        else -> AcidGreen.copy(alpha = 0.25f + 0.75f * value)
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

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MutedText)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}
