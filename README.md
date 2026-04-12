# Family Guardian — Dad App v2.0

Super-simple launcher for elderly users (Samsung S23). Protects against
accidental touches, scam popups, and rogue app installs.

## What's in v2.0

| Feature | Status |
|---|---|
| 2-column large-tile grid | ✅ New |
| Hebrew date in clock bar | ✅ New |
| SOS contact picker (from phone contacts) | ✅ New |
| SOS screen: vibrate + flash while counting down | ✅ New |
| Admin: visual app picker (checkboxes) | ✅ New |
| Watchdog AlarmManager (revives service every 5 min) | ✅ New |
| Smarter whitelist (Samsung/Google system UI never blocked) | ✅ New |
| Redirect cooldown (no loop spam) | ✅ Fixed |
| SOS pulse animation | ✅ New |

## First-time setup on dad's phone

1. Build & install the APK via Android Studio or `gradlew installDebug`
2. Set Family Guardian as **Default Home App** (Android will ask on first launch)
3. Go to **Settings → Accessibility → Family Guardian → Enable**
4. Long-press the **⚙** button for 3 seconds → enter PIN (default: `1234`)
5. Tap **"בחר מאנשי קשר"** to pick your number as the SOS contact
6. Tap **"בחר אפליקציות"** to tick which apps dad can use
7. Save

## Permissions needed
- Accessibility Service (scam popup killer)
- Draw over other apps (overlay for quick redirects)
- Battery optimization exemption (keeps running)
- READ_CONTACTS (SOS contact picker)
- CALL_PHONE, SEND_SMS (SOS)

## Build
```
cd E:\DADAPP
gradlew.bat installDebug
```
Requires Android Studio with SDK 35 installed.
