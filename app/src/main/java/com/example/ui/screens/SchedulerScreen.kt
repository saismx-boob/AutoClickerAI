package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Strings
import com.example.model.Script
import com.example.ui.theme.*

@Composable
fun SchedulerScreen(
    scripts: List<Script>,
    strings: Strings,
    onToggleSchedule: (Script, Boolean) -> Unit,
    onUpdateScheduleTime: (Script, Int, List<Int>) -> Unit
) {
    var scriptToEditSchedule by remember { mutableStateOf<Script?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("scheduler_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Scheduler Info Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalSageLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = strings.schedulerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalTextPrimary
                        )
                        Text(
                            text = "Exécutez vos tâches aux heures voulues automatiquement sans réveiller l'écran.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalTextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Mes Planifications (${scripts.count { it.isScheduled }}/${scripts.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
        }

        if (scripts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null, tint = NaturalTextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Aucun script disponible", style = MaterialTheme.typography.bodyMedium, color = NaturalTextSecondary)
                    }
                }
            }
        } else {
            items(scripts, key = { it.id }) { script ->
                val hours = script.scheduleTimeMinutes / 60
                val minutes = script.scheduleTimeMinutes % 60
                val timeString = String.format("%02d:%02d", hours, minutes)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_card_${script.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (script.isScheduled) NaturalForestGreen else NaturalCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = script.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = NaturalAmberWarm, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Déclenchement : $timeString",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalAmberWarm
                                    )
                                }
                            }

                            Switch(
                                checked = script.isScheduled,
                                onCheckedChange = { onToggleSchedule(script, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NaturalForestGreen,
                                    checkedTrackColor = NaturalSageLight,
                                    uncheckedThumbColor = NaturalTextMuted,
                                    uncheckedTrackColor = NaturalOliveLight
                                ),
                                modifier = Modifier.testTag("toggle_schedule_${script.id}")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Days of week chips
                        val dayNames = listOf("L", "M", "M", "J", "V", "S", "D")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dayNames.forEachIndexed { idx, day ->
                                val dayNum = idx + 1
                                val isSelected = script.scheduleDays.contains(dayNum)

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newDays = if (isSelected) {
                                                script.scheduleDays.filter { it != dayNum }
                                            } else {
                                                script.scheduleDays + dayNum
                                            }
                                            onUpdateScheduleTime(script, script.scheduleTimeMinutes, newDays)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) NaturalSageLight else NaturalOliveLight,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NaturalSageBorder else NaturalOliveBorder
                                    )
                                ) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) NaturalForestGreen else NaturalTextMuted,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Time change button
                        OutlinedButton(
                            onClick = { scriptToEditSchedule = script },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = NaturalTextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Changer l'heure ($timeString)", color = NaturalTextPrimary)
                        }
                    }
                }
            }
        }
    }

    // Time picker simple modal
    scriptToEditSchedule?.let { script ->
        var hourInput by remember { mutableStateOf((script.scheduleTimeMinutes / 60).toString()) }
        var minInput by remember { mutableStateOf((script.scheduleTimeMinutes % 60).toString()) }

        AlertDialog(
            onDismissRequest = { scriptToEditSchedule = null },
            title = { Text("Définir l'heure d'activation", color = NaturalTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourInput,
                        onValueChange = { hourInput = it },
                        label = { Text("Heure (0-23)") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalForestGreen,
                            unfocusedBorderColor = NaturalOliveBorder
                        )
                    )
                    Text(" : ", style = MaterialTheme.typography.titleLarge, color = NaturalTextPrimary)
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { minInput = it },
                        label = { Text("Min (0-59)") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalForestGreen,
                            unfocusedBorderColor = NaturalOliveBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = hourInput.toIntOrNull()?.coerceIn(0, 23) ?: 8
                        val m = minInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        onUpdateScheduleTime(script, h * 60 + m, script.scheduleDays)
                        scriptToEditSchedule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToEditSchedule = null }) {
                    Text("Annuler", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }
}
