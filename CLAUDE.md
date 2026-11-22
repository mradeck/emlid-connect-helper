# Emlid Connect Helper – Projektdokumentation

**Aktuelle Version:** `v26.11.2.2` (`versionName = "2611.2.2"`, `versionCode = 26110202`)  
**Repository:** https://github.com/mradeck/emlid-connect-helper (privat)  
**Zielplattform:** Android 12+ (`minSdk = 31`, `compileSdk = 36`, getestet auf Samsung Galaxy S24 Ultra · One UI 9.0 / Android 17)

## Versions-Schema (Android-Mapping)

Folgt dem globalen Schema `vYY.MM.major.subversion`:
- `app/build.gradle.kts`:
  - `versionName = "YYMM.major.sub"` (z. B. `"2611.2.2"`)
  - `versionCode = YYMM * 10000 + major * 100 + sub` (z. B. `26110202`)
- Zentraler Parser & Konstanten: `app/src/main/java/de/emlidhelper/app/config/AppMetadata.kt`
- UI-Anzeige: Vollständiger Versions-String `AppMetadata.APP_VERSION_CODE` (`v26.11.2.2`) dauerhaft in der `TopAppBar` (`MainActivity.kt`) und im Footer (`AppFooter.kt`).

## Versions-Historie

| Version | `versionName` | `versionCode` | Datum | Änderungen |
| :--- | :--- | :--- | :--- | :--- |
| `v26.11.2.2` | `2611.2.2` | `26110202` | 2026-11-22 | Praxishinweise (DE/EN + In-App) zu RS4-Pro-WLAN vor Gebäuden, Fahrzeug-Bluetooth + Flugmodus-Fix und Check nach Android-/One-UI-Systemupdates |
| `v26.11.2.1` | `2611.2.1` | `26110201` | 2026-11-22 | „Reach-WLAN vergessen („Immer verbinden“ zurücksetzen)“-Button via Shizuku (`cmd wifi list-networks` / `forget-network`) + manueller Fallback & Parser-Tests |
| `v26.11.2.0` | `2611.2.0` | `26110200` | 2026-11-22 | RS4-Pro-IP-Browser-Button, DE/EN-Flaggen-Umschalter, Hell/Dunkel-Theme, Portfolio-Footer (Apps/Support/Lizenzen/Impressum/Ko-fi), `README.en.md`, Android-12+- & One-UI-9.0-Doku |
| `v26.11.1.0` | `2611.1.0` | `26110100` | 2026-11-22 | Erstversion: 1-Tap-Reset (Shizuku + Auto-Cache + Fallback), BT/WLAN/VPN-Monitor, Samsung-Checkliste, DE/EN-Anleitungen, `AppMetadata`-Versionierung |

## Build, Test & Installation (macOS)

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architektur-Anker

- `app/src/main/java/de/emlidhelper/app/config/AppMetadata.kt` – Versions-Parser (`vYY.MM.major.sub`)
- `app/src/main/java/de/emlidhelper/app/shizuku/` – AIDL `PrivilegedShellService` + `ShizukuController`
- `app/src/main/java/de/emlidhelper/app/accessibility/EmlidCacheAccessibilityService.kt` – strikt geschützter Auto-Cache-Service
- `app/src/main/java/de/emlidhelper/app/monitor/` – `BluetoothMonitor`, `NetworkMonitor`, `SamsungSettingsMonitor`
- `app/src/main/java/de/emlidhelper/app/reset/ResetOrchestrator.kt` – Vollautomatik (Variante B) + geführter Fallback (Variante A)
- `app/src/main/java/de/emlidhelper/app/ui/` – `HelperViewModel` + Compose-Screens (`Dashboard`, `Samsung`, `Automation`)
