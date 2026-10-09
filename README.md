# ⭐ Starcord

A clean, minimal Discord token extractor with a star-themed UI.

## Features
- Loads Discord in a full-screen WebView (desktop UA)
- Auto-extracts your token from localStorage every 2 seconds
- One-tap copy with haptic feedback
- Token preview in the status bar
- Zero data leaves your device

## UI Design
- Midnight purple / star colour palette
- Compact bottom panel — no scrolling, fits every screen size
- Works on phone, tablet, and even PC-connected displays

## Build (GitHub Actions)
1. Push this repo to GitHub
2. Actions → **Build Starcord APK** → runs automatically on every push to `main`
3. Download `starcord-debug.apk` from the Artifacts section

## Manual Install
```
adb install app-debug.apk
```
Or enable *Install unknown apps* on your phone and open the APK.
