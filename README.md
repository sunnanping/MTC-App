# MTC App

Android wrapper for [solo.trae.cn](https://solo.trae.cn)

## Features

- Native Android app experience without browser UI
- Perfect adaptation for Android 16 Edge-to-Edge
- Smart floating action button (FAB) with drag & snap
- Offline detection with auto-reconnect
- Splash screen with TRAE branding
- Cloud build via GitHub Actions

## Tech Stack

- **Framework**: Capacitor 8
- **Runtime**: Node.js 22 + npm
- **Android**: Java 21 + Gradle 8
- **CI/CD**: GitHub Actions

## Installation

1. Go to [Releases](../../releases) page
2. Download the latest `MTC-App-v1.0.x.apk`
3. Install on your Android device

## Local Development

```bash
# Clone the repository
git clone <repo-url>
cd MTC-App

# Install dependencies
npm install

# Sync Capacitor
npx cap sync android

# Open in Android Studio
npx cap open android
```

## Build

Push to `main` branch or manually trigger GitHub Actions workflow to build APK automatically.

## Credits

- Built with [Capacitor](https://capacitorjs.com/)
- Powered by [TRAE SOLO](https://solo.trae.cn)
