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
import de.emlidhelper.app.data.WifiNetworkStatus
import java.net.Inet4Address

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
    fun inspect(): WifiNetworkStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiEnabled = runCatching { wm?.isWifiEnabled == true }.getOrDefault(false)

        var isVpnActive = false
        var wifiConnected = false
        var hasValidatedInternet = false
        var ipAddress: String? = null
        var gatewayAddress: String? = null
        var wifiSsid: String? = null
        var activeTransportLabel = "Kein aktives Netzwerk"

        if (cm != null) {
            val allNetworks = runCatching { cm.allNetworks }.getOrDefault(emptyArray())
            for (network in allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    isVpnActive = true
                }
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiConnected = true
                    hasValidatedInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                    // Android 12+ WifiInfo via NetworkCapabilities
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
                activeCaps == null -> "Offline / Kein Netz"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN aktiv"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WLAN"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobilfunk"
                activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Verbunden"
            }
        }

        // Fallback to WifiManager.connectionInfo for SSID if not already resolved
        if (wifiConnected && wifiSsid == null && wm != null) {
            @Suppress("DEPRECATION")
            val legacyInfo = runCatching { wm.connectionInfo }.getOrNull()
            wifiSsid = cleanSsid(legacyInfo?.ssid)
        }

        val ssidUnavailableReason = when {
            !wifiConnected -> null
            wifiSsid != null -> null
            !hasLocationPermission() -> "Standort-Berechtigung nötig, um den WLAN-Namen (SSID) anzuzeigen"
            !isLocationServiceEnabled() -> "Standort (GPS) einschalten, damit Android den WLAN-Namen freigibt"
            else -> "WLAN-Name vom System verborgen"
        }

        val isEmlidHotspot = wifiSsid?.let { ssid ->
            val lower = ssid.lowercase()
            lower.startsWith("reach:") || lower.contains("reach") || lower.contains("emlid")
        } ?: false

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
}
