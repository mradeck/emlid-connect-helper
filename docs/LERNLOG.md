# Lernlog – Emlid Connect Helper

### v2.2: Feld-Praxisfallen (Gebäude-WLAN, Fahrzeug-Bluetooth, Systemupdates) (`v26.11.2.2`)

- **Problem:** Im Praxistest fielen drei weitere reale Störquellen auf: (1) Vor dem Gebäude bucht sich der RS4 Pro automatisch in bekannte Gebäude-WLANs ein und schaltet seinen eigenen `reach:XX:XX`-Hotspot ab. (2) Neben dem eigenen Fahrzeug stören noch verbundene Auto-Bluetooth-Geräte (Freisprechanlage, Android Auto etc.) die Verbindung. (3) Android-/Samsung-Systemupdates setzen einzelne Einstellungen still zurück.
- **Lösung:** Alle drei Fälle inklusive konkreter Abhilfen (außerhalb Gebäude-WLAN-Reichweite testen / ins selbe WLAN wechseln, Flugmodus-Reset gegen Fahrzeug-BT, kurzer Check im *Samsung*- & *Automation*-Tab nach jedem OS-Update) in `ANLEITUNG.md`, `GUIDE.md`, `README.md`, `README.en.md` und direkt in den In-App-Praxishinweisen (`AppStrings.kt` / `SamsungSettingsScreen.kt`) hinterlegt.
- **Sonderfälle / Edge-Cases:** Bei Tests direkt am Parkplatz/Gebäude können Gebäude-WLAN *und* Fahrzeug-BT gleichzeitig auftreten – die Dashboard-Karten (Bluetooth-Liste + WLAN-SSID) zeigen beide sofort sichtbar an.
- **Merksatz:** Kein `reach:XX:XX`-Hotspot vor dem Haus → RS4 Pro hängt im bekannten Gebäude-WLAN; BT-Zicken am Auto → Flugmodus-Reset; nach jedem Handy-Update → 10-Sekunden-Check in der App.

### v2.1: Reach-WLAN vergessen („Immer verbinden“ zurücksetzen) (`v26.11.2.1`)

- **Problem:** Wurde beim ersten Verbinden mit `reach:XX:XX` die Abfrage *„Verbunden ohne Internet“ → „Immer verbinden“* verpasst oder weggeklickt, merkt sich Android diese Auswahl dauerhaft für das gespeicherte WLAN-Profil und fragt nicht mehr nach.
- **Lösung:** Gezielter Button **„Reach-WLAN vergessen („Immer verbinden“ zurücksetzen)“** in der WLAN-Karte: Mit Shizuku liest die App gespeicherte Netze via `cmd wifi list-networks`, filtert nur `reach:*`/`emlid`-Profile heraus und entfernt sie nach Rückfrage per `cmd wifi forget-network <id>` (ohne Shizuku öffnet sich eine 3-Schritte-Kurzhilfe + WLAN-Einstellungen).
- **Sonderfälle / Edge-Cases:** Auf Samsung One UI 9.0 / Android 17 listet `cmd wifi list-networks` dieselbe `Network Id` teils doppelt (z. B. `open` + `owe^` oder `wpa2-psk` + `sae^`) – der Parser dedupliziert deshalb zwingend nach `networkId` (`ShizukuWifiParserTest`).
- **Merksatz:** Kein kompletter Netzwerk-Reset nötig – das einzelne Entfernen des `reach:XX:XX`-Profils erzwingt sofort wieder den *„Immer verbinden“*-Dialog.

### v2: Hotspot-IP-Browser, DE/EN, Hell/Dunkel, Portfolio-Footer & One-UI-9.0-WLAN-Beobachtung (`v26.11.2.0`)

- **Problem:** Im Feld fehlten ein direkter 1-Tap-Sprung zur Web-Oberfläche des RS4 Pro über dessen Hotspot-IP, eine DE/EN- & Hell/Dunkel-Umschaltung sowie die einheitliche App-Fußzeile (wie bei `gps2utm.netlify.app`). Außerdem trat bei aktiver Verbindung zum Reach-WLAN (`reach:XX:XX`, *„Verbunden ohne Internet“*) unter **Samsung One UI 9.0 / Android 17** das Verhalten auf, dass **Chrome** normal über 5G ins Internet kam, die **native Gemini-App** aber „Kein Internet“ meldete.
- **Lösung:** Gateway-/Empfänger-IP-Erkennung (`LinkProperties` + `DhcpInfo` + `192.168.42.1`-Fallback) mit *„RS4 Pro im Browser öffnen“*-Button, vollständige Live-i18n (`AppStrings`, SVG-Flaggen DE/GB), Hell/Dunkel-Theme-Toggle, Portfolio-Footer mit Support-/Lizenz-Dialogen, `README.en.md` sowie Android-12+-/One-UI-9.0-Dokumentation.
- **Sonderfälle / Edge-Cases:**
  - Android hält bei *„Verbunden ohne Internet“* Mobilfunk als Standard-Route (`NET_CAPABILITY_VALIDATED`) offen; einzelne Apps (wie die Gemini-App) prüfen aber das aktive WLAN-Interface separat und melden Offline trotz funktionierendem 5G.
  - **Testversions-Pflicht:** Auch vor jedem lokalen Test-Build/APK-Install die Versionsnummer direkt hochzählen, damit im App-Header eindeutig erkennbar ist, welcher Stand getestet wird.
- **Merksatz:** Wenn bei aktivem Reach-Hotspot einzelne Apps „Offline“ melden obwohl Chrome funktioniert, im Browser suchen oder den RS4 Pro ins mobile Hotspot-WLAN des Smartphones hängen – und jede Testversion trägt bereits ihre eigene hochgezählte Versionsnummer.

### v1: Erstversion & Samsung-S24-Ultra-Erkenntnisse (`v26.11.1.0`)

- **Problem:** Auf dem Samsung Galaxy S24 Ultra zeigt Emlid Flow (`com.emlid.reachview3`) den Emlid Reach RS4 Pro zwar an, baut aber sporadisch keine Verbindung auf – selbst wenige Minuten nach einer erfolgreichen Sitzung. Manuelles Cache-Leeren und ein kurzer Flugmodus-Zyklus lösten das Problem zuverlässig, waren im Feld aber umständlich.
- **Lösung:** Native Kotlin-/Jetpack-Compose-App mit 1-Tap-Reset (Variante B über Shizuku + strikt abgesicherte Bedienungshilfe für *nur Cache leeren*, plus automatischer Fallback auf Variante A mit Live-Flugmodus-Countdown), Live-Bluetooth-/WLAN-/VPN-Monitor, Samsung-Checkliste und zentraler `AppMetadata`-Versionierung (`vYY.MM.major.subversion`, sichtbar in der App-Kopfzeile).
- **Sonderfälle / Edge-Cases:**
  - **Samsung Intelligent Wi-Fi 9.0.0 (One UI 9.0 / Android 17):** Der Schalter *„Zu mobilen Daten wechseln“* ist komplett **ausgegraut**, solange WLAN ausgeschaltet ist. Erst WLAN einschalten, dann ausschalten.
  - **Cache vs. Daten:** Selbst Shizuku (UID 2000) darf nicht gezielt nur den Cache einer fremden App leeren, und `pm clear` würde lokale Emlid-Projekte/Logins löschen. Deshalb übernimmt eine eng eingegrenzte Bedienungshilfe (nur `com.android.settings`, harter Verbotsfilter für *Daten löschen* / *Clear data*) den Auto-Klick auf *Speicher → Cache leeren*.
  - **Samsung Auto Blocker & ADB:** Bei aktiviertem Auto Blocker oder veraltetem ADB-Daemon erscheint das S24 Ultra nicht in `adb devices`; `adb kill-server` + Bestätigung von *„USB-Debugging zulassen?“* löst den Hänger.
  - **RS4 Pro Neustart:** Den Netzschalter niemals länger als 5 Sekunden gedrückt halten (sonst Factory Mode).
- **Merksatz:** Geräteerkennung (BLE-Broadcast) ≠ Datenkanal – ein gezielter Reset aus Force-Stop, reinem Cache-Leeren und Flugmodus-Zyklus plus sauberer Samsung-Konfiguration (Akku *Nicht eingeschränkt*, *Zu mobilen Daten wechseln* AUS) hält Emlid Flow zuverlässig stabil.
