package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Strings
import com.example.model.BatteryMode
import com.example.model.PlaybackState
import com.example.model.Script
import com.example.service.ClickerEngine
import com.example.ui.components.HumanizeMeter
import com.example.ui.components.ServiceBanner
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    scripts: List<Script>,
    strings: Strings,
    isServiceActive: Boolean,
    onGrantService: () -> Unit,
    onNewScript: () -> Unit,
    onEditScript: (Script) -> Unit,
    onDeleteScript: (Script) -> Unit,
    onDuplicateScript: (Script) -> Unit,
    onRunScript: (Script) -> Unit,
    onOpenAi: () -> Unit,
    onOpenSandbox: () -> Unit,
    engine: ClickerEngine
) {
    val playbackState by engine.playbackState.collectAsState()
    val currentRunningScript by engine.currentScript.collectAsState()
    val totalClicks by engine.totalClicksExecuted.collectAsState()

    Scaffold(
        containerColor = NaturalBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewScript,
                containerColor = NaturalForestGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("create_script_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.newScript,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                ServiceBanner(
                    isServiceActive = isServiceActive,
                    strings = strings,
                    onGrantClick = onGrantService
                )
            }

            // Hero Task Status Banner (matches Natural Tones Design reference)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("hero_status_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalSageLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (playbackState == PlaybackState.RUNNING) Icons.Default.PlayArrow else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = NaturalForestGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (playbackState == PlaybackState.RUNNING) "TÂCHE EN COURS" else "MOTEUR D'AUTOMATISATION",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = NaturalForestGreen
                                    )
                                    Text(
                                        text = if (playbackState == PlaybackState.RUNNING) (currentRunningScript?.title ?: "En cours") else "Système Prêt & Protégé",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalTextPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                            ) {
                                Text(
                                    text = if (playbackState == PlaybackState.RUNNING) "Actif" else "Veille",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalForestGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress indicator bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Anti-Bot & Humanisation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextSecondary
                                )
                                Text(
                                    text = "98% (Actif)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalForestGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.98f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(NaturalForestGreen)
                                )
                            }
                        }
                    }
                }
            }

            // Overview Stats Grid
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = strings.activeAutomations,
                            value = "${scripts.size}",
                            icon = Icons.Default.AutoMode,
                            color = NaturalForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = strings.totalRuns,
                            value = "${scripts.sumOf { it.runCount } + totalClicks}",
                            icon = Icons.Default.TouchApp,
                            color = NaturalAmberWarm,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = strings.humanizationIndex,
                            value = "98% Anti-Bot",
                            icon = Icons.Default.Shield,
                            color = NaturalForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = strings.batteryEfficiency,
                            value = "Mode Éco (~2%)",
                            icon = Icons.Default.BatterySaver,
                            color = NaturalForestGreenLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions Hub
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("quick_actions_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.quickStart,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenAi,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_quick_prompt_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NaturalForestGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Générateur IA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = onOpenSandbox,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sandbox_quick_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = NaturalForestGreen
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                            ) {
                                Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testeur Live", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Currently Running Banner
            if (playbackState == PlaybackState.RUNNING || playbackState == PlaybackState.PAUSED) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("running_status_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (playbackState == PlaybackState.RUNNING) NaturalSageLight else NaturalOliveLight
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (playbackState == PlaybackState.RUNNING) NaturalSageBorder else NaturalAmberWarm
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (playbackState == PlaybackState.RUNNING) NaturalForestGreen else NaturalAmberWarm)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (playbackState == PlaybackState.RUNNING) "En cours : ${currentRunningScript?.title}" else "En pause",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalTextPrimary
                                    )
                                    Text(
                                        text = "$totalClicks actions exécutées • Protection anti-bot active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaturalTextSecondary
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        if (playbackState == PlaybackState.RUNNING) engine.pause() else engine.resume()
                                    },
                                    modifier = Modifier.testTag("pause_resume_button")
                                ) {
                                    Icon(
                                        imageVector = if (playbackState == PlaybackState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Pause/Resume",
                                        tint = NaturalForestGreen
                                    )
                                }
                                IconButton(
                                    onClick = { engine.stop() },
                                    modifier = Modifier.testTag("stop_running_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = NaturalRose
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Script List Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentScripts,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )
                    Text(
                        text = "${scripts.size} scripts",
                        style = MaterialTheme.typography.labelMedium,
                        color = NaturalTextMuted
                    )
                }
            }

            // Empty State
            if (scripts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("empty_scripts_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = NaturalTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.noScriptsYet,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.noScriptsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNewScript,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White)
                            ) {
                                Text(strings.newScript, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(scripts, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        strings = strings,
                        isRunning = currentRunningScript?.id == script.id && playbackState == PlaybackState.RUNNING,
                        onRun = { onRunScript(script) },
                        onEdit = { onEditScript(script) },
                        onDuplicate = { onDuplicateScript(script) },
                        onDelete = { onDeleteScript(script) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NaturalOliveLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = NaturalTextSecondary
            )
        }
    }
}

@Composable
fun ScriptCard(
    script: Script,
    strings: Strings,
    isRunning: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("script_card_${script.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRunning) NaturalForestGreen else NaturalCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NaturalOliveLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (script.steps.any { it.conditionText.isNotEmpty() }) Icons.Default.FindInPage else Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = NaturalForestGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = script.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalTextPrimary
                        )
                        Text(
                            text = "${script.steps.size} actions • ${script.runCount} exécutions",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalTextSecondary
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options", tint = NaturalTextSecondary)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(NaturalSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.editScript, color = NaturalTextPrimary) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NaturalForestGreen) }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.duplicateScript, color = NaturalTextPrimary) },
                            onClick = { showMenu = false; onDuplicate() },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NaturalAmberWarm) }
                        )
                        HorizontalDivider(color = NaturalCardBorder)
                        DropdownMenuItem(
                            text = { Text(strings.deleteScript, color = NaturalRose) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NaturalRose) }
                        )
                    }
                }
            }

            if (script.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = script.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalTextSecondary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags & Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NaturalSageLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                    ) {
                        Text(
                            text = "${script.humanizeConfig.antiBotScore}% Furtif",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalForestGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NaturalOlivePill,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                    ) {
                        Text(
                            text = script.batteryMode.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = NaturalTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = NaturalTextSecondary)
                    }

                    Button(
                        onClick = onRun,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) NaturalRose else NaturalForestGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("run_script_button_${script.id}")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRunning) "Stop" else strings.runNow,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
