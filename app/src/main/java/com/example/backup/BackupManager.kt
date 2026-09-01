package com.example.backup

import android.content.Context
import com.example.data.ScriptRepository
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.BackupSnapshot
import com.example.model.BatteryMode
import com.example.model.HumanizeConfig
import com.example.model.LocalBackupFile
import com.example.model.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val repository: ScriptRepository) {

    /**
     * Creates a local JSON backup file saved directly into device internal storage.
     */
    suspend fun createLocalPhoneBackup(
        context: Context,
        note: String = "Sauvegarde locale sur la mémoire du téléphone"
    ): LocalBackupFile = withContext(Dispatchers.IO) {
        val scripts = repository.allScripts.first()
        val json = exportScriptsToJson(scripts)

        val backupDir = File(context.filesDir, "local_backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
        val fileName = "autoclicker_backup_$dateFormatted.json"
        val targetFile = File(backupDir, fileName)
        targetFile.writeText(json)

        // Also record in database snapshot table for unified history
        val snapshot = BackupSnapshot(
            id = "local_$timestamp",
            timestamp = timestamp,
            scriptCount = scripts.size,
            totalRuns = scripts.sumOf { it.runCount },
            jsonContent = json,
            note = "$note (Fichier: $fileName)"
        )
        repository.saveBackup(snapshot)

        LocalBackupFile(
            fileName = fileName,
            filePath = targetFile.absolutePath,
            sizeBytes = targetFile.length(),
            timestamp = timestamp,
            scriptCount = scripts.size,
            note = note
        )
    }

    /**
     * Lists all backup files stored locally on the device filesystem.
     */
    suspend fun listLocalPhoneBackups(context: Context): List<LocalBackupFile> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "local_backups")
        if (!backupDir.exists()) return@withContext emptyList()

        val files = backupDir.listFiles { file -> file.extension == "json" } ?: return@withContext emptyList()
        files.sortedByDescending { it.lastModified() }.map { file ->
            val content = try { file.readText() } catch (_: Exception) { "" }
            val scriptCount = try {
                val root = JSONObject(content)
                root.optJSONArray("scripts")?.length() ?: 0
            } catch (_: Exception) { 0 }

            LocalBackupFile(
                fileName = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                timestamp = file.lastModified(),
                scriptCount = scriptCount,
                note = "Fichier local stocké sur le téléphone"
            )
        }
    }

    /**
     * Restores scripts from a local file path.
     */
    suspend fun restoreFromLocalFile(filePath: String): Int = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext 0
        val content = file.readText()
        val scripts = parseScriptsFromJson(content)
        for (script in scripts) {
            repository.saveScript(script)
        }
        scripts.size
    }

    /**
     * Deletes a local backup file from device storage.
     */
    suspend fun deleteLocalPhoneBackup(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else false
    }

    suspend fun createCloudBackup(note: String = "Sauvegarde Cloud Automatique"): BackupSnapshot = withContext(Dispatchers.IO) {
        val scripts = repository.allScripts.first()
        val totalRuns = scripts.sumOf { it.runCount }
        val json = exportScriptsToJson(scripts)

        val snapshot = BackupSnapshot(
            id = "snap_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            scriptCount = scripts.size,
            totalRuns = totalRuns,
            jsonContent = json,
            note = note
        )

        repository.saveBackup(snapshot)
        snapshot
    }

    suspend fun restoreSnapshot(snapshot: BackupSnapshot): Int = withContext(Dispatchers.IO) {
        val scripts = parseScriptsFromJson(snapshot.jsonContent)
        for (script in scripts) {
            repository.saveScript(script)
        }
        scripts.size
    }

    fun exportScriptsToJson(scripts: List<Script>): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "AutoClicker AI")

        val scriptsArray = JSONArray()
        for (script in scripts) {
            val scriptObj = JSONObject()
            scriptObj.put("id", script.id)
            scriptObj.put("title", script.title)
            scriptObj.put("description", script.description)
            scriptObj.put("createdAt", script.createdAt)
            scriptObj.put("lastRunAt", script.lastRunAt)
            scriptObj.put("runCount", script.runCount)
            scriptObj.put("isScheduled", script.isScheduled)
            scriptObj.put("scheduleTimeMinutes", script.scheduleTimeMinutes)
            scriptObj.put("isRepeatDaily", script.isRepeatDaily)
            scriptObj.put("batteryMode", script.batteryMode.name)

            // Humanize
            val humanObj = JSONObject()
            humanObj.put("enabled", script.humanizeConfig.enabled)
            humanObj.put("jitterRadiusPx", script.humanizeConfig.jitterRadiusPx.toDouble())
            humanObj.put("timeVariancePercentage", script.humanizeConfig.timeVariancePercentage.toDouble())
            humanObj.put("naturalBezierCurves", script.humanizeConfig.naturalBezierCurves)
            humanObj.put("antiBotScore", script.humanizeConfig.antiBotScore)
            scriptObj.put("humanizeConfig", humanObj)

            // Steps
            val stepsArray = JSONArray()
            for (step in script.steps) {
                val stepObj = JSONObject()
                stepObj.put("id", step.id)
                stepObj.put("stepIndex", step.stepIndex)
                stepObj.put("actionType", step.actionType.name)
                stepObj.put("label", step.label)
                stepObj.put("x", step.x.toDouble())
                stepObj.put("y", step.y.toDouble())
                stepObj.put("endX", step.endX.toDouble())
                stepObj.put("endY", step.endY.toDouble())
                stepObj.put("inputText", step.inputText)
                stepObj.put("delayAfterMs", step.delayAfterMs)
                stepObj.put("delayVarianceMs", step.delayVarianceMs)
                stepObj.put("pressDurationMs", step.pressDurationMs)
                stepObj.put("jitterRadiusPx", step.jitterRadiusPx.toDouble())
                stepObj.put("conditionText", step.conditionText)
                stepObj.put("loopCount", step.loopCount)
                stepObj.put("imageTemplateType", step.imageTemplateType)
                stepObj.put("imageTemplateName", step.imageTemplateName)
                stepObj.put("imageConfidenceThreshold", step.imageConfidenceThreshold.toDouble())
                stepObj.put("imageSearchRegion", step.imageSearchRegion)
                stepsArray.put(stepObj)
            }
            scriptObj.put("steps", stepsArray)
            scriptsArray.put(scriptObj)
        }
        root.put("scripts", scriptsArray)
        return root.toString(2)
    }

    fun parseScriptsFromJson(jsonString: String): List<Script> {
        val list = mutableListOf<Script>()
        try {
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("scripts") ?: JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val humanObj = obj.optJSONObject("humanizeConfig")
                val humanConfig = if (humanObj != null) {
                    HumanizeConfig(
                        enabled = humanObj.optBoolean("enabled", true),
                        jitterRadiusPx = humanObj.optDouble("jitterRadiusPx", 6.0).toFloat(),
                        timeVariancePercentage = humanObj.optDouble("timeVariancePercentage", 15.0).toFloat(),
                        naturalBezierCurves = humanObj.optBoolean("naturalBezierCurves", true),
                        antiBotScore = humanObj.optInt("antiBotScore", 98)
                    )
                } else HumanizeConfig()

                val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
                val steps = mutableListOf<ActionStep>()
                for (j in 0 until stepsArray.length()) {
                    val sObj = stepsArray.getJSONObject(j)
                    val actionType = try {
                        ActionType.valueOf(sObj.optString("actionType", "TAP"))
                    } catch (_: Exception) { ActionType.TAP }

                    steps.add(
                        ActionStep(
                            id = sObj.optString("id", java.util.UUID.randomUUID().toString()),
                            stepIndex = sObj.optInt("stepIndex", j + 1),
                            actionType = actionType,
                            label = sObj.optString("label", ""),
                            x = sObj.optDouble("x", 540.0).toFloat(),
                            y = sObj.optDouble("y", 960.0).toFloat(),
                            endX = sObj.optDouble("endX", 540.0).toFloat(),
                            endY = sObj.optDouble("endY", 400.0).toFloat(),
                            inputText = sObj.optString("inputText", ""),
                            delayAfterMs = sObj.optLong("delayAfterMs", 1000L),
                            delayVarianceMs = sObj.optLong("delayVarianceMs", 120L),
                            pressDurationMs = sObj.optLong("pressDurationMs", 85L),
                            jitterRadiusPx = sObj.optDouble("jitterRadiusPx", 6.0).toFloat(),
                            conditionText = sObj.optString("conditionText", ""),
                            loopCount = sObj.optInt("loopCount", 1),
                            imageTemplateType = sObj.optString("imageTemplateType", "CHEST_REWARD"),
                            imageTemplateName = sObj.optString("imageTemplateName", "Coffre au Trésor"),
                            imageConfidenceThreshold = sObj.optDouble("imageConfidenceThreshold", 0.75).toFloat(),
                            imageSearchRegion = sObj.optString("imageSearchRegion", "FULL_SCREEN")
                        )
                    )
                }

                val mode = try {
                    BatteryMode.valueOf(obj.optString("batteryMode", "BALANCED"))
                } catch (_: Exception) { BatteryMode.BALANCED }

                list.add(
                    Script(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "Script Importé"),
                        description = obj.optString("description", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastRunAt = obj.optLong("lastRunAt", 0L),
                        runCount = obj.optInt("runCount", 0),
                        isScheduled = obj.optBoolean("isScheduled", false),
                        scheduleTimeMinutes = obj.optInt("scheduleTimeMinutes", 480),
                        batteryMode = mode,
                        humanizeConfig = humanConfig,
                        steps = steps
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
