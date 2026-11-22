package de.emlidhelper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.emlidhelper.app.data.HelperUiState
import de.emlidhelper.app.data.ShizukuState
import de.emlidhelper.app.i18n.AppStrings
import de.emlidhelper.app.ui.components.AppFooter
import de.emlidhelper.app.ui.theme.StatusError
import de.emlidhelper.app.ui.theme.StatusInfo
import de.emlidhelper.app.ui.theme.StatusOk
import de.emlidhelper.app.ui.theme.StatusWarn

@Composable
fun AutomationScreen(
    state: HelperUiState,
    strings: AppStrings,
    onRequestShizukuPermission: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onClearLogs: () -> Unit
) {
    val auto = state.automation

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                strings.autoHeading,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                strings.autoSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Shizuku Card
        item {
            val (badgeText, badgeColor) = when (auto.shizukuState) {
                ShizukuState.READY -> strings.shizukuStatusReady(auto.shizukuUid ?: 2000) to StatusOk
                ShizukuState.CONNECTING -> strings.shizukuStatusConnecting to StatusInfo
                ShizukuState.PERMISSION_REQUIRED -> strings.shizukuStatusPermReq to StatusWarn
                ShizukuState.NOT_RUNNING -> strings.shizukuStatusOff to StatusWarn
                ShizukuState.NOT_INSTALLED -> strings.shizukuStatusNotInstalled to StatusError
            }

            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = badgeColor)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            strings.shizukuTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(badgeText, badgeColor)
                    }

                    Text(
                        strings.shizukuBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (auto.shizukuState != ShizukuState.READY) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(strings.shizukuSetupHeading, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Text(strings.shizukuStep1, style = MaterialTheme.typography.bodySmall)
                            Text(strings.shizukuStep2, style = MaterialTheme.typography.bodySmall)
                            Text(strings.shizukuStep3, style = MaterialTheme.typography.bodySmall)
                            Text(strings.shizukuStep4, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (auto.shizukuState) {
                            ShizukuState.PERMISSION_REQUIRED -> {
                                Button(onClick = onRequestShizukuPermission, shape = RoundedCornerShape(10.dp)) {
                                    Text(strings.shizukuRequestPermButton)
                                }
                            }

                            ShizukuState.NOT_INSTALLED -> {
                                Button(onClick = onOpenShizukuApp, shape = RoundedCornerShape(10.dp)) {
                                    Text(strings.shizukuInstallButton)
                                }
                            }

                            ShizukuState.NOT_RUNNING -> {
                                Button(onClick = onOpenShizukuApp, shape = RoundedCornerShape(10.dp)) {
                                    Text(strings.shizukuOpenButton)
                                }
                            }

                            else -> Unit
                        }
                        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
                            Text(strings.checkStatusButton)
                        }
                    }
                }
            }
        }

        // 2. Accessibility Service Card
        item {
            val enabled = auto.accessibilityEnabled
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = if (enabled) StatusOk else StatusWarn
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            strings.accessTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            if (enabled) strings.accessStatusOn else strings.accessStatusOff,
                            if (enabled) StatusOk else StatusWarn
                        )
                    }

                    Text(
                        strings.accessBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        strings.accessPathHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(onClick = onOpenAccessibilitySettings, shape = RoundedCornerShape(10.dp)) {
                        Text(if (enabled) strings.accessOpenSettings else strings.accessEnableNow)
                    }
                }
            }
        }

        // 3. Standard Permissions Card
        item {
            val allGranted = auto.hasBluetoothPermission && auto.hasLocationPermission
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = if (allGranted) StatusOk else StatusError
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            strings.permsCardTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            if (allGranted) strings.permsOk else strings.permsMissing,
                            if (allGranted) StatusOk else StatusError
                        )
                    }
                    Text(
                        strings.permsDetail(auto.hasBluetoothPermission, auto.hasLocationPermission),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!allGranted) {
                        Button(onClick = onRequestPermissions, shape = RoundedCornerShape(10.dp)) {
                            Text(strings.permsGrantButton)
                        }
                    }
                }
            }
        }

        // 4. Live Diagnostic Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.logHeading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearLogs) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = strings.clearLog)
                }
            }
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    strings.logEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.logs) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "[${entry.timestamp}] ",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (entry.isError) StatusError else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            AppFooter(strings = strings, language = state.language)
        }
    }
}
