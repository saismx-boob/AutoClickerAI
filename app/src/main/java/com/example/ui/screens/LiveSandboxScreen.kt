package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Strings
import com.example.model.*
import com.example.service.ClickerEngine
import com.example.service.TouchVisualEvent
import com.example.ui.theme.*

data class TargetPin(
    val id: Int,
    var x: Float,
    var y: Float,
    val type: ActionType = ActionType.TAP,
    var endX: Float = 0f,
    var endY: Float = 0f
)

enum class SandboxMode {
    SIMULATION_TEST,
    IMAGE_DETECTOR_LAB,
    VISUAL_RECORDING
}

enum class RecordInteractionMode {
    TAP,
    SWIPE,
    TEXT_INPUT
}

data class DetectableCanvasItem(
    val id: String,
    val preset: com.example.service.ImageTemplatePreset,
    var x: Float,
    var y: Float,
    val width: Float = 80f,
    val height: Float = 80f
)

@Composable
fun LiveSandboxScreen(
    scripts: List<Script>,
    strings: Strings,
    engine: ClickerEngine,
    onSaveRecordedScript: (Script) -> Unit = {}
) {
    val context = LocalContext.current
    val playbackState by engine.playbackState.collectAsState()
    val totalClicks by engine.totalClicksExecuted.collectAsState()
    val logs by engine.logs.collectAsState()

    var activeMode by remember { mutableStateOf(SandboxMode.SIMULATION_TEST) }
    var selectedScript by remember { mutableStateOf(scripts.firstOrNull()) }

    // Simulation Pins
    var pins by remember {
        mutableStateOf(
            listOf(
                TargetPin(1, 200f, 350f, ActionType.TAP),
                TargetPin(2, 500f, 500f, ActionType.OCR_TEXT_MATCH),
                TargetPin(3, 350f, 680f, ActionType.SWIPE, endX = 350f, endY = 250f)
            )
        )
    }

    // Recording State
    var isRecording by remember { mutableStateOf(false) }
    var recordingInteractionMode by remember { mutableStateOf(RecordInteractionMode.TAP) }
    var recordedActions by remember { mutableStateOf<List<RecordedAction>>(emptyList()) }
    var lastActionTimestamp by remember { mutableStateOf(0L) }
    var swipeStartOffset by remember { mutableStateOf<Offset?>(null) }
    var currentDragOffset by remember { mutableStateOf<Offset?>(null) }

    // Dialog for Text Input during recording
    var showTextInputDialog by remember { mutableStateOf(false) }
    var pendingTextTargetOffset by remember { mutableStateOf<Offset?>(null) }
    var inputFieldText by remember { mutableStateOf("Texte automatique") }

    // Dialog for Saving Recorded Script
    var showSaveScriptDialog by remember { mutableStateOf(false) }
    var recordedScriptTitle by remember { mutableStateOf("Script Enregistré") }
    var recordedScriptDesc by remember { mutableStateOf("Séquence capturée avec l'enregistreur visuel interactif") }
    var defaultStepDelayMs by remember { mutableStateOf(800L) }

    // Image Detection Lab State
    var selectedCvTemplate by remember { mutableStateOf(com.example.service.ImageTemplatePreset.CHEST_REWARD) }
    var cvConfidenceThreshold by remember { mutableStateOf(0.80f) }
    var isScanningCv by remember { mutableStateOf(false) }
    var cvMatchResult by remember { mutableStateOf<com.example.service.ImageMatchResult?>(null) }
    var scanAnimationProgress by remember { mutableStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "cvScanRadar")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserAnimation"
    )

    var canvasItems by remember {
        mutableStateOf(
            listOf(
                DetectableCanvasItem("1", com.example.service.ImageTemplatePreset.CHEST_REWARD, x = 180f, y = 280f),
                DetectableCanvasItem("2", com.example.service.ImageTemplatePreset.CLOSE_CROSS, x = 620f, y = 80f),
                DetectableCanvasItem("3", com.example.service.ImageTemplatePreset.PLAY_START, x = 320f, y = 480f),
                DetectableCanvasItem("4", com.example.service.ImageTemplatePreset.STAR_BONUS, x = 120f, y = 140f),
                DetectableCanvasItem("5", com.example.service.ImageTemplatePreset.GOLD_COIN, x = 520f, y = 380f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("sandbox_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector (Sandbox Test vs Image CV vs Visual Recording)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NaturalCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { activeMode = SandboxMode.SIMULATION_TEST },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeMode == SandboxMode.SIMULATION_TEST) NaturalForestGreen else Color.Transparent,
                        contentColor = if (activeMode == SandboxMode.SIMULATION_TEST) Color.White else NaturalTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { activeMode = SandboxMode.IMAGE_DETECTOR_LAB },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeMode == SandboxMode.IMAGE_DETECTOR_LAB) NaturalForestGreen else Color.Transparent,
                        contentColor = if (activeMode == SandboxMode.IMAGE_DETECTOR_LAB) Color.White else NaturalTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vision CV (Images)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { activeMode = SandboxMode.VISUAL_RECORDING },
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeMode == SandboxMode.VISUAL_RECORDING) NaturalAmberWarm else Color.Transparent,
                        contentColor = if (activeMode == SandboxMode.VISUAL_RECORDING) Color.White else NaturalTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enregistreur", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Top Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (activeMode == SandboxMode.SIMULATION_TEST) {
                    // Sandbox Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.sandboxTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalTextPrimary
                            )
                            Text(
                                text = "${pins.size} points cibles • $totalClicks clics simulés",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalTextSecondary
                            )
                        }

                        // Play/Pause/Stop/Pin Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (playbackState == PlaybackState.RUNNING) {
                                        engine.pause()
                                    } else {
                                        val scriptToRun = selectedScript ?: Script(
                                            title = "Test Live Sandbox",
                                            steps = pins.mapIndexed { idx, pin ->
                                                ActionStep(
                                                    stepIndex = idx + 1,
                                                    actionType = pin.type,
                                                    x = pin.x * 2.5f,
                                                    y = pin.y * 2.5f,
                                                    endX = pin.endX * 2.5f,
                                                    endY = pin.endY * 2.5f,
                                                    delayAfterMs = 800L,
                                                    conditionText = if (pin.type == ActionType.OCR_TEXT_MATCH) "Réclamer" else ""
                                                )
                                            }
                                        )
                                        engine.startScript(scriptToRun)
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (playbackState == PlaybackState.RUNNING) NaturalAmberWarm else NaturalForestGreen)
                                    .testTag("sandbox_play_button")
                            ) {
                                Icon(
                                    imageVector = if (playbackState == PlaybackState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { engine.stop() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NaturalOliveLight)
                                    .testTag("sandbox_stop_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = NaturalRose)
                            }

                            IconButton(
                                onClick = {
                                    val nextId = pins.size + 1
                                    pins = pins + TargetPin(nextId, 250f + (nextId * 30f) % 300f, 400f + (nextId * 40f) % 300f)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NaturalOliveLight)
                                    .testTag("sandbox_add_pin_button")
                            ) {
                                Icon(Icons.Default.AddLocationAlt, contentDescription = "Ajouter Pin", tint = NaturalForestGreen)
                            }

                            IconButton(
                                onClick = { pins = emptyList() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NaturalOliveLight)
                            ) {
                                Icon(Icons.Default.LayersClear, contentDescription = "Clear", tint = NaturalTextMuted)
                            }
                        }
                    }
                } else if (activeMode == SandboxMode.IMAGE_DETECTOR_LAB) {
                    // Image Computer Vision Lab Header
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NaturalForestGreen))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Laboratoire Détection d'Images",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalTextPrimary
                                    )
                                }
                                Text(
                                    text = "Computer Vision & Reconnaissance de motifs visuels en direct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        isScanningCv = true
                                        val targetItem = canvasItems.firstOrNull { it.preset == selectedCvTemplate }
                                        val conf = if (targetItem != null) (0.92f + (Math.random().toFloat() * 0.07f)).coerceAtMost(0.99f) else (0.45f + Math.random().toFloat() * 0.2f)
                                        val match = com.example.service.ImageMatchResult(
                                            found = targetItem != null && conf >= cvConfidenceThreshold,
                                            confidence = conf,
                                            centerX = targetItem?.x ?: (selectedCvTemplate.defaultX * 0.4f),
                                            centerY = targetItem?.y ?: (selectedCvTemplate.defaultY * 0.4f),
                                            bounds = if (targetItem != null) {
                                                android.graphics.RectF(
                                                    targetItem.x - 35f,
                                                    targetItem.y - 35f,
                                                    targetItem.x + 35f,
                                                    targetItem.y + 35f
                                                )
                                            } else android.graphics.RectF(),
                                            templateName = selectedCvTemplate.label
                                        )
                                        cvMatchResult = match
                                        if (match.found) {
                                            engine.addLog("CV: Motif '${selectedCvTemplate.label}' détecté à (${match.centerX.toInt()}, ${match.centerY.toInt()}) [Confiance: ${(match.confidence * 100).toInt()}%]", "SUCCESS")
                                        } else {
                                            engine.addLog("CV: Motif '${selectedCvTemplate.label}' introuvable ou sous le seuil (${(conf * 100).toInt()}% < ${(cvConfidenceThreshold * 100).toInt()}%)", "WARNING")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scanner", fontWeight = FontWeight.Bold)
                                }

                                cvMatchResult?.let { match ->
                                    if (match.found) {
                                        Button(
                                            onClick = {
                                                engine.emitTouchEvent(match.centerX, match.centerY, label = "Vision CV")
                                                engine.addLog("Clic automatique Computer Vision déclenché sur '${selectedCvTemplate.label}' à (${match.centerX.toInt()}, ${match.centerY.toInt()})", "TRIGGER")
                                                Toast.makeText(context, "Clic simulé sur l'image détectée !", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NaturalAmberWarm, contentColor = Color.White),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cliquer", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Template Selector Horizontal Scroll
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(com.example.service.ImageTemplatePreset.entries) { preset ->
                                val isSelected = selectedCvTemplate == preset
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) NaturalForestGreen else NaturalOliveLight,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NaturalForestGreen else NaturalOliveBorder
                                    ),
                                    modifier = Modifier.clickable {
                                        selectedCvTemplate = preset
                                        cvMatchResult = null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = preset.emoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = preset.label.split("/").first().trim(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) Color.White else NaturalTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Confidence Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seuil détection : ${(cvConfidenceThreshold * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NaturalTextPrimary
                            )
                            Slider(
                                value = cvConfidenceThreshold,
                                onValueChange = { cvConfidenceThreshold = it },
                                valueRange = 0.50f..0.98f,
                                modifier = Modifier.fillMaxWidth(0.75f),
                                colors = SliderDefaults.colors(
                                    thumbColor = NaturalForestGreen,
                                    activeTrackColor = NaturalForestGreen
                                )
                            )
                        }
                    }
                } else {
                    // Visual Recorder Header
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isRecording) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NaturalRose))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (isRecording) "Enregistrement en cours..." else "Enregistreur Prêt",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isRecording) NaturalRose else NaturalTextPrimary
                                    )
                                }
                                Text(
                                    text = "${recordedActions.size} actions capturées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isRecording) {
                                    Button(
                                        onClick = {
                                            isRecording = true
                                            recordedActions = emptyList()
                                            lastActionTimestamp = System.currentTimeMillis()
                                            Toast.makeText(context, "Enregistrement démarré ! Touchez l'écran ci-dessous.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NaturalRose, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Enregistrer", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isRecording = false
                                            if (recordedActions.isNotEmpty()) {
                                                recordedScriptTitle = "Script Enregistré ${recordedActions.size} Actions"
                                                showSaveScriptDialog = true
                                            } else {
                                                Toast.makeText(context, "Aucune action enregistrée", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Terminer", fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = {
                                            isRecording = false
                                            recordedActions = emptyList()
                                        },
                                        modifier = Modifier.clip(CircleShape).background(NaturalOliveLight)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Annuler", tint = NaturalRose)
                                    }
                                }
                            }
                        }

                        // Interaction Tool Selector (Tap / Swipe / Text Input)
                        if (isRecording) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = recordingInteractionMode == RecordInteractionMode.TAP,
                                    onClick = { recordingInteractionMode = RecordInteractionMode.TAP },
                                    label = { Text("👆 Clic / Tap") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NaturalForestGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = recordingInteractionMode == RecordInteractionMode.SWIPE,
                                    onClick = { recordingInteractionMode = RecordInteractionMode.SWIPE },
                                    label = { Text("↔ Glissement") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NaturalAmberWarm,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = recordingInteractionMode == RecordInteractionMode.TEXT_INPUT,
                                    onClick = { recordingInteractionMode = RecordInteractionMode.TEXT_INPUT },
                                    label = { Text("⌨ Saisie Texte") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NaturalForestGreenLight,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Preview Screen Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("sandbox_canvas_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isRecording) NaturalRose.copy(alpha = 0.5f) else NaturalSageBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isRecording, recordingInteractionMode) {
                        if (isRecording) {
                            when (recordingInteractionMode) {
                                RecordInteractionMode.TAP -> {
                                    detectTapGestures { offset ->
                                        val now = System.currentTimeMillis()
                                        val delay = (now - lastActionTimestamp).coerceIn(300L, 5000L)
                                        lastActionTimestamp = now

                                        val action = RecordedAction(
                                            actionType = ActionType.TAP,
                                            x = offset.x,
                                            y = offset.y,
                                            delayMs = delay,
                                            pressDurationMs = 85L,
                                            label = "Clic #${recordedActions.size + 1} (${offset.x.toInt()}, ${offset.y.toInt()})"
                                        )
                                        recordedActions = recordedActions + action
                                        engine.addLog("Action enregistrée : Clic à (${offset.x.toInt()}, ${offset.y.toInt()})", "SUCCESS")
                                    }
                                }

                                RecordInteractionMode.TEXT_INPUT -> {
                                    detectTapGestures { offset ->
                                        pendingTextTargetOffset = offset
                                        showTextInputDialog = true
                                    }
                                }

                                RecordInteractionMode.SWIPE -> {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            swipeStartOffset = offset
                                            currentDragOffset = offset
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentDragOffset = (currentDragOffset ?: Offset.Zero) + dragAmount
                                        },
                                        onDragEnd = {
                                            val start = swipeStartOffset
                                            val end = currentDragOffset
                                            if (start != null && end != null) {
                                                val now = System.currentTimeMillis()
                                                val delay = (now - lastActionTimestamp).coerceIn(400L, 5000L)
                                                lastActionTimestamp = now

                                                val action = RecordedAction(
                                                    actionType = ActionType.SWIPE,
                                                    x = start.x,
                                                    y = start.y,
                                                    endX = end.x,
                                                    endY = end.y,
                                                    delayMs = delay,
                                                    pressDurationMs = 320L,
                                                    label = "Swipe #${recordedActions.size + 1} -> (${end.x.toInt()}, ${end.y.toInt()})"
                                                )
                                                recordedActions = recordedActions + action
                                                engine.addLog("Action enregistrée : Glissement de (${start.x.toInt()}, ${start.y.toInt()}) vers (${end.x.toInt()}, ${end.y.toInt()})", "TRIGGER")
                                            }
                                            swipeStartOffset = null
                                            currentDragOffset = null
                                        }
                                    )
                                }
                            }
                        } else if (activeMode == SandboxMode.IMAGE_DETECTOR_LAB) {
                            detectTapGestures { offset ->
                                // Reposition or select the chosen template item at touched location
                                val existing = canvasItems.firstOrNull { it.preset == selectedCvTemplate }
                                canvasItems = if (existing != null) {
                                    canvasItems.map { if (it.id == existing.id) it.copy(x = offset.x, y = offset.y) else it }
                                } else {
                                    canvasItems + DetectableCanvasItem(
                                        id = (canvasItems.size + 1).toString(),
                                        preset = selectedCvTemplate,
                                        x = offset.x,
                                        y = offset.y
                                    )
                                }
                                cvMatchResult = null
                                engine.addLog("Motif '${selectedCvTemplate.label}' placé à (${offset.x.toInt()}, ${offset.y.toInt()})", "TRIGGER")
                            }
                        } else {
                            detectTapGestures { offset ->
                                val nextId = pins.size + 1
                                pins = pins + TargetPin(nextId, offset.x, offset.y)
                            }
                        }
                    }
            ) {
                // Mock app interface elements / Image Lab Canvas
                if (activeMode == SandboxMode.IMAGE_DETECTOR_LAB) {
                    // Render interactive image targets
                    Box(modifier = Modifier.fillMaxSize()) {
                        canvasItems.forEach { item ->
                            val isSelectedTarget = item.preset == selectedCvTemplate
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelectedTarget) NaturalForestGreen.copy(alpha = 0.12f) else NaturalCardSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelectedTarget) 2.dp else 1.dp,
                                    if (isSelectedTarget) NaturalForestGreen else NaturalOliveBorder
                                ),
                                modifier = Modifier
                                    .offset(x = (item.x - 40).dp, y = (item.y - 40).dp)
                                    .size(80.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = item.preset.emoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.preset.label.split("/").first().trim(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = if (isSelectedTarget) NaturalForestGreen else NaturalTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NaturalOliveLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                            ) {
                                Text(
                                    text = "📱 Écran Cible Enregistreur",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isRecording) NaturalRose.copy(alpha = 0.15f) else NaturalForestGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) NaturalRose else NaturalForestGreen)
                            ) {
                                Text(
                                    text = if (isRecording) "● REC" else "PAUSE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isRecording) NaturalRose else NaturalForestGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Center simulated OCR / Text Field target
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Simulated Input Field
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = NaturalCardSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = NaturalTextMuted)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (recordedActions.any { it.actionType == ActionType.TEXT_INPUT }) "Texte saisi : ${recordedActions.lastOrNull { it.actionType == ActionType.TEXT_INPUT }?.textValue}" else "Zone de saisie de texte...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (recordedActions.any { it.actionType == ActionType.TEXT_INPUT }) NaturalForestGreen else NaturalTextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NaturalAmberWarm)
                                    .border(1.5.dp, NaturalAmberWarmLight, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🎁 Réclamer Récompense",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Bottom simulation bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("🏠 Accueil", style = MaterialTheme.typography.labelSmall, color = NaturalTextMuted)
                            Text("⚔ Combat", style = MaterialTheme.typography.labelSmall, color = NaturalAmberWarm)
                            Text("⏩ Suivant", style = MaterialTheme.typography.labelSmall, color = NaturalForestGreen)
                        }
                    }
                }

                // Canvas drawing pins, recorded actions, and drag swipe paths
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (activeMode == SandboxMode.IMAGE_DETECTOR_LAB) {
                        // Draw animated laser radar beam across canvas
                        val beamY = laserY * size.height
                        drawLine(
                            color = NaturalForestGreen.copy(alpha = 0.5f),
                            start = Offset(0f, beamY),
                            end = Offset(size.width, beamY),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Draw CV Match Bounding Box & Target Reticle
                        cvMatchResult?.let { match ->
                            if (match.found) {
                                val box = match.bounds
                                val rectTopLeft = Offset(box.left, box.top)
                                val rectSize = androidx.compose.ui.geometry.Size(box.width(), box.height())

                                // Bounding box stroke
                                drawRect(
                                    color = NaturalForestGreen,
                                    topLeft = rectTopLeft,
                                    size = rectSize,
                                    style = Stroke(width = 3.dp.toPx())
                                )

                                // Center crosshair
                                val cx = match.centerX
                                val cy = match.centerY
                                drawLine(
                                    color = NaturalForestGreen,
                                    start = Offset(cx - 16f, cy),
                                    end = Offset(cx + 16f, cy),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                                drawLine(
                                    color = NaturalForestGreen,
                                    start = Offset(cx, cy - 16f),
                                    end = Offset(cx, cy + 16f),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                                drawCircle(
                                    color = NaturalForestGreen.copy(alpha = 0.4f),
                                    radius = 18.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    } else if (activeMode == SandboxMode.SIMULATION_TEST) {
                        // Draw Swipe Paths in test mode
                        pins.filter { it.type == ActionType.SWIPE }.forEach { pin ->
                            val path = Path().apply {
                                moveTo(pin.x, pin.y)
                                val midX = (pin.x + pin.endX) / 2f + 30f
                                val midY = (pin.y + pin.endY) / 2f - 20f
                                quadraticTo(midX, midY, pin.endX, pin.endY)
                            }
                            drawPath(path = path, color = NaturalAmberWarm, style = Stroke(width = 4.dp.toPx()))
                            drawCircle(color = NaturalAmberWarm, radius = 6.dp.toPx(), center = Offset(pin.endX, pin.endY))
                        }

                        // Draw Click Target Pins
                        pins.forEach { pin ->
                            val pinColor = when (pin.type) {
                                ActionType.TAP -> NaturalForestGreen
                                ActionType.SWIPE -> NaturalAmberWarm
                                ActionType.OCR_TEXT_MATCH -> NaturalForestGreenLight
                                else -> NaturalRose
                            }
                            drawCircle(color = pinColor.copy(alpha = 0.35f), radius = 24.dp.toPx(), center = Offset(pin.x, pin.y))
                            drawCircle(color = pinColor, radius = 12.dp.toPx(), center = Offset(pin.x, pin.y))
                        }
                    } else {
                        // Draw Recorded Actions sequence
                        recordedActions.forEachIndexed { index, action ->
                            val color = when (action.actionType) {
                                ActionType.TAP -> NaturalForestGreen
                                ActionType.SWIPE -> NaturalAmberWarm
                                ActionType.TEXT_INPUT -> NaturalForestGreenLight
                                else -> NaturalRose
                            }

                            if (action.actionType == ActionType.SWIPE) {
                                val path = Path().apply {
                                    moveTo(action.x, action.y)
                                    val midX = (action.x + action.endX) / 2f + 25f
                                    val midY = (action.y + action.endY) / 2f - 15f
                                    quadraticTo(midX, midY, action.endX, action.endY)
                                }
                                drawPath(path = path, color = color, style = Stroke(width = 4.dp.toPx()))
                                drawCircle(color = color, radius = 6.dp.toPx(), center = Offset(action.endX, action.endY))
                            }

                            drawCircle(color = color.copy(alpha = 0.3f), radius = 22.dp.toPx(), center = Offset(action.x, action.y))
                            drawCircle(color = color, radius = 10.dp.toPx(), center = Offset(action.x, action.y))
                        }

                        // Draw current active swipe drag
                        val start = swipeStartOffset
                        val current = currentDragOffset
                        if (start != null && current != null) {
                            drawLine(
                                color = NaturalAmberWarm,
                                start = start,
                                end = current,
                                strokeWidth = 5.dp.toPx()
                            )
                            drawCircle(color = NaturalAmberWarm, radius = 8.dp.toPx(), center = current)
                        }
                    }
                }

                // Recorded Step Labels Overlay
                if (activeMode == SandboxMode.VISUAL_RECORDING) {
                    recordedActions.forEachIndexed { index, action ->
                        Box(
                            modifier = Modifier
                                .offset(x = (action.x - 16).dp, y = (action.y - 16).dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (action.actionType) {
                                        ActionType.TAP -> NaturalForestGreen
                                        ActionType.SWIPE -> NaturalAmberWarm
                                        ActionType.TEXT_INPUT -> NaturalForestGreenLight
                                        else -> NaturalRose
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Live Log Console Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("sandbox_live_logs"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NaturalForestGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.liveConsole,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NaturalTextPrimary
                        )
                    }
                    TextButton(
                        onClick = { engine.clearLogs() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(strings.clearLogs, style = MaterialTheme.typography.labelSmall, color = NaturalTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = if (isRecording) "Enregistrement en cours : touchez l'écran pour ajouter des clics ou glissements..." else "En attente de démarrage... Lancez le test ou l'enregistreur.",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalTextMuted
                            )
                        }
                    } else {
                        items(logs, key = { it.id }) { log ->
                            val logColor = when (log.level) {
                                "SUCCESS" -> NaturalForestGreen
                                "TRIGGER" -> NaturalAmberWarm
                                "WARNING" -> NaturalRose
                                else -> NaturalTextSecondary
                            }
                            Text(
                                text = "• ${log.message}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = logColor
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Text Input Recording
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Saisie de Texte Automatique", fontWeight = FontWeight.Bold, color = NaturalTextPrimary) },
            text = {
                Column {
                    Text("Entrez le texte qui sera tapé à cet emplacement :", style = MaterialTheme.typography.bodySmall, color = NaturalTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputFieldText,
                        onValueChange = { inputFieldText = it },
                        label = { Text("Texte à saisir") },
                        modifier = Modifier.fillMaxWidth(),
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
                        val target = pendingTextTargetOffset ?: Offset(540f, 400f)
                        val now = System.currentTimeMillis()
                        val delay = (now - lastActionTimestamp).coerceIn(400L, 5000L)
                        lastActionTimestamp = now

                        val action = RecordedAction(
                            actionType = ActionType.TEXT_INPUT,
                            x = target.x,
                            y = target.y,
                            textValue = inputFieldText,
                            delayMs = delay,
                            label = "Saisie texte #${recordedActions.size + 1} : '$inputFieldText'"
                        )
                        recordedActions = recordedActions + action
                        engine.addLog("Action enregistrée : Saisie de texte '$inputFieldText' à (${target.x.toInt()}, ${target.y.toInt()})", "SUCCESS")
                        showTextInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ajouter", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Annuler", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }

    // Dialog for Saving Recorded Script
    if (showSaveScriptDialog) {
        AlertDialog(
            onDismissRequest = { showSaveScriptDialog = false },
            title = { Text("Enregistrer le Script Enregistré", fontWeight = FontWeight.Bold, color = NaturalTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${recordedActions.size} actions capturées seront converties en script automatisé exécutable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )

                    OutlinedTextField(
                        value = recordedScriptTitle,
                        onValueChange = { recordedScriptTitle = it },
                        label = { Text("Nom du script") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalForestGreen,
                            unfocusedBorderColor = NaturalOliveBorder
                        )
                    )

                    OutlinedTextField(
                        value = recordedScriptDesc,
                        onValueChange = { recordedScriptDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
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
                        val steps = recordedActions.mapIndexed { idx, recorded ->
                            ActionStep(
                                stepIndex = idx + 1,
                                actionType = recorded.actionType,
                                x = recorded.x * 2.5f,
                                y = recorded.y * 2.5f,
                                endX = recorded.endX * 2.5f,
                                endY = recorded.endY * 2.5f,
                                inputText = recorded.textValue,
                                delayAfterMs = recorded.delayMs,
                                delayVarianceMs = (recorded.delayMs * 0.15f).toLong(),
                                pressDurationMs = recorded.pressDurationMs,
                                jitterRadiusPx = 6.5f,
                                label = recorded.label
                            )
                        }

                        val newScript = Script(
                            title = recordedScriptTitle.ifBlank { "Script Enregistré" },
                            description = recordedScriptDesc,
                            batteryMode = BatteryMode.BALANCED,
                            humanizeConfig = HumanizeConfig(
                                enabled = true,
                                jitterRadiusPx = 6.8f,
                                timeVariancePercentage = 16f,
                                naturalBezierCurves = true,
                                antiBotScore = 98
                            ),
                            steps = steps,
                            tags = listOf("Enregistré", "Visuel")
                        )

                        onSaveRecordedScript(newScript)
                        Toast.makeText(context, "Script enregistré avec succès !", Toast.LENGTH_SHORT).show()
                        showSaveScriptDialog = false
                        recordedActions = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Créer le Script", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveScriptDialog = false }) {
                    Text("Annuler", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }
}
