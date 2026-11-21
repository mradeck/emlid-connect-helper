package de.emlidhelper.app.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import de.emlidhelper.app.data.BluetoothStatus
import de.emlidhelper.app.data.CheckSeverity
import de.emlidhelper.app.data.SamsungCheckItem
import de.emlidhelper.app.data.SettingActionType
import de.emlidhelper.app.data.ShizukuState
import de.emlidhelper.app.data.WifiNetworkStatus
import de.emlidhelper.app.shizuku.ShizukuController

class SamsungSettingsMonitor(private val context: Context) {

    fun findEmlidPackageName(): String? {
        val candidates = listOf(EMLID_FLOW_PACKAGE, EMLID_FLOW_ALT_PACKAGE)
        for (pkg in candidates) {
            val installed = try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
            if (installed) return pkg
        }
        return null
    }

    fun isEmlidBatteryUnrestricted(packageName: String): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching { pm.isIgnoringBatteryOptimizations(packageName) }.getOrDefault(false)
    }

    suspend fun buildChecklist(
        bluetooth: BluetoothStatus,
        wifi: WifiNetworkStatus,
        locationServiceEnabled: Boolean,
        shizuku: ShizukuController
    ): List<SamsungCheckItem> {
        val items = mutableListOf<SamsungCheckItem>()
        val emlidPkg = findEmlidPackageName()
        val shizukuReady = shizuku.state.value == ShizukuState.READY

        // 1. Emlid Flow App installed
        items.add(
            if (emlidPkg != null) {
                SamsungCheckItem(
                    id = "emlid_installed",
                    title = "Emlid Flow App",
                    statusText = "Installiert ($emlidPkg)",
                    detailText = "Erkannt und direkt startbar.",
                    severity = CheckSeverity.OK,
                    actionLabel = "Öffnen",
                    actionType = SettingActionType.LAUNCH_EMLID_FLOW
                )
            } else {
                SamsungCheckItem(
                    id = "emlid_installed",
                    title = "Emlid Flow App",
                    statusText = "Nicht gefunden",
                    detailText = "Bitte Emlid Flow aus dem Play Store installieren.",
                    severity = CheckSeverity.ERROR
                )
            }
        )

        // 2. Battery Optimization (Samsung One UI kills BLE sockets aggressively!)
        if (emlidPkg != null) {
            val unrestricted = isEmlidBatteryUnrestricted(emlidPkg)
            items.add(
                if (unrestricted) {
                    SamsungCheckItem(
                        id = "battery_unrestricted",
                        title = "Akkuoptimierung (Emlid Flow)",
                        statusText = "Nicht eingeschränkt (Optimal)",
                        detailText = "One UI drosselt Emlid Flow nicht im Hintergrund.",
                        severity = CheckSeverity.OK,
                        actionLabel = "Prüfen",
                        actionType = SettingActionType.OPEN_EMLID_APP_INFO
                    )
                } else {
                    SamsungCheckItem(
                        id = "battery_unrestricted",
                        title = "Akkuoptimierung (Emlid Flow)",
                        statusText = "Eingeschränkt / Optimiert!",
                        detailText = "Samsung One UI trennt BLE-/Netzwerk-Sockets im Standby. Unter App-Info → Akku auf „Nicht eingeschränkt“ stellen.",
                        severity = CheckSeverity.ERROR,
                        actionLabel = "App-Info öffnen",
                        actionType = SettingActionType.OPEN_EMLID_APP_INFO,
                        autoFixLabel = if (shizukuReady) "Per Shizuku freigeben" else null,
                        autoFixType = if (shizukuReady) SettingActionType.FIX_BATTERY_SHIZUKU else SettingActionType.NONE
                    )
                }
            )
        }

        // 3. Bluetooth system pairing check
        val pairedEmlid = bluetooth.pairedEmlidDevices
        items.add(
            if (pairedEmlid.isEmpty()) {
                SamsungCheckItem(
                    id = "bt_system_pairing",
                    title = "Android-Bluetooth-Kopplung",
                    statusText = "Sauber (kein Reach gekoppelt)",
                    detailText = "Der RS4 Pro sollte nur direkt in Emlid Flow verbunden werden, nicht manuell in den Android-BT-Einstellungen.",
                    severity = CheckSeverity.OK
                )
            } else {
                val names = pairedEmlid.joinToString(", ") { it.name }
                SamsungCheckItem(
                    id = "bt_system_pairing",
                    title = "Android-Bluetooth-Kopplung",
                    statusText = "Warnung: $names gekoppelt!",
                    detailText = "Manuelle Systemkopplung kann sich mit Emlid Flow gegenseitig blockieren. Bitte in den Android-BT-Einstellungen entkoppeln.",
                    severity = CheckSeverity.ERROR,
                    actionLabel = "BT-Einstellungen",
                    actionType = SettingActionType.OPEN_BLUETOOTH_SETTINGS
                )
            }
        )

        // 4. VPN check
        items.add(
            if (!wifi.isVpnActive) {
                SamsungCheckItem(
                    id = "vpn_check",
                    title = "VPN / Lokale Firewall",
                    statusText = "Kein aktives VPN",
                    detailText = "Lokaler Datenkanal zum RS4 Pro wird nicht durch einen VPN-Tunnel blockiert.",
                    severity = CheckSeverity.OK
                )
            } else {
                SamsungCheckItem(
                    id = "vpn_check",
                    title = "VPN / Lokale Firewall",
                    statusText = "Aktives VPN erkannt!",
                    detailText = "Ein aktives VPN blockiert häufig die lokale Verbindung zum RS4 Pro, obwohl das Gerät per BLE angezeigt wird.",
                    severity = CheckSeverity.ERROR,
                    actionLabel = "VPN-Einstellungen",
                    actionType = SettingActionType.OPEN_VPN_SETTINGS
                )
            }
        )

        // 5. Location service (needed on Android for BLE scanning & Wi-Fi SSID)
        items.add(
            if (locationServiceEnabled) {
                SamsungCheckItem(
                    id = "location_service",
                    title = "Standortdienst (für BLE & WLAN)",
                    statusText = "Aktiviert",
                    detailText = "Android benötigt aktivierten Standort für zuverlässiges BLE-Scanning und WLAN-SSID-Erkennung.",
                    severity = CheckSeverity.OK
                )
            } else {
                SamsungCheckItem(
                    id = "location_service",
                    title = "Standortdienst (für BLE & WLAN)",
                    statusText = "Deaktiviert!",
                    detailText = "Bitte Standort einschalten, damit BLE-Discovery und WLAN-Diagnose auf dem S24 Ultra zuverlässig arbeiten.",
                    severity = CheckSeverity.WARNING,
                    actionLabel = "Standort öffnen",
                    actionType = SettingActionType.OPEN_LOCATION_SETTINGS
                )
            }
        )

        // 6. Samsung Intelligent Wi-Fi check
        val intelligentWifiInfo = inspectIntelligentWifi(shizukuReady, shizuku)
        items.add(intelligentWifiInfo)

        return items
    }

    private suspend fun inspectIntelligentWifi(
        shizukuReady: Boolean,
        shizuku: ShizukuController
    ): SamsungCheckItem {
        // Read Samsung settings safely (direct ContentResolver or Shizuku)
        val rawMap = mutableMapOf<String, String>()
        runCatching {
            Settings.Global.getString(context.contentResolver, "wifi_watchdog_poor_network_test_enabled")
                ?.let { rawMap["wifi_watchdog_poor_network_test_enabled"] = it }
            Settings.System.getString(context.contentResolver, "wifi_switch_to_mobile_data_enabled")
                ?.let { rawMap["wifi_switch_to_mobile_data_enabled"] = it }
        }
        if (shizukuReady) {
            rawMap.putAll(shizuku.querySamsungIntelligentWifi())
        }

        val mobileSwitch = rawMap["wifi_switch_to_mobile_data_enabled"]
            ?: rawMap["wifi_watchdog_poor_network_test_enabled"]

        return when (mobileSwitch) {
            "0" -> SamsungCheckItem(
                id = "samsung_intelligent_wifi",
                title = "Samsung Intelligentes WLAN",
                statusText = "Automatischer Wechsel AUS (OK)",
                detailText = "„Zu mobilen Daten wechseln“ ist aus. Bei Nutzung des Reach-Hotspots zusätzlich „Zu besseren WLANs wechseln“ aus lassen.",
                severity = CheckSeverity.OK,
                actionLabel = "WLAN-Einstellungen",
                actionType = SettingActionType.OPEN_WIFI_SETTINGS
            )

            "1" -> SamsungCheckItem(
                id = "samsung_intelligent_wifi",
                title = "Samsung Intelligentes WLAN",
                statusText = "Zu mobilen Daten wechseln ist AN!",
                detailText = "WLAN → 3-Punkte-Menü → Intelligentes WLAN: „Zu mobilen Daten wechseln“ & „Zu besseren WLAN-Netzwerken wechseln“ deaktivieren.",
                severity = CheckSeverity.WARNING,
                actionLabel = "WLAN-Einstellungen",
                actionType = SettingActionType.OPEN_WIFI_SETTINGS
            )

            else -> SamsungCheckItem(
                id = "samsung_intelligent_wifi",
                title = "Samsung Intelligentes WLAN",
                statusText = "Manuell in One UI prüfen",
                detailText = "WLAN → 3-Punkte-Menü → Intelligentes WLAN: „Zu mobilen Daten wechseln“ & „Zu besseren WLAN-Netzwerken wechseln“ ausschalten. Bei „Verbunden ohne Internet“ → „Immer verbinden“.",
                severity = CheckSeverity.INFO,
                actionLabel = "WLAN-Einstellungen",
                actionType = SettingActionType.OPEN_WIFI_SETTINGS
            )
        }
    }

    fun openEmlidAppInfo(packageName: String = findEmlidPackageName() ?: EMLID_FLOW_PACKAGE) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openWifiSettings() {
        startSafe(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun openBluetoothSettings() {
        startSafe(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openVpnSettings() {
        startSafe(Intent(Settings.ACTION_VPN_SETTINGS))
    }

    fun openLocationSettings() {
        startSafe(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun openAirplaneModeSettings() {
        startSafe(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    }

    fun openAccessibilitySettings() {
        startSafe(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openShizukuOrPlayStore() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val storeIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(storeIntent) }
        }
    }

    fun launchEmlidFlow(): Boolean {
        val pkg = findEmlidPackageName() ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
        return true
    }

    private fun startSafe(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    companion object {
        const val EMLID_FLOW_PACKAGE = "com.emlid.reachview3"
        const val EMLID_FLOW_ALT_PACKAGE = "com.emlid.flow"
    }
}
