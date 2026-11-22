# Emlid Connect Helper `v26.11.2.0`

**Sprache / Language:** **Deutsch** | **[English (README.en.md)](README.en.md)**  
**Aktuelle Version:** `v26.11.2.0` (`versionName = "2611.2.0"`, `versionCode = 26110200`, Stand: November 2026)

Native Android-Helfer-App (Kotlin / Jetpack Compose) für zuverlässige Verbindungen zwischen Android-Smartphones (primär optimiert & getestet auf **Samsung Galaxy S24 Ultra**) und einem **Emlid Reach RS4 Pro** (sowie weiteren Emlid-Reach-Empfängern) mit **Emlid Flow** (`com.emlid.reachview3`).

> **Ausführliche Schritt-für-Schritt-Anleitungen (inkl. aller Menüpfade & Nebenbedingungen):**  
> - **Deutsch:** [ANLEITUNG.md](ANLEITUNG.md)  
> - **English:** [GUIDE.md](GUIDE.md)

---

## Kompatibilität & getestete Umgebung

Obwohl die App aus einem typischen Samsung-Problem heraus entstanden ist (aggressives One-UI-Energiemanagement & *Intelligent Wi-Fi*), nutzt sie Standard-Android-APIs und funktioniert grundsätzlich auf **allen Android-Smartphones und -Tablets ab Android 12 (`minSdk 31`)**.

### Getestete Referenz-Umgebung
- **Gerät:** Samsung Galaxy S24 Ultra (`SM-S928B`)
- **System:** **Samsung One UI 9.0 / Android 17** *(inkl. Intelligent Wi-Fi `9.0.0`)*
- **Empfänger & App:** Emlid Reach RS4 Pro + Emlid Flow (`com.emlid.reachview3`)

### Geschätzte Kompatibilität nach Geräteklasse

| Geräteklasse | Geschätzter Status | Hinweise |
| :--- | :--- | :--- |
| **Samsung Galaxy (One UI 6 – 9.0 / Android 14 – 17)** | **Voll unterstützt** *(Referenz)* | Inkl. Live-Prüfung für Akkuoptimierung, *Intelligent Wi-Fi* und One-UI-Menüpfade. |
| **Google Pixel / Motorola / Fairphone / Stock Android (Android 12+)** | **Sehr hoch** | Dashboard, Bluetooth-/WLAN-/VPN-Monitor, Shizuku-Reset & Auto-Cache (DE/EN-Systemlabels) funktionieren direkt; der Samsung-spezifische *Intelligent Wi-Fi*-Punkt wird sauber als Info-Hinweis angezeigt. |
| **Xiaomi (HyperOS/MIUI), OnePlus, Oppo, Huawei/Honor (Android 12+)** | **Hoch (mit kleinen Abweichungen)** | Grundfunktionen & Shizuku-Reset laufen; Hersteller-Sondermenüs (z. B. „Autostart“ oder abweichende Speicher-Labels) können für *Auto-Cache* ggf. manuellen Fallback nutzen. |
| **Android 11 oder älter (`API < 31`)** | **Nicht unterstützt** | App setzt `minSdk = 31` (Android 12+) für moderne Bluetooth- & Netzwerk-APIs voraus. |

---

## Funktionen (Variante B + automatischer Fallback auf Variante A)

- **1-Tap Emlid Reset & Verbinden:**
  - Beendet **Emlid Flow** sauber (`am force-stop com.emlid.reachview3` per Shizuku).
  - Leert automatisch **nur den Cache** von Emlid Flow (per strikt geschützter Bedienungshilfe – klickt niemals auf *Daten löschen*).
  - Schaltet den **Flugmodus** für eine frei wählbare Zeit (10 / 15 / 20 / 30 Sek.) ein und danach automatisch wieder aus.
  - Prüft, ob **alle Bluetooth-Verbindungen getrennt** sind (inkl. Deep-Scan über `dumpsys bluetooth_manager`) und startet anschließend **Emlid Flow**.
  - **Automatischer Fallback:** Ist Shizuku einmal nicht gestartet, führt die App dich automatisch mit Live-Flugmodus-Erkennung, Countdown und Vibration durch den Reset.
- **Live-Bluetooth-Monitor:** Zeigt aktiv verbundene BLE-/Bluetooth-Geräte an, meldet grün, wenn der Kanal komplett frei ist, und warnt, falls der RS4 Pro versehentlich in den Android-Bluetooth-Einstellungen gekoppelt wurde.
- **Live-WLAN- & VPN-Monitor:** Zeigt verbundene WLAN-SSID (mit Hervorhebung für `reach:XX:XX`), lokale IP, Gateway/Empfänger-IP, Internetstatus („Ohne Internet“) und warnt bei aktivem VPN.
- **Geräte-/Samsung-Checkliste:** Live-Prüfung für Akkuoptimierung (*Nicht eingeschränkt*, inkl. 1-Tap-Fix via Shizuku), VPN, Standort, BT-Kopplung und Samsung *Intelligent Wi-Fi*.
- **Direktstart:** Eigener Button zum direkten Öffnen von **Emlid Flow**.

---

## Praxis-Hinweis (Samsung One UI 9.0): Kein Internet in der Gemini-App trotz funktionierendem Chrome

Wenn das Smartphone per WLAN mit dem Empfänger-Hotspot (`reach:XX:XX`, *„Verbunden ohne Internet“*) verbunden ist, tritt unter **Samsung One UI 9.0 / Android 17** ein bekanntes App-Verhalten auf:
- **Chrome** routet Webanfragen sauber über Mobilfunk (5G) weiter – Internet im Browser funktioniert.
- Einzelne Apps wie die **native Gemini-App** erkennen jedoch das aktive WLAN ohne Internet (`!NET_CAPABILITY_VALIDATED`) und melden **„Kein Internet“**, sodass man WLAN kurz ausschalten müsste.
- **Praxis-Workarounds im Feld:**
  1. **Chrome nutzen** (z. B. `gemini.google.com` im Browser), solange das Reach-WLAN verbunden ist, **oder**
  2. **Empfohlener Feld-Setup:** Den **Reach RS4 Pro als Client mit dem mobilen Hotspot des Smartphones verbinden** (statt das Smartphone mit `reach:XX:XX`). Dann behalten **alle** Apps (Gemini, Chrome, NTRIP-Korrekturdaten) uneingeschränktes 5G-Internet!

---

## Installation & Build

### Option 1: Fertige APK über GitHub Releases (ohne Build)
Fertige, direkt installierbare `.apk`-Pakete können über den Bereich **Releases** dieses Repositories heruntergeladen und direkt auf dem Smartphone (oder per `adb install`) installiert werden – ein Google Play Developer Konto ist dafür nicht erforderlich.

### Option 2: Lokal bauen (macOS mit Android Studio JDK)
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Einrichtung für Vollautomatik (Variante B, ohne Root)
Alle Klickpfade und wichtigen Nebenbedingungen (z. B. *WLAN einschalten, bevor „Zu mobilen Daten wechseln“ geändert werden kann*, *Auto Blocker*, *Eingeschränkte Einstellungen zulassen*, *Shizuku im WLAN koppeln*) stehen ausführlich in **[ANLEITUNG.md](ANLEITUNG.md)**.

## Neu in v26.11.2.0
- **RS4-Pro-Hotspot-IP direkt im Browser öffnen:** Automatische Erkennung der Empfänger-/Gateway-IP (`LinkProperties` + `DhcpInfo` + `192.168.42.1`-Fallback) mit 1-Tap-Button *„RS4 Pro im Browser öffnen“*.
- **DE / EN Sprachumschalter** mit Vektor-Flaggen-Icon in der Kopfzeile (komplette App live zweisprachig).
- **Hell / Dunkel Theme-Umschalter** in der Kopfzeile (inkl. Statusleisten-Anpassung und Speicherung).
- **Fußzeile wie bei `gps2utm.netlify.app`:** Weitere Apps (9 Tools), Quellcode, Hilfe/Bugreport/Kontakt-Dialog, Lizenzen/Copyright-Dialog, Impressum & Ko-fi.
- **Englische README (`README.en.md`), Android-12+-Kompatibilitätstabelle** (Referenz: S24 Ultra, One UI 9.0 / Android 17) und Praxis-Hinweis zum One-UI-Netzwerkverhalten (Gemini-App vs. Chrome).

## Neu in v26.11.1.0
- Erste vollständige Version (Variante B + automatischer Fallback auf Variante A) mit 1-Tap-Reset (Force-Stop + Auto-Cache + Flugmodus-Timer), Live-Bluetooth-/WLAN-/VPN-Monitor, Samsung-Checkliste, Direktstart-Button für Emlid Flow und verbindlichem Versionsschema (`AppMetadata`, dauerhaft sichtbar in der App-Kopfzeile).
