package com.nshd.dayloop.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.R
import com.nshd.dayloop.data.RemindMode
import com.nshd.dayloop.ui.AppViewModel
import com.nshd.dayloop.ui.theme.AcidGreen

@Composable
fun SettingsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val prefs by viewModel.alarmPrefs.collectAsState()
    val themeDark by viewModel.themeDark.collectAsState()
    val appState by viewModel.state.collectAsState()
    val context = LocalContext.current

    var dataStatus by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri != null && json != null) {
            dataStatus = try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                "Backup exported ✓"
            } catch (e: Exception) {
                "Export failed — try again"
            }
        }
    }
    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText().orEmpty()
                viewModel.importJson(text) { ok ->
                    dataStatus = if (ok) {
                        "Backup restored ✓"
                    } else {
                        "That file is not a DayLoop backup"
                    }
                }
            } catch (e: Exception) {
                dataStatus = "Import failed — try again"
            }
        }
    }

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
                text = "DayLoop v0.8.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("Dark 🌙", themeDark) { viewModel.setThemeDark(true) }
                ModeChip("Light ☀️", !themeDark) { viewModel.setThemeDark(false) }
            }

            Text(
                text = "REMINDERS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                text = "Every timed item can be Off, Notify (a phone notification at its time), or Alarm (rings full-screen until you act). Medications can also snooze a dose by 30 minutes with Later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "After a phone restart, reminders re-arm on boot — opening DayLoop once also re-arms everything. If notifications are off, allow them for DayLoop in Android Settings → Apps → DayLoop → Notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "ALARM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                text = "Default for new timed items",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("Notify", prefs.defaultMode == RemindMode.NOTIFY) {
                    viewModel.setDefaultRemindMode(RemindMode.NOTIFY)
                }
                ModeChip("Alarm ⏰", prefs.defaultMode == RemindMode.ALARM) {
                    viewModel.setDefaultRemindMode(RemindMode.ALARM)
                }
                ModeChip("Off", prefs.defaultMode == RemindMode.OFF) {
                    viewModel.setDefaultRemindMode(RemindMode.OFF)
                }
            }
            SwitchRow("Alarm sound", prefs.sound) { viewModel.setAlarmSound(it) }
            SwitchRow("Vibration", prefs.vibrate) { viewModel.setAlarmVibrate(it) }
            Text(
                text = "Alarms play at your phone's alarm volume — check it's turned up. For alarms to ring exactly on time on iQOO / Vivo phones: Android Settings → Battery → DayLoop → allow high background power use, and keep DayLoop out of battery optimisation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "DATA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                text = "Back up everything (items + history) to a JSON file, and restore it on any phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportJson { json ->
                            pendingExport = json
                            createDoc.launch("dayloop-backup.json")
                        }
                    }
                ) { Text("Export backup") }
                OutlinedButton(
                    onClick = { openDoc.launch(arrayOf("application/json", "text/plain")) }
                ) { Text("Import backup") }
            }
            dataStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AcidGreen
                )
            }

            val archivedItems = appState.allItems.filter { it.archived }
            if (archivedItems.isNotEmpty()) {
                Text(
                    text = "ARCHIVED",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
                archivedItems.forEach { archivedItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = archivedItem.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedButton(
                            onClick = { viewModel.setArchived(archivedItem, false) }
                        ) { Text("Restore") }
                    }
                }
            }
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
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
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
