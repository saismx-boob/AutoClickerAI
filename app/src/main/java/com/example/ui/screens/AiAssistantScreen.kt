package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiGenerationResult
import com.example.ai.GeminiScriptService
import com.example.localization.Strings
import com.example.model.AiOptimizationAnalysis
import com.example.model.Script
import com.example.ui.components.HumanizeMeter
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AiTabMode {
    GENERATE_FROM_PROMPT,
    OPTIMIZE_EXISTING_SCRIPT
}

@Composable
fun AiAssistantScreen(
    strings: Strings,
    existingScripts: List<Script> = emptyList(),
    onSaveGeneratedScript: (Script) -> Unit,
    onTestInSandbox: (Script) -> Unit
) {
    val scope = rememberCoroutineScope()

    var activeAiTab by remember { mutableStateOf(AiTabMode.GENERATE_FROM_PROMPT) }

    // Generation state
    var userPrompt by remember {
        mutableStateOf("Clique sur 'Réclamer' toutes les 3 secondes, glisse vers le haut avec un mouvement naturel et répète 20 fois.")
    }
    var isGenerating by remember { mutableStateOf(false) }
    var generationResult by remember { mutableStateOf<AiGenerationResult?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Optimization state
    var selectedScriptToOptimize by remember { mutableStateOf<Script?>(existingScripts.firstOrNull()) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationResult by remember { mutableStateOf<AiOptimizationAnalysis?>(null) }

    val presetTemplates = listOf(
        "🎁 " + strings.templateDailyCheckin to "Clique sur 'Réclamer' dès que le texte apparaît, attend 2 secondes puis clique sur la croix pour fermer.",
        "🎮 " + strings.templateGameFarming to "Farming de combat : clique rapidement à (540, 1100), active la compétence à (750, 1350) et relance si 'Victoire' est détecté.",
        "📱 " + strings.templateSocialScroll to "Fais défiler l'écran vers le haut toutes les 4 secondes avec des pauses variables de lecture et un double clic pour aimer.",
        "🔍 " + strings.templateOcrClaim to "Surveille l'écran et clique automatiquement sur le bouton 'Accepter' ou 'Valider' avec vérification OCR."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_assistant_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // AI Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NaturalSageLight
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    NaturalSageBorder
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.aiTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalTextPrimary
                            )
                            Text(
                                text = "Propulsé par Google Gemini • Analyse Anti-Bot & Éco-Batterie",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalForestGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector: Generate vs Optimize
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { activeAiTab = AiTabMode.GENERATE_FROM_PROMPT },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeAiTab == AiTabMode.GENERATE_FROM_PROMPT) NaturalForestGreen else Color.Transparent,
                                    contentColor = if (activeAiTab == AiTabMode.GENERATE_FROM_PROMPT) Color.White else NaturalTextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                elevation = null
                            ) {
                                Text("Créer Script", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { activeAiTab = AiTabMode.OPTIMIZE_EXISTING_SCRIPT },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeAiTab == AiTabMode.OPTIMIZE_EXISTING_SCRIPT) NaturalForestGreen else Color.Transparent,
                                    contentColor = if (activeAiTab == AiTabMode.OPTIMIZE_EXISTING_SCRIPT) Color.White else NaturalTextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                elevation = null
                            ) {
                                Text("Analyser & Optimiser", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (activeAiTab == AiTabMode.GENERATE_FROM_PROMPT) {
            // Prompt Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Création par Prompt en Langage Naturel",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = userPrompt,
                            onValueChange = { userPrompt = it },
                            label = { Text("Décrivez votre automatisation") },
                            placeholder = { Text(strings.aiPromptHint) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalForestGreen,
                                unfocusedBorderColor = NaturalSageBorder,
                                focusedTextColor = NaturalTextPrimary,
                                unfocusedTextColor = NaturalTextPrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (userPrompt.isNotBlank() && !isGenerating) {
                                    isGenerating = true
                                    saveSuccessMessage = null
                                    scope.launch {
                                        val result = GeminiScriptService.generateScriptFromPrompt(userPrompt)
                                        generationResult = result
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating && userPrompt.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NaturalForestGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_with_ai_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.aiGenerating, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.generateScriptWithAi, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Preset Templates
            item {
                Column {
                    Text(
                        text = strings.aiTemplates,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetTemplates.forEach { (title, prompt) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { userPrompt = prompt },
                                shape = RoundedCornerShape(14.dp),
                                color = NaturalCardSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = NaturalTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.NorthWest, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // AI Generation Result Card
            generationResult?.let { result ->
                val script = result.script

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_result_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalForestGreen)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = script.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalForestGreen
                                    )
                                    Text(
                                        text = "${script.steps.size} actions configurées • ${script.batteryMode.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalTextSecondary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NaturalSageLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                                ) {
                                    Text(
                                        text = "${script.humanizeConfig.antiBotScore}% Furtif",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalForestGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = result.analysisFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalTextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HumanizeMeter(config = script.humanizeConfig, strings = strings)

                            Spacer(modifier = Modifier.height(14.dp))

                            // Steps Preview Summary
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                script.steps.forEachIndexed { i, step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NaturalOliveLight, RoundedCornerShape(10.dp))
                                            .border(1.dp, NaturalOliveBorder, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${i + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NaturalForestGreen
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = step.label.ifBlank { "${step.actionType.label} (pause ${step.delayAfterMs}ms)" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NaturalTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onTestInSandbox(script) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                                ) {
                                    Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tester Live", color = NaturalForestGreen, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onSaveGeneratedScript(script)
                                        saveSuccessMessage = "Script '${script.title}' enregistré dans vos automatisations !"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NaturalForestGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_ai_script_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                                }
                            }

                            saveSuccessMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "✓ $msg",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalForestGreen
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Optimize Existing Script Tab
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Optimisation IA d'un Script Existant",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NaturalTextPrimary
                        )
                        Text(
                            text = "L'IA analyse le comportement anti-bot, la consommation de batterie et optimise les délais pour une fluidité humaine parfaite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalTextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (existingScripts.isEmpty()) {
                            Text(
                                text = "Aucun script disponible à optimiser. Créez d'abord un script.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalTextMuted
                            )
                        } else {
                            Text("Sélectionnez le script à analyser :", style = MaterialTheme.typography.labelMedium, color = NaturalTextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                existingScripts.forEach { script ->
                                    val isSelected = selectedScriptToOptimize?.id == script.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedScriptToOptimize = script
                                                optimizationResult = null
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) NaturalSageLight else NaturalOliveLight,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.5.dp,
                                            if (isSelected) NaturalForestGreen else NaturalOliveBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = script.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = NaturalTextPrimary
                                                )
                                                Text(
                                                    text = "${script.steps.size} actions • ${script.humanizeConfig.antiBotScore}% furtivité",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NaturalTextSecondary
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NaturalForestGreen)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val target = selectedScriptToOptimize
                                    if (target != null && !isOptimizing) {
                                        isOptimizing = true
                                        scope.launch {
                                            val analysis = GeminiScriptService.analyzeAndOptimizeScript(target)
                                            optimizationResult = analysis
                                            isOptimizing = false
                                        }
                                    }
                                },
                                enabled = selectedScriptToOptimize != null && !isOptimizing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NaturalForestGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isOptimizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyse IA en cours...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lancer l'Optimisation IA", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Optimization Result Card
            val currentOptResult = optimizationResult
            if (currentOptResult != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalForestGreen)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Rapport d'Analyse IA",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalForestGreen
                                    )
                                    Text(
                                        text = currentOptResult.batterySavingsEstimate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalTextSecondary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NaturalSageLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                                ) {
                                    Text(
                                        text = "${currentOptResult.scoreAfter}% Indétectable",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NaturalForestGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentOptResult.fluidityNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = NaturalTextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Suggestions List
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                currentOptResult.optimizationsApplied.forEach { sugg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NaturalSageLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = sugg, style = MaterialTheme.typography.bodySmall, color = NaturalTextPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onSaveGeneratedScript(currentOptResult.optimizedScript)
                                    saveSuccessMessage = "Script optimisé '${currentOptResult.optimizedScript.title}' sauvegardé !"
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NaturalForestGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Appliquer les Optimisations IA", fontWeight = FontWeight.Bold)
                            }

                            saveSuccessMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "✓ $msg",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalForestGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
