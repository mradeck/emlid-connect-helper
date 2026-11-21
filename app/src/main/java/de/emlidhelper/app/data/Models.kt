package de.emlidhelper.app.data

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val connectionType: String, // e.g. "BLE / GATT", "Audio / HFP", "A2DP", "System (Shizuku)"
    val isEmlidRelated: Boolean,
    val isConnected: Boolean
)

data class BluetoothStatus(
    val hasPermission: Boolean = false,
    val isEnabled: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val connectedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val pairedEmlidDevices: List<BluetoothDeviceInfo> = emptyList(),
    val pairedCount: Int = 0,
    val shizukuEnhanced: Boolean = false
) {
    val isChannelClear: Boolean
        get() = hasPermission && isEnabled && !isAirplaneModeOn && connectedDevices.isEmpty()
}

data class WifiNetworkStatus(
    val isWifiEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val ssid: String? = null,
    val ssidUnavailableReason: String? = null,
    val ipAddress: String? = null,
    val gatewayAddress: String? = null,
    val hasValidatedInternet: Boolean = false,
    val isEmlidHotspot: Boolean = false,
    val isVpnActive: Boolean = false,
    val activeTransportLabel: String = "Unbekannt"
)

enum class CheckSeverity {
    OK,
    WARNING,
    ERROR,
    INFO
}

enum class SettingActionType {
    NONE,
    OPEN_EMLID_APP_INFO,
    OPEN_EMLID_BATTERY,
    FIX_BATTERY_SHIZUKU,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_VPN_SETTINGS,
    OPEN_LOCATION_SETTINGS,
    LAUNCH_EMLID_FLOW
}

data class SamsungCheckItem(
    val id: String,
    val title: String,
    val statusText: String,
    val detailText: String,
    val severity: CheckSeverity,
    val actionLabel: String? = null,
    val actionType: SettingActionType = SettingActionType.NONE,
    val autoFixLabel: String? = null,
    val autoFixType: SettingActionType = SettingActionType.NONE
)

enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    CONNECTING,
    READY
}

data class AutomationCapabilities(
    val shizukuState: ShizukuState = ShizukuState.NOT_INSTALLED,
    val shizukuUid: Int? = null,
    val accessibilityEnabled: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isLocationServiceEnabled: Boolean = false,
    val emlidPackageName: String? = null
) {
    val isFullAutoReady: Boolean
        get() = shizukuState == ShizukuState.READY && accessibilityEnabled
}

data class ResetConfig(
    val airplaneWaitSeconds: Int = 15,
    val clearCache: Boolean = true,
    val forceStopEmlid: Boolean = true,
    val toggleAirplaneMode: Boolean = true,
    val launchEmlidAfter: Boolean = true
)

enum class ResetPhase {
    IDLE,
    FORCE_STOPPING_EMLID,
    CLEARING_CACHE_AUTO,
    WAITING_MANUAL_CACHE,
    ENABLING_AIRPLANE_MODE,
    WAITING_MANUAL_AIRPLANE_ON,
    AIRPLANE_COUNTDOWN,
    DISABLING_AIRPLANE_MODE,
    WAITING_MANUAL_AIRPLANE_OFF,
    WAITING_RADIOS_RECOVERY,
    VERIFYING_BLUETOOTH_CLEAR,
    LAUNCHING_EMLID,
    COMPLETED,
    FAILED
}

data class ResetProgress(
    val phase: ResetPhase = ResetPhase.IDLE,
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val title: String = "",
    val detail: String = "",
    val countdownRemainingSeconds: Int = 0,
    val countdownTotalSeconds: Int = 0,
    val isGuidedFallback: Boolean = false,
    val errorMessage: String? = null
) {
    val isRunning: Boolean
        get() = phase != ResetPhase.IDLE && phase != ResetPhase.COMPLETED && phase != ResetPhase.FAILED
}

data class DiagnosticLogEntry(
    val timestamp: String,
    val message: String,
    val isError: Boolean = false
)

data class HelperUiState(
    val bluetooth: BluetoothStatus = BluetoothStatus(),
    val wifi: WifiNetworkStatus = WifiNetworkStatus(),
    val samsungChecks: List<SamsungCheckItem> = emptyList(),
    val automation: AutomationCapabilities = AutomationCapabilities(),
    val resetConfig: ResetConfig = ResetConfig(),
    val resetProgress: ResetProgress = ResetProgress(),
    val logs: List<DiagnosticLogEntry> = emptyList(),
    val lastRefreshTime: String = "–"
)
