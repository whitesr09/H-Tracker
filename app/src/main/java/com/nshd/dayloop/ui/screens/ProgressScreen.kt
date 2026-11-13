package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.MutedText

@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "DAYLOOP",
            style = MaterialTheme.typography.labelLarge,
            color = AcidGreen
        )
        Text(
            text = "Progress",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Strength scores, streaks and your heatmap will live here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedText
        )
    }
}
