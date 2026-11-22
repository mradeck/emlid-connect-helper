package de.emlidhelper.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.emlidhelper.app.config.AppMetadata
import de.emlidhelper.app.data.AppLanguage
import de.emlidhelper.app.i18n.AppStrings

private data class PortfolioAppLink(
    val label: String,
    val url: String
)

private val PORTFOLIO_APPS = listOf(
    PortfolioAppLink("GPS / UTM Converter", "https://gps2utm.netlify.app/"),
    PortfolioAppLink("Geodata Inspector & Cleaner", "https://geodata-inspector-cleaner.netlify.app/"),
    PortfolioAppLink("GeoTIFF Inspector", "https://mradeck.github.io/geotiff-inspector/"),
    PortfolioAppLink("MeasureMap", "https://measuremap-viewer-editor.netlify.app/"),
    PortfolioAppLink("Geoid-TIFF 2 PGM Converter", "https://geoid-tiff2pgm-converter.netlify.app/"),
    PortfolioAppLink("DXF Coordinate Forge", "https://dxf-coordinate-forge.netlify.app/"),
    PortfolioAppLink("PointCloud Manager", "https://www.pointcloud-manager.com"),
    PortfolioAppLink("SkyCheck", "https://skycheck-de.netlify.app/"),
    PortfolioAppLink("michael-radeck.de", "https://www.michael-radeck.de")
)

private const val REPO_URL = "https://github.com/mradeck/emlid-connect-helper"
private const val IMPRINT_URL = "https://www.multikopterschule.de/Kontakt-Impressum/IMPRESSUM/"
private const val KOFI_URL = "https://ko-fi.com/mradeck/tip"
private const val CONTACT_EMAIL = "michael.radeck@email.de"
private val KOFI_ACCENT = Color(0xFFFF5E5B)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppFooter(
    strings: AppStrings,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSupportDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // More apps / Weitere Apps
        Text(
            text = strings.footerMoreApps,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PORTFOLIO_APPS.forEach { app ->
                FooterChipLink(
                    text = app.label,
                    external = true,
                    onClick = { openExternalUrl(context, app.url) }
                )
            }
        }

        // Legal / Support / Source / Imprint / Ko-fi row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            FooterChipLink(
                text = strings.footerSourceCode,
                external = true,
                onClick = { openExternalUrl(context, REPO_URL) }
            )
            FooterChipLink(
                text = strings.footerSupport,
                external = false,
                onClick = { showSupportDialog = true }
            )
            FooterChipLink(
                text = strings.footerLicenses,
                external = false,
                onClick = { showLicensesDialog = true }
            )
            FooterChipLink(
                text = strings.footerImprint,
                external = true,
                onClick = { openExternalUrl(context, IMPRINT_URL) }
            )
            // Ko-fi pill matching Michael's SPA apps
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(KOFI_ACCENT.copy(alpha = 0.16f))
                    .clickable { openExternalUrl(context, KOFI_URL) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = KOFI_ACCENT,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = strings.footerKofi,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = KOFI_ACCENT
                    )
                }
            }
        }

        // Copyright & Version line
        Text(
            text = strings.footerCopyrightLine(AppMetadata.APP_VERSION_CODE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }

    if (showSupportDialog) {
        SupportDialog(
            strings = strings,
            language = language,
            onDismiss = { showSupportDialog = false },
            onOpenUrl = { url -> openExternalUrl(context, url) }
        )
    }

    if (showLicensesDialog) {
        LicensesDialog(
            strings = strings,
            onDismiss = { showLicensesDialog = false }
        )
    }
}

@Composable
private fun FooterChipLink(
    text: String,
    external: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (external) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun SupportDialog(
    strings: AppStrings,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val version = AppMetadata.APP_VERSION_CODE
    val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    val guideUrl = if (language == AppLanguage.EN) "$REPO_URL/blob/main/GUIDE.md" else "$REPO_URL/blob/main/ANLEITUNG.md"

    val bugSubject = if (language == AppLanguage.EN) {
        "Emlid Connect Helper $version – Bug report"
    } else {
        "Emlid Connect Helper $version – Bugreport"
    }
    val bugBody = if (language == AppLanguage.EN) {
        "Steps to reproduce:\n\nExpected result:\n\nActual result / error message:\n\nDevice & Android version: $deviceInfo"
    } else {
        "Schritte zum Fehler:\n\nErwartetes Ergebnis:\n\nTatsächliches Ergebnis / Fehlermeldung:\n\nGerät & Android-Version: $deviceInfo"
    }
    val contactSubject = if (language == AppLanguage.EN) {
        "Emlid Connect Helper $version – Contact / help"
    } else {
        "Emlid Connect Helper $version – Kontakt / Hilfe"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = AppMetadata.APP_NAME,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(strings.supportTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(strings.supportIntro, style = MaterialTheme.typography.bodyMedium)

                OutlinedButton(
                    onClick = { onOpenUrl(guideUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.supportOpenGuide)
                }

                OutlinedButton(
                    onClick = {
                        val mailto = "mailto:$CONTACT_EMAIL?subject=${Uri.encode(bugSubject)}&body=${Uri.encode(bugBody)}"
                        onOpenUrl(mailto)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.supportBugEmail)
                }

                OutlinedButton(
                    onClick = {
                        val mailto = "mailto:$CONTACT_EMAIL?subject=${Uri.encode(contactSubject)}"
                        onOpenUrl(mailto)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.supportContactEmail)
                }

                Text(
                    text = CONTACT_EMAIL,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = strings.supportBugHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strings.supportPrivacyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.okButton)
            }
        }
    )
}

@Composable
private fun LicensesDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = strings.licensesEyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(strings.licensesTitle, fontWeight = FontWeight.Bold)
                Text(
                    text = strings.licensesSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(strings.licensesIntro, style = MaterialTheme.typography.bodySmall)
                LicenseRow(AppMetadata.APP_NAME, strings.licenseAppDesc)
                LicenseRow("AndroidX & Jetpack Compose", strings.licenseAndroidxDesc)
                LicenseRow("Kotlin & Coroutines", strings.licenseKotlinDesc)
                LicenseRow("Shizuku API", strings.licenseShizukuDesc)
                HorizontalDivider()
                Text(
                    text = "${strings.licenseDateStamp} · ${AppMetadata.APP_VERSION_CODE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.okButton)
            }
        }
    )
}

@Composable
private fun LicenseRow(component: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Text(component, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun openExternalUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
