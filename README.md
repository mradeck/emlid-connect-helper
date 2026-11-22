# Emlid Connect Helper (Samsung Galaxy S24 Ultra)

Native Android-Helfer-App (Kotlin / Jetpack Compose) für zuverlässige Verbindungen zwischen einem **Samsung Galaxy S24 Ultra** und einem **Emlid Reach RS4 Pro** mit **Emlid Flow** (`com.emlid.reachview3`).

> **User Guides (Step-by-Step, incl. all menus & prerequisites):**  
> - **Deutsch:** [ANLEITUNG.md](ANLEITUNG.md)  
> - **English:** [GUIDE.md](GUIDE.md)

## Funktionen (Variante B + automatischer Fallback auf Variante A)

- **1-Tap Emlid Reset & Verbinden:**
  - Beendet **Emlid Flow** sauber (`am force-stop com.emlid.reachview3` per Shizuku).
  - Leert automatisch **nur den Cache** von Emlid Flow (per strikt geschützter Bedienungshilfe – klickt niemals auf *Daten löschen*).
  - Schaltet den **Flugmodus** für eine frei wählbare Zeit (10 / 15 / 20 / 30 Sek.) ein und danach automatisch wieder aus.
  - Prüft, ob **alle Bluetooth-Verbindungen getrennt** sind (inkl. Deep-Scan über `dumpsys bluetooth_manager`) und startet anschließend **Emlid Flow**.
  - **Automatischer Fallback:** Ist Shizuku einmal nicht gestartet, führt die App dich automatisch mit Live-Flugmodus-Erkennung, Countdown und Vibration durch den Reset.
- **Live-Bluetooth-Monitor:** Zeigt aktiv verbundene BLE-/Bluetooth-Geräte an, meldet grün, wenn der Kanal komplett frei ist, und warnt, falls der RS4 Pro versehentlich in den Android-Bluetooth-Einstellungen gekoppelt wurde.
- **Live-WLAN- & VPN-Monitor:** Zeigt verbundene WLAN-SSID (mit Hervorhebung für `reach:XX:XX`), lokale IP, Gateway/Empfänger-IP, Internetstatus („Ohne Internet“) und warnt bei aktivem VPN.
- **Samsung S24 Ultra Checkliste:** Live-Prüfung für Akkuoptimierung (*Nicht eingeschränkt*, inkl. 1-Tap-Fix via Shizuku), VPN, Standort, BT-Kopplung und Samsung *Intelligentes WLAN*.

## APK bauen & auf dem S24 Ultra installieren

### Build lokal (macOS mit Android Studio JDK)
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```
Die fertige APK liegt anschließend unter:
`app/build/outputs/apk/debug/app-debug.apk`

### Installation auf dem Galaxy S24 Ultra (kein Play-Store-Konto nötig)
**Per USB (empfohlen):**
```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Oder die `app-debug.apk` direkt aufs S24 kopieren und dort antippen (*Installation aus unbekannter Quelle zulassen*).

## Einrichtung für Vollautomatik (Variante B, ohne Root)

1. **App-Berechtigungen erteilen:** Beim ersten Start Bluetooth (*Geräte in der Nähe*) & Standort (*WLAN-SSID / BLE*) erlauben.
2. **Shizuku einrichten (einmalig ~2 Min.):**
   - **Shizuku** aus dem Google Play Store installieren.
   - Auf dem S24 Ultra *Entwickleroptionen → WLAN-Debugging* aktivieren.
   - In Shizuku *Koppeln* → *Starten*.
   - In **Emlid Helper → Tab „Automation“** auf *Berechtigung anfragen* tippen.
3. **Auto-Cache (Bedienungshilfe) aktivieren:**
   - Falls bei Sideloading ausgegraut: *Einstellungen → Apps → Emlid Helper → 3-Punkte-Menü oben rechts → Eingeschränkte Einstellungen zulassen*.
   - Danach *Einstellungen → Eingabehilfe → Installierte Apps → Emlid Helper – Auto-Cache leeren* einschalten.
