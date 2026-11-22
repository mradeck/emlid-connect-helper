package de.emlidhelper.app.config

import org.junit.Assert.assertEquals
import org.junit.Test

class AppMetadataTest {

    @Test
    fun parsePackageVersion_standardFormat() {
        val parsed = AppMetadata.parsePackageVersion("2611.1.0")
        assertEquals("26", parsed.year2)
        assertEquals("11", parsed.month2)
        assertEquals(1, parsed.major)
        assertEquals(0, parsed.subversion)
    }

    @Test
    fun parsePackageVersion_hotfixAndShortYymmPadding() {
        val hotfix = AppMetadata.parsePackageVersion("2611.1.2")
        assertEquals("26", hotfix.year2)
        assertEquals("11", hotfix.month2)
        assertEquals(1, hotfix.major)
        assertEquals(2, hotfix.subversion)

        val padded = AppMetadata.parsePackageVersion("605.67.1")
        assertEquals("06", padded.year2)
        assertEquals("05", padded.month2)
        assertEquals(67, padded.major)
        assertEquals(1, padded.subversion)
    }

    @Test
    fun appMetadata_matchesCurrentBuildConfig() {
        assertEquals("2611.2.0", AppMetadata.APP_PACKAGE_VERSION)
        assertEquals("v26.11.2.0", AppMetadata.APP_VERSION_CODE)
        assertEquals("Emlid Connect Helper v26.11.2.0", AppMetadata.APP_TITLE)
        assertEquals(2, AppMetadata.APP_VERSION_REVISION)
    }
}
