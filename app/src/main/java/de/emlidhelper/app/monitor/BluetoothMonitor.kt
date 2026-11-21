package de.emlidhelper.app.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import de.emlidhelper.app.data.BluetoothDeviceInfo
import de.emlidhelper.app.data.BluetoothStatus
import de.emlidhelper.app.data.ShizukuState
import de.emlidhelper.app.shizuku.ShizukuController

class BluetoothMonitor(private val context: Context) {

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isAirplaneModeOn(): Boolean {
        return runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    suspend fun inspect(shizuku: ShizukuController): BluetoothStatus {
        val airplaneOn = isAirplaneModeOn()
        val hasPerm = hasBluetoothPermission()
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = btManager?.adapter

        if (!hasPerm) {
            return BluetoothStatus(
                hasPermission = false,
                isEnabled = adapter?.isEnabled == true,
                isAirplaneModeOn = airplaneOn
            )
        }

        val isEnabled = runCatching { adapter?.isEnabled == true }.getOrDefault(false)
        val connectedMap = linkedMapOf<String, BluetoothDeviceInfo>()
        val pairedEmlid = mutableListOf<BluetoothDeviceInfo>()
        var pairedCount = 0

        if (isEnabled && adapter != null && btManager != null) {
            // 1. Active BLE / GATT devices
            val gattDevices = runCatching {
                btManager.getConnectedDevices(BluetoothProfile.GATT) +
                    btManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
            }.getOrDefault(emptyList())

            for (device in gattDevices) {
                val addr = device.address ?: continue
                val name = safeDeviceName(device)
                connectedMap[addr] = BluetoothDeviceInfo(
                    name = name,
                    address = addr,
                    connectionType = "BLE / GATT",
                    isEmlidRelated = isEmlidName(name),
                    isConnected = true
                )
            }

            // 2. Bonded devices: check active ACL connection & Emlid system pairing warning
            val bonded = runCatching { adapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
            pairedCount = bonded.size
            for (device in bonded) {
                val addr = device.address ?: continue
                val name = safeDeviceName(device)
                val emlid = isEmlidName(name)
                val active = isDeviceConnectedReflection(device)

                if (emlid) {
                    pairedEmlid.add(
                        BluetoothDeviceInfo(
                            name = name,
                            address = addr,
                            connectionType = "Gekoppelt (Android-System)",
                            isEmlidRelated = true,
                            isConnected = active
                        )
                    )
                }

                if (active && !connectedMap.containsKey(addr)) {
                    connectedMap[addr] = BluetoothDeviceInfo(
                        name = name,
                        address = addr,
                        connectionType = "Bluetooth (Aktiv)",
                        isEmlidRelated = emlid,
                        isConnected = true
                    )
                }
            }
        }

        // 3. Optional Shizuku enhancement: inspect dumpsys bluetooth_manager for hidden BLE/ACL links
        var shizukuEnhanced = false
        if (isEnabled && shizuku.state.value == ShizukuState.READY) {
            val dump = shizuku.dumpBluetoothConnections()
            if (dump.isSuccess && dump.output.isNotBlank()) {
                shizukuEnhanced = true
                parseDumpsysConnectedDevices(dump.output).forEach { info ->
                    if (!connectedMap.containsKey(info.address)) {
                        connectedMap[info.address] = info
                    }
                }
            }
        }

        return BluetoothStatus(
            hasPermission = true,
            isEnabled = isEnabled,
            isAirplaneModeOn = airplaneOn,
            connectedDevices = connectedMap.values.toList(),
            pairedEmlidDevices = pairedEmlid,
            pairedCount = pairedCount,
            shizukuEnhanced = shizukuEnhanced
        )
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String {
        return runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unbekanntes Gerät"
    }

    private fun isDeviceConnectedReflection(device: BluetoothDevice): Boolean {
        return runCatching {
            val method = device.javaClass.getMethod("isConnected")
            (method.invoke(device) as? Boolean) == true
        }.getOrDefault(false)
    }

    private fun parseDumpsysConnectedDevices(output: String): List<BluetoothDeviceInfo> {
        val found = linkedMapOf<String, BluetoothDeviceInfo>()
        val macRegex = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")

        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val lower = trimmed.lowercase()
            val looksConnected =
                (lower.contains("state=connected") ||
                    lower.contains("connectionstate: connected") ||
                    lower.contains("mstate: connected") ||
                    lower.contains("connected devices:") ||
                    (lower.contains("acl") && lower.contains("connected") && !lower.contains("disconnected")))

            if (looksConnected) {
                val macMatch = macRegex.find(trimmed)
                if (macMatch != null) {
                    val mac = macMatch.value.uppercase()
                    // Try to extract name if present in parentheses or after name=
                    val extractedName = Regex("""(?:name[=:]\s*|[\[(])([^,)\]]{2,32})""")
                        .find(trimmed)?.groupValues?.getOrNull(1)?.trim()
                        ?.takeIf { !macRegex.matches(it) && !it.equals("connected", true) }
                        ?: "Bluetooth-Gerät ($mac)"

                    found[mac] = BluetoothDeviceInfo(
                        name = extractedName,
                        address = mac,
                        connectionType = "System / BLE (Shizuku)",
                        isEmlidRelated = isEmlidName(extractedName) || lower.contains("reach") || lower.contains("emlid"),
                        isConnected = true
                    )
                }
            }
        }
        return found.values.toList()
    }

    companion object {
        private val EMLID_KEYWORDS = listOf("reach", "emlid", "rs4", "rs3", "rs2", "rs+", "rx")

        fun isEmlidName(name: String): Boolean {
            val lower = name.lowercase()
            return EMLID_KEYWORDS.any { lower.contains(it) }
        }
    }
}
