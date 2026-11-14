package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.R
import com.nshd.dayloop.data.AlarmPrefs
import com.nshd.dayloop.data.RemindMode
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.MutedText
import com.nshd.dayloop.ui.theme.OffWhite
import com.nshd.dayloop.ui.theme.TileBorder
import com.nshd.dayloop.ui.theme.TileRaised

@Composable
fun SettingsScreen(
    prefs: AlarmPrefs,
    onSoundChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onDefaultModeChange: (RemindMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
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
                text = "DayLoop v0.5.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )

            Text(
                text = "REMINDERS",
                style = MaterialTheme.typography.labelLarge,
                color = MutedText,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                text = "Every timed item can be Off, Notify (a phone notification at its time), or Alarm (rings full-screen until you act). Medications can also snooze a dose by 30 minutes with Later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Text(
                text = "After a phone restart, reminders re-arm on boot — opening DayLoop once also re-arms everything. If notifications are off, allow them for DayLoop in Android Settings → Apps → DayLoop → Notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )

            Text(
                text = "ALARM",
                style = MaterialTheme.typography.labelLarge,
                color = MutedText,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                text = "Default for new timed items",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("Notify", prefs.defaultMode == RemindMode.NOTIFY) {
                    onDefaultModeChange(RemindMode.NOTIFY)
                }
                ModeChip("Alarm ⏰", prefs.defaultMode == RemindMode.ALARM) {
                    onDefaultModeChange(RemindMode.ALARM)
                }
                ModeChip("Off", prefs.defaultMode == RemindMode.OFF) {
                    onDefaultModeChange(RemindMode.OFF)
                }
            }
            SwitchRow("Alarm sound", prefs.sound, onSoundChange)
            SwitchRow("Vibration", prefs.vibrate, onVibrateChange)
            Text(
                text = "Alarms play at your phone's alarm volume — check it's turned up. For alarms to ring exactly on time on iQOO / Vivo phones: Android Settings → Battery → DayLoop → allow high background power use, and keep DayLoop out of battery optimisation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
        }

        DeveloperFooter()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AcidGreen,
                uncheckedThumbColor = OffWhite,
                uncheckedTrackColor = TileRaised
            )
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

/** Compact, centered developer block, pinned to the bottom of Settings. */
@Composable
private fun DeveloperFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "DEVELOPER",
            style = MaterialTheme.typography.labelLarge,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Built by N S H D",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocialIcon(
                iconRes = R.drawable.ic_social_instagram,
                label = "Instagram",
                url = "https://www.instagram.com/__nshd.__?stkn=emExd3hxZndzN21o"
            )
            SocialIcon(
                iconRes = R.drawable.ic_social_whatsapp,
                label = "WhatsApp",
                url = "https://wa.me/+918590455801"
            )
            SocialIcon(
                iconRes = R.drawable.ic_social_telegram,
                label = "Telegram",
                url = "https://t.me/nshd_0"
            )
            SocialIcon(
                iconRes = R.drawable.ic_social_email,
                label = "Email",
                url = "mailto:nishadahammed081@gmail.com"
            )
        }
    }
}

@Composable
private fun SocialIcon(iconRes: Int, label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(TileRaised)
            .border(1.dp, TileBorder, CircleShape)
            .clickable { uriHandler.openUri(url) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
    }
}
