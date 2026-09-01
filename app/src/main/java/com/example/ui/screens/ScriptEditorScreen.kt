package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Strings
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.HumanizeConfig
import com.example.model.Script
import com.example.service.Humanizer
import com.example.ui.components.BatteryProfileSelector
import com.example.ui.components.HumanizeMeter
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    initialScript: Script?,
    strings: Strings,
    onSaveScript: (Script) -> Unit,
    onNavigateBack: () -> Unit,
    onTestInSandbox: (Script) -> Unit
) {
    var title by remember { mutableStateOf(initialScript?.title ?: "Nouvelle Automatisation") }
    var description by remember { mutableStateOf(initialScript?.description ?: "") }
    var steps by remember { mutableStateOf(initialScript?.steps ?: emptyList()) }
    var batteryMode by remember { mutableStateOf(initialScript?.batteryMode ?: BatteryMode.BALANCED) }

    var humanizeConfig by remember {
        mutableStateOf(initialScript?.humanizeConfig ?: HumanizeConfig())
    }

    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    var showAddStepDialog by remember { mutableStateOf(false) }
    var stepToEdit by remember { mutableStateOf<ActionStep?>(null) }

    val antiBotScore = remember(humanizeConfig) {
        Humanizer.calculateAntiBotScore(humanizeConfig)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialScript == null) strings.newScript else strings.editScript,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = NaturalTextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val current = Script(
                                id = initialScript?.id ?: java.util.UUID.randomUUID().toString(),
                                title = title,
                                description = description,
                                batteryMode = batteryMode,
                                humanizeConfig = humanizeConfig.copy(antiBotScore = antiBotScore),
                                steps = steps
                            )
                            onTestInSandbox(current)
                        },
                        modifier = Modifier.testTag("editor_test_button")
                    ) {
                        Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = "Tester", tint = NaturalAmberWarm)
                    }

                    Button(
                        onClick = {
                            val saved = Script(
                                id = initialScript?.id ?: java.util.UUID.randomUUID().toString(),
                                title = title.ifBlank { "Script sans titre" },
                                description = description,
                                createdAt = initialScript?.createdAt ?: System.currentTimeMillis(),
                                lastRunAt = initialScript?.lastRunAt ?: 0L,
                                runCount = initialScript?.runCount ?: 0,
                                isScheduled = initialScript?.isScheduled ?: false,
                                scheduleTimeMinutes = initialScript?.scheduleTimeMinutes ?: 480,
                                batteryMode = batteryMode,
                                humanizeConfig = humanizeConfig.copy(antiBotScore = antiBotScore),
                                steps = steps.mapIndexed { i, s -> s.copy(stepIndex = i + 1) }
                            )
                            onSaveScript(saved)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_script_top_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sauvegarder", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaturalSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("script_editor_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(strings.scriptName) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("script_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalOliveBorder,
                                focusedTextColor = NaturalTextPrimary,
                                unfocusedTextColor = NaturalTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(strings.scriptDescription) },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("script_desc_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalOliveBorder,
                                focusedTextColor = NaturalTextPrimary,
                                unfocusedTextColor = NaturalTextPrimary
                            )
                        )
                    }
                }
            }

            // Humanize Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = NaturalForestGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.humanizeTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary
                                )
                            }
                            Switch(
                                checked = humanizeConfig.enabled,
                                onCheckedChange = { humanizeConfig = humanizeConfig.copy(enabled = it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NaturalForestGreen,
                                    checkedTrackColor = NaturalSageLight,
                                    uncheckedThumbColor = NaturalTextMuted,
                                    uncheckedTrackColor = NaturalOliveLight
                                )
                            )
                        }

                        if (humanizeConfig.enabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Micro-décalage spatial: ±${humanizeConfig.jitterRadiusPx.toInt()} px",
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalTextSecondary
                            )
                            Slider(
                                value = humanizeConfig.jitterRadiusPx,
                                onValueChange = { humanizeConfig = humanizeConfig.copy(jitterRadiusPx = it) },
                                valueRange = 2f..16f,
                                steps = 14,
                                colors = SliderDefaults.colors(thumbColor = NaturalForestGreen, activeTrackColor = NaturalForestGreen)
                            )

                            Text(
                                text = "Variation du rythme: ±${humanizeConfig.timeVariancePercentage.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalTextSecondary
                            )
                            Slider(
                                value = humanizeConfig.timeVariancePercentage,
                                onValueChange = { humanizeConfig = humanizeConfig.copy(timeVariancePercentage = it) },
                                valueRange = 5f..35f,
                                steps = 6,
                                colors = SliderDefaults.colors(thumbColor = NaturalAmberWarm, activeTrackColor = NaturalAmberWarm)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.naturalCurves,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalTextPrimary
                                )
                                Checkbox(
                                    checked = humanizeConfig.naturalBezierCurves,
                                    onCheckedChange = { humanizeConfig = humanizeConfig.copy(naturalBezierCurves = it ?: true) },
                                    colors = CheckboxDefaults.colors(checkedColor = NaturalForestGreen)
                                )
                            }
                        }
                    }
                }
            }

            // Battery profile selector
            item {
                BatteryProfileSelector(
                    selectedMode = batteryMode,
                    onModeSelected = { batteryMode = it },
                    strings = strings
                )
            }

            // Timeline header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chronologie des Actions (${steps.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )

                    Button(
                        onClick = {
                            stepToEdit = ActionStep(
                                stepIndex = steps.size + 1,
                                actionType = ActionType.TAP
                            )
                            editingStepIndex = null
                            showAddStepDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                        modifier = Modifier.testTag("add_step_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.addAction, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Steps timeline list
            if (steps.isEmpty()) {
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
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = NaturalTextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Aucune action configurée", style = MaterialTheme.typography.bodyMedium, color = NaturalTextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    stepToEdit = ActionStep(stepIndex = 1, actionType = ActionType.TAP)
                                    showAddStepDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                            ) {
                                Text("+ Ajouter une action", color = NaturalForestGreen)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(steps) { index, step ->
                    StepItemCard(
                        step = step,
                        index = index,
                        totalSteps = steps.size,
                        strings = strings,
                        onEdit = {
                            stepToEdit = step
                            editingStepIndex = index
                            showAddStepDialog = true
                        },
                        onDelete = {
                            steps = steps.filterIndexed { i, _ -> i != index }
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = steps.toMutableList()
                                val temp = mutable[index]
                                mutable[index] = mutable[index - 1]
                                mutable[index - 1] = temp
                                steps = mutable
                            }
                        },
                        onMoveDown = {
                            if (index < steps.size - 1) {
                                val mutable = steps.toMutableList()
                                val temp = mutable[index]
                                mutable[index] = mutable[index + 1]
                                mutable[index + 1] = temp
                                steps = mutable
                            }
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Step Dialog
    if (showAddStepDialog && stepToEdit != null) {
        StepConfigDialog(
            initialStep = stepToEdit!!,
            strings = strings,
            onDismiss = { showAddStepDialog = false },
            onConfirm = { updatedStep ->
                if (editingStepIndex != null) {
                    steps = steps.toMutableList().apply { set(editingStepIndex!!, updatedStep) }
                } else {
                    steps = steps + updatedStep.copy(stepIndex = steps.size + 1)
                }
                showAddStepDialog = false
            }
        )
    }
}

@Composable
fun StepItemCard(
    step: ActionStep,
    index: Int,
    totalSteps: Int,
    strings: Strings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_card_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NaturalForestGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NaturalForestGreen
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (step.actionType) {
                            ActionType.TAP -> strings.actionTap
                            ActionType.DOUBLE_TAP -> strings.actionDoubleTap
                            ActionType.LONG_PRESS -> strings.actionLongPress
                            ActionType.SWIPE -> strings.actionSwipe
                            ActionType.TEXT_INPUT -> "Saisie de texte"
                            ActionType.WAIT -> strings.actionWait
                            ActionType.OCR_TEXT_MATCH -> strings.actionOcrText
                            ActionType.IMAGE_MATCH -> strings.actionImageMatch
                            ActionType.LOOP_START -> strings.actionLoop
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )

                    if (step.conditionText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NaturalAmberWarm.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "'${step.conditionText}'",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalAmberWarm,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val detailText = when (step.actionType) {
                    ActionType.TAP, ActionType.DOUBLE_TAP, ActionType.LONG_PRESS ->
                        "Cible (${step.x.toInt()}px, ${step.y.toInt()}px) • Pause: ${step.delayAfterMs}ms"
                    ActionType.SWIPE ->
                        "De (${step.x.toInt()}, ${step.y.toInt()}) vers (${step.endX.toInt()}, ${step.endY.toInt()}) • Pause: ${step.delayAfterMs}ms"
                    ActionType.TEXT_INPUT ->
                        "Saisie : '${step.inputText}' • Pause: ${step.delayAfterMs}ms"
                    ActionType.WAIT ->
                        "Pause: ${step.delayAfterMs}ms (±${step.delayVarianceMs}ms)"
                    ActionType.OCR_TEXT_MATCH ->
                        "Recherche '${step.conditionText}' • Clic auto si trouvé"
                    ActionType.IMAGE_MATCH -> {
                        val preset = com.example.service.ImageTemplatePreset.entries.firstOrNull { it.id == step.imageTemplateType }
                        val emoji = preset?.emoji ?: "🖼️"
                        "$emoji Détection '${step.imageTemplateName}' • Seuil ${(step.imageConfidenceThreshold * 100).toInt()}%"
                    }
                    ActionType.LOOP_START ->
                        "Répéter toute la séquence ${step.loopCount} fois"
                }

                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalTextSecondary
                )
            }

            // Step actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Haut", tint = if (index > 0) NaturalTextPrimary else NaturalTextMuted)
                }
                IconButton(onClick = onMoveDown, enabled = index < totalSteps - 1) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bas", tint = if (index < totalSteps - 1) NaturalTextPrimary else NaturalTextMuted)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = NaturalForestGreen)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = NaturalError)
                }
            }
        }
    }
}

@Composable
fun StepConfigDialog(
    initialStep: ActionStep,
    strings: Strings,
    onDismiss: () -> Unit,
    onConfirm: (ActionStep) -> Unit
) {
    var actionType by remember { mutableStateOf(initialStep.actionType) }
    var xStr by remember { mutableStateOf(initialStep.x.toInt().toString()) }
    var yStr by remember { mutableStateOf(initialStep.y.toInt().toString()) }
    var endXStr by remember { mutableStateOf(initialStep.endX.toInt().toString()) }
    var endYStr by remember { mutableStateOf(initialStep.endY.toInt().toString()) }
    var delayStr by remember { mutableStateOf(initialStep.delayAfterMs.toString()) }
    var varianceStr by remember { mutableStateOf(initialStep.delayVarianceMs.toString()) }
    var conditionText by remember { mutableStateOf(initialStep.conditionText) }
    var loopCountStr by remember { mutableStateOf(initialStep.loopCount.toString()) }
    var label by remember { mutableStateOf(initialStep.label) }

    // Image detection specific state
    var selectedImagePreset by remember {
        mutableStateOf(
            com.example.service.ImageTemplatePreset.entries.firstOrNull { it.id == initialStep.imageTemplateType }
                ?: com.example.service.ImageTemplatePreset.CHEST_REWARD
        )
    }
    var imageConfidence by remember { mutableStateOf(initialStep.imageConfidenceThreshold) }
    var selectedRegion by remember {
        mutableStateOf(
            try {
                com.example.service.SearchRegion.valueOf(initialStep.imageSearchRegion)
            } catch (e: Exception) {
                com.example.service.SearchRegion.FULL_SCREEN
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configurer l'Action",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(text = "Type d'action", style = MaterialTheme.typography.labelMedium, color = NaturalTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            ActionType.TAP to "Clic",
                            ActionType.SWIPE to "Swipe",
                            ActionType.OCR_TEXT_MATCH to "OCR",
                            ActionType.IMAGE_MATCH to "Image CV",
                            ActionType.WAIT to "Pause"
                        ).forEach { (type, typeTitle) ->
                            val isSelected = actionType == type
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NaturalForestGreen else NaturalOliveLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { actionType = type }
                            ) {
                                Text(
                                    text = typeTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else NaturalTextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Image Detection Configuration UI
                if (actionType == ActionType.IMAGE_MATCH) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NaturalSageLight.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalForestGreen.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Reconnaissance d'Image (Computer Vision)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalForestGreen
                                )
                                Text(
                                    text = "Détecte automatiquement le motif visuel à l'écran et clique exactement sur son centre.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalTextSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Modèle visuel cible :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = NaturalTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Presets Grid
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    com.example.service.ImageTemplatePreset.entries.chunked(2).forEach { rowPresets ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowPresets.forEach { preset ->
                                                val isPresSelected = selectedImagePreset == preset
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isPresSelected) NaturalForestGreen else Color.White,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isPresSelected) NaturalForestGreen else NaturalOliveBorder
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            selectedImagePreset = preset
                                                            xStr = preset.defaultX.toInt().toString()
                                                            yStr = preset.defaultY.toInt().toString()
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = preset.emoji, fontSize = 16.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = preset.label.split("/").first().trim(),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = if (isPresSelected) FontWeight.Bold else FontWeight.Normal
                                                            ),
                                                            color = if (isPresSelected) Color.White else NaturalTextPrimary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Confidence Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seuil de similarité : ${(imageConfidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalTextPrimary
                                    )
                                    Text(
                                        text = when {
                                            imageConfidence < 0.70f -> "Tolérant"
                                            imageConfidence <= 0.85f -> "Recommandé"
                                            else -> "Strict"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalForestGreen
                                    )
                                }
                                Slider(
                                    value = imageConfidence,
                                    onValueChange = { imageConfidence = it },
                                    valueRange = 0.50f..0.98f,
                                    steps = 24,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NaturalForestGreen,
                                        activeTrackColor = NaturalForestGreen
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Region Selector
                                Text(
                                    text = "Zone de recherche d'image :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = NaturalTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    com.example.service.SearchRegion.entries.forEach { region ->
                                        val isRegSelected = selectedRegion == region
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isRegSelected) NaturalForestGreen.copy(alpha = 0.15f) else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isRegSelected) NaturalForestGreen else NaturalOliveBorder
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedRegion = region }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = region.label,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NaturalTextPrimary
                                                )
                                                if (isRegSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = NaturalForestGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (actionType == ActionType.TAP || actionType == ActionType.DOUBLE_TAP || actionType == ActionType.LONG_PRESS || actionType == ActionType.SWIPE || actionType == ActionType.OCR_TEXT_MATCH || actionType == ActionType.IMAGE_MATCH) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = xStr,
                                onValueChange = { xStr = it },
                                label = { Text(if (actionType == ActionType.IMAGE_MATCH) "X repli (px)" else "Pos X (px)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NaturalForestGreen,
                                    unfocusedBorderColor = NaturalOliveBorder
                                )
                            )
                            OutlinedTextField(
                                value = yStr,
                                onValueChange = { yStr = it },
                                label = { Text(if (actionType == ActionType.IMAGE_MATCH) "Y repli (px)" else "Pos Y (px)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NaturalForestGreen,
                                    unfocusedBorderColor = NaturalOliveBorder
                                )
                            )
                        }
                    }
                }

                if (actionType == ActionType.SWIPE) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = endXStr,
                                onValueChange = { endXStr = it },
                                label = { Text("Fin X (px)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NaturalForestGreen,
                                    unfocusedBorderColor = NaturalOliveBorder
                                )
                            )
                            OutlinedTextField(
                                value = endYStr,
                                onValueChange = { endYStr = it },
                                label = { Text("Fin Y (px)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NaturalForestGreen,
                                    unfocusedBorderColor = NaturalOliveBorder
                                )
                            )
                        }
                    }
                }

                if (actionType == ActionType.OCR_TEXT_MATCH) {
                    item {
                        OutlinedTextField(
                            value = conditionText,
                            onValueChange = { conditionText = it },
                            label = { Text("Texte à détecter (OCR)") },
                            placeholder = { Text("Ex: 'Réclamer', 'Suivant'") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalOliveBorder
                            )
                        )
                    }
                }

                if (actionType == ActionType.LOOP_START) {
                    item {
                        OutlinedTextField(
                            value = loopCountStr,
                            onValueChange = { loopCountStr = it },
                            label = { Text("Nombre de répétitions") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalOliveBorder
                            )
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = delayStr,
                            onValueChange = { delayStr = it },
                            label = { Text("Délai (ms)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalOliveBorder
                            )
                        )
                        OutlinedTextField(
                            value = varianceStr,
                            onValueChange = { varianceStr = it },
                            label = { Text("Variance (±ms)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalOliveBorder
                            )
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Note / Description (Optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalForestGreen,
                            unfocusedBorderColor = NaturalOliveBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = initialStep.copy(
                        actionType = actionType,
                        x = xStr.toFloatOrNull() ?: 540f,
                        y = yStr.toFloatOrNull() ?: 960f,
                        endX = endXStr.toFloatOrNull() ?: 540f,
                        endY = endYStr.toFloatOrNull() ?: 400f,
                        delayAfterMs = delayStr.toLongOrNull() ?: 1000L,
                        delayVarianceMs = varianceStr.toLongOrNull() ?: 100L,
                        conditionText = conditionText,
                        imageTemplateType = selectedImagePreset.id,
                        imageTemplateName = selectedImagePreset.label,
                        imageConfidenceThreshold = imageConfidence,
                        imageSearchRegion = selectedRegion.name,
                        loopCount = loopCountStr.toIntOrNull() ?: 1,
                        label = label
                    )
                    onConfirm(updated)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White)
            ) {
                Text("Valider", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = NaturalTextSecondary)
            }
        },
        containerColor = NaturalSurface
    )
}
