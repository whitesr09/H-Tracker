package com.nshd.dayloop.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.dayloop.ui.theme.AcidGreen
import com.nshd.dayloop.ui.theme.MutedText
import com.nshd.dayloop.ui.theme.TileBorder
import com.nshd.dayloop.ui.theme.TileDark
import com.nshd.dayloop.ui.theme.TileRaised
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DAYLOOP",
            style = MaterialTheme.typography.labelLarge,
            color = AcidGreen
        )
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = today,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )

        Spacer(Modifier.height(4.dp))

        HeroTile()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Whatshot,
                title = "Streak",
                bigText = "0 days",
                subtitle = "Start your first loop today"
            )
            BentoTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Medication,
                title = "Next dose",
                bigText = "None",
                subtitle = "No medications scheduled"
            )
        }

        BentoTile(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.PlayCircle,
            title = "Routines",
            bigText = "No routines yet",
            subtitle = "Timed routines will appear here"
        )

        WeekTile()
    }
}

@Composable
private fun HeroTile() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(104.dp)) {
                drawArc(
                    color = TileRaised,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "0%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Today's loop",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "0 of 0 done",
                style = MaterialTheme.typography.bodyLarge,
                color = AcidGreen
            )
            Text(
                text = "No habits yet — add your first one and close the loop.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
        }
    }
}

@Composable
private fun BentoTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    bigText: String,
    subtitle: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AcidGreen,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MutedText
        )
        Text(
            text = bigText,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
    }
}

@Composable
private fun WeekTile() {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val todayIndex = LocalDate.now().dayOfWeek.value - 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TileDark)
            .border(1.dp, TileBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "YOUR WEEK",
            style = MaterialTheme.typography.labelLarge,
            color = MutedText
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (index == todayIndex) AcidGreen else TileRaised)
                    )
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (index == todayIndex) AcidGreen else MutedText
                    )
                }
            }
        }
    }
}
