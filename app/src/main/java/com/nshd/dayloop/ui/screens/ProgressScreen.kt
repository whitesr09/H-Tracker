package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ProgressScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "DAYLOOP", style = MaterialTheme.typography.labelLarge, color = AcidGreen)
        Text(text = "Progress", style = MaterialTheme.typography.headlineLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(Modifier.weight(1f), "CHECK-INS", "${state.totalCheckins}")
            StatTile(Modifier.weight(1f), "STREAK", "${state.streak} days")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(Modifier.weight(1f), "ITEMS TRACKED", "${state.totalItems}")
            StatTile(Modifier.weight(1f), "TODAY", "${state.doneCount}/${state.dueItems.size}")
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Strength scores and the full heatmap arrive in the next level.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
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
