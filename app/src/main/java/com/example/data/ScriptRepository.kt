package com.example.data

import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.BackupSnapshot
import com.example.model.BatteryMode
import com.example.model.HumanizeConfig
import com.example.model.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ScriptRepository(private val scriptDao: ScriptDao) {

    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts().map { entities ->
        entities.map { mapEntityToDomain(it) }
    }

    val scheduledScripts: Flow<List<Script>> = scriptDao.getScheduledScripts().map { entities ->
        entities.map { mapEntityToDomain(it) }
    }

    val allBackups: Flow<List<BackupSnapshot>> = scriptDao.getAllBackups().map { entities ->
        entities.map {
            BackupSnapshot(
                id = it.id,
                timestamp = it.timestamp,
                scriptCount = it.scriptCount,
                totalRuns = it.totalRuns,
                jsonContent = it.jsonContent,
                note = it.note
            )
        }
    }

    suspend fun getScriptById(id: String): Script? = withContext(Dispatchers.IO) {
        scriptDao.getScriptById(id)?.let { mapEntityToDomain(it) }
    }

    suspend fun saveScript(script: Script) = withContext(Dispatchers.IO) {
        scriptDao.insertScript(mapDomainToEntity(script))
    }

    suspend fun deleteScript(id: String) = withContext(Dispatchers.IO) {
        scriptDao.deleteScriptById(id)
    }

    suspend fun recordScriptRun(id: String) = withContext(Dispatchers.IO) {
        scriptDao.recordScriptRun(id, System.currentTimeMillis())
    }

    suspend fun saveBackup(snapshot: BackupSnapshot) = withContext(Dispatchers.IO) {
        scriptDao.insertBackup(
            BackupEntity(
                id = snapshot.id,
                timestamp = snapshot.timestamp,
                scriptCount = snapshot.scriptCount,
                totalRuns = snapshot.totalRuns,
                jsonContent = snapshot.jsonContent,
                note = snapshot.note
            )
        )
    }

    suspend fun deleteBackup(id: String) = withContext(Dispatchers.IO) {
        scriptDao.deleteBackupById(id)
    }

    suspend fun seedSampleScriptsIfEmpty() = withContext(Dispatchers.IO) {
        // Preset 1: Farming & OCR
        val farmingScript = Script(
            id = "preset_farming_ocr",
            title = "Récolte de Jeu & Détection OCR",
            description = "Automatise les clics de récompense avec vérification visuelle par OCR et swipe naturel.",
            runCount = 142,
            lastRunAt = System.currentTimeMillis() - 3600_000,
            batteryMode = BatteryMode.ECO,
            humanizeConfig = HumanizeConfig(
                enabled = true,
                jitterRadiusPx = 8f,
                timeVariancePercentage = 18f,
                naturalBezierCurves = true,
                antiBotScore = 96
            ),
            tags = listOf("Jeu", "OCR", "Éco"),
            steps = listOf(
                ActionStep(
                    stepIndex = 1,
                    actionType = ActionType.WAIT,
                    delayAfterMs = 1200L,
                    label = "Attente chargement interface"
                ),
                ActionStep(
                    stepIndex = 2,
                    actionType = ActionType.OCR_TEXT_MATCH,
                    conditionText = "Réclamer",
                    x = 540f,
                    y = 1350f,
                    delayAfterMs = 800L,
                    delayVarianceMs = 120L,
                    jitterRadiusPx = 7f,
                    label = "Si texte 'Réclamer' visible -> Clic"
                ),
                ActionStep(
                    stepIndex = 3,
                    actionType = ActionType.SWIPE,
                    x = 540f,
                    y = 1500f,
                    endX = 540f,
                    endY = 550f,
                    delayAfterMs = 1500L,
                    label = "Glissement vers le haut (Bezier)"
                ),
                ActionStep(
                    stepIndex = 4,
                    actionType = ActionType.TAP,
                    x = 780f,
                    y = 820f,
                    delayAfterMs = 600L,
                    delayVarianceMs = 90L,
                    jitterRadiusPx = 5f,
                    label = "Validation du coffre"
                ),
                ActionStep(
                    stepIndex = 5,
                    actionType = ActionType.LOOP_START,
                    loopCount = 15,
                    label = "Répéter 15 cycles"
                )
            )
        )

        // Preset 2: Daily Check-in
        val dailyCheckin = Script(
            id = "preset_daily_checkin",
            title = "Récompense Quotidienne 08:00",
            description = "Planifié tous les matins pour collecter le bonus quotidien et fermer les popups.",
            isScheduled = true,
            scheduleTimeMinutes = 480, // 08:00
            scheduleDays = listOf(1, 2, 3, 4, 5, 6, 7),
            runCount = 38,
            batteryMode = BatteryMode.BALANCED,
            humanizeConfig = HumanizeConfig(
                enabled = true,
                jitterRadiusPx = 6f,
                timeVariancePercentage = 15f,
                antiBotScore = 98
            ),
            tags = listOf("Planifié", "Quotidien", "Anti-Bot"),
            steps = listOf(
                ActionStep(
                    stepIndex = 1,
                    actionType = ActionType.TAP,
                    x = 540f,
                    y = 960f,
                    pressDurationMs = 90L,
                    delayAfterMs = 2000L,
                    label = "Ouvrir l'onglet Récompense"
                ),
                ActionStep(
                    stepIndex = 2,
                    actionType = ActionType.OCR_TEXT_MATCH,
                    conditionText = "Recevoir",
                    x = 540f,
                    y = 1200f,
                    delayAfterMs = 1500L,
                    label = "Cliquer sur Recevoir"
                ),
                ActionStep(
                    stepIndex = 3,
                    actionType = ActionType.TAP,
                    x = 950f,
                    y = 120f,
                    delayAfterMs = 500L,
                    label = "Fermer la croix de popup"
                )
            )
        )

        // Preset 3: Social Scroller
        val socialScroller = Script(
            id = "preset_social_scroll",
            title = "Défilement Réseaux & Like Furtif",
            description = "Fait défiler un fil d'actualité à vitesse variable et aime périodiquement avec micro-pauses.",
            runCount = 75,
            batteryMode = BatteryMode.ECO,
            humanizeConfig = HumanizeConfig(
                enabled = true,
                jitterRadiusPx = 9f,
                timeVariancePercentage = 25f,
                naturalBezierCurves = true,
                antiBotScore = 99
            ),
            tags = listOf("Social", "Swipe", "Furtif"),
            steps = listOf(
                ActionStep(
                    stepIndex = 1,
                    actionType = ActionType.WAIT,
                    delayAfterMs = 3200L,
                    delayVarianceMs = 600L,
                    label = "Temps de lecture humain"
                ),
                ActionStep(
                    stepIndex = 2,
                    actionType = ActionType.DOUBLE_TAP,
                    x = 540f,
                    y = 900f,
                    delayAfterMs = 1200L,
                    label = "Double tap pour aimer"
                ),
                ActionStep(
                    stepIndex = 3,
                    actionType = ActionType.SWIPE,
                    x = 540f,
                    y = 1600f,
                    endX = 540f,
                    endY = 400f,
                    delayAfterMs = 2500L,
                    delayVarianceMs = 400L,
                    label = "Glissement fluide"
                ),
                ActionStep(
                    stepIndex = 4,
                    actionType = ActionType.LOOP_START,
                    loopCount = 30,
                    label = "Répéter 30 posts"
                )
            )
        )

        saveScript(farmingScript)
        saveScript(dailyCheckin)
        saveScript(socialScroller)
    }

    private fun mapEntityToDomain(entity: ScriptEntity): Script {
        val days = entity.scheduleDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        val tags = entity.tagsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val steps = parseStepsJson(entity.stepsJson, entity.id)

        val batteryMode = try {
            BatteryMode.valueOf(entity.batteryModeName)
        } catch (_: Exception) {
            BatteryMode.BALANCED
        }

        return Script(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            createdAt = entity.createdAt,
            lastRunAt = entity.lastRunAt,
            runCount = entity.runCount,
            isScheduled = entity.isScheduled,
            scheduleTimeMinutes = entity.scheduleTimeMinutes,
            scheduleDays = if (days.isEmpty()) listOf(1, 2, 3, 4, 5) else days,
            isRepeatDaily = entity.isRepeatDaily,
            batteryMode = batteryMode,
            humanizeConfig = HumanizeConfig(
                enabled = entity.humanizeEnabled,
                jitterRadiusPx = entity.jitterRadiusPx,
                timeVariancePercentage = entity.timeVariancePercentage,
                naturalBezierCurves = entity.naturalBezierCurves,
                antiBotScore = entity.antiBotScore
            ),
            steps = steps,
            tags = tags
        )
    }

    private fun mapDomainToEntity(script: Script): ScriptEntity {
        return ScriptEntity(
            id = script.id,
            title = script.title,
            description = script.description,
            createdAt = script.createdAt,
            lastRunAt = script.lastRunAt,
            runCount = script.runCount,
            isScheduled = script.isScheduled,
            scheduleTimeMinutes = script.scheduleTimeMinutes,
            scheduleDaysCsv = script.scheduleDays.joinToString(","),
            isRepeatDaily = script.isRepeatDaily,
            batteryModeName = script.batteryMode.name,
            humanizeEnabled = script.humanizeConfig.enabled,
            jitterRadiusPx = script.humanizeConfig.jitterRadiusPx,
            timeVariancePercentage = script.humanizeConfig.timeVariancePercentage,
            naturalBezierCurves = script.humanizeConfig.naturalBezierCurves,
            antiBotScore = script.humanizeConfig.antiBotScore,
            stepsJson = serializeStepsJson(script.steps),
            tagsCsv = script.tags.joinToString(",")
        )
    }

    private fun serializeStepsJson(steps: List<ActionStep>): String {
        val array = JSONArray()
        for (step in steps) {
            val obj = JSONObject()
            obj.put("id", step.id)
            obj.put("stepIndex", step.stepIndex)
            obj.put("actionType", step.actionType.name)
            obj.put("x", step.x.toDouble())
            obj.put("y", step.y.toDouble())
            obj.put("endX", step.endX.toDouble())
            obj.put("endY", step.endY.toDouble())
            obj.put("inputText", step.inputText)
            obj.put("delayAfterMs", step.delayAfterMs)
            obj.put("delayVarianceMs", step.delayVarianceMs)
            obj.put("pressDurationMs", step.pressDurationMs)
            obj.put("jitterRadiusPx", step.jitterRadiusPx.toDouble())
            obj.put("conditionText", step.conditionText)
            obj.put("conditionPattern", step.conditionPattern)
            obj.put("timeoutMs", step.timeoutMs)
            obj.put("loopCount", step.loopCount)
            obj.put("label", step.label)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseStepsJson(json: String, scriptId: String): List<ActionStep> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<ActionStep>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("actionType", ActionType.TAP.name)
                val actionType = try {
                    ActionType.valueOf(typeName)
                } catch (_: Exception) {
                    ActionType.TAP
                }
                list.add(
                    ActionStep(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        scriptId = scriptId,
                        stepIndex = obj.optInt("stepIndex", i + 1),
                        actionType = actionType,
                        x = obj.optDouble("x", 540.0).toFloat(),
                        y = obj.optDouble("y", 960.0).toFloat(),
                        endX = obj.optDouble("endX", 540.0).toFloat(),
                        endY = obj.optDouble("endY", 400.0).toFloat(),
                        inputText = obj.optString("inputText", ""),
                        delayAfterMs = obj.optLong("delayAfterMs", 1000L),
                        delayVarianceMs = obj.optLong("delayVarianceMs", 100L),
                        pressDurationMs = obj.optLong("pressDurationMs", 85L),
                        jitterRadiusPx = obj.optDouble("jitterRadiusPx", 6.0).toFloat(),
                        conditionText = obj.optString("conditionText", ""),
                        conditionPattern = obj.optString("conditionPattern", ""),
                        timeoutMs = obj.optLong("timeoutMs", 5000L),
                        loopCount = obj.optInt("loopCount", 1),
                        label = obj.optString("label", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
