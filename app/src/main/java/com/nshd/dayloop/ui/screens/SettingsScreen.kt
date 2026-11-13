package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.MutedText

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
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
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "DayLoop v0.1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )

        Text(
            text = "DEVELOPER",
            style = MaterialTheme.typography.labelLarge,
            color = MutedText,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "Built by N S H D",
            style = MaterialTheme.typography.titleMedium
        )

        DeveloperLink("Instagram", "https://www.instagram.com/__nshd.__?stkn=emExd3hxZndzN21o")
        DeveloperLink("WhatsApp", "https://wa.me/+918590455801")
        DeveloperLink("Telegram", "https://t.me/nshd_0")
        DeveloperLink("Email", "mailto:nishadahammed081@gmail.com")
    }
}

@Composable
private fun DeveloperLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = AcidGreen
        )
    }
}
