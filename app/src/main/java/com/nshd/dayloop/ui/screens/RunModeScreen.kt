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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nshd.dayloop.data.TrackItem
import kotlinx.coroutines.delay

/**
 * Full-screen routine runner: steps count down one by one.
 * Finishing (or tapping "Finish now") marks the routine done for today.
 */
@Composable
fun RunModeScreen(
    item: TrackItem,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val steps = item.routineSteps
    var stepIndex by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(stepIndex) {
        secondsLeft = if (stepIndex < steps.size) steps[stepIndex].durationMinutes * 60 else 0
    }

    LaunchedEffect(running, stepIndex) {
        while (running && stepIndex < steps.size) {
            if (secondsLeft <= 0) {
                if (stepIndex >= steps.size - 1) {
                    running = false
                    onFinish()
                    break
                } else {
                    stepIndex++
                }
            } else {
                delay(1000)
                secondsLeft--
            }
        }
    }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "RUN MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = item.name, style = MaterialTheme.typography.headlineMedium)
                }
                TextButton(onClick = onExit) { Text("Exit", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            if (steps.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "No steps yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Edit this routine and add timed steps — Run mode will count each one down here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mark done for today", style = MaterialTheme.typography.titleMedium) }
            } else {
                val step = steps[stepIndex]
                val totalSeconds = steps.sumOf { it.durationMinutes * 60 }
                val elapsedSeconds = steps.take(stepIndex).sumOf { it.durationMinutes * 60 } +
                    (step.durationMinutes * 60 - secondsLeft)
                val progress = if (totalSeconds == 0) 0f else
                    (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STEP ${stepIndex + 1} OF ${steps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = step.name, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = formatClock(secondsLeft),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }

                steps.forEachIndexed { index, s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${index + 1}. ${s.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = when {
                                index < stepIndex -> MaterialTheme.colorScheme.primary
                                index == stepIndex -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(text = "${s.durationMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (!started) {
                    Button(
                        onClick = { started = true; running = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start routine", style = MaterialTheme.typography.titleMedium) }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { running = !running },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.weight(1f)
                        ) { Text(if (running) "Pause" else "Resume") }
                        TextButton(
                            onClick = {
                                if (stepIndex >= steps.size - 1) {
                                    running = false
                                    onFinish()
                                } else {
                                    stepIndex++
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Next step", color = MaterialTheme.colorScheme.primary) }
                    }
                    TextButton(
                        onClick = { running = false; onFinish() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Finish now — mark done", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
