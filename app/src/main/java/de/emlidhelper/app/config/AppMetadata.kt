package de.emlidhelper.app.config

import de.emlidhelper.app.BuildConfig

data class ParsedAppVersion(
    val year2: String,
    val month2: String,
    val major: Int,
    val subversion: Int
)

object AppMetadata {
    const val APP_NAME = "Emlid Connect Helper"
    val APP_PACKAGE_VERSION: String = BuildConfig.VERSION_NAME

    private val parsed = parsePackageVersion(APP_PACKAGE_VERSION)

    val APP_VERSION_YEAR2: String = parsed.year2
    val APP_VERSION_MONTH2: String = parsed.month2
    val APP_VERSION_MAJOR: Int = parsed.major
    val APP_VERSION_SUB: Int = parsed.subversion
    val APP_VERSION_REVISION: Int = parsed.major

    val APP_VERSION_CODE: String =
        "v${parsed.year2}.${parsed.month2}.${parsed.major}.${parsed.subversion}"

    val APP_TITLE: String = "$APP_NAME $APP_VERSION_CODE"

    fun parsePackageVersion(raw: String): ParsedAppVersion {
        val parts = raw.trim().split(".")
        val yymmPadded = parts.getOrNull(0).orEmpty().padStart(4, '0')
        val year2 = yymmPadded.dropLast(2).takeLast(2).padStart(2, '0')
        val month2 = yymmPadded.takeLast(2)
        val major = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val subversion = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return ParsedAppVersion(
            year2 = year2,
            month2 = month2,
            major = major,
            subversion = subversion
        )
    }
}
