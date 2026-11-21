package de.emlidhelper.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.emlidhelper.app.data.CheckSeverity
import de.emlidhelper.app.data.HelperUiState
import de.emlidhelper.app.data.SamsungCheckItem
import de.emlidhelper.app.data.SettingActionType
import de.emlidhelper.app.ui.theme.StatusError
import de.emlidhelper.app.ui.theme.StatusInfo
import de.emlidhelper.app.ui.theme.StatusOk
import de.emlidhelper.app.ui.theme.StatusWarn

@Composable
fun SamsungSettingsScreen(
    state: HelperUiState,
    onRefresh: () -> Unit,
    onAction: (SettingActionType) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Samsung S24 Ultra Checkliste",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "One-UI-Einstellungen für stabile Emlid-Verbindungen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Neu prüfen")
                }
            }
        }

        items(state.samsungChecks, key = { it.id }) { item ->
            SamsungCheckCard(item = item, onAction = onAction)
        }

        item {
            EmlidFieldTipsCard()
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SamsungCheckCard(
    item: SamsungCheckItem,
    onAction: (SettingActionType) -> Unit
) {
    val (icon, tint) = when (item.severity) {
        CheckSeverity.OK -> Icons.Default.CheckCircle to StatusOk
        CheckSeverity.WARNING -> Icons.Default.Warning to StatusWarn
        CheckSeverity.ERROR -> Icons.Default.Error to StatusError
        CheckSeverity.INFO -> Icons.Default.Info to StatusInfo
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(item.statusText, style = MaterialTheme.typography.bodySmall, color = tint, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.detailText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.actionLabel != null || item.autoFixLabel != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.autoFixLabel != null && item.autoFixType != SettingActionType.NONE) {
                        Button(
                            onClick = { onAction(item.autoFixType) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(item.autoFixLabel)
                        }
                    }
                    if (item.actionLabel != null && item.actionType != SettingActionType.NONE) {
                        OutlinedButton(
                            onClick = { onAction(item.actionType) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(item.actionLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmlidFieldTipsCard() {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = StatusInfo)
                Spacer(Modifier.width(8.dp))
                Text(
                    "RS4 Pro Praxis-Hinweise",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "• WLAN-Alternative: In Emlid Flow unter Profil → App-Einstellungen → Präferenzen lässt sich „Erweiterte Verbindung“ aktivieren (Direktzugriff per IP statt Bluetooth).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "• Firmware prüfen: In der Firmware-Reihe 34.x gab es bekannte Bluetooth-Stabilitätsprobleme – bei bestehender Verbindung Firmware & Emlid Flow aktuell halten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "• Neustart-Warnung: Den Netzschalter am RS4 Pro beim Neustart niemals länger als 5 Sekunden gedrückt halten – sonst wechselt der Empfänger in den Werksmodus (Factory Mode).",
                style = MaterialTheme.typography.bodySmall,
                color = StatusWarn
            )
            Text(
                "• Zweitgeräte: Sicherstellen, dass kein Tablet oder Zweit-Smartphone im Hintergrund noch mit dem RS4 Pro verbunden ist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
