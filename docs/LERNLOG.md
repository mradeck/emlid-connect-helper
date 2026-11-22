# Lernlog – Emlid Connect Helper

### v1: Erstversion & Samsung-S24-Ultra-Erkenntnisse (`v26.11.1.0`)

- **Problem:** Auf dem Samsung Galaxy S24 Ultra zeigt Emlid Flow (`com.emlid.reachview3`) den Emlid Reach RS4 Pro zwar an, baut aber sporadisch keine Verbindung auf – selbst wenige Minuten nach einer erfolgreichen Sitzung. Manuelles Cache-Leeren und ein kurzer Flugmodus-Zyklus lösten das Problem zuverlässig, waren im Feld aber umständlich.
- **Lösung:** Native Kotlin-/Jetpack-Compose-App mit 1-Tap-Reset (Variante B über Shizuku + strikt abgesicherte Bedienungshilfe für *nur Cache leeren*, plus automatischer Fallback auf Variante A mit Live-Flugmodus-Countdown), Live-Bluetooth-/WLAN-/VPN-Monitor, Samsung-Checkliste und zentraler `AppMetadata`-Versionierung (`vYY.MM.major.subversion`, sichtbar in der App-Kopfzeile).
- **Sonderfälle / Edge-Cases:**
  - **Samsung Intelligent Wi-Fi 9.0.0:** Der Schalter *„Zu mobilen Daten wechseln“* ist komplett **ausgegraut**, solange WLAN ausgeschaltet ist. Erst WLAN einschalten, dann ausschalten.
  - **Cache vs. Daten:** Selbst Shizuku (UID 2000) darf nicht gezielt nur den Cache einer fremden App leeren, und `pm clear` würde lokale Emlid-Projekte/Logins löschen. Deshalb übernimmt eine eng eingegrenzte Bedienungshilfe (nur `com.android.settings`, harter Verbotsfilter für *Daten löschen* / *Clear data*) den Auto-Klick auf *Speicher → Cache leeren*.
  - **Samsung Auto Blocker & ADB:** Bei aktiviertem Auto Blocker oder veraltetem ADB-Daemon erscheint das S24 Ultra nicht in `adb devices`; `adb kill-server` + Bestätigung von *„USB-Debugging zulassen?“* löst den Hänger.
  - **RS4 Pro Neustart:** Den Netzschalter niemals länger als 5 Sekunden gedrückt halten (sonst Factory Mode).
- **Merksatz:** Geräteerkennung (BLE-Broadcast) ≠ Datenkanal – ein gezielter Reset aus Force-Stop, reinem Cache-Leeren und Flugmodus-Zyklus plus sauberer Samsung-Konfiguration (Akku *Nicht eingeschränkt*, *Zu mobilen Daten wechseln* AUS) hält Emlid Flow zuverlässig stabil.
