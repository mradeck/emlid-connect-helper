package de.emlidhelper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.emlidhelper.app.data.BluetoothStatus
import de.emlidhelper.app.data.CheckSeverity
import de.emlidhelper.app.data.HelperUiState
import de.emlidhelper.app.data.ResetConfig
import de.emlidhelper.app.data.ResetPhase
import de.emlidhelper.app.data.ResetProgress
import de.emlidhelper.app.data.ShizukuState
import de.emlidhelper.app.data.WifiNetworkStatus
import de.emlidhelper.app.ui.theme.StatusError
import de.emlidhelper.app.ui.theme.StatusErrorContainerDark
import de.emlidhelper.app.ui.theme.StatusErrorContainerLight
import de.emlidhelper.app.ui.theme.StatusInfo
import de.emlidhelper.app.ui.theme.StatusOk
import de.emlidhelper.app.ui.theme.StatusOkContainerDark
import de.emlidhelper.app.ui.theme.StatusOkContainerLight
import de.emlidhelper.app.ui.theme.StatusWarn
import de.emlidhelper.app.ui.theme.StatusWarnContainerDark
import de.emlidhelper.app.ui.theme.StatusWarnContainerLight

@Composable
fun DashboardScreen(
    state: HelperUiState,
    onRefresh: () -> Unit,
    onStartReset: () -> Unit,
    onCancelReset: () -> Unit,
    onConfirmManualStep: () -> Unit,
    onDismissResetResult: () -> Unit,
    onUpdateResetConfig: ((ResetConfig) -> ResetConfig) -> Unit,
    onQuickResetBluetooth: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenAirplaneSettings: () -> Unit,
    onLaunchEmlidFlow: () -> Unit,
    onRequestPermissions: () -> Unit,
    onGoToAutomationTab: () -> Unit,
    onGoToSamsungTab: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val errorCount = state.samsungChecks.count { it.severity == CheckSeverity.ERROR }
    val warnCount = state.samsungChecks.count { it.severity == CheckSeverity.WARNING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Permission banner if BT or Location missing
        if (!state.automation.hasBluetoothPermission || !state.automation.hasLocationPermission) {
            item {
                PermissionWarningCard(onRequestPermissions = onRequestPermissions)
            }
        }

        // 2. Overall Readiness Banner
        item {
            ReadinessBannerCard(
                bluetooth = state.bluetooth,
                wifi = state.wifi,
                errorCount = errorCount,
                warnCount = warnCount,
                lastRefresh = state.lastRefreshTime,
                isDark = isDark,
                onRefresh = onRefresh,
                onGoToSamsungTab = onGoToSamsungTab
            )
        }

        // 3. Main 1-Tap Reset Action Card (or active Reset Progress)
        item {
            ResetControlCard(
                state = state,
                isDark = isDark,
                onStartReset = onStartReset,
                onCancelReset = onCancelReset,
                onConfirmManualStep = onConfirmManualStep,
                onDismissResetResult = onDismissResetResult,
                onUpdateResetConfig = onUpdateResetConfig,
                onOpenAirplaneSettings = onOpenAirplaneSettings,
                onLaunchEmlidFlow = onLaunchEmlidFlow,
                onGoToAutomationTab = onGoToAutomationTab
            )
        }

        // 4. Bluetooth Monitor Card
        item {
            BluetoothMonitorCard(
                bluetooth = state.bluetooth,
                shizukuReady = state.automation.shizukuState == ShizukuState.READY,
                isDark = isDark,
                onQuickResetBluetooth = onQuickResetBluetooth,
                onOpenBluetoothSettings = onOpenBluetoothSettings
            )
        }

        // 5. Wi-Fi & Network Monitor Card
        item {
            WifiMonitorCard(
                wifi = state.wifi,
                isDark = isDark,
                onOpenWifiSettings = onOpenWifiSettings,
                onOpenVpnSettings = onOpenVpnSettings,
                onRequestPermissions = onRequestPermissions
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PermissionWarningCard(onRequestPermissions: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Berechtigungen fehlen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Für Bluetooth-Geräte & WLAN-Namen (SSID) benötigt die App Bluetooth- und Standortfreigabe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRequestPermissions) {
                Text("Freigeben")
            }
        }
    }
}

@Composable
private fun ReadinessBannerCard(
    bluetooth: BluetoothStatus,
    wifi: WifiNetworkStatus,
    errorCount: Int,
    warnCount: Int,
    lastRefresh: String,
    isDark: Boolean,
    onRefresh: () -> Unit,
    onGoToSamsungTab: () -> Unit
) {
    val allGood = bluetooth.isChannelClear && !wifi.isVpnActive && errorCount == 0
    val bgColor = when {
        allGood -> if (isDark) StatusOkContainerDark else StatusOkContainerLight
        errorCount > 0 || wifi.isVpnActive -> if (isDark) StatusErrorContainerDark else StatusErrorContainerLight
        else -> if (isDark) StatusWarnContainerDark else StatusWarnContainerLight
    }
    val accent = when {
        allGood -> StatusOk
        errorCount > 0 || wifi.isVpnActive -> StatusError
        else -> StatusWarn
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when {
                        allGood -> Icons.Default.CheckCircle
                        errorCount > 0 || wifi.isVpnActive -> Icons.Default.Error
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            allGood -> "Bereit für Emlid RS4 Pro"
                            bluetooth.isAirplaneModeOn -> "Flugmodus ist aktiv"
                            errorCount > 0 -> "$errorCount Samsung-Problem(e) erkannt"
                            bluetooth.connectedDevices.isNotEmpty() -> "${bluetooth.connectedDevices.size} BT-Gerät(e) noch verbunden"
                            else -> "Hinweis prüfen ($warnCount)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stand: $lastRefresh · ${if (wifi.isEmlidHotspot) "Reach-Hotspot aktiv" else wifi.ssid ?: wifi.activeTransportLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                }
            }

            if (errorCount > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onGoToSamsungTab,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Zur Samsung-Checkliste ($errorCount offen) →", color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResetControlCard(
    state: HelperUiState,
    isDark: Boolean,
    onStartReset: () -> Unit,
    onCancelReset: () -> Unit,
    onConfirmManualStep: () -> Unit,
    onDismissResetResult: () -> Unit,
    onUpdateResetConfig: ((ResetConfig) -> ResetConfig) -> Unit,
    onOpenAirplaneSettings: () -> Unit,
    onLaunchEmlidFlow: () -> Unit,
    onGoToAutomationTab: () -> Unit
) {
    val progress = state.resetProgress
    val config = state.resetConfig
    val shizukuReady = state.automation.shizukuState == ShizukuState.READY
    val accessReady = state.automation.accessibilityEnabled
    var showOptions by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "1-Tap Emlid Reset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            shizukuReady && accessReady -> "Vollautomatik aktiv (Shizuku + Auto-Cache)"
                            shizukuReady -> "Shizuku aktiv (Cache manuell / Auto-Cache optional)"
                            accessReady -> "Auto-Cache aktiv (Flugmodus mit geführtem Timer)"
                            else -> "Geführter Modus (Fallback – Variante A)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shizukuReady && accessReady) StatusOk else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showOptions = !showOptions }) {
                    Icon(Icons.Default.Settings, contentDescription = "Optionen")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Active reset progress OR Completed banner OR Main Button
            if (progress.isRunning) {
                ActiveResetProgressBlock(
                    progress = progress,
                    onCancelReset = onCancelReset,
                    onConfirmManualStep = onConfirmManualStep,
                    onOpenAirplaneSettings = onOpenAirplaneSettings
                )
            } else {
                if (progress.phase == ResetPhase.COMPLETED || progress.phase == ResetPhase.FAILED) {
                    val ok = progress.phase == ResetPhase.COMPLETED
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (ok) {
                                if (isDark) StatusOkContainerDark else StatusOkContainerLight
                            } else {
                                if (isDark) StatusErrorContainerDark else StatusErrorContainerLight
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (ok) StatusOk else StatusError
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(progress.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(progress.detail, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = onDismissResetResult) {
                                Icon(Icons.Default.Close, contentDescription = "Schließen", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Wait duration chips
                Text(
                    "Flugmodus-Wartezeit:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 20, 30).forEach { sec ->
                        FilterChip(
                            selected = config.airplaneWaitSeconds == sec,
                            onClick = { onUpdateResetConfig { it.copy(airplaneWaitSeconds = sec) } },
                            label = { Text("${sec} Sek.") }
                        )
                    }
                }

                if (showOptions) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    ResetOptionSwitch(
                        label = "Emlid Flow zwangsbeenden",
                        checked = config.forceStopEmlid,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(forceStopEmlid = v) } }
                    )
                    ResetOptionSwitch(
                        label = "Nur Cache leeren (Daten bleiben erhalten)",
                        checked = config.clearCache,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(clearCache = v) } }
                    )
                    ResetOptionSwitch(
                        label = "Flugmodus aus/ein (BT & WLAN Reset)",
                        checked = config.toggleAirplaneMode,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(toggleAirplaneMode = v) } }
                    )
                    ResetOptionSwitch(
                        label = "Danach Emlid Flow starten",
                        checked = config.launchEmlidAfter,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(launchEmlidAfter = v) } }
                    )
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onStartReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AirplanemodeActive, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Emlid Reset & Verbinden starten",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaunchEmlidFlow,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Emlid Flow")
                    }
                    if (!shizukuReady || !accessReady) {
                        OutlinedButton(
                            onClick = onGoToAutomationTab,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Vollautomatik einrichten")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveResetProgressBlock(
    progress: ResetProgress,
    onCancelReset: () -> Unit,
    onConfirmManualStep: () -> Unit,
    onOpenAirplaneSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (progress.phase == ResetPhase.AIRPLANE_COUNTDOWN && progress.countdownTotalSeconds > 0) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = {
                            progress.countdownRemainingSeconds.toFloat() / progress.countdownTotalSeconds.toFloat()
                        },
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        "${progress.countdownRemainingSeconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Schritt ${progress.currentStep} von ${progress.totalSteps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    progress.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    progress.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = {
                if (progress.totalSteps > 0) {
                    (progress.currentStep.toFloat() / progress.totalSteps.toFloat()).coerceIn(0f, 1f)
                } else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (progress.phase == ResetPhase.WAITING_MANUAL_CACHE) {
                Button(onClick = onConfirmManualStep, modifier = Modifier.weight(1f)) {
                    Text("Cache geleert – Weiter")
                }
            }
            if (progress.phase == ResetPhase.WAITING_MANUAL_AIRPLANE_ON ||
                progress.phase == ResetPhase.WAITING_MANUAL_AIRPLANE_OFF
            ) {
                OutlinedButton(onClick = onOpenAirplaneSettings, modifier = Modifier.weight(1f)) {
                    Text("Flugmodus-Menü")
                }
            }
            OutlinedButton(
                onClick = onCancelReset,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
            ) {
                Text("Abbrechen")
            }
        }
    }
}

@Composable
private fun ResetOptionSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BluetoothMonitorCard(
    bluetooth: BluetoothStatus,
    shizukuReady: Boolean,
    isDark: Boolean,
    onQuickResetBluetooth: () -> Unit,
    onOpenBluetoothSettings: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = when {
                        !bluetooth.isEnabled -> Icons.Default.BluetoothDisabled
                        bluetooth.connectedDevices.isNotEmpty() -> Icons.Default.BluetoothConnected
                        else -> Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = when {
                        !bluetooth.isEnabled -> StatusWarn
                        bluetooth.connectedDevices.isEmpty() -> StatusOk
                        else -> StatusInfo
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Bluetooth-Verbindungen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            bluetooth.isAirplaneModeOn -> "Flugmodus aktiv"
                            !bluetooth.isEnabled -> "Bluetooth ist ausgeschaltet"
                            !bluetooth.hasPermission -> "Bluetooth-Berechtigung erforderlich"
                            bluetooth.connectedDevices.isEmpty() -> "Keine Geräte verbunden – Kanal ist frei"
                            else -> "${bluetooth.connectedDevices.size} Gerät(e) aktiv verbunden"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            bluetooth.isChannelClear -> StatusOk
                            !bluetooth.isEnabled -> StatusWarn
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (bluetooth.shizukuEnhanced) {
                    StatusPill(text = "Deep-Scan", color = StatusInfo)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Warning if Emlid is mistakenly paired in Android BT settings
            if (bluetooth.pairedEmlidDevices.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) StatusErrorContainerDark else StatusErrorContainerLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Emlid in Android-BT gekoppelt!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatusError
                            )
                            Text(
                                "${bluetooth.pairedEmlidDevices.joinToString { it.name }} sollte in den Android-Einstellungen entkoppelt und nur in Emlid Flow verbunden werden.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Connected devices list OR clean green badge
            if (bluetooth.isEnabled && bluetooth.hasPermission) {
                if (bluetooth.connectedDevices.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) StatusOkContainerDark else StatusOkContainerLight)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOk, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Alle Bluetooth-Geräte getrennt – nichts mehr verbunden.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bluetooth.connectedDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (device.isEmlidRelated) StatusOk else StatusInfo)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        device.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${device.connectionType} · ${device.address}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (device.isEmlidRelated) {
                                    StatusPill(text = "Emlid", color = StatusOk)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onQuickResetBluetooth,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (shizukuReady) "BT trennen & resetten" else "BT-Einstellungen")
                }
                if (shizukuReady) {
                    OutlinedButton(
                        onClick = onOpenBluetoothSettings,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("System-BT")
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiMonitorCard(
    wifi: WifiNetworkStatus,
    isDark: Boolean,
    onOpenWifiSettings: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (wifi.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = when {
                        wifi.isEmlidHotspot -> StatusOk
                        wifi.isConnected -> StatusInfo
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "WLAN & Netzwerk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            !wifi.isWifiEnabled -> "WLAN ist ausgeschaltet"
                            !wifi.isConnected -> "Mit keinem WLAN verbunden (${wifi.activeTransportLabel})"
                            wifi.ssid != null -> "Verbunden mit: ${wifi.ssid}"
                            else -> "Mit WLAN verbunden"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wifi.isEmlidHotspot) StatusOk else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (wifi.isEmlidHotspot) {
                    StatusPill(text = "Reach-Hotspot", color = StatusOk)
                }
            }

            if (wifi.ssidUnavailableReason != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onRequestPermissions,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(wifi.ssidUnavailableReason, style = MaterialTheme.typography.bodySmall, color = StatusWarn)
                }
            }

            if (wifi.isConnected) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoRow("WLAN-Name (SSID)", wifi.ssid ?: "Unbekannt")
                    InfoRow("Geräte-IP", wifi.ipAddress ?: "–")
                    InfoRow("Gateway / Empfänger-IP", wifi.gatewayAddress ?: "–")
                    InfoRow(
                        "Internet-Status",
                        if (wifi.hasValidatedInternet) "Mit Internet" else "Ohne Internet (typisch für Reach-Hotspot)"
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // VPN Status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (wifi.isVpnActive) {
                            if (isDark) StatusErrorContainerDark else StatusErrorContainerLight
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (wifi.isVpnActive) Icons.Default.VpnKey else Icons.Default.Router,
                    contentDescription = null,
                    tint = if (wifi.isVpnActive) StatusError else StatusOk,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (wifi.isVpnActive) "VPN ist AKTIV (kann RS4 Pro blockieren!)" else "Kein VPN aktiv",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (wifi.isVpnActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (wifi.isVpnActive) {
                    TextButton(onClick = onOpenVpnSettings) {
                        Text("Trennen", color = StatusError)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenWifiSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("WLAN-Einstellungen öffnen")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
