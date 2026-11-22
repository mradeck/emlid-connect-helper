package de.emlidhelper.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiNetworkStatusTest {

    @Test
    fun browseUrl_usesDetectedGatewayWhenConnected() {
        val status = WifiNetworkStatus(
            isWifiEnabled = true,
            isConnected = true,
            ssid = "reach:12:34",
            gatewayAddress = "192.168.42.1",
            isEmlidHotspot = true
        )
        assertEquals("192.168.42.1", status.browseIpAddress)
        assertEquals("http://192.168.42.1", status.receiverBrowseUrl)
    }

    @Test
    fun browseUrl_fallsBackToDefaultReachIpForEmlidHotspot() {
        val status = WifiNetworkStatus(
            isWifiEnabled = true,
            isConnected = true,
            ssid = "reach:ab:cd",
            gatewayAddress = null,
            isEmlidHotspot = true
        )
        assertEquals("192.168.42.1", status.browseIpAddress)
        assertEquals("http://192.168.42.1", status.receiverBrowseUrl)
    }

    @Test
    fun browseUrl_isNullWhenNotConnected() {
        val status = WifiNetworkStatus(
            isWifiEnabled = true,
            isConnected = false,
            gatewayAddress = "192.168.42.1",
            isEmlidHotspot = true
        )
        assertNull(status.browseIpAddress)
        assertNull(status.receiverBrowseUrl)
    }
}
