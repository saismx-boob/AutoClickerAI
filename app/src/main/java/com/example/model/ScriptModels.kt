package com.example.model

enum class ActionType(val label: String, val iconName: String) {
    TAP("Tap", "TouchApp"),
    DOUBLE_TAP("Double Tap", "TouchApp"),
    LONG_PRESS("Long Press", "Timer"),
    SWIPE("Swipe / Drag", "Swipe"),
    TEXT_INPUT("Saisie de Texte", "Keyboard"),
    WAIT("Wait / Delay", "HourglassEmpty"),
    OCR_TEXT_MATCH("OCR Text Condition", "FindInPage"),
    IMAGE_MATCH("Image Recognition", "ImageSearch"),
    LOOP_START("Loop Block", "Repeat")
}

enum class BatteryMode(val title: String, val intervalScale: Float, val powerLabel: String) {
    ECO("Mode Éco", 1.5f, "~2% / hr"),
    BALANCED("Équilibré", 1.0f, "~4% / hr"),
    TURBO("Turbo Vitesse", 0.5f, "~7% / hr")
}

data class HumanizeConfig(
    val enabled: Boolean = true,
    val jitterRadiusPx: Float = 6f,
    val timeVariancePercentage: Float = 15f,
    val naturalBezierCurves: Boolean = true,
    val microPauseProbability: Float = 0.05f,
    val antiBotScore: Int = 98
)

data class ActionStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val scriptId: String = "",
    val stepIndex: Int = 0,
    val actionType: ActionType = ActionType.TAP,
    val x: Float = 540f,
    val y: Float = 960f,
    val endX: Float = 540f,
    val endY: Float = 400f,
    val inputText: String = "",
    val delayAfterMs: Long = 1000L,
    val delayVarianceMs: Long = 150L,
    val pressDurationMs: Long = 85L,
    val jitterRadiusPx: Float = 6f,
    val conditionText: String = "",
    val conditionPattern: String = "",
    val imageTemplateType: String = "CHEST_REWARD",
    val imageTemplateName: String = "Coffre au Trésor",
    val imageConfidenceThreshold: Float = 0.80f,
    val imageSearchRegion: String = "FULL_SCREEN",
    val imageTemplateBase64: String = "",
    val imageTimeoutMs: Long = 5000L,
    val timeoutMs: Long = 5000L,
    val loopCount: Int = 1,
    val label: String = ""
)

data class RecordedAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: ActionType = ActionType.TAP,
    val x: Float = 540f,
    val y: Float = 960f,
    val endX: Float = 540f,
    val endY: Float = 400f,
    val text: String = "",
    val textValue: String = "",
    val delayMs: Long = 800L,
    val pressDurationMs: Long = 85L,
    val label: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Script(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = 0L,
    val runCount: Int = 0,
    val isScheduled: Boolean = false,
    val scheduleTimeMinutes: Int = 480, // 08:00
    val scheduleDays: List<Int> = listOf(1, 2, 3, 4, 5), // Mon-Fri
    val isRepeatDaily: Boolean = true,
    val batteryMode: BatteryMode = BatteryMode.BALANCED,
    val humanizeConfig: HumanizeConfig = HumanizeConfig(),
    val steps: List<ActionStep> = emptyList(),
    val tags: List<String> = emptyList()
)

data class ExecutionLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val level: String = "INFO", // INFO, SUCCESS, WARNING, TRIGGER
    val stepNumber: Int? = null
)

data class BackupSnapshot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val scriptCount: Int = 0,
    val totalRuns: Int = 0,
    val jsonContent: String = "",
    val note: String = "Sauvegarde locale / Cloud"
)

data class LocalBackupFile(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val scriptCount: Int,
    val note: String
)

data class AiOptimizationAnalysis(
    val scriptId: String,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val batterySavingsEstimate: String,
    val fluidityNotes: String,
    val optimizationsApplied: List<String>,
    val optimizedScript: Script,
    val isFromGeminiLive: Boolean = true
)

enum class PlaybackState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR
}
