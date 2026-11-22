# Emlid Connect Helper `v26.11.2.1`

**Language / Sprache:** **[Deutsch (README.md)](README.md)** | **English**  
**Current Version:** `v26.11.2.1` (`versionName = "2611.2.1"`, `versionCode = 26110201`, November 2026)

Native Android helper app (Kotlin / Jetpack Compose) for reliable connections between Android smartphones (primarily built and tested on **Samsung Galaxy S24 Ultra**) and an **Emlid Reach RS4 Pro** (as well as other Emlid Reach receivers) running with **Emlid Flow** (`com.emlid.reachview3`).

> **Step-by-Step User Guides (incl. every menu path & prerequisites):**  
> - **English:** [GUIDE.md](GUIDE.md)  
> - **Deutsch:** [ANLEITUNG.md](ANLEITUNG.md)

---

## Compatibility & Tested Environment

Although this app was created to solve a common Samsung issue (aggressive One UI background battery management & *Intelligent Wi-Fi* switching), it is built on standard Android APIs and works across **general Android 12+ smartphones and tablets (`minSdk 31`)** — not just Samsung devices.

### Tested Reference Device
- **Device:** Samsung Galaxy S24 Ultra (`SM-S928B`)
- **OS / UI:** **Samsung One UI 9.0 / Android 17** *(incl. Intelligent Wi-Fi `9.0.0`)*
- **Receiver & App:** Emlid Reach RS4 Pro + Emlid Flow (`com.emlid.reachview3`)

### Estimated Compatibility by Device Family

| Device Family | Estimated Support | Notes |
| :--- | :--- | :--- |
| **Samsung Galaxy (One UI 6 – 9.0 / Android 14 – 17)** | **Full support** *(reference)* | Includes live checks for battery optimization, *Intelligent Wi-Fi*, and One UI menu paths. |
| **Google Pixel / Motorola / Fairphone / Stock Android (Android 12+)** | **Very high** | Dashboard, Bluetooth/Wi-Fi/VPN monitor, Shizuku reset & Auto-Cache (EN/DE labels) work out of the box; the Samsung-specific *Intelligent Wi-Fi* item gracefully falls back to an informational note. |
| **Xiaomi (HyperOS/MIUI), OnePlus, Oppo, Huawei/Honor (Android 12+)** | **High (minor UI variances)** | Core monitoring & Shizuku reset work; vendor-specific menus (e.g., extra "Autostart" switches or custom Storage button labels) may fall back to manual cache confirmation. |
| **Android 11 or older (`API < 31`)** | **Not supported** | Requires `minSdk = 31` (Android 12+) for modern Bluetooth & network APIs. |

---

## Features (Variant B Full Automation + Automatic Variant A Fallback)

- **1-Tap Emlid Reset & Connect:**
  - Cleanly force-stops **Emlid Flow** (`am force-stop com.emlid.reachview3` via Shizuku).
  - Automatically clears **only the cache** of Emlid Flow via a strictly scoped Accessibility Service (hard-guarded to **never** tap *Clear data*).
  - Toggles **Airplane Mode** ON for a configurable wait duration (10 / 15 / 20 / 30 s) and automatically turns it back OFF.
  - Verifies that **all Bluetooth connections are cleared** (including deep inspection via `dumpsys bluetooth_manager`) and automatically launches **Emlid Flow**.
  - **Automatic Fallback:** If Shizuku is not running (e.g., after a phone reboot in the field), the app seamlessly switches to a guided mode with live Airplane Mode detection, automatic countdown, and vibration prompts.
- **Live Bluetooth Monitor:** Lists active BLE/Bluetooth connections, shows a clear green status when no device is connected, and warns if an Emlid Reach receiver is mistakenly paired in Android's system Bluetooth settings.
- **Live Wi-Fi & VPN Monitor:** Displays the connected Wi-Fi SSID (highlighting `reach:XX:XX` hotspots), local IP, gateway/receiver IP, internet validation status ("Connected without internet"), and warns when a VPN is active.
- **Device / Samsung Checklist:** Live checks for Emlid Flow battery optimization (*Unrestricted*, with 1-tap Shizuku fix), VPN, Location service, Bluetooth pairing, and Samsung *Intelligent Wi-Fi*.
- **Quick Launch:** Dedicated button to launch **Emlid Flow** directly.

---

## Field Note (Samsung One UI 9.0): No Internet in Gemini App While Chrome Works

When connected to the receiver's Wi-Fi hotspot (`reach:XX:XX`, *"Connected without internet"*), a notable networking quirk can occur on **Samsung One UI 9.0 / Android 17**:
- **Chrome** routes web requests normally over mobile data (5G), so browsing works.
- Certain apps — notably the **native Gemini app** — inspect the active Wi-Fi without validated internet (`!NET_CAPABILITY_VALIDATED`) and report **"No internet"**, forcing you to temporarily disable Wi-Fi to look something up.
- **Field Workarounds:**
  1. Use **Chrome** (e.g., `gemini.google.com` in the browser) while connected to the Reach Wi-Fi hotspot, **or**
  2. **Recommended field setup:** Connect the **Reach RS4 Pro as a Wi-Fi client to your phone's Mobile Hotspot** instead of connecting the phone to `reach:XX:XX`. That way **every** app (Gemini, Chrome, NTRIP corrections) keeps full 5G internet access!

---

## Installation & Build

### Option 1: Prebuilt APK via GitHub Releases (No Build Needed)
Ready-to-install `.apk` packages can be downloaded from the **Releases** section of this repository and installed directly on your Android device (or via `adb install`) — no Google Play Developer account is required.

### Option 2: Build Locally (macOS with Android Studio JDK)
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Full Automation Setup (Variant B, No Root)
Every menu path and important prerequisite (such as *turning Wi-Fi ON before "Switch to mobile data" becomes clickable*, *Auto Blocker*, *Allow restricted settings*, and *pairing Shizuku on Wi-Fi*) is documented step by step in **[GUIDE.md](GUIDE.md)**.

## New in v26.11.2.1
- **Forget Reach Wi-Fi (Reset "Always connect"):** New button inside the Wi-Fi card that removes only saved `reach:*` Wi-Fi profiles via Shizuku (`cmd wifi forget-network`, with a guided manual fallback), forcing Android to show the *"Connected without internet" → "Always connect"* prompt fresh on the next connection.

## New in v26.11.2.0
- **Browse RS4 Pro Hotspot IP:** Automatic receiver/gateway IP detection (`LinkProperties` + `DhcpInfo` + `192.168.42.1` fallback) with a 1-tap *"Browse RS4 Pro"* button.
- **EN / DE Language Toggle** with crisp SVG vector flag icons in the top bar (full live bilingual UI).
- **Light / Dark Theme Toggle** in the top bar (persistent, with matching status/nav bar icon contrast).
- **Portfolio Footer matching `gps2utm.netlify.app`:** More apps (9 tools), Source code, Help/Bug report/Contact dialog, Licenses/Copyright dialog, Imprint & Ko-fi.
- **English README (`README.en.md`), Android 12+ compatibility matrix** (reference: S24 Ultra, One UI 9.0 / Android 17), and One UI field note (Gemini app offline vs. Chrome online on no-internet Reach Wi-Fi).

## New in v26.11.1.0
- Initial full release (Variant B + automatic Variant A fallback) featuring 1-tap reset (force-stop + auto-cache + airplane mode timer), live Bluetooth/Wi-Fi/VPN monitoring, Samsung checklist, Emlid Flow quick launch, and global version schema (`AppMetadata`, permanently visible in the app header).
