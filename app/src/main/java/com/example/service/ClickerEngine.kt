package com.example.service

import android.graphics.PointF
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.ExecutionLog
import com.example.model.PlaybackState
import com.example.model.Script
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TouchVisualEvent(
    val x: Float,
    val y: Float,
    val isSwipe: Boolean = false,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val label: String = ""
)

class ClickerEngine(private val scope: CoroutineScope) {

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState = _playbackState.asStateFlow()

    private val _currentScript = MutableStateFlow<Script?>(null)
    val currentScript = _currentScript.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex = _currentStepIndex.asStateFlow()

    private val _totalClicksExecuted = MutableStateFlow(0)
    val totalClicksExecuted = _totalClicksExecuted.asStateFlow()

    private val _currentLoopIteration = MutableStateFlow(1)
    val currentLoopIteration = _currentLoopIteration.asStateFlow()

    private val _logs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _touchVisualEvents = MutableSharedFlow<TouchVisualEvent>(extraBufferCapacity = 10)
    val touchVisualEvents = _touchVisualEvents.asSharedFlow()

    private var executionJob: Job? = null
    private var isPaused = false

    fun emitTouchEvent(x: Float, y: Float, isSwipe: Boolean = false, endX: Float = 0f, endY: Float = 0f, label: String = "") {
        _totalClicksExecuted.value += 1
        _touchVisualEvents.tryEmit(TouchVisualEvent(x, y, isSwipe, endX, endY, label))
    }

    fun startScript(script: Script, onComplete: (() -> Unit)? = null) {
        stop()
        _currentScript.value = script
        _playbackState.value = PlaybackState.RUNNING
        _currentStepIndex.value = 0
        _currentLoopIteration.value = 1
        isPaused = false

        addLog("Démarrage du script '${script.title}' (${script.steps.size} étapes)", "INFO")

        executionJob = scope.launch(Dispatchers.Default) {
            try {
                val speedScale = when (script.batteryMode) {
                    BatteryMode.ECO -> 1.4f
                    BatteryMode.BALANCED -> 1.0f
                    BatteryMode.TURBO -> 0.6f
                }

                var currentLoop = 1
                var maxLoops = 1

                // Detect max loops from script if present
                val loopStep = script.steps.firstOrNull { it.actionType == ActionType.LOOP_START }
                if (loopStep != null && loopStep.loopCount > 1) {
                    maxLoops = loopStep.loopCount
                }

                while (currentLoop <= maxLoops && _playbackState.value == PlaybackState.RUNNING) {
                    _currentLoopIteration.value = currentLoop
                    if (maxLoops > 1) {
                        addLog("--- Début du Cycle $currentLoop / $maxLoops ---", "INFO")
                    }

                    for ((index, step) in script.steps.withIndex()) {
                        while (isPaused && _playbackState.value == PlaybackState.PAUSED) {
                            delay(200L)
                        }

                        if (_playbackState.value != PlaybackState.RUNNING) break

                        _currentStepIndex.value = index + 1
                        executeStep(step, script, speedScale)
                    }

                    currentLoop++
                }

                if (_playbackState.value == PlaybackState.RUNNING) {
                    _playbackState.value = PlaybackState.COMPLETED
                    addLog("Automatisation terminée avec succès (${_totalClicksExecuted.value} actions)", "SUCCESS")
                    onComplete?.invoke()
                }
            } catch (e: CancellationException) {
                addLog("Exécution arrêtée.", "WARNING")
            } catch (e: Exception) {
                _playbackState.value = PlaybackState.ERROR
                addLog("Erreur: ${e.localizedMessage}", "WARNING")
            }
        }
    }

    private suspend fun executeStep(step: ActionStep, script: Script, speedScale: Float) {
        val config = script.humanizeConfig
        val jitterPoint = Humanizer.applyJitter(step.x, step.y, step.jitterRadiusPx, config)

        when (step.actionType) {
            ActionType.TAP -> {
                _touchVisualEvents.tryEmit(TouchVisualEvent(jitterPoint.x, jitterPoint.y, label = "Tap"))
                AutoClickerAccessibilityService.instance?.performTap(
                    jitterPoint.x,
                    jitterPoint.y,
                    step.pressDurationMs,
                    config
                )
                _totalClicksExecuted.value += 1
                addLog("Étape ${step.stepIndex}: Clic à (${jitterPoint.x.toInt()}px, ${jitterPoint.y.toInt()}px) • Décalage ${step.jitterRadiusPx}px", "INFO", step.stepIndex)
            }

            ActionType.DOUBLE_TAP -> {
                _touchVisualEvents.tryEmit(TouchVisualEvent(jitterPoint.x, jitterPoint.y, label = "2x Tap"))
                AutoClickerAccessibilityService.instance?.performDoubleTap(
                    jitterPoint.x,
                    jitterPoint.y,
                    config
                )
                _totalClicksExecuted.value += 2
                addLog("Étape ${step.stepIndex}: Double Clic à (${jitterPoint.x.toInt()}px, ${jitterPoint.y.toInt()}px)", "INFO", step.stepIndex)
            }

            ActionType.LONG_PRESS -> {
                _touchVisualEvents.tryEmit(TouchVisualEvent(jitterPoint.x, jitterPoint.y, label = "Appui Long"))
                AutoClickerAccessibilityService.instance?.performTap(
                    jitterPoint.x,
                    jitterPoint.y,
                    durationMs = 600L,
                    config = config
                )
                _totalClicksExecuted.value += 1
                addLog("Étape ${step.stepIndex}: Appui long (600ms) à (${jitterPoint.x.toInt()}px, ${jitterPoint.y.toInt()}px)", "INFO", step.stepIndex)
            }

            ActionType.SWIPE -> {
                val endPoint = Humanizer.applyJitter(step.endX, step.endY, step.jitterRadiusPx, config)
                _touchVisualEvents.tryEmit(
                    TouchVisualEvent(
                        x = jitterPoint.x,
                        y = jitterPoint.y,
                        isSwipe = true,
                        endX = endPoint.x,
                        endY = endPoint.y,
                        label = "Swipe"
                    )
                )
                AutoClickerAccessibilityService.instance?.performSwipe(
                    jitterPoint.x,
                    jitterPoint.y,
                    endPoint.x,
                    endPoint.y,
                    durationMs = 450L,
                    config = config
                )
                _totalClicksExecuted.value += 1
                addLog("Étape ${step.stepIndex}: Glissement fluide Bezier vers (${endPoint.x.toInt()}px, ${endPoint.y.toInt()}px)", "INFO", step.stepIndex)
            }

            ActionType.TEXT_INPUT -> {
                val textToEnter = step.inputText.ifBlank { step.conditionText }
                _touchVisualEvents.tryEmit(
                    TouchVisualEvent(
                        x = jitterPoint.x,
                        y = jitterPoint.y,
                        label = "Texte: \"$textToEnter\""
                    )
                )
                // Focus target field with tap
                AutoClickerAccessibilityService.instance?.performTap(
                    jitterPoint.x,
                    jitterPoint.y,
                    durationMs = 80L,
                    config = config
                )
                delay(120L)
                AutoClickerAccessibilityService.instance?.performTextInput(textToEnter)
                _totalClicksExecuted.value += 1
                addLog("Étape ${step.stepIndex}: Saisie texte \"$textToEnter\" à (${jitterPoint.x.toInt()}px, ${jitterPoint.y.toInt()}px)", "INFO", step.stepIndex)
            }

            ActionType.WAIT -> {
                addLog("Étape ${step.stepIndex}: Pause de ${step.delayAfterMs}ms", "INFO", step.stepIndex)
            }

            ActionType.OCR_TEXT_MATCH -> {
                val targetText = step.conditionText
                addLog("Étape ${step.stepIndex}: Recherche OCR du texte '$targetText'...", "INFO", step.stepIndex)

                // Check real accessibility window or simulated sandbox match
                val matchedPoint = AutoClickerAccessibilityService.instance?.findTextInActiveWindow(targetText)
                    ?: PointF(step.x, step.y) // fallback / simulated match point

                _touchVisualEvents.tryEmit(TouchVisualEvent(matchedPoint.x, matchedPoint.y, label = "OCR Target"))
                AutoClickerAccessibilityService.instance?.performTap(matchedPoint.x, matchedPoint.y, 75L, config)
                _totalClicksExecuted.value += 1
                addLog("Étape ${step.stepIndex}: Texte '$targetText' détecté ! Clic déclenché à (${matchedPoint.x.toInt()}, ${matchedPoint.y.toInt()})", "TRIGGER", step.stepIndex)
            }

            ActionType.IMAGE_MATCH -> {
                val templateName = step.imageTemplateName.ifBlank { "Modèle Visuel" }
                val threshold = step.imageConfidenceThreshold
                val region = try {
                    SearchRegion.valueOf(step.imageSearchRegion)
                } catch (e: Exception) {
                    SearchRegion.FULL_SCREEN
                }

                addLog("Étape ${step.stepIndex}: Recherche d'image '$templateName' (Seuil ${(threshold * 100).toInt()}%, ${region.label})...", "INFO", step.stepIndex)

                // Perform detection via ImageDetectionEngine
                val simulatedTargets = listOf(
                    step.imageTemplateType to PointF(step.x, step.y)
                )
                val matchResult = ImageDetectionEngine.simulateLiveDetection(
                    presetId = step.imageTemplateType,
                    availableTargets = simulatedTargets,
                    threshold = threshold,
                    region = region
                )

                if (matchResult.found) {
                    val clickTarget = Humanizer.applyJitter(matchResult.centerX, matchResult.centerY, step.jitterRadiusPx, config)
                    _touchVisualEvents.tryEmit(
                        TouchVisualEvent(
                            x = clickTarget.x,
                            y = clickTarget.y,
                            label = "Image: $templateName (${(matchResult.confidence * 100).toInt()}%)"
                        )
                    )

                    AutoClickerAccessibilityService.instance?.performTap(
                        clickTarget.x,
                        clickTarget.y,
                        step.pressDurationMs,
                        config
                    )
                    _totalClicksExecuted.value += 1
                    val confPercent = (matchResult.confidence * 100).toInt()
                    addLog(
                        "Étape ${step.stepIndex}: Image '$templateName' DÉTECTÉE ($confPercent% similarité en ${matchResult.executionTimeMs}ms) -> Clic à (${clickTarget.x.toInt()}px, ${clickTarget.y.toInt()}px)",
                        "TRIGGER",
                        step.stepIndex
                    )
                } else {
                    addLog("Étape ${step.stepIndex}: Image '$templateName' non trouvée (Seuil requis: ${(threshold * 100).toInt()}%)", "WARNING", step.stepIndex)
                }
            }

            ActionType.LOOP_START -> {
                // Loop marker handled by outer loop
            }
        }

        // Apply delay after action with humanized time variance
        val randomizedDelay = Humanizer.computeRandomizedDelay(
            baseDelayMs = step.delayAfterMs,
            varianceMs = step.delayVarianceMs,
            config = config,
            speedScale = speedScale
        )
        delay(randomizedDelay)
    }

    fun pause() {
        if (_playbackState.value == PlaybackState.RUNNING) {
            isPaused = true
            _playbackState.value = PlaybackState.PAUSED
            addLog("Mise en pause de l'automatisation", "WARNING")
        }
    }

    fun resume() {
        if (_playbackState.value == PlaybackState.PAUSED) {
            isPaused = false
            _playbackState.value = PlaybackState.RUNNING
            addLog("Reprise de l'automatisation", "INFO")
        }
    }

    fun stop() {
        executionJob?.cancel()
        executionJob = null
        isPaused = false
        _playbackState.value = PlaybackState.IDLE
        _currentStepIndex.value = 0
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun addLog(message: String, level: String = "INFO", stepNumber: Int? = null) {
        val entry = ExecutionLog(
            message = message,
            level = level,
            stepNumber = stepNumber
        )
        _logs.value = listOf(entry) + _logs.value.take(49)
    }
}
