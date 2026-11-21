package de.emlidhelper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import de.emlidhelper.app.ui.theme.StatusError
import de.emlidhelper.app.ui.theme.StatusInfo
import de.emlidhelper.app.ui.theme.StatusOk
import de.emlidhelper.app.ui.theme.StatusWarn

@Composable
fun AutomationScreen(
    state: HelperUiState,
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
                "Variante B: Vollautomatik",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Ermöglicht 1-Tap-Flugmodus, Force-Stop, Deep-Bluetooth-Scan & Auto-Cache ohne Root.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Shizuku Card
        item {
            val (badgeText, badgeColor) = when (auto.shizukuState) {
                ShizukuState.READY -> "Aktiv (UID ${auto.shizukuUid ?: 2000})" to StatusOk
                ShizukuState.CONNECTING -> "Verbinde…" to StatusInfo
                ShizukuState.PERMISSION_REQUIRED -> "Freigabe nötig" to StatusWarn
                ShizukuState.NOT_RUNNING -> "Dienst aus" to StatusWarn
                ShizukuState.NOT_INSTALLED -> "Nicht installiert" to StatusError
            }

            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = badgeColor)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "1. Shizuku (Flugmodus, Force-Stop & BT)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(badgeText, badgeColor)
                    }

                    Text(
                        "Ohne Root-Rechte erlaubt Shizuku über Androids Wireless Debugging das automatische Schalten von Flugmodus/Bluetooth, Zwangsbeenden von Emlid Flow und Freigeben der Akkuoptimierung.",
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
                            Text("Einrichtung auf dem S24 Ultra (einmalig ca. 2 Min.):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Text("1. App „Shizuku“ aus dem Play Store installieren.", style = MaterialTheme.typography.bodySmall)
                            Text("2. Entwickleroptionen → „WLAN-Debugging“ aktivieren.", style = MaterialTheme.typography.bodySmall)
                            Text("3. In Shizuku auf „Koppeln“ → „Starten“ tippen.", style = MaterialTheme.typography.bodySmall)
                            Text("4. Hier auf „Berechtigung anfragen“ tippen.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (auto.shizukuState) {
                            ShizukuState.PERMISSION_REQUIRED -> {
                                Button(onClick = onRequestShizukuPermission, shape = RoundedCornerShape(10.dp)) {
                                    Text("Berechtigung anfragen")
                                }
                            }

                            ShizukuState.NOT_INSTALLED -> {
                                Button(onClick = onOpenShizukuApp, shape = RoundedCornerShape(10.dp)) {
                                    Text("Shizuku installieren")
                                }
                            }

                            ShizukuState.NOT_RUNNING -> {
                                Button(onClick = onOpenShizukuApp, shape = RoundedCornerShape(10.dp)) {
                                    Text("Shizuku öffnen")
                                }
                            }

                            else -> Unit
                        }
                        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
                            Text("Status prüfen")
                        }
                    }
                }
            }
        }

        // 2. Accessibility Service Card (Auto Clear Cache)
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
                            "2. Auto-Cache leeren (Bedienungshilfe)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(if (enabled) "Aktiv" else "Aus", if (enabled) StatusOk else StatusWarn)
                    }

                    Text(
                        "Android verbietet es selbst Shizuku, nur den Cache einer anderen App ohne Datenverlust zu leeren. Die Bedienungshilfe öffnet beim Reset kurz die App-Info von Emlid Flow und tippt ausschließlich auf „Speicher“ → „Cache leeren“ (niemals „Daten löschen“).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Pfad auf Samsung One UI: Einstellungen → Eingabehilfe → Installierte Apps → „Emlid Helper – Auto-Cache leeren“ aktivieren.\n" +
                            "Hinweis bei Sideloading (Android 13+): Falls der Schalter ausgegraut ist, zuerst Einstellungen → Apps → Emlid Helper → 3-Punkte-Menü oben rechts → „Eingeschränkte Einstellungen zulassen“.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(onClick = onOpenAccessibilitySettings, shape = RoundedCornerShape(10.dp)) {
                        Text(if (enabled) "Eingabehilfe-Einstellungen öffnen" else "Jetzt in Eingabehilfe aktivieren")
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
                            "3. App-Berechtigungen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(if (allGranted) "OK" else "Fehlt", if (allGranted) StatusOk else StatusError)
                    }
                    Text(
                        "• Bluetooth (Geräte in der Nähe): ${if (auto.hasBluetoothPermission) "Erteilt" else "Fehlt"}\n" +
                            "• Standort (für WLAN-SSID & BLE): ${if (auto.hasLocationPermission) "Erteilt" else "Fehlt"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!allGranted) {
                        Button(onClick = onRequestPermissions, shape = RoundedCornerShape(10.dp)) {
                            Text("Berechtigungen erteilen")
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
                    "Diagnose-Protokoll",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearLogs) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Log leeren")
                }
            }
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    "Noch keine Einträge.",
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

        item { Spacer(Modifier.height(12.dp)) }
    }
}
