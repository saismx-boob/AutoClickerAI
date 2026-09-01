package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.HumanizeConfig
import com.example.ui.theme.*

@Composable
fun AppHeader(
    title: String,
    strings: Strings,
    currentLanguage: AppLanguage,
    onLanguageClick: () -> Unit,
    isServiceActive: Boolean,
    onServiceClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder.copy(alpha = 0.4f)),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(NaturalForestGreen, NaturalForestGreenLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = NaturalTextPrimary
                        )
                        Text(
                            text = "Mode Humain Actif • " + strings.noRootNeeded,
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalTextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Switcher Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NaturalOlivePill,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onLanguageClick() }
                            .testTag("language_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = currentLanguage.flag, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLanguage.displayName.take(3).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Accessibility Service Status Badge
                    val serviceBgColor = if (isServiceActive) NaturalSageLight else NaturalAmberLight.copy(alpha = 0.5f)
                    val serviceBorderColor = if (isServiceActive) NaturalSageBorder else NaturalAmberWarm.copy(alpha = 0.5f)
                    val serviceTextColor = if (isServiceActive) NaturalForestGreen else NaturalAmberWarm

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = serviceBgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, serviceBorderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onServiceClick() }
                            .testTag("service_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(serviceTextColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceActive) "Prêt" else "Service OFF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = serviceTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceBanner(
    isServiceActive: Boolean,
    strings: Strings,
    onGrantClick: () -> Unit
) {
    AnimatedVisibility(visible = !isServiceActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("service_permission_banner"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = NaturalOliveLight
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalAmberWarm.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NaturalAmberWarm.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = NaturalAmberWarm
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.accessibilityStatus,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )
                    Text(
                        text = "Activez le service pour permettre les clics et swipes automatisés sans accès root.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NaturalAmberWarm,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("grant_service_button")
                ) {
                    Text(text = strings.grantService, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun HumanizeMeter(
    config: HumanizeConfig,
    strings: Strings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("humanize_meter_card"),
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
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NaturalForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.humanizeTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NaturalSageLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                ) {
                    Text(
                        text = "${config.antiBotScore}% Anti-Bot",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NaturalForestGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.humanizeDesc,
                style = MaterialTheme.typography.bodySmall,
                color = NaturalTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.Grain,
                    label = "Jitter",
                    value = "±${config.jitterRadiusPx.toInt()} px",
                    color = NaturalForestGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.Schedule,
                    label = "Variance",
                    value = "±${config.timeVariancePercentage.toInt()}%",
                    color = NaturalAmberWarm,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.Gesture,
                    label = "Courbes",
                    value = if (config.naturalBezierCurves) "Actif" else "Désactivé",
                    color = NaturalForestGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = NaturalOliveLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = NaturalTextMuted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
fun BatteryProfileSelector(
    selectedMode: BatteryMode,
    onModeSelected: (BatteryMode) -> Unit,
    strings: Strings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battery_profile_card"),
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
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = NaturalForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.batteryTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )
                }

                Text(
                    text = selectedMode.powerLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalForestGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatteryMode.values().forEach { mode ->
                    val isSelected = mode == selectedMode
                    val activeColor = when (mode) {
                        BatteryMode.ECO -> NaturalForestGreen
                        BatteryMode.BALANCED -> NaturalForestGreenLight
                        BatteryMode.TURBO -> NaturalAmberWarm
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModeSelected(mode) }
                            .testTag("battery_mode_${mode.name}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) NaturalSageLight else NaturalOliveLight,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NaturalSageBorder else NaturalOliveBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (mode) {
                                    BatteryMode.ECO -> "Éco"
                                    BatteryMode.BALANCED -> "Équilibré"
                                    BatteryMode.TURBO -> "Turbo"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) NaturalForestGreenDark else NaturalTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
