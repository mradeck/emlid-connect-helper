package de.emlidhelper.app.ui

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.emlidhelper.app.accessibility.AccessibilityBridge
import de.emlidhelper.app.data.AppLanguage
import de.emlidhelper.app.data.AppThemeMode
import de.emlidhelper.app.data.AutomationCapabilities
import de.emlidhelper.app.data.DiagnosticLogEntry
import de.emlidhelper.app.data.HelperPreferences
import de.emlidhelper.app.data.HelperUiState
import de.emlidhelper.app.data.ResetConfig
import de.emlidhelper.app.data.SettingActionType
import de.emlidhelper.app.data.ShizukuState
import de.emlidhelper.app.monitor.BluetoothMonitor
import de.emlidhelper.app.monitor.NetworkMonitor
import de.emlidhelper.app.monitor.SamsungSettingsMonitor
import de.emlidhelper.app.reset.ResetOrchestrator
import de.emlidhelper.app.shizuku.ShizukuController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HelperViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = HelperPreferences(appContext)
    private val bluetoothMonitor = BluetoothMonitor(appContext)
    private val networkMonitor = NetworkMonitor(appContext)
    private val settingsMonitor = SamsungSettingsMonitor(appContext)

    private val _uiState = MutableStateFlow(
        HelperUiState(
            language = prefs.loadLanguage(),
            themeMode = prefs.loadThemeMode(),
            resetConfig = prefs.loadResetConfig()
        )
    )
    val uiState: StateFlow<HelperUiState> = _uiState.asStateFlow()

    private val shizukuController = ShizukuController(appContext) { msg, isErr ->
        appendLog(msg, isErr)
    }

    private val resetOrchestrator = ResetOrchestrator(
        context = appContext,
        shizuku = shizukuController,
        bluetoothMonitor = bluetoothMonitor,
        settingsMonitor = settingsMonitor,
        onRefreshStatus = { refreshAllInternal() },
        onLog = { msg, isErr -> appendLog(msg, isErr) }
    )

    private var resetJob: Job? = null
    private var receiverRegistered = false
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

    private val systemStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshAll()
        }
    }

    init {
        AccessibilityBridge.logCallback = { msg, isErr -> appendLog(msg, isErr) }
        shizukuController.start()
        registerSystemReceivers()

        viewModelScope.launch {
            shizukuController.state.collect {
                refreshAllInternal()
            }
        }

        viewModelScope.launch {
            AccessibilityBridge.isServiceConnected.collect {
                refreshAllInternal()
            }
        }

        viewModelScope.launch {
            resetOrchestrator.progress.collect { progress ->
                _uiState.update { it.copy(resetProgress = progress) }
            }
        }

        appendLog("Emlid Connect Helper gestartet", false)
        refreshAll()
    }

    private fun registerSystemReceivers() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        ContextCompat.registerReceiver(
            appContext,
            systemStateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.DE) AppLanguage.EN else AppLanguage.DE
        prefs.saveLanguage(next)
        _uiState.update { it.copy(language = next) }
        refreshAll()
    }

    fun toggleTheme(isCurrentlyDark: Boolean) {
        val next = if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        prefs.saveThemeMode(next)
        _uiState.update { it.copy(themeMode = next) }
    }

    fun browseReceiverIp() {
        val url = _uiState.value.wifi.receiverBrowseUrl ?: return
        appendLog("Browser öffnen: $url", false)
        settingsMonitor.openUrlInBrowser(url)
    }

    fun forgetSavedReachWifiNetworks() {
        viewModelScope.launch {
            val isEn = _uiState.value.language == AppLanguage.EN
            if (shizukuController.state.value != ShizukuState.READY) {
                settingsMonitor.openWifiSettings()
                return@launch
            }
            val targets = shizukuController.listSavedWifiNetworks().filter { it.isEmlidRelated }
            if (targets.isEmpty()) {
                val msg = if (isEn) {
                    "No saved Reach/Emlid Wi-Fi profile found."
                } else {
                    "Kein gespeichertes Reach-/Emlid-WLAN-Profil gefunden."
                }
                appendLog(msg, false)
                _uiState.update { it.copy(wifiForgetFeedback = msg) }
                return@launch
            }

            val removed = mutableListOf<String>()
            val failed = mutableListOf<String>()
            for (net in targets) {
                val res = shizukuController.forgetWifiNetwork(net.networkId, net.ssid)
                if (res.isSuccess) {
                    removed.add(net.ssid)
                } else {
                    failed.add("${net.ssid} (${res.output})")
                }
            }

            val feedback = if (failed.isEmpty()) {
                if (isEn) {
                    "Forgotten: ${removed.joinToString(", ")}. Reconnect now and tap \"Always connect\"."
                } else {
                    "Entfernt: ${removed.joinToString(", ")}. Jetzt neu verbinden & „Immer verbinden“ wählen."
                }
            } else {
                if (isEn) {
                    "Removed: ${removed.joinToString(", ").ifBlank { "none" }} · Failed: ${failed.joinToString(", ")}"
                } else {
                    "Entfernt: ${removed.joinToString(", ").ifBlank { "keine" }} · Fehler: ${failed.joinToString(", ")}"
                }
            }
            appendLog(feedback, failed.isNotEmpty())
            _uiState.update { it.copy(wifiForgetFeedback = feedback) }
            delay(450)
            refreshAllInternal()
        }
    }

    fun dismissWifiForgetFeedback() {
        _uiState.update { it.copy(wifiForgetFeedback = null) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            shizukuController.refreshState()
            refreshAllInternal()
        }
    }

    private suspend fun refreshAllInternal() {
        val lang = _uiState.value.language
        val bt = bluetoothMonitor.inspect(shizukuController)
        val wifi = networkMonitor.inspect(lang)
        val savedEmlidWifi = if (shizukuController.state.value == ShizukuState.READY) {
            shizukuController.listSavedWifiNetworks().filter { it.isEmlidRelated }
        } else {
            emptyList()
        }
        val locEnabled = networkMonitor.isLocationServiceEnabled()
        val checks = settingsMonitor.buildChecklist(bt, wifi, locEnabled, shizukuController, lang)
        val capabilities = AutomationCapabilities(
            shizukuState = shizukuController.state.value,
            shizukuUid = shizukuController.uid.value,
            accessibilityEnabled = AccessibilityBridge.isEnabledInSettings(appContext),
            hasBluetoothPermission = bluetoothMonitor.hasBluetoothPermission(),
            hasLocationPermission = networkMonitor.hasLocationPermission(),
            isLocationServiceEnabled = locEnabled,
            emlidPackageName = settingsMonitor.findEmlidPackageName()
        )
        val nowStr = timeFormat.format(Date())

        _uiState.update { current ->
            current.copy(
                bluetooth = bt,
                wifi = wifi,
                savedEmlidWifiNetworks = savedEmlidWifi,
                samsungChecks = checks,
                automation = capabilities,
                lastRefreshTime = nowStr
            )
        }
    }

    fun updateResetConfig(transform: (ResetConfig) -> ResetConfig) {
        val updated = transform(_uiState.value.resetConfig)
        prefs.saveResetConfig(updated)
        _uiState.update { it.copy(resetConfig = updated) }
    }

    fun startFullReset() {
        if (resetJob?.isActive == true) return
        val config = _uiState.value.resetConfig
        resetJob = viewModelScope.launch {
            resetOrchestrator.runFullReset(config)
        }
    }

    fun confirmManualResetStep() {
        resetOrchestrator.confirmManualStep()
    }

    fun cancelReset() {
        resetJob?.cancel()
        resetJob = null
        resetOrchestrator.cancelReset()
    }

    fun dismissResetResult() {
        resetOrchestrator.dismissCompletedBanner()
    }

    fun quickResetBluetoothOnly() {
        viewModelScope.launch {
            if (shizukuController.state.value == ShizukuState.READY) {
                appendLog("Schnell-Reset: Bluetooth wird kurz getrennt & neu gestartet…", false)
                shizukuController.setBluetoothEnabled(false)
                delay(3000)
                shizukuController.setBluetoothEnabled(true)
                delay(1800)
                refreshAllInternal()
                appendLog("Bluetooth-Schnell-Reset abgeschlossen", false)
            } else {
                appendLog("Shizuku nicht aktiv – öffne Android-Bluetooth-Einstellungen", false)
                settingsMonitor.openBluetoothSettings()
            }
        }
    }

    fun handleSettingAction(action: SettingActionType) {
        when (action) {
            SettingActionType.NONE -> Unit
            SettingActionType.OPEN_EMLID_APP_INFO,
            SettingActionType.OPEN_EMLID_BATTERY -> settingsMonitor.openEmlidAppInfo()

            SettingActionType.FIX_BATTERY_SHIZUKU -> {
                val pkg = settingsMonitor.findEmlidPackageName() ?: return
                viewModelScope.launch {
                    val res = shizukuController.addBatteryWhitelist(pkg)
                    if (res.isSuccess) {
                        appendLog("Akkuoptimierung für $pkg per Shizuku deaktiviert!", false)
                    } else {
                        appendLog("Akku-Whitelist fehlgeschlagen: ${res.output}", true)
                    }
                    delay(400)
                    refreshAllInternal()
                }
            }

            SettingActionType.OPEN_WIFI_SETTINGS -> settingsMonitor.openWifiSettings()
            SettingActionType.OPEN_BLUETOOTH_SETTINGS -> settingsMonitor.openBluetoothSettings()
            SettingActionType.OPEN_VPN_SETTINGS -> settingsMonitor.openVpnSettings()
            SettingActionType.OPEN_LOCATION_SETTINGS -> settingsMonitor.openLocationSettings()
            SettingActionType.LAUNCH_EMLID_FLOW -> {
                if (!settingsMonitor.launchEmlidFlow()) {
                    appendLog("Emlid Flow konnte nicht gestartet werden", true)
                }
            }
        }
    }

    fun requestShizukuPermission() {
        shizukuController.requestPermission()
    }

    fun openShizukuApp() {
        settingsMonitor.openShizukuOrPlayStore()
    }

    fun openAccessibilitySettings() {
        settingsMonitor.openAccessibilitySettings()
    }

    fun openAirplaneModeSettings() {
        settingsMonitor.openAirplaneModeSettings()
    }

    fun launchEmlidFlow() {
        settingsMonitor.launchEmlidFlow()
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun appendLog(message: String, isError: Boolean) {
        val entry = DiagnosticLogEntry(
            timestamp = timeFormat.format(Date()),
            message = message,
            isError = isError
        )
        _uiState.update { state ->
            state.copy(logs = (listOf(entry) + state.logs).take(60))
        }
    }

    override fun onCleared() {
        super.onCleared()
        AccessibilityBridge.logCallback = null
        shizukuController.stop()
        if (receiverRegistered) {
            runCatching { appContext.unregisterReceiver(systemStateReceiver) }
            receiverRegistered = false
        }
    }
}
