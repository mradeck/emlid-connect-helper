package de.emlidhelper.app.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import de.emlidhelper.app.data.AppLanguage
import de.emlidhelper.app.data.WifiNetworkStatus
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteOrder

class NetworkMonitor(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationServiceEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { lm.isLocationEnabled }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun inspect(language: AppLanguage = AppLanguage.DE): WifiNetworkStatus {
        val isEn = language == AppLanguage.EN
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiEnabled = runCatching { wm?.isWifiEnabled == true }.getOrDefault(false)

        var isVpnActive = false
        var wifiConnected = false
        var hasValidatedInternet = false
        var ipAddress: String? = null
        var gatewayAddress: String? = null
        var wifiSsid: String? = null
        var activeTransportLabel = if (isEn) "No active network" else "Kein aktives Netzwerk"

        if (cm != null) {
            @Suppress("DEPRECATION")
            val allNetworks = runCatching { cm.allNetworks }.getOrDefault(emptyArray())
            for (network in allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    isVpnActive = true
                }
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiConnected = true
                    hasValidatedInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                    val transportInfo = caps.transportInfo as? WifiInfo
                    val candidateSsid = cleanSsid(transportInfo?.ssid)
                    if (candidateSsid != null) {
                        wifiSsid = candidateSsid
                    }

                    val linkProps = cm.getLinkProperties(network)
                    if (linkProps != null) {
                        ipAddress = linkProps.linkAddresses
                            .mapNotNull { it.address as? Inet4Address }
                            .firstOrNull { !it.isLoopbackAddress }
                            ?.hostAddress

                        gatewayAddress = linkProps.routes
                            .mapNotNull { it.gateway as? Inet4Address }
                            .firstOrNull { !it.isAnyLocalAddress && !it.isLoopbackAddress }
                            ?.hostAddress
                    }
                }
            }

            val activeCaps = cm.getNetworkCapabilities(cm.activeNetwork)
            activeTransportLabel = when {
                activeCaps == null -> if (isEn) "Offline / No network" else "Offline / Kein Netz"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> if (isEn) "VPN active" else "VPN aktiv"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> if (isEn) "Wi-Fi" else "WLAN"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> if (isEn) "Mobile data" else "Mobilfunk"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> if (isEn) "Connected" else "Verbunden"
            }
        }

        // Fallback to WifiManager.connectionInfo & DhcpInfo for SSID and gateway IP
        if (wifiConnected && wm != null) {
            if (wifiSsid == null) {
                @Suppress("DEPRECATION")
                val legacyInfo = runCatching { wm.connectionInfo }.getOrNull()
                wifiSsid = cleanSsid(legacyInfo?.ssid)
            }
            if (gatewayAddress == null) {
                @Suppress("DEPRECATION")
                val dhcpGateway = runCatching { wm.dhcpInfo?.gateway ?: 0 }.getOrDefault(0)
                gatewayAddress = intToIpv4(dhcpGateway)
            }
            if (ipAddress == null) {
                @Suppress("DEPRECATION")
                val dhcpIp = runCatching { wm.dhcpInfo?.ipAddress ?: 0 }.getOrDefault(0)
                ipAddress = intToIpv4(dhcpIp)
            }
        }

        val ssidUnavailableReason = when {
            !wifiConnected -> null
            wifiSsid != null -> null
            !hasLocationPermission() -> if (isEn) {
                "Location permission required to display the Wi-Fi name (SSID)"
            } else {
                "Standort-Berechtigung nötig, um den WLAN-Namen (SSID) anzuzeigen"
            }

            !isLocationServiceEnabled() -> if (isEn) {
                "Turn Location (GPS) ON so Android exposes the Wi-Fi name"
            } else {
                "Standort (GPS) einschalten, damit Android den WLAN-Namen freigibt"
            }

            else -> if (isEn) "Wi-Fi name hidden by system" else "WLAN-Name vom System verborgen"
        }

        val isEmlidHotspot = wifiSsid?.let { ssid ->
            val lower = ssid.lowercase()
            lower.startsWith("reach:") || lower.contains("reach") || lower.contains("emlid")
        } ?: (gatewayAddress == "192.168.42.1")

        return WifiNetworkStatus(
            isWifiEnabled = wifiEnabled,
            isConnected = wifiConnected,
            ssid = wifiSsid,
            ssidUnavailableReason = ssidUnavailableReason,
            ipAddress = ipAddress,
            gatewayAddress = gatewayAddress,
            hasValidatedInternet = hasValidatedInternet,
            isEmlidHotspot = isEmlidHotspot,
            isVpnActive = isVpnActive,
            activeTransportLabel = activeTransportLabel
        )
    }

    private fun cleanSsid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().removePrefix("\"").removeSuffix("\"")
        if (cleaned.equals("<unknown ssid>", ignoreCase = true) || cleaned == "0x") return null
        return cleaned
    }

    private fun intToIpv4(ipInt: Int): String? {
        if (ipInt == 0) return null
        val ordered = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            Integer.reverseBytes(ipInt)
        } else {
            ipInt
        }
        val bytes = byteArrayOf(
            ((ordered ushr 24) and 0xFF).toByte(),
            ((ordered ushr 16) and 0xFF).toByte(),
            ((ordered ushr 8) and 0xFF).toByte(),
            (ordered and 0xFF).toByte()
        )
        return runCatching { InetAddress.getByAddress(bytes).hostAddress }.getOrNull()
            ?.takeIf { it != "0.0.0.0" }
    }
}
