package de.emlidhelper.app.shizuku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuWifiParserTest {

    @Test
    fun parseSavedWifiNetworks_deduplicatesIdsAndDetectsReach() {
        val sample = """
            Network Id      SSID                         Security type
            0            (m)athaeser mobilespot           open
            0            (m)athaeser mobilespot           owe^
            1            @BayernWLAN                      open
            1            @BayernWLAN                      owe^
            7            reach:45:ab                      wpa2-psk
            7            reach:45:ab                      sae^
            12           Emlid RS4 Field                  wpa2-psk
        """.trimIndent()

        val parsed = ShizukuController.parseSavedWifiNetworks(sample)
        assertEquals(4, parsed.size)

        assertEquals(0, parsed[0].networkId)
        assertEquals("(m)athaeser mobilespot", parsed[0].ssid)
        assertFalse(parsed[0].isEmlidRelated)

        assertEquals(7, parsed[2].networkId)
        assertEquals("reach:45:ab", parsed[2].ssid)
        assertTrue(parsed[2].isEmlidRelated)

        assertEquals(12, parsed[3].networkId)
        assertEquals("Emlid RS4 Field", parsed[3].ssid)
        assertTrue(parsed[3].isEmlidRelated)
    }

    @Test
    fun parseSavedWifiNetworks_handlesEmptyOutput() {
        assertTrue(ShizukuController.parseSavedWifiNetworks("").isEmpty())
        assertTrue(ShizukuController.parseSavedWifiNetworks("Network Id      SSID      Security type").isEmpty())
    }
}
