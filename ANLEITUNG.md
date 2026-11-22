# Schritt-für-Schritt-Anleitung: Emlid Connect Helper (Samsung Galaxy S24 Ultra)

Diese Anleitung führt dich **Menü für Menü** durch Installation, Einrichtung und Nutzung von **Emlid Connect Helper** auf einem **Samsung Galaxy S24 Ultra** (getestet mit **One UI 9.0 / Android 17**, *Intelligent Wi-Fi 9.0.0*) für den **Emlid Reach RS4 Pro** mit **Emlid Flow**.  
*(Hinweis: Die App funktioniert grundsätzlich auch auf **anderen Android-12+-Smartphones** wie Google Pixel, Motorola, OnePlus oder Xiaomi – dort heißen lediglich einzelne Systemeinstellungs-Menüs leicht anders und der Punkt „Samsung Intelligent Wi-Fi“ entfällt.)*

---

## Wichtige Nebenbedingungen vorab (häufige Stolperfallen!)

Einige Einstellungen auf Samsung-Geräten sind **ausgegraut** oder funktionieren nicht, wenn bestimmte Voraussetzungen fehlen. Behalte diese Punkte im Hinterkopf:

| Wenn das passiert… | Woran es liegt (Nebenbedingung) | Lösung |
| :--- | :--- | :--- |
| **„Zu mobilen Daten wechseln“** in *Intelligent Wi-Fi* ist **ausgegraut** | **WLAN ist ausgeschaltet** (oder mobile Daten sind aus). | Erst **WLAN einschalten**, dann lässt sich der Schalter antippen und ausschalten. |
| Der **WLAN-Name (SSID)** wird nicht angezeigt (`Unbekannt`) oder BLE findet nichts | **Standort (GPS)** ist deaktiviert oder die Standort-Berechtigung fehlt. | Standort in den Schnelleinstellungen **einschalten** + Standort-Berechtigung der App erteilen. |
| **USB-Debugging** lässt sich nicht aktivieren oder der Computer erkennt das Handy per USB nicht | **Samsung „Automatische Sperre“ (Auto Blocker)** ist aktiv – oder das USB-Kabel ist ein reines Ladekabel. | *Einstellungen → Sicherheit und Datenschutz → Automatische Sperre* kurz **ausschalten** und ein **Datenkabel** nutzen (Meldung „USB für Dateiübertragung“ muss erscheinen). |
| Schalter in der **Eingabehilfe (Bedienungshilfe)** ist **ausgegraut** | Android 13+ Sicherheitsregel (**„Eingeschränkte Einstellungen“**) bei manuell installierten APKs. | *Einstellungen → Apps → Emlid Helper → 3-Punkte-Menü oben rechts → **Eingeschränkte Einstellungen zulassen***, danach zurück in die Eingabehilfe. |
| **Shizuku** lässt sich nicht koppeln oder starten | **WLAN-Debugging** funktioniert nur, solange das Smartphone mit einem **WLAN** verbunden ist. | Kurz mit einem beliebigen WLAN verbinden, dann koppeln & starten. |
| **Shizuku** zeigt nach einem Handy-Neustart „Dienst aus“ | Android beendet ADB-/Shizuku-Dienste nach jedem kompletten Neustart. | Shizuku-App kurz öffnen → **Starten** (oder ohne Shizuku den automatischen geführten Modus nutzen). |
| **Gemini-App meldet „Kein Internet“**, während **Chrome normal funktioniert** (bei aktivem Reach-WLAN) | Unter Samsung One UI 9.0 / Android 17 bleibt bei *„Verbunden ohne Internet“* das 5G-Routing für Chrome aktiv, einzelne Apps (wie die native Gemini-App) prüfen aber das aktive WLAN ohne Internet und streiken. | Entweder im **Chrome-Browser** (`gemini.google.com`) suchen, WLAN kurz trennen – oder noch besser: den **RS4 Pro mit dem mobilen Hotspot des S24 Ultra verbinden** (statt umgekehrt), dann haben alle Apps durchgehend 5G-Internet. |

> **Wichtiger Sicherheitshinweis für Emlid Flow:**  
> Beim Zurücksetzen **immer nur „Cache leeren“** verwenden – **niemals „Daten löschen“**, da sonst lokale Projekte und Logins in Emlid Flow entfernt werden!  
> Beim Neustart des **Reach RS4 Pro** den Netzschalter **nie länger als 5 Sekunden** gedrückt halten (sonst wechselt der Empfänger in den Werksmodus / Factory Mode).

---

## Teil 1: App auf dem Samsung Galaxy S24 Ultra installieren

Du brauchst **kein Google Play Developer Konto** – die App wird direkt als APK installiert.

### Weg A: Direkt per USB-Kabel vom Mac/PC (empfohlen)
*Vorteil: Geht in wenigen Sekunden und umgeht meist automatisch die Android-Sperre „Eingeschränkte Einstellungen“.*

1. **Entwickleroptionen freischalten** *(nur einmalig nötig)*:
   - Öffne **Einstellungen → Telefoninfo → Softwareinformationen**.
   - Tippe **7× schnell hintereinander auf „Buildnummer“** (PIN eingeben, falls gefragt).
   - Unten erscheint die Meldung *„Der Entwicklermodus wurde aktiviert.“*
2. **Automatische Sperre prüfen:**
   - Öffne **Einstellungen → Sicherheit und Datenschutz → Automatische Sperre**.
   - Falls eingeschaltet: **ausschalten** (kann nach der Einrichtung wieder aktiviert werden).
3. **USB-Debugging einschalten:**
   - Öffne **Einstellungen → Entwickleroptionen** (ganz unten im Hauptmenü).
   - Schalte **USB-Debugging** auf **AN** und bestätige mit **OK**.
4. **Kabel anschließen & freigeben:**
   - Verbinde das S24 Ultra mit einem **USB-Datenkabel** mit dem Computer und entsperre das Display.
   - Ziehe die Benachrichtigungsleiste herunter: Dort sollte **„USB für Dateiübertragung“** stehen (falls „Nur laden“, antippen und **Dateien übertragen** wählen).
   - Bestätige das Popup **„USB-Debugging zulassen?“** auf dem Handy mit **Zulassen** *(optional Haken bei „Von diesem Computer immer zulassen“)*.
5. **APK installieren (im Terminal auf dem Mac):**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Weg B: APK-Datei direkt auf dem Smartphone öffnen
1. Falls **Automatische Sperre** aktiv ist (*Einstellungen → Sicherheit und Datenschutz → Automatische Sperre*), diese kurz **ausschalten**.
2. Die Datei `app-debug.apk` aufs S24 Ultra kopieren (z. B. in den Ordner *Downloads*) und in der App **Eigene Dateien** antippen.
3. Falls gefragt, für *Eigene Dateien* **„Aus dieser Quelle zulassen“** aktivieren und auf **Installieren** tippen.

---

## Teil 2: Erster Start & App-Berechtigungen

1. Öffne **Emlid Helper** auf dem S24 Ultra.
2. Tippe ganz oben im roten Hinweisbanner auf **„Freigeben“**.
3. Erlaube beide Systemabfragen:
   - **Geräte in der Nähe (Bluetooth):** **Zulassen** *(damit die App aktive Bluetooth-/BLE-Verbindungen prüfen kann)*
   - **Standort:** **Bei Nutzung der App** + **Genauer Standort** *(Nebenbedingung von Android, um den WLAN-Namen / SSID wie `reach:XX:XX` auszulesen)*

---

## Teil 3: Alle Samsung S24 Ultra Einstellungen auf „Grün“ bringen

Wechsle unten in der App auf den Tab **„Samsung“**. Gehe alle Punkte durch, bis jeder Punkt einen **grünen Haken** zeigt:

### 3.1 Akkuoptimierung für Emlid Flow = „Nicht eingeschränkt“
*Warum:* Samsung One UI schläfert Hintergrundprozesse schnell ein und kappt dadurch aktive BLE-/Netzwerk-Sockets zu Emlid Flow.

- **Ohne Shizuku:**
  1. Tippe im Tab *Samsung* bei *Akkuoptimierung (Emlid Flow)* auf **„App-Info öffnen“**.
  2. Tippe in den Samsung-App-Infos auf **Akku**.
  3. Wähle **Nicht eingeschränkt** *(nicht „Optimiert“ und nicht „Eingeschränkt“)*.
  4. Zurück zu *Emlid Helper* → der Punkt wird automatisch **grün**.
- **Mit Shizuku (noch schneller):**
  - Einfach direkt auf **„Per Shizuku freigeben“** tippen → fertig!

### 3.2 Samsung Intelligent Wi-Fi korrekt einstellen
*Warum:* Wenn du dich mit dem WLAN-Hotspot des RS4 Pro (`reach:XX:XX`) verbindest, hat dieses WLAN kein Internet. Samsung leitet dann Datenpakete im Hintergrund auf 5G/Mobilfunk um oder wechselt das WLAN – die Verbindung bricht ab.

1. **Wichtige Nebenbedingung:** Schalte zuerst **WLAN EIN** (*Einstellungen → Verbindungen → WLAN → Schalter oben rechts auf AN*).  
   *(Ist WLAN aus, ist „Zu mobilen Daten wechseln“ ausgegraut und kann nicht geändert werden!)*
2. Tippe im WLAN-Menü oben rechts auf das **3-Punkte-Menü (⋮)** → **Intelligent Wi-Fi**.
3. Schalte folgende beiden Punkte auf **AUS**:
   - **Zu mobilen Daten wechseln** → **AUS**
   - **Zu besseren WLANs wechseln** → **AUS**
4. **Zusatzhinweis beim ersten Verbinden mit `reach:XX:XX`:**  
   Erscheint die Android-Meldung *„Verbunden ohne Internet“*, tippe sie an und wähle dauerhaft **Immer verbinden**.

### 3.3 Bluetooth-Systemkopplung bereinigen
*Warum:* Der Reach RS4 Pro wird ausschließlich **direkt in Emlid Flow** verbunden. Ist er zusätzlich manuell in den Android-Bluetooth-Einstellungen gekoppelt, blockieren sich System und App gegenseitig.

1. Zeigt die Checkliste eine Warnung, tippe auf **„BT-Einstellungen“** (*Einstellungen → Verbindungen → Bluetooth*).
2. Suche unter *Gekoppelte Geräte* nach Einträgen wie `Reach`, `RS4` oder `Emlid`.
3. Tippe rechts daneben auf das **Zahnrad-Symbol** → unten auf **Entkoppeln**.

### 3.4 VPN prüfen
*Warum:* Ein laufendes VPN (oder lokale Firewall-Apps) blockiert oft den lokalen Datenkanal zum Empfänger, obwohl der RS4 Pro per BLE sichtbar bleibt.

- Zeigt die Checkliste *„Aktives VPN erkannt!“*, tippe auf **„VPN-Einstellungen“** (oder öffne deine VPN-App) und **trenne das VPN** während der Vermessung.

### 3.5 Standortdienst aktivieren
- Ziehe die Samsung-Schnelleinstellungen von oben herunter und stelle sicher, dass **Standort** **AN** ist.

---

## Teil 4: Vollautomatik (Variante B) einrichten – ohne Root

Damit der Button **„Emlid Reset & Verbinden starten“** wirklich alles vollautomatisch per **1 Tipp** erledigt (Emlid Flow zwangsbeenden → Cache leeren → Flugmodus ein/aus → Bluetooth prüfen → Emlid Flow starten), richte im Tab **„Automation“** diese beiden Bausteine ein:

### 4.1 Shizuku einrichten (für automatischen Flugmodus, Force-Stop & Deep-BT-Scan)

> **Nebenbedingung:** Dein S24 Ultra muss während der Einrichtung mit einem **WLAN** verbunden sein, und die **Entwickleroptionen** (siehe Teil 1) müssen aktiv sein.

1. Öffne in *Emlid Helper* den Tab **„Automation“** und tippe auf **„Shizuku installieren“** (oder suche **Shizuku** direkt im Google Play Store) und installiere die App.
2. Öffne die **Shizuku**-App und tippe unter *„Über drahtloses Debugging starten“* auf **Koppeln**.
3. Falls Shizuku nach **Benachrichtigungen** fragt: **Zulassen** (wird für die Code-Eingabe benötigt).
4. Tippe in Shizuku auf **Entwickleroptionen** (oder öffne *Einstellungen → Entwickleroptionen*).
5. Scrolle zu **WLAN-Debugging**:
   - Schalte **WLAN-Debugging** auf **AN** (Popup „WLAN-Debugging in diesem Netzwerk zulassen?“ → Haken setzen & **Zulassen**).
   - Tippe direkt auf den Text **„WLAN-Debugging“** (nicht nur auf den Schalter), um das Untermenü zu öffnen.
6. Tippe auf **„Gerät über Kopplungscode koppeln“** – es erscheint ein **6-stelliger Code**.
7. Ziehe die **Benachrichtigungsleiste** herunter: Dort wartet die Shizuku-Meldung *„Kopplungsdienst gefunden“* → tippe auf **Kopplungscode eingeben**, gib den 6-stelligen Code ein und bestätige.
8. Wechsle zurück in die **Shizuku**-App und tippe auf **Starten**. Nach wenigen Sekunden steht oben *„Shizuku läuft“*.
9. Wechsle zurück in **Emlid Helper → Tab „Automation“**:
   - Tippe auf **„Berechtigung anfragen“** → **Immer zulassen**.
   - Oben rechts bei Shizuku steht jetzt grün **„Aktiv (UID 2000)“**!

### 4.2 Auto-Cache leeren (Bedienungshilfe) aktivieren

*Warum:* Aus Sicherheitsgründen darf selbst Shizuku nicht gezielt nur den Cache einer fremden App leeren. Unsere Bedienungshilfe öffnet beim Reset kurz die App-Info von Emlid Flow, tippt automatisch auf **Speicher → Cache leeren** (mit hartem Schutzfilter gegen *Daten löschen*) und kehrt sofort zurück. Außerhalb des Resets ist sie komplett inaktiv.

1. Tippe in **Emlid Helper → Tab „Automation“** auf **„Jetzt in Eingabehilfe aktivieren“** *(Pfad: Einstellungen → Eingabehilfe)*.
2. Tippe auf **Installierte Apps** → **Emlid Helper – Auto-Cache leeren**.
3. Schalte den Dienst auf **AN** und bestätige mit **Zulassen**.
   - **Falls der Schalter ausgegraut ist („Eingeschränkte Einstellung“):**
     1. Öffne **Einstellungen → Apps → Emlid Helper**.
     2. Tippe oben rechts auf das **3-Punkte-Menü (⋮)** → **Eingeschränkte Einstellungen zulassen** (PIN/Fingerabdruck bestätigen).
     3. Gehe zurück zu *Einstellungen → Eingabehilfe → Installierte Apps → Emlid Helper – Auto-Cache leeren* → jetzt lässt sich der Schalter aktivieren!

Wenn beide Punkte grün sind, steht im Dashboard: **„Vollautomatik aktiv (Shizuku + Auto-Cache)“**.

---

## Teil 5: Nutzung im Feld (Alltag mit dem RS4 Pro)

### Normaler Ablauf bei Verbindungsproblemen
1. Öffne **Emlid Helper** (Tab **Dashboard**).
2. Kontrolliere auf einen Blick:
   - **Bluetooth-Verbindungen:** Zeigt an, ob noch ein anderes Gerät oder eine hängende Session aktiv ist.
   - **WLAN & Netzwerk:** Zeigt dein aktuelles WLAN (z. B. `reach:XX:XX` inkl. IP-Adresse) und ob ein VPN stört.
3. Wähle bei Bedarf die **Flugmodus-Wartezeit** (`10`, `15`, `20` oder `30 Sek.` – empfohlen: **15 Sek.**).
4. Tippe auf **„Emlid Reset & Verbinden starten“**.
5. Die App erledigt alles der Reihe nach:
   - Emlid Flow zwangsbeenden
   - Nur den Cache von Emlid Flow leeren
   - Flugmodus einschalten → Countdown abwarten → Flugmodus wieder ausschalten
   - Prüfen, ob alle Bluetooth-Verbindungen sauber getrennt sind
   - **Emlid Flow** automatisch starten

### Falls Shizuku unterwegs mal nicht läuft (automatischer Fallback)
Nach einem Handy-Neustart im Feld ohne WLAN startet Shizuku nicht automatisch. **Emlid Helper funktioniert trotzdem nahtlos weiter:**
- Tippe wie gewohnt auf **„Emlid Reset & Verbinden starten“**.
- Cache-Leeren läuft über die Bedienungshilfe weiterhin automatisch (oder öffnet die App-Info).
- Beim Schritt *Flugmodus* ziehst du einfach die Schnelleinstellungen herunter und tippst auf **Flugmodus AN** → die App erkennt das **automatisch**, zählt 15 Sekunden herunter, **vibriert**, meldet *„Jetzt Flugmodus wieder AUSschalten!“*, wartet auf Bluetooth/WLAN und startet **Emlid Flow**.

### Wenn es am Empfänger (RS4 Pro) liegt – kurze Feld-Checkliste
- **Zweitgerät prüfen:** Ist ein Tablet oder zweites Smartphone noch per BT/WLAN mit dem RS4 Pro verbunden?
- **WLAN statt Bluetooth:** In Emlid Flow unter *Profil → App-Einstellungen → Präferenzen* lässt sich **Erweiterte Verbindung** aktivieren (Verbindung über den Reach-Hotspot per IP).
- **Firmware-Stand:** Sobald die Verbindung steht, Firmware des RS4 Pro und die Emlid-Flow-App aktuell halten (ältere 34.x-Firmware hatte bekannte Bluetooth-Stabilitätsprobleme).
- **Sauberer Neustart des RS4 Pro:** Netzschalter drücken, aber **nicht länger als 5 Sekunden gedrückt halten** (sonst Factory Mode!).
