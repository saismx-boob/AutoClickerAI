package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    strings: Strings,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    isAmoledMode: Boolean,
    onToggleAmoled: (Boolean) -> Unit,
    isHapticEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    isServiceActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {
    var showLanguageModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // App Identity & No-root Engine Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalSageLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = NaturalForestGreen)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Auto Clicker AI Pro",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalTextPrimary
                            )
                            Text(
                                text = "Version 2.4.0 • Moteur Sans Root • Jetpack Compose",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalForestGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Fonctionne grâce aux APIs d'accessibilité natives d'Android (dispatchGesture) sans nécessiter de droits Super-Utilisateur / Root.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )
                }
            }
        }

        // Section General
        item {
            Text(
                text = "Général & Langue",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showLanguageModal = true }
                    .testTag("settings_language_item"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NaturalAmberWarm)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = strings.language, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = NaturalTextPrimary)
                            Text(text = "Changer la langue de l'interface", style = MaterialTheme.typography.labelSmall, color = NaturalTextMuted)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${currentLanguage.flag} ${currentLanguage.displayName}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NaturalAmberWarm)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NaturalTextMuted)
                    }
                }
            }
        }

        // Display & Theme
        item {
            Text(
                text = "Affichage & Économie d'Énergie",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
        }

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
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = NaturalForestGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Mode Éco / Sombre Naturel", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = NaturalTextPrimary)
                                Text(text = "Économise la batterie et adoucit l'affichage", style = MaterialTheme.typography.labelSmall, color = NaturalTextMuted)
                            }
                        }

                        Switch(
                            checked = isAmoledMode,
                            onCheckedChange = onToggleAmoled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalForestGreen,
                                checkedTrackColor = NaturalSageLight,
                                uncheckedThumbColor = NaturalTextMuted,
                                uncheckedTrackColor = NaturalOliveLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NaturalCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = NaturalAmberWarm)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Retour Haptique (Vibration)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = NaturalTextPrimary)
                                Text(text = "Vibration subtile à chaque déclenchement", style = MaterialTheme.typography.labelSmall, color = NaturalTextMuted)
                            }
                        }

                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = onToggleHaptic,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaturalAmberWarm,
                                checkedTrackColor = NaturalSageLight,
                                uncheckedThumbColor = NaturalTextMuted,
                                uncheckedTrackColor = NaturalOliveLight
                            )
                        )
                    }
                }
            }
        }

        // Accessibility Service Info & Action
        item {
            Text(
                text = "Permissions & Système",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
        }

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
                            Icon(Icons.Default.Accessibility, contentDescription = null, tint = NaturalForestGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Service d'Accessibilité", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = NaturalTextPrimary)
                                Text(
                                    text = if (isServiceActive) "État : Connecté et opérationnel" else "État : Désactivé",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isServiceActive) NaturalForestGreen else NaturalAmberWarm
                                )
                            }
                        }

                        Button(
                            onClick = onOpenAccessibilitySettings,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServiceActive) NaturalOliveLight else NaturalForestGreen,
                                contentColor = if (isServiceActive) NaturalTextPrimary else Color.White
                            )
                        ) {
                            Text(text = if (isServiceActive) "Gérer" else "Activer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Language Selector Dialog
    if (showLanguageModal) {
        AlertDialog(
            onDismissRequest = { showLanguageModal = false },
            title = {
                Text(text = strings.selectLanguage, fontWeight = FontWeight.Bold, color = NaturalTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onLanguageSelected(lang)
                                    showLanguageModal = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NaturalSageLight else NaturalOliveLight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NaturalSageBorder else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = lang.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) NaturalForestGreen else NaturalTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NaturalForestGreen)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageModal = false }) {
                    Text("Fermer", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }
}
