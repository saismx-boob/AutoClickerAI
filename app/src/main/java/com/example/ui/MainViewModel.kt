package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.data.AppDatabase
import com.example.data.ScriptRepository
import com.example.localization.AppLanguage
import com.example.localization.LocalizationProvider
import com.example.model.BackupSnapshot
import com.example.model.Script
import com.example.service.AutoClickerAccessibilityService
import com.example.service.ClickerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavDestination {
    DASHBOARD,
    AI_ASSISTANT,
    SANDBOX,
    SCHEDULER,
    BACKUP,
    SETTINGS,
    EDITOR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = ScriptRepository(database.scriptDao())
    val backupManager = BackupManager(repository)
    val engine = ClickerEngine(viewModelScope)

    val allScripts = repository.allScripts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allBackups = repository.allBackups.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val isServiceActive = AutoClickerAccessibilityService.isServiceBound

    private val _localBackupFiles = MutableStateFlow<List<com.example.model.LocalBackupFile>>(emptyList())
    val localBackupFiles = _localBackupFiles.asStateFlow()

    private val _currentDestination = MutableStateFlow(AppNavDestination.DASHBOARD)
    val currentDestination = _currentDestination.asStateFlow()

    private val _scriptBeingEdited = MutableStateFlow<Script?>(null)
    val scriptBeingEdited = _scriptBeingEdited.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.FRENCH)
    val currentLanguage = _currentLanguage.asStateFlow()

    private val _isAmoledMode = MutableStateFlow(false)
    val isAmoledMode = _isAmoledMode.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled = _isHapticEnabled.asStateFlow()

    val currentStrings = MutableStateFlow(LocalizationProvider.getStrings(AppLanguage.FRENCH))

    init {
        viewModelScope.launch {
            repository.seedSampleScriptsIfEmpty()
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        currentStrings.value = LocalizationProvider.getStrings(language)
    }

    fun toggleAmoled(enabled: Boolean) {
        _isAmoledMode.value = enabled
    }

    fun toggleHaptic(enabled: Boolean) {
        _isHapticEnabled.value = enabled
    }

    fun navigateTo(destination: AppNavDestination) {
        _currentDestination.value = destination
    }

    fun openNewScriptEditor() {
        _scriptBeingEdited.value = null
        _currentDestination.value = AppNavDestination.EDITOR
    }

    fun openEditScript(script: Script) {
        _scriptBeingEdited.value = script
        _currentDestination.value = AppNavDestination.EDITOR
    }

    fun saveScript(script: Script) {
        viewModelScope.launch {
            repository.saveScript(script)
            _currentDestination.value = AppNavDestination.DASHBOARD
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            repository.deleteScript(script.id)
        }
    }

    fun duplicateScript(script: Script) {
        viewModelScope.launch {
            val copy = script.copy(
                id = java.util.UUID.randomUUID().toString(),
                title = "${script.title} (Copie)",
                createdAt = System.currentTimeMillis(),
                runCount = 0
            )
            repository.saveScript(copy)
        }
    }

    fun runScript(script: Script) {
        if (engine.playbackState.value == com.example.model.PlaybackState.RUNNING && engine.currentScript.value?.id == script.id) {
            engine.stop()
        } else {
            engine.startScript(script) {
                viewModelScope.launch {
                    repository.recordScriptRun(script.id)
                }
            }
        }
    }

    fun toggleSchedule(script: Script, isScheduled: Boolean) {
        viewModelScope.launch {
            repository.saveScript(script.copy(isScheduled = isScheduled))
        }
    }

    fun updateScheduleTime(script: Script, timeMinutes: Int, days: List<Int>) {
        viewModelScope.launch {
            repository.saveScript(
                script.copy(
                    scheduleTimeMinutes = timeMinutes,
                    scheduleDays = days,
                    isScheduled = true
                )
            )
        }
    }

    fun createCloudBackup() {
        viewModelScope.launch {
            backupManager.createCloudBackup()
        }
    }

    fun refreshLocalBackups() {
        viewModelScope.launch {
            _localBackupFiles.value = backupManager.listLocalPhoneBackups(getApplication())
        }
    }

    fun createLocalBackup(note: String = "Sauvegarde locale sur le téléphone") {
        createLocalPhoneBackup(note)
    }

    fun restoreLocalBackup(filePath: String, onDone: (() -> Unit)? = null) {
        restoreLocalPhoneBackup(filePath, onDone)
    }

    fun deleteLocalBackup(filePath: String) {
        deleteLocalPhoneBackup(filePath)
    }

    fun createLocalPhoneBackup(note: String = "Sauvegarde locale sur le téléphone") {
        viewModelScope.launch {
            backupManager.createLocalPhoneBackup(getApplication(), note)
            refreshLocalBackups()
        }
    }

    fun restoreLocalPhoneBackup(filePath: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            backupManager.restoreFromLocalFile(filePath)
            onDone?.invoke()
        }
    }

    fun deleteLocalPhoneBackup(filePath: String) {
        viewModelScope.launch {
            backupManager.deleteLocalPhoneBackup(filePath)
            refreshLocalBackups()
        }
    }

    fun restoreBackup(snapshot: BackupSnapshot) {
        viewModelScope.launch {
            backupManager.restoreSnapshot(snapshot)
        }
    }

    fun deleteBackup(snapshot: BackupSnapshot) {
        viewModelScope.launch {
            repository.deleteBackup(snapshot.id)
        }
    }

    fun exportScriptsJson(): String {
        return backupManager.exportScriptsToJson(allScripts.value)
    }

    fun importScriptsJson(json: String) {
        viewModelScope.launch {
            val list = backupManager.parseScriptsFromJson(json)
            for (script in list) {
                repository.saveScript(script)
            }
        }
    }
}
