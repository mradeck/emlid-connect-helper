package de.emlidhelper.app.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import de.emlidhelper.app.BuildConfig
import de.emlidhelper.app.data.SavedWifiNetwork
import de.emlidhelper.app.data.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class ShellResult(
    val exitCode: Int,
    val output: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class ShizukuController(
    private val context: Context,
    private val onLog: (String, Boolean) -> Unit
) {
    private val _state = MutableStateFlow(ShizukuState.NOT_INSTALLED)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val _uid = MutableStateFlow<Int?>(null)
    val uid: StateFlow<Int?> = _uid.asStateFlow()

    @Volatile
    private var shellService: IPrivilegedShellService? = null
    private var isBound = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, PrivilegedShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null && service.pingBinder()) {
                shellService = IPrivilegedShellService.Stub.asInterface(service)
                _state.value = ShizukuState.READY
                _uid.value = runCatching { Shizuku.getUid() }.getOrNull()
                onLog("Shizuku Shell-Dienst verbunden (UID ${_uid.value ?: "?"})", false)
            } else {
                shellService = null
                _state.value = ShizukuState.NOT_RUNNING
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            isBound = false
            refreshState()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        isBound = false
        _uid.value = null
        refreshState()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    onLog("Shizuku-Berechtigung erteilt", false)
                    bindServiceIfReady()
                } else {
                    onLog("Shizuku-Berechtigung abgelehnt", true)
                    _state.value = ShizukuState.PERMISSION_REQUIRED
                }
            }
        }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshState()
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        if (isBound) {
            runCatching { Shizuku.unbindUserService(userServiceArgs, serviceConnection, true) }
            isBound = false
        }
        shellService = null
    }

    fun refreshState() {
        val shizukuInstalled = isShizukuAppInstalled()
        if (!Shizuku.pingBinder()) {
            _uid.value = null
            _state.value = if (shizukuInstalled) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
            return
        }

        if (Shizuku.isPreV11()) {
            _state.value = ShizukuState.NOT_RUNNING
            return
        }

        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        if (!granted) {
            _state.value = ShizukuState.PERMISSION_REQUIRED
            return
        }

        if (shellService != null && shellService!!.asBinder().pingBinder()) {
            _state.value = ShizukuState.READY
            _uid.value = runCatching { Shizuku.getUid() }.getOrNull()
        } else {
            bindServiceIfReady()
        }
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            refreshState()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindServiceIfReady()
        } else {
            runCatching {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }.onFailure {
                onLog("Shizuku-Anfrage fehlgeschlagen: ${it.message}", true)
            }
        }
    }

    private fun bindServiceIfReady() {
        if (shellService != null && shellService!!.asBinder().pingBinder()) {
            _state.value = ShizukuState.READY
            return
        }
        _state.value = ShizukuState.CONNECTING
        runCatching {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
            isBound = true
        }.onFailure {
            onLog("Shizuku UserService Bind-Fehler: ${it.message}", true)
            _state.value = ShizukuState.NOT_RUNNING
        }
    }

    private fun isShizukuAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun runCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        val service = shellService
        if (service == null || !service.asBinder().pingBinder()) {
            return@withContext ShellResult(-1, "Shizuku-Dienst nicht verbunden")
        }
        return@withContext try {
            val raw = service.exec(command)
            val firstNewline = raw.indexOf('\n')
            val header = if (firstNewline >= 0) raw.substring(0, firstNewline) else raw
            val body = if (firstNewline >= 0) raw.substring(firstNewline + 1) else ""
            val code = header.removePrefix("EXIT:").trim().toIntOrNull() ?: -1
            ShellResult(code, body.trim())
        } catch (e: Exception) {
            ShellResult(-1, "Binder-Fehler: ${e.message}")
        }
    }

    suspend fun forceStopPackage(packageName: String): ShellResult {
        onLog("Shizuku: am force-stop $packageName", false)
        return runCommand("am force-stop $packageName")
    }

    suspend fun setAirplaneMode(enabled: Boolean): ShellResult {
        val action = if (enabled) "enable" else "disable"
        onLog("Shizuku: Flugmodus -> $action", false)
        val result = runCommand("cmd connectivity airplane-mode $action")
        if (result.isSuccess) return result

        // Fallback for older/custom implementations
        val flag = if (enabled) "1" else "0"
        return runCommand(
            "settings put global airplane_mode_on $flag && " +
                "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled"
        )
    }

    suspend fun setBluetoothEnabled(enabled: Boolean): ShellResult {
        val action = if (enabled) "enable" else "disable"
        onLog("Shizuku: Bluetooth -> $action", false)
        val res = runCommand("cmd bluetooth_manager $action")
        return if (res.isSuccess) res else runCommand("svc bluetooth $action")
    }

    suspend fun setWifiEnabled(enabled: Boolean): ShellResult {
        val action = if (enabled) "enabled" else "disabled"
        onLog("Shizuku: WLAN -> $action", false)
        val res = runCommand("cmd wifi set-wifi-enabled $action")
        return if (res.isSuccess) res else runCommand("svc wifi ${if (enabled) "enable" else "disable"}")
    }

    suspend fun addBatteryWhitelist(packageName: String): ShellResult {
        onLog("Shizuku: Akku-Whitelist für $packageName setzen", false)
        return runCommand("dumpsys deviceidle whitelist +$packageName")
    }

    suspend fun dumpBluetoothConnections(): ShellResult {
        return runCommand("dumpsys bluetooth_manager")
    }

    suspend fun querySamsungIntelligentWifi(): Map<String, String> {
        val keys = listOf(
            "global:wifi_watchdog_poor_network_test_enabled",
            "system:wifi_watchdog_poor_network_test_enabled",
            "system:wifi_switch_to_mobile_data_enabled",
            "global:wifi_switch_to_mobile_data_enabled",
            "global:wifi_poor_connection_warning"
        )
        val results = mutableMapOf<String, String>()
        for (entry in keys) {
            val (ns, key) = entry.split(":")
            val res = runCommand("settings get $ns $key")
            if (res.isSuccess && res.output.isNotBlank() && res.output != "null") {
                results[key] = res.output.trim()
            }
        }
        return results
    }

    suspend fun listSavedWifiNetworks(): List<SavedWifiNetwork> {
        val res = runCommand("cmd wifi list-networks")
        if (!res.isSuccess || res.output.isBlank()) return emptyList()
        return parseSavedWifiNetworks(res.output)
    }

    suspend fun forgetWifiNetwork(networkId: Int, ssidLabel: String): ShellResult {
        onLog("Shizuku: WLAN-Profil entfernen ($ssidLabel, ID $networkId)", false)
        return runCommand("cmd wifi forget-network $networkId")
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 4201
        private val SAVED_WIFI_LINE_REGEX = Regex("""^(\d+)\s+(.+?)\s+(\S+)$""")

        fun parseSavedWifiNetworks(output: String): List<SavedWifiNetwork> {
            val seenIds = linkedMapOf<Int, SavedWifiNetwork>()
            output.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith("Network Id", ignoreCase = true)) return@forEach
                val match = SAVED_WIFI_LINE_REGEX.matchEntire(line) ?: return@forEach
                val id = match.groupValues[1].toIntOrNull() ?: return@forEach
                val ssid = match.groupValues[2].trim().removePrefix("\"").removeSuffix("\"")
                if (ssid.isBlank()) return@forEach
                val lower = ssid.lowercase()
                val isEmlid = lower.startsWith("reach:") || lower.contains("reach") || lower.contains("emlid")
                seenIds.putIfAbsent(id, SavedWifiNetwork(networkId = id, ssid = ssid, isEmlidRelated = isEmlid))
            }
            return seenIds.values.toList()
        }
    }
}
