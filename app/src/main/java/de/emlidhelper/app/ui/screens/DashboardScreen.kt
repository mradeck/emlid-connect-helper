package de.emlidhelper.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
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
import de.emlidhelper.app.i18n.AppStrings
import de.emlidhelper.app.ui.components.AppFooter
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
    strings: AppStrings,
    isDark: Boolean,
    onRefresh: () -> Unit,
    onStartReset: () -> Unit,
    onCancelReset: () -> Unit,
    onConfirmManualStep: () -> Unit,
    onDismissResetResult: () -> Unit,
    onUpdateResetConfig: ((ResetConfig) -> ResetConfig) -> Unit,
    onQuickResetBluetooth: () -> Unit,
    onBrowseReceiverIp: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenAirplaneSettings: () -> Unit,
    onLaunchEmlidFlow: () -> Unit,
    onRequestPermissions: () -> Unit,
    onGoToAutomationTab: () -> Unit,
    onGoToSamsungTab: () -> Unit
) {
    val errorCount = state.samsungChecks.count { it.severity == CheckSeverity.ERROR }
    val warnCount = state.samsungChecks.count { it.severity == CheckSeverity.WARNING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!state.automation.hasBluetoothPermission || !state.automation.hasLocationPermission) {
            item {
                PermissionWarningCard(strings = strings, onRequestPermissions = onRequestPermissions)
            }
        }

        item {
            ReadinessBannerCard(
                bluetooth = state.bluetooth,
                wifi = state.wifi,
                strings = strings,
                errorCount = errorCount,
                warnCount = warnCount,
                lastRefresh = state.lastRefreshTime,
                isDark = isDark,
                onRefresh = onRefresh,
                onGoToSamsungTab = onGoToSamsungTab
            )
        }

        item {
            ResetControlCard(
                state = state,
                strings = strings,
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

        item {
            BluetoothMonitorCard(
                bluetooth = state.bluetooth,
                strings = strings,
                shizukuReady = state.automation.shizukuState == ShizukuState.READY,
                isDark = isDark,
                onQuickResetBluetooth = onQuickResetBluetooth,
                onOpenBluetoothSettings = onOpenBluetoothSettings
            )
        }

        item {
            WifiMonitorCard(
                wifi = state.wifi,
                strings = strings,
                isDark = isDark,
                onBrowseReceiverIp = onBrowseReceiverIp,
                onOpenWifiSettings = onOpenWifiSettings,
                onOpenVpnSettings = onOpenVpnSettings,
                onRequestPermissions = onRequestPermissions
            )
        }

        item {
            AppFooter(strings = strings, language = state.language)
        }
    }
}

@Composable
private fun PermissionWarningCard(
    strings: AppStrings,
    onRequestPermissions: () -> Unit
) {
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
                    strings.permissionsMissingTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    strings.permissionsMissingBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRequestPermissions) {
                Text(strings.grantButton)
            }
        }
    }
}

@Composable
private fun ReadinessBannerCard(
    bluetooth: BluetoothStatus,
    wifi: WifiNetworkStatus,
    strings: AppStrings,
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
                            allGood -> strings.readyForRs4
                            bluetooth.isAirplaneModeOn -> strings.airplaneModeActive
                            errorCount > 0 -> strings.issuesDetected(errorCount)
                            bluetooth.connectedDevices.isNotEmpty() -> strings.btStillConnected(bluetooth.connectedDevices.size)
                            else -> strings.warningsToCheck(warnCount)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${strings.updatedAtPrefix} $lastRefresh · ${if (wifi.isEmlidHotspot) strings.reachHotspotActive else wifi.ssid ?: wifi.activeTransportLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = strings.refresh)
                }
            }

            if (errorCount > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onGoToSamsungTab,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(strings.goToChecklist(errorCount), color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResetControlCard(
    state: HelperUiState,
    strings: AppStrings,
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
                        strings.resetCardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            shizukuReady && accessReady -> strings.modeFullAuto
                            shizukuReady -> strings.modeShizukuOnly
                            accessReady -> strings.modeAutoCacheOnly
                            else -> strings.modeGuidedFallback
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shizukuReady && accessReady) StatusOk else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showOptions = !showOptions }) {
                    Icon(Icons.Default.Settings, contentDescription = strings.options)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (progress.isRunning) {
                ActiveResetProgressBlock(
                    progress = progress,
                    strings = strings,
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
                                Icon(Icons.Default.Close, contentDescription = strings.close, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    strings.airplaneWaitLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, 30).forEach { sec ->
                        FilterChip(
                            selected = config.airplaneWaitSeconds == sec,
                            onClick = { onUpdateResetConfig { it.copy(airplaneWaitSeconds = sec) } },
                            label = { Text("$sec ${strings.secShort}") }
                        )
                    }
                }

                if (showOptions) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    ResetOptionSwitch(
                        label = strings.optForceStop,
                        checked = config.forceStopEmlid,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(forceStopEmlid = v) } }
                    )
                    ResetOptionSwitch(
                        label = strings.optClearCacheOnly,
                        checked = config.clearCache,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(clearCache = v) } }
                    )
                    ResetOptionSwitch(
                        label = strings.optToggleAirplane,
                        checked = config.toggleAirplaneMode,
                        onCheckedChange = { v -> onUpdateResetConfig { it.copy(toggleAirplaneMode = v) } }
                    )
                    ResetOptionSwitch(
                        label = strings.optLaunchEmlidAfter,
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
                        strings.startResetButton,
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
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Emlid Flow")
                    }
                    if (!shizukuReady || !accessReady) {
                        OutlinedButton(
                            onClick = onGoToAutomationTab,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.setupFullAutoButton)
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
    strings: AppStrings,
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
                    strings.stepOf(progress.currentStep, progress.totalSteps),
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
                    Text(strings.cacheClearedContinue)
                }
            }
            if (progress.phase == ResetPhase.WAITING_MANUAL_AIRPLANE_ON ||
                progress.phase == ResetPhase.WAITING_MANUAL_AIRPLANE_OFF
            ) {
                OutlinedButton(onClick = onOpenAirplaneSettings, modifier = Modifier.weight(1f)) {
                    Text(strings.airplaneSettingsButton)
                }
            }
            OutlinedButton(
                onClick = onCancelReset,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
            ) {
                Text(strings.cancelButton)
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
    strings: AppStrings,
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
                        strings.btTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            bluetooth.isAirplaneModeOn -> strings.airplaneModeActive
                            !bluetooth.isEnabled -> strings.btDisabled
                            !bluetooth.hasPermission -> strings.btPermissionRequired
                            bluetooth.connectedDevices.isEmpty() -> strings.btNoneConnectedSubtitle
                            else -> strings.btActiveCount(bluetooth.connectedDevices.size)
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
                                strings.btPairedEmlidWarningTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatusError
                            )
                            Text(
                                strings.btPairedEmlidWarningBody(bluetooth.pairedEmlidDevices.joinToString { it.name }),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

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
                            strings.btAllDisconnectedBanner,
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
                    Text(if (shizukuReady) strings.btDisconnectResetButton else strings.btSettingsButton)
                }
                if (shizukuReady) {
                    OutlinedButton(
                        onClick = onOpenBluetoothSettings,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.btSystemShortButton)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiMonitorCard(
    wifi: WifiNetworkStatus,
    strings: AppStrings,
    isDark: Boolean,
    onBrowseReceiverIp: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val browseIp = wifi.browseIpAddress

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
                        strings.wifiTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            !wifi.isWifiEnabled -> strings.wifiDisabled
                            !wifi.isConnected -> strings.wifiNotConnected(wifi.activeTransportLabel)
                            wifi.ssid != null -> strings.wifiConnectedTo(wifi.ssid)
                            else -> strings.wifiConnectedGeneric
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
                    InfoRow(strings.wifiSsidLabel, wifi.ssid ?: strings.wifiUnknown)
                    InfoRow(strings.wifiDeviceIpLabel, wifi.ipAddress ?: "–")
                    InfoRow(strings.wifiGatewayIpLabel, browseIp ?: "–")
                    InfoRow(
                        strings.wifiInternetStatusLabel,
                        if (wifi.hasValidatedInternet) strings.wifiWithInternet else strings.wifiWithoutInternet
                    )
                }

                // Direct 1-Tap Browse RS4 Pro / Gateway IP button
                if (browseIp != null) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onBrowseReceiverIp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (wifi.isEmlidHotspot) {
                            ButtonDefaults.buttonColors(
                                containerColor = StatusOk,
                                contentColor = Color(0xFF052E16)
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (wifi.isEmlidHotspot) {
                                strings.browseRs4IpButton(browseIp)
                            } else {
                                strings.browseGatewayIpButton(browseIp)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

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
                    text = if (wifi.isVpnActive) strings.vpnActiveWarning else strings.vpnInactiveOk,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (wifi.isVpnActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (wifi.isVpnActive) {
                    TextButton(onClick = onOpenVpnSettings) {
                        Text(strings.disconnectVpnButton, color = StatusError)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenWifiSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.openWifiSettingsButton)
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
