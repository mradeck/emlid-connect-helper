package de.emlidhelper.app.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import de.emlidhelper.app.data.AppLanguage
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
        shizuku: ShizukuController,
        language: AppLanguage = AppLanguage.DE
    ): List<SamsungCheckItem> {
        val isEn = language == AppLanguage.EN
        val items = mutableListOf<SamsungCheckItem>()
        val emlidPkg = findEmlidPackageName()
        val shizukuReady = shizuku.state.value == ShizukuState.READY

        // 1. Emlid Flow App installed
        items.add(
            if (emlidPkg != null) {
                SamsungCheckItem(
                    id = "emlid_installed",
                    title = "Emlid Flow App",
                    statusText = if (isEn) "Installed ($emlidPkg)" else "Installiert ($emlidPkg)",
                    detailText = if (isEn) "Detected and ready to launch." else "Erkannt und direkt startbar.",
                    severity = CheckSeverity.OK,
                    actionLabel = if (isEn) "Open" else "Öffnen",
                    actionType = SettingActionType.LAUNCH_EMLID_FLOW
                )
            } else {
                SamsungCheckItem(
                    id = "emlid_installed",
                    title = "Emlid Flow App",
                    statusText = if (isEn) "Not found" else "Nicht gefunden",
                    detailText = if (isEn) {
                        "Please install Emlid Flow from the Google Play Store."
                    } else {
                        "Bitte Emlid Flow aus dem Play Store installieren."
                    },
                    severity = CheckSeverity.ERROR
                )
            }
        )

        // 2. Battery Optimization
        if (emlidPkg != null) {
            val unrestricted = isEmlidBatteryUnrestricted(emlidPkg)
            items.add(
                if (unrestricted) {
                    SamsungCheckItem(
                        id = "battery_unrestricted",
                        title = if (isEn) "Battery Optimization (Emlid Flow)" else "Akkuoptimierung (Emlid Flow)",
                        statusText = if (isEn) "Unrestricted (Optimal)" else "Nicht eingeschränkt (Optimal)",
                        detailText = if (isEn) {
                            "System will not throttle Emlid Flow in the background."
                        } else {
                            "One UI drosselt Emlid Flow nicht im Hintergrund."
                        },
                        severity = CheckSeverity.OK,
                        actionLabel = if (isEn) "Inspect" else "Prüfen",
                        actionType = SettingActionType.OPEN_EMLID_APP_INFO
                    )
                } else {
                    SamsungCheckItem(
                        id = "battery_unrestricted",
                        title = if (isEn) "Battery Optimization (Emlid Flow)" else "Akkuoptimierung (Emlid Flow)",
                        statusText = if (isEn) "Restricted / Optimized!" else "Eingeschränkt / Optimiert!",
                        detailText = if (isEn) {
                            "Samsung One UI kills BLE/network sockets during standby. Open App Info → Battery and select \"Unrestricted\"."
                        } else {
                            "Samsung One UI trennt BLE-/Netzwerk-Sockets im Standby. Unter App-Info → Akku auf „Nicht eingeschränkt“ stellen."
                        },
                        severity = CheckSeverity.ERROR,
                        actionLabel = if (isEn) "Open App Info" else "App-Info öffnen",
                        actionType = SettingActionType.OPEN_EMLID_APP_INFO,
                        autoFixLabel = if (shizukuReady) {
                            if (isEn) "Fix via Shizuku" else "Per Shizuku freigeben"
                        } else null,
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
                    title = if (isEn) "Android Bluetooth Pairing" else "Android-Bluetooth-Kopplung",
                    statusText = if (isEn) "Clean (no Reach paired)" else "Sauber (kein Reach gekoppelt)",
                    detailText = if (isEn) {
                        "Connect the RS4 Pro directly inside Emlid Flow, not manually in Android system Bluetooth settings."
                    } else {
                        "Der RS4 Pro sollte nur direkt in Emlid Flow verbunden werden, nicht manuell in den Android-BT-Einstellungen."
                    },
                    severity = CheckSeverity.OK
                )
            } else {
                val names = pairedEmlid.joinToString(", ") { it.name }
                SamsungCheckItem(
                    id = "bt_system_pairing",
                    title = if (isEn) "Android Bluetooth Pairing" else "Android-Bluetooth-Kopplung",
                    statusText = if (isEn) "Warning: $names paired!" else "Warnung: $names gekoppelt!",
                    detailText = if (isEn) {
                        "Manual system pairing can conflict with Emlid Flow. Please unpair in Android Bluetooth settings."
                    } else {
                        "Manuelle Systemkopplung kann sich mit Emlid Flow gegenseitig blockieren. Bitte in den Android-BT-Einstellungen entkoppeln."
                    },
                    severity = CheckSeverity.ERROR,
                    actionLabel = if (isEn) "BT Settings" else "BT-Einstellungen",
                    actionType = SettingActionType.OPEN_BLUETOOTH_SETTINGS
                )
            }
        )

        // 4. VPN check
        items.add(
            if (!wifi.isVpnActive) {
                SamsungCheckItem(
                    id = "vpn_check",
                    title = if (isEn) "VPN / Local Firewall" else "VPN / Lokale Firewall",
                    statusText = if (isEn) "No active VPN" else "Kein aktives VPN",
                    detailText = if (isEn) {
                        "Local data channel to the RS4 Pro is not blocked by a VPN tunnel."
                    } else {
                        "Lokaler Datenkanal zum RS4 Pro wird nicht durch einen VPN-Tunnel blockiert."
                    },
                    severity = CheckSeverity.OK
                )
            } else {
                SamsungCheckItem(
                    id = "vpn_check",
                    title = if (isEn) "VPN / Local Firewall" else "VPN / Lokale Firewall",
                    statusText = if (isEn) "Active VPN detected!" else "Aktives VPN erkannt!",
                    detailText = if (isEn) {
                        "An active VPN often blocks local communication with the RS4 Pro even when BLE discovery sees it."
                    } else {
                        "Ein aktives VPN blockiert häufig die lokale Verbindung zum RS4 Pro, obwohl das Gerät per BLE angezeigt wird."
                    },
                    severity = CheckSeverity.ERROR,
                    actionLabel = if (isEn) "VPN Settings" else "VPN-Einstellungen",
                    actionType = SettingActionType.OPEN_VPN_SETTINGS
                )
            }
        )

        // 5. Location service
        items.add(
            if (locationServiceEnabled) {
                SamsungCheckItem(
                    id = "location_service",
                    title = if (isEn) "Location Service (BLE & Wi-Fi)" else "Standortdienst (für BLE & WLAN)",
                    statusText = if (isEn) "Enabled" else "Aktiviert",
                    detailText = if (isEn) {
                        "Android requires Location to be ON for reliable BLE scanning and Wi-Fi SSID detection."
                    } else {
                        "Android benötigt aktivierten Standort für zuverlässiges BLE-Scanning und WLAN-SSID-Erkennung."
                    },
                    severity = CheckSeverity.OK
                )
            } else {
                SamsungCheckItem(
                    id = "location_service",
                    title = if (isEn) "Location Service (BLE & Wi-Fi)" else "Standortdienst (für BLE & WLAN)",
                    statusText = if (isEn) "Disabled!" else "Deaktiviert!",
                    detailText = if (isEn) {
                        "Enable Location so BLE discovery and Wi-Fi diagnostics work reliably."
                    } else {
                        "Bitte Standort einschalten, damit BLE-Discovery und WLAN-Diagnose zuverlässig arbeiten."
                    },
                    severity = CheckSeverity.WARNING,
                    actionLabel = if (isEn) "Open Location" else "Standort öffnen",
                    actionType = SettingActionType.OPEN_LOCATION_SETTINGS
                )
            }
        )

        // 6. Samsung Intelligent Wi-Fi check
        items.add(inspectIntelligentWifi(shizukuReady, shizuku, isEn))

        return items
    }

    private suspend fun inspectIntelligentWifi(
        shizukuReady: Boolean,
        shizuku: ShizukuController,
        isEn: Boolean
    ): SamsungCheckItem {
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
                title = if (isEn) "Samsung Intelligent Wi-Fi" else "Samsung Intelligentes WLAN",
                statusText = if (isEn) "Auto-switch OFF (OK)" else "Automatischer Wechsel AUS (OK)",
                detailText = if (isEn) {
                    "\"Switch to mobile data\" is OFF. When using the Reach hotspot, also keep \"Switch to better Wi-Fi networks\" OFF."
                } else {
                    "„Zu mobilen Daten wechseln“ ist aus. Bei Nutzung des Reach-Hotspots zusätzlich „Zu besseren WLANs wechseln“ aus lassen."
                },
                severity = CheckSeverity.OK,
                actionLabel = if (isEn) "Wi-Fi Settings" else "WLAN-Einstellungen",
                actionType = SettingActionType.OPEN_WIFI_SETTINGS
            )

            "1" -> SamsungCheckItem(
                id = "samsung_intelligent_wifi",
                title = if (isEn) "Samsung Intelligent Wi-Fi" else "Samsung Intelligentes WLAN",
                statusText = if (isEn) "Switch to mobile data is ON!" else "Zu mobilen Daten wechseln ist AN!",
                detailText = if (isEn) {
                    "Turn Wi-Fi ON first (otherwise the toggle is greyed out!), then open Wi-Fi → 3-dot menu → Intelligent Wi-Fi and disable \"Switch to mobile data\" & \"Switch to better Wi-Fi networks\"."
                } else {
                    "Zuerst WLAN einschalten (sonst ausgegraut!), dann WLAN → 3-Punkte-Menü → Intelligent Wi-Fi: „Zu mobilen Daten wechseln“ & „Zu besseren WLANs wechseln“ ausschalten."
                },
                severity = CheckSeverity.WARNING,
                actionLabel = if (isEn) "Wi-Fi Settings" else "WLAN-Einstellungen",
                actionType = SettingActionType.OPEN_WIFI_SETTINGS
            )

            else -> SamsungCheckItem(
                id = "samsung_intelligent_wifi",
                title = if (isEn) "Samsung Intelligent Wi-Fi" else "Samsung Intelligentes WLAN",
                statusText = if (isEn) "Check manually (Samsung One UI)" else "Manuell in One UI prüfen",
                detailText = if (isEn) {
                    "On Samsung devices: turn Wi-Fi ON, open Wi-Fi → 3-dot menu → Intelligent Wi-Fi and turn OFF \"Switch to mobile data\" & \"Switch to better Wi-Fi networks\". On non-Samsung phones, confirm \"Keep Wi-Fi without internet\"."
                } else {
                    "Auf Samsung-Geräten: erst WLAN AN, dann WLAN → 3-Punkte-Menü → Intelligent Wi-Fi → „Zu mobilen Daten wechseln“ & „Zu besseren WLANs wechseln“ aus. Bei „Verbunden ohne Internet“ → „Immer verbinden“."
                },
                severity = CheckSeverity.INFO,
                actionLabel = if (isEn) "Wi-Fi Settings" else "WLAN-Einstellungen",
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

    fun openUrlInBrowser(url: String) {
        startSafe(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
