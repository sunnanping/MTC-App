# MTC App

A multi-site browser Android application with built-in AI chat services, built with Capacitor framework.

## Features

- **Multi-site Browser**: Switch between multiple AI chat services with a single tap
- **Native Android Experience**: Full-screen WebView without browser UI
- **Edge-to-Edge Support**: Perfect adaptation for Android 16 Edge-toEdge display
- **Customizable Sites**: Add, edit, and delete custom websites
- **Local Storage**: Site configurations saved locally
- **Splash Screen**: Branded launch screen with TRAE branding
- **Cloud Build**: Automated APK builds via GitHub Actions

## Built-in AI Services

| Service | URL |
|---------|-----|
| TRAE SOLO | https://solo.trae.cn |
| DeepSeek | https://chat.deepseek.com |
| Doubao | https://www.doubao.com/chat |
| Kimi | https://www.kimi.com |
| NotebookLM | https://notebooklm.google.com |

## Tech Stack

### Frontend (Web Layer)
| Technology | Version | Purpose |
|------------|---------|---------|
| HTML5 | - | UI Structure |
| CSS3 | - | Styling & Layout |
| JavaScript (ES6+) | - | Business Logic |
| localStorage | - | Data Persistence |
| Vite | ^5.0.0 | Build Tool |

### Mobile Framework
| Technology | Version | Purpose |
|------------|---------|---------|
| Capacitor Core | ^8.3.0 | Cross-platform Runtime |
| Capacitor Android | ^8.3.0 | Android Platform |
| Capacitor Splash Screen | ^8.0.1 | Launch Screen |

### Android Native
| Technology | Version | Purpose |
|------------|---------|---------|
| Android Gradle Plugin | 8.9.1 | Build System |
| Gradle | 8.11.1 | Build Automation |
| Java | 21 | Runtime |
| AndroidX WebKit | 1.14.0 | WebView Support |
| compileSdk | 36 | SDK Version |
| targetSdk | 36 | Target Version |
| minSdk | 24 | Minimum Version |

### CI/CD
| Technology | Purpose |
|------------|---------|
| GitHub Actions | Automated Build & Release |

## Project Structure

```
MTC-App/
├── .github/
│   └── workflows/
│       └── build.yml              # GitHub Actions workflow for APK build
│
├── android/                       # Android native project
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/cn/trae/mtc/
│   │   │   │   └── MainActivity.java    # Main Android Activity
│   │   │   ├── res/
│   │   │   │   ├── drawable/            # Icons & splash graphics
│   │   │   │   ├── layout/              # Layout XML files
│   │   │   │   ├── mipmap-anydpi-v26/   # Adaptive icons
│   │   │   │   ├── values/              # Strings, colors, styles
│   │   │   │   └── xml/                 # Config & file paths
│   │   │   └── AndroidManifest.xml      # App manifest
│   │   ├── build.gradle                  # App-level Gradle config
│   │   ├── capacitor.build.gradle        # Capacitor generated config
│   │   └── debug.keystore                # Debug signing key
│   ├── gradle/wrapper/
│   │   ├── gradle-wrapper.jar            # Gradle wrapper JAR
│   │   └── gradle-wrapper.properties     # Gradle version config
│   ├── capacitor-cordova-android-plugins/
│   ├── build.gradle                      # Project-level Gradle config
│   ├── variables.gradle                  # SDK & dependency versions
│   ├── settings.gradle                   # Project settings
│   ├── gradle.properties                 # Gradle properties
│   └── gradlew                           # Gradle wrapper script
│
├── www/                           # Web assets (production)
│   ├── index.html                 # Main app entry
│   └── error.html                 # Error page
│
├── dist/                          # Vite build output
│   └── index.html
│
├── index.html                     # Source HTML (development)
├── vite.config.ts                 # Vite configuration
├── capacitor.config.json          # Capacitor configuration
├── package.json                   # NPM dependencies
├── package-lock.json              # Dependency lock file
├── .gitignore                     # Git ignore rules
└── README.md                      # This file
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│ Android App (APK)                                        │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Android WebView (Native Component)                  │ │
│ │ ┌─────────────────────────────────────────────────┐ │ │
│ │ │ Capacitor Bridge (JS-Native Bridge)            │ │ │
│ │ │ ┌─────────────────────────────────────────────┐ │ │ │
│ │ │ │ MTC App Frontend UI                        │ │ │ │
│ │ │ │ - Navbar (Site Icon Navigation)            │ │ │ │
│ │ │ │ - Iframe (Embedded Browser)                │ │ │ │
│ │ │ │ - Modal (Add/Edit Sites)                   │ │ │ │
│ │ │ │ - Context Menu (Edit/Delete)               │ │ │ │
│ │ │ └─────────────────────────────────────────────┘ │ │ │
│ │ └─────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## WebView Capabilities

### Enabled Permissions
- `INTERNET` - Network access
- `ACCESS_NETWORK_STATE` - Network status check
- `ACCESS_WIFI_STATE` - WiFi status check
- `usesCleartextTraffic` - HTTP allowed

### iframe Sandbox Permissions
- `allow-scripts` - JavaScript execution
- `allow-same-origin` - Same-origin access
- `allow-forms` - Form submission
- `allow-popups` - Popup windows
- `allow-top-navigation` - Top-level navigation

### Network Access
- `<access origin="*" />` - All domains allowed

## Installation

### Download APK
1. Go to [Releases](../../releases) page
2. Download the latest `MTC-App-v1.0.x.apk`
3. Install on your Android device (enable "Unknown sources" if needed)

## Local Development

```bash
# Clone the repository
git clone https://github.com/sunnanping/MTC-App.git
cd MTC-App

# Install dependencies
npm install

# Build frontend
npm run build

# Sync Capacitor
npx cap sync android

# Open in Android Studio
npx cap open android
```

## Build APK

### Automatic Build (GitHub Actions)
Push to `main` branch or manually trigger the workflow:
1. Go to Actions page
2. Select "Build Android APK" workflow
3. Click "Run workflow"

### Manual Build
```bash
cd android
./gradlew assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

## Version Compatibility

| AGP Version | Minimum Gradle | Minimum JDK |
|-------------|----------------|-------------|
| 8.9.1       | 8.11.1         | 17          |

## Credits

- Built with [Capacitor](https://capacitorjs.com/)
- Powered by [TRAE SOLO](https://solo.trae.cn)

## License

MIT License
