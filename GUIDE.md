# Step-by-Step User Guide: Emlid Connect Helper (Samsung Galaxy S24 Ultra)

This guide walks you **menu by menu** through installing, configuring, and using **Emlid Connect Helper** on a **Samsung Galaxy S24 Ultra** (tested on **One UI 9.0 / Android 17**, *Intelligent Wi-Fi 9.0.0*) for the **Emlid Reach RS4 Pro** with **Emlid Flow**.  
*(Note: The app also works on **other Android 12+ smartphones** such as Google Pixel, Motorola, OnePlus, or Xiaomi — only a few system menu names differ slightly and the Samsung-specific "Intelligent Wi-Fi" step does not apply.)*

---

## Important Prerequisites & Gotchas (Read First!)

Several settings on Samsung Galaxy devices are **greyed out** or will not work unless specific conditions are met. Keep these in mind:

| If this happens… | Why (Prerequisite / Side Condition) | How to fix it |
| :--- | :--- | :--- |
| **"Switch to mobile data"** in *Intelligent Wi-Fi* is **greyed out** | **Wi-Fi is turned OFF** (or Mobile Data is off). | Turn **Wi-Fi ON** first — the toggle immediately becomes clickable so you can switch it OFF. |
| **Wi-Fi name (SSID)** shows as `Unknown` or BLE does not detect devices | **Location (GPS)** is turned off or the app lacks Location permission. | Turn **Location ON** in Quick Settings + grant Precise Location permission to the app. |
| **USB debugging** cannot be enabled or your computer does not see the phone over USB | **Samsung "Auto Blocker"** is enabled — or the USB cable is a charge-only cable. | Turn off *Settings → Security and privacy → Auto Blocker* temporarily and use a **data-capable USB cable** ("USB for file transfer" must appear in notifications). |
| The **Accessibility Service** toggle is **greyed out** | Android 13+ **"Restricted Settings"** protection for manually sideloaded APKs. | Open *Settings → Apps → Emlid Helper → 3-dot menu (top right) → **Allow restricted settings***, then return to Accessibility. |
| **Shizuku** cannot pair or start | **Wireless debugging** only works while the phone is connected to a **Wi-Fi network**. | Connect to any Wi-Fi network briefly, then pair & start Shizuku. |
| **Shizuku** shows "Service stopped" after rebooting the phone | Android stops ADB/Shizuku services after every full device restart. | Open the Shizuku app briefly → tap **Start** (or simply use Emlid Helper's built-in guided fallback mode). |
| **Gemini app reports "No internet"** while **Chrome works normally** (when connected to Reach Wi-Fi) | On Samsung One UI 9.0 / Android 17, Chrome routes traffic over 5G even when connected to a *"Connected without internet"* Wi-Fi hotspot, whereas some apps (such as the native Gemini app) inspect the non-internet Wi-Fi interface and treat the phone as offline. | Either use **Chrome** (`gemini.google.com`), toggle Wi-Fi off briefly — or even better: connect the **RS4 Pro as a client to your phone's Mobile Hotspot** (instead of connecting your phone to `reach:XX:XX`) so every app keeps full 5G internet. |
| Missed or dismissed **"Connected without internet" → "Always connect"** | Android stores that choice per saved Wi-Fi network (`reach:XX:XX`). | In *Emlid Helper*'s Wi-Fi card, tap **"Forget Reach Wi-Fi (Reset 'Always connect')"** (or manually: *Wi-Fi → gear icon next to `reach:XX:XX` → Forget*; if RS4 Pro is off: *3-dot menu → Advanced settings → Manage networks*). Reconnect (default Emlid password is usually `emlidreach`) and tap **Always connect**. |
| **RS4 Pro hotspot (`reach:XX:XX`) does not appear when testing outside near a building** | The RS4 Pro automatically connects to **known Wi-Fi networks** (e.g., office/home Wi-Fi still reaching outside) and **disables its own hotspot** while connected. | Either move further away from the building (out of Wi-Fi range so the RS4 Pro switches back to hotspot mode), or connect your phone to the same building Wi-Fi to reach the RS4 Pro there (or remove the building Wi-Fi from the RS4 Pro if you want hotspot mode outside). |
| **Connection fails when standing near your vehicle** | Your phone may still be connected to **vehicle Bluetooth devices** (hands-free audio, Android Auto, OBD adapters, etc.), interfering with the BLE handshake. | Check the Bluetooth card in *Emlid Helper* to see what is still connected. Toggling **Airplane Mode** (via the 1-tap reset in the app or manually) usually resolves this immediately — also step a few meters away from the vehicle / switch ignition off if needed. |
| **Issues suddenly reappear after an Android / One UI system update** | System updates frequently reset individual settings silently (such as Battery Optimization back to "Optimized", *Intelligent Wi-Fi*, permissions, or stopping Shizuku after the reboot). | After every system update, briefly open **Emlid Helper → Device/Samsung tab** and **Automation tab** — the app immediately flags any reset settings in red/amber so you can fix them in seconds. |

> **Important Safety Notes for Emlid Flow & Reach RS4 Pro:**  
> When resetting Emlid Flow, **always tap "Clear cache" only** — **never "Clear data"**, as clearing data deletes local projects and logins!  
> When rebooting the **Reach RS4 Pro**, **never hold the power button longer than 5 seconds**, or the receiver will enter Factory Mode.

---

## Part 1: Install the App on Your Samsung Galaxy S24 Ultra

You do **not** need a Google Play Developer account — the app is installed directly as an APK.

### Method A: Directly via USB Cable from Mac/PC (Recommended)
*Advantage: Takes just seconds and usually avoids Android's "Restricted Settings" block on Accessibility Services.*

1. **Enable Developer Options** *(one-time setup)*:
   - Open **Settings → About phone → Software information**.
   - Tap **Build number 7 times** in a row (enter your PIN if prompted).
   - You will see *"Developer mode has been turned on."*
2. **Check Samsung Auto Blocker:**
   - Open **Settings → Security and privacy → Auto Blocker**.
   - If enabled, turn it **OFF** temporarily (you can turn it back on after setup).
3. **Enable USB Debugging:**
   - Open **Settings → Developer options** (at the bottom of Settings).
   - Turn **USB debugging** **ON** and confirm with **OK**.
4. **Connect USB Cable & Authorize:**
   - Connect your S24 Ultra to your computer using a **USB data cable** and unlock the screen.
   - Swipe down the notification shade: it should say **"USB for file transfer"** (if it says "Charging only", tap it and choose **Transferring files**).
   - Confirm the **"Allow USB debugging?"** popup on the phone by tapping **Allow** *(optionally check "Always allow from this computer")*.
5. **Install the APK (in Terminal on macOS):**
   ```bash
   ~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Method B: Open the APK Directly on the Phone
1. If **Auto Blocker** is enabled (*Settings → Security and privacy → Auto Blocker*), turn it **OFF** temporarily.
2. Copy `app-debug.apk` to your S24 Ultra (e.g., into the *Downloads* folder) and tap it in Samsung **My Files**.
3. If prompted, enable **"Allow from this source"** for *My Files*, then tap **Install**.

---

## Part 2: First Launch & App Permissions

1. Open **Emlid Helper** on your S24 Ultra.
2. Tap **"Freigeben" (Grant)** in the red banner at the top.
3. Allow both system prompts:
   - **Nearby devices (Bluetooth):** **Allow** *(lets the app monitor active Bluetooth/BLE connections)*
   - **Location:** **While using the app** + **Precise location** *(required by Android to display Wi-Fi SSIDs like `reach:XX:XX` and scan BLE reliably)*

---

## Part 3: Turn All Samsung S24 Ultra Checks "Green"

Switch to the **"Samsung"** tab at the bottom of the app and work through each item until every check shows a **green checkmark**:

### 3.1 Battery Optimization for Emlid Flow = "Unrestricted"
*Why:* Samsung One UI aggressively suspends background apps and drops active BLE/network sockets to Emlid Flow.

- **Without Shizuku:**
  1. On the *Samsung* tab under *Battery optimization (Emlid Flow)*, tap **Open App Info**.
  2. Tap **Battery**.
  3. Select **Unrestricted** *(not "Optimized" and not "Restricted")*.
  4. Return to *Emlid Helper* → the check immediately turns **green**.
- **With Shizuku (1-Tap):**
  - Tap **"Per Shizuku freigeben"** directly → done!

### 3.2 Configure Samsung Intelligent Wi-Fi
*Why:* When connected to the RS4 Pro Wi-Fi hotspot (`reach:XX:XX`), Android detects "no internet" and silently routes traffic over 5G/mobile data or switches away to another Wi-Fi network, breaking the connection.

1. **Important Prerequisite:** First turn **Wi-Fi ON** (*Settings → Connections → Wi-Fi → toggle ON*).  
   *(If Wi-Fi is OFF, "Switch to mobile data" is greyed out and cannot be changed!)*
2. In the Wi-Fi menu, tap the **3-dot menu (⋮)** in the top right → **Intelligent Wi-Fi**.
3. Turn **OFF** both of these toggles:
   - **Switch to mobile data** → **OFF**
   - **Switch to better Wi-Fi networks** → **OFF**
4. **Additional note when connecting to `reach:XX:XX`:**  
   When Android shows *"Connected without internet"*, tap the notification and select **Keep Wi-Fi connection / Always connect**.

### 3.3 Clean Up System Bluetooth Pairing
*Why:* The Reach RS4 Pro should be connected **only inside Emlid Flow**. If it is also manually paired in Android's system Bluetooth settings, Android and Emlid Flow can block each other.

1. If the checklist shows a warning, tap **Bluetooth Settings** (*Settings → Connections → Bluetooth*).
2. Under *Paired devices*, look for entries such as `Reach`, `RS4`, or `Emlid`.
3. Tap the **gear icon** next to it → tap **Unpair**.

### 3.4 Check VPN
*Why:* An active VPN (or local firewall app) often blocks local traffic to the receiver even though BLE discovery still shows the device.

- If the checklist reports an active VPN, tap **VPN Settings** (or open your VPN app) and **disconnect the VPN** while surveying.

### 3.5 Enable Location Service
- Swipe down Quick Settings and make sure **Location** is turned **ON**.

---

## Part 4: Set Up Full 1-Tap Automation (Variant B — No Root Required)

To let the **"Emlid Reset & Verbinden starten"** button automatically perform every step with **1 tap** (Force-stop Emlid Flow → Clear cache only → Toggle Airplane Mode → Verify Bluetooth is clear → Launch Emlid Flow), configure both components on the **"Automation"** tab:

### 4.1 Set Up Shizuku (Automatic Airplane Mode, Force-Stop & Deep BT Scan)

> **Prerequisite:** Your S24 Ultra must be connected to a **Wi-Fi network** during setup, and **Developer options** (Part 1) must be enabled.

1. On the **Automation** tab of *Emlid Helper*, tap **Install Shizuku** (or install **Shizuku** from the Google Play Store).
2. Open **Shizuku** and under *"Start via Wireless debugging"*, tap **Pairing**.
3. Allow **Notifications** for Shizuku when prompted (needed to enter the pairing code).
4. Tap **Developer options** (or open *Settings → Developer options*).
5. Scroll to **Wireless debugging**:
   - Turn **Wireless debugging** **ON** (check *"Always allow on this network"* → **Allow**).
   - Tap directly on the text **"Wireless debugging"** (not just the switch) to open its sub-screen.
6. Tap **"Pair device with pairing code"** — a **6-digit code** appears.
7. Swipe down the **notification shade**: tap the Shizuku notification *"Pairing service found"* → **Enter pairing code**, type the 6-digit code, and submit.
8. Return to the **Shizuku** app and tap **Start**. Within seconds it shows *"Shizuku is running"*.
9. Return to **Emlid Helper → Automation tab**:
   - Tap **Request permission** → **Allow all the time**.
   - Shizuku now shows green **"Active (UID 2000)"**!

### 4.2 Enable Auto-Cache Clearing (Accessibility Service)

*Why:* Even Shizuku cannot clear *only* another app's cache without risking app data. Our strictly scoped Accessibility Service briefly opens Emlid Flow's App Info during a reset, taps **Storage → Clear cache** (with a hard safeguard that explicitly forbids tapping *Clear data*), and returns immediately. Outside of an active reset, it does nothing.

1. On the **Automation** tab, tap **Enable in Accessibility** (*Settings → Accessibility*).
2. Tap **Installed apps** → **Emlid Helper – Auto-Cache leeren**.
3. Turn the switch **ON** and tap **Allow**.
   - **If the switch is greyed out ("Restricted setting"):**
     1. Open **Settings → Apps → Emlid Helper**.
     2. Tap the **3-dot menu (⋮)** in the top right → **Allow restricted settings** (confirm with PIN/fingerprint).
     3. Go back to *Settings → Accessibility → Installed apps → Emlid Helper – Auto-Cache leeren* → the switch is now enabled!

Once both items are green, the Dashboard displays **"Vollautomatik aktiv (Shizuku + Auto-Cache)"**.

---

## Part 5: Daily Field Workflow with the RS4 Pro

### Standard Reset When Connection Hangs
1. Open **Emlid Helper** (**Dashboard** tab).
2. Check live status at a glance:
   - **Bluetooth connections:** Shows whether any device or stuck BLE session is still connected.
   - **Wi-Fi & Network:** Shows your current SSID (e.g., `reach:XX:XX` with IP/gateway) and VPN status.
3. Choose the **Airplane Mode wait duration** (`10`, `15`, `20`, or `30 sec` — recommended: **15 sec**).
4. Tap **"Emlid Reset & Verbinden starten"**.
5. The app runs all steps automatically:
   - Force-stops Emlid Flow
   - Clears **only** Emlid Flow's cache
   - Turns Airplane Mode ON → runs countdown → turns Airplane Mode OFF
   - Verifies that all Bluetooth connections are cleared
   - Automatically launches **Emlid Flow**

### If Shizuku Is Not Running in the Field (Automatic Fallback)
After a phone reboot in the field without Wi-Fi, Shizuku does not auto-start. **Emlid Helper continues working seamlessly:**
- Tap **"Emlid Reset & Verbinden starten"** as usual.
- Cache clearing still runs automatically via Accessibility (or opens App Info).
- At the Airplane Mode step, simply pull down Quick Settings and tap **Airplane Mode ON** → the app detects it **automatically**, runs the 15-second countdown, **vibrates**, prompts you to turn Airplane Mode OFF, waits for Bluetooth/Wi-Fi recovery, and launches **Emlid Flow**.

### Receiver-Side & Field Environment Checklist (Reach RS4 Pro)
- **Known Wi-Fi near buildings:** The RS4 Pro automatically joins known Wi-Fi networks (office/home) and turns off its own `reach:XX:XX` hotspot when it does. Test outside building Wi-Fi range for hotspot mode — or connect your phone to the same building Wi-Fi.
- **Vehicle Bluetooth nearby:** When standing close to your car, lingering vehicle Bluetooth links (hands-free, Android Auto, etc.) can interfere — check the Bluetooth card in *Emlid Helper* and run the **Airplane Mode reset**.
- **After phone system updates:** After any Android / One UI update, briefly check the **Device/Samsung** and **Automation** tabs in *Emlid Helper* — the app automatically detects and flags settings that were reset by the update.
- **Check secondary devices:** Make sure no tablet or second phone is still connected to the RS4 Pro in the background.
- **Wi-Fi alternative to Bluetooth:** In Emlid Flow under *Profile → App settings → Preferences*, enable **Advanced connection** to connect directly via Wi-Fi/IP.
- **Keep firmware updated:** Once connected, verify RS4 Pro firmware and Emlid Flow updates (earlier 34.x firmware had known Bluetooth stability issues).
- **Clean receiver reboot:** Press the RS4 Pro power button, but **never hold it longer than 5 seconds** (otherwise it enters Factory Mode!).
