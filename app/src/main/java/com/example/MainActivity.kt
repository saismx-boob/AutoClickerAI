package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Strings
import com.example.ui.AppNavDestination
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.screens.*
import com.example.ui.theme.*

data class NavItem(
    val destination: AppNavDestination,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val strings by viewModel.currentStrings.collectAsState()
            val isAmoledMode by viewModel.isAmoledMode.collectAsState()
            val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
            val currentDestination by viewModel.currentDestination.collectAsState()
            val isServiceActive by viewModel.isServiceActive.collectAsState()
            val scripts by viewModel.allScripts.collectAsState()
            val backups by viewModel.allBackups.collectAsState()
            val localBackupFiles by viewModel.localBackupFiles.collectAsState()
            val scriptBeingEdited by viewModel.scriptBeingEdited.collectAsState()

            AutoClickerTheme(isAmoledMode = isAmoledMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            if (currentDestination != AppNavDestination.EDITOR) {
                                AppHeader(
                                    title = strings.appTitle,
                                    strings = strings,
                                    currentLanguage = currentLanguage,
                                    onLanguageClick = {
                                        viewModel.navigateTo(AppNavDestination.SETTINGS)
                                    },
                                    isServiceActive = isServiceActive,
                                    onServiceClick = {
                                        openAccessibilitySettings()
                                    }
                                )
                            }
                        },
                        bottomBar = {
                            if (currentDestination != AppNavDestination.EDITOR) {
                                BottomNavigationBar(
                                    currentDestination = currentDestination,
                                    strings = strings,
                                    onSelectDestination = { dest ->
                                        viewModel.navigateTo(dest)
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Crossfade(targetState = currentDestination, label = "screen_transition") { dest ->
                                when (dest) {
                                    AppNavDestination.DASHBOARD -> DashboardScreen(
                                        scripts = scripts,
                                        strings = strings,
                                        isServiceActive = isServiceActive,
                                        onGrantService = { openAccessibilitySettings() },
                                        onNewScript = { viewModel.openNewScriptEditor() },
                                        onEditScript = { script -> viewModel.openEditScript(script) },
                                        onDeleteScript = { script -> viewModel.deleteScript(script) },
                                        onDuplicateScript = { script -> viewModel.duplicateScript(script) },
                                        onRunScript = { script -> viewModel.runScript(script) },
                                        onOpenAi = { viewModel.navigateTo(AppNavDestination.AI_ASSISTANT) },
                                        onOpenSandbox = { viewModel.navigateTo(AppNavDestination.SANDBOX) },
                                        engine = viewModel.engine
                                    )

                                    AppNavDestination.AI_ASSISTANT -> AiAssistantScreen(
                                        strings = strings,
                                        existingScripts = scripts,
                                        onSaveGeneratedScript = { generated ->
                                            viewModel.saveScript(generated)
                                        },
                                        onTestInSandbox = { script ->
                                            viewModel.navigateTo(AppNavDestination.SANDBOX)
                                        }
                                    )

                                    AppNavDestination.SANDBOX -> LiveSandboxScreen(
                                        scripts = scripts,
                                        strings = strings,
                                        engine = viewModel.engine,
                                        onSaveRecordedScript = { recordedScript ->
                                            viewModel.saveScript(recordedScript)
                                        }
                                    )

                                    AppNavDestination.SCHEDULER -> SchedulerScreen(
                                        scripts = scripts,
                                        strings = strings,
                                        onToggleSchedule = { script, isScheduled ->
                                            viewModel.toggleSchedule(script, isScheduled)
                                        },
                                        onUpdateScheduleTime = { script, time, days ->
                                            viewModel.updateScheduleTime(script, time, days)
                                        }
                                    )

                                    AppNavDestination.BACKUP -> BackupScreen(
                                        backups = backups,
                                        localBackupFiles = localBackupFiles,
                                        strings = strings,
                                        onCreateBackup = { viewModel.createCloudBackup() },
                                        onCreateLocalBackup = { note -> viewModel.createLocalBackup(note) },
                                        onRestoreLocalBackup = { path -> viewModel.restoreLocalBackup(path) },
                                        onDeleteLocalBackup = { path -> viewModel.deleteLocalBackup(path) },
                                        onRefreshLocalBackups = { viewModel.refreshLocalBackups() },
                                        onRestoreBackup = { snapshot -> viewModel.restoreBackup(snapshot) },
                                        onDeleteBackup = { snapshot -> viewModel.deleteBackup(snapshot) },
                                        onExportJson = { viewModel.exportScriptsJson() },
                                        onImportJson = { json -> viewModel.importScriptsJson(json) }
                                    )

                                    AppNavDestination.SETTINGS -> SettingsScreen(
                                        strings = strings,
                                        currentLanguage = currentLanguage,
                                        onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                                        isAmoledMode = isAmoledMode,
                                        onToggleAmoled = { viewModel.toggleAmoled(it) },
                                        isHapticEnabled = isHapticEnabled,
                                        onToggleHaptic = { viewModel.toggleHaptic(it) },
                                        isServiceActive = isServiceActive,
                                        onOpenAccessibilitySettings = { openAccessibilitySettings() }
                                    )

                                    AppNavDestination.EDITOR -> ScriptEditorScreen(
                                        initialScript = scriptBeingEdited,
                                        strings = strings,
                                        onSaveScript = { script -> viewModel.saveScript(script) },
                                        onNavigateBack = { viewModel.navigateTo(AppNavDestination.DASHBOARD) },
                                        onTestInSandbox = { script ->
                                            viewModel.navigateTo(AppNavDestination.SANDBOX)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun BottomNavigationBar(
    currentDestination: AppNavDestination,
    strings: Strings,
    onSelectDestination: (AppNavDestination) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("main_bottom_nav")
        ) {
            val items = listOf(
                NavItem(AppNavDestination.DASHBOARD, strings.tabDashboard, Icons.Default.Home),
                NavItem(AppNavDestination.AI_ASSISTANT, strings.tabAi, Icons.Default.AutoAwesome),
                NavItem(AppNavDestination.SANDBOX, strings.tabSandbox, Icons.Default.SmartDisplay),
                NavItem(AppNavDestination.SCHEDULER, strings.tabScheduler, Icons.Default.Alarm),
                NavItem(AppNavDestination.BACKUP, strings.tabBackup, Icons.Default.CloudSync),
                NavItem(AppNavDestination.SETTINGS, strings.tabSettings, Icons.Default.Settings)
            )

            items.forEach { item ->
                val isSelected = currentDestination == item.destination
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectDestination(item.destination) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) NaturalForestGreen else NaturalTextMuted
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NaturalForestGreen else NaturalTextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = NaturalSageLight,
                        selectedIconColor = NaturalForestGreen,
                        unselectedIconColor = NaturalTextMuted
                    ),
                    modifier = Modifier.testTag("nav_item_${item.destination.name.lowercase()}")
                )
            }
        }
    }
}
