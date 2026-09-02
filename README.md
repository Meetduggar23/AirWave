<p align="center">
  <img src="appicon.png" alt="AirWave Logo" width="200"/>
</p>

<h1 align="center">AirWave</h1>

<p align="center">
  <strong>Chat Nearby. Stay Connected.</strong>
</p>

<p align="center">
  An offline Bluetooth chat application for nearby device-to-device communication — no internet required.
</p>

---

## About

AirWave is a modern Android messaging app that lets you chat with people nearby using **Bluetooth RFCOMM**. No servers, no cloud, no data plan needed. Just open the app, discover nearby users, connect, and start chatting.

## Features

- **Offline Bluetooth Chat** — Send and receive messages directly between nearby devices
- **Device Discovery** — Find nearby AirWave users automatically
- **Local Profile** — Create a nickname-based identity, no account required
- **Chat History** — All conversations stored locally on your device
- **Voice-to-Text** — Speak your messages using Android speech recognition
- **Theme System** — Light, Dark, or System Default theme
- **8 Accent Colors** — Blue, Purple, Green, Orange, Red, Teal, Pink, and Default AirWave
- **Text Scaling** — Small, Default, Large, Extra Large text sizes
- **High Contrast** — Accessibility-friendly high contrast mode
- **Multi-Language** — English and Hindi support
- **Reduced Motion** — Accessibility option to minimize animations
- **Notifications** — Local notifications for new messages and connections
- **Material Design 3** — Clean, modern UI following Material Design guidelines

## Screenshots

| Splash | Onboarding | Home | Nearby Users | Chat | Settings |
|--------|-----------|------|-------------|------|----------|
| Branded splash screen | 3-page feature walkthrough | Status cards, quick actions | Scan & connect nearby devices | Message bubbles, voice input | Theme, accent, language, accessibility |

## Architecture

```
UI (Fragments + XML Layouts)
    ↓
ViewModel / Managers
    ↓
Bluetooth Layer (RFCOMM Client/Server + Message Threading)
    ↓
Local Storage (SQLite + SharedPreferences)
```

## Tech Stack

- **Language:** Kotlin
- **UI:** XML Layouts + Material Design 3
- **Navigation:** Jetpack Navigation Component
- **Database:** SQLite (raw SQLiteDatabase)
- **Storage:** SharedPreferences
- **Bluetooth:** Android Bluetooth RFCOMM APIs
- **Async:** Kotlin Coroutines
- **Images:** Glide

## Requirements

- Android 7.0 (API 24) or higher
- Bluetooth-enabled device
- Two physical Android phones for testing chat

## Permissions

| Permission | Purpose |
|-----------|---------|
| `BLUETOOTH` | Core Bluetooth communication |
| `BLUETOOTH_ADMIN` | Device discovery |
| `BLUETOOTH_CONNECT` | Connect to nearby devices (Android 12+) |
| `BLUETOOTH_SCAN` | Discover nearby devices (Android 12+) |
| `BLUETOOTH_ADVERTISE` | Be visible to other AirWave users |
| `ACCESS_FINE_LOCATION` | Required for Bluetooth discovery on older Android versions |
| `RECORD_AUDIO` | Voice-to-text input |
| `POST_NOTIFICATIONS` | Message and connection notifications |

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Build and run on two physical Android devices
4. Create a profile on each device
5. Enable Bluetooth and grant permissions
6. Tap **Find Nearby Users** to discover and connect

## Privacy

- Messages are stored **locally** on your device only
- No cloud messaging or online accounts required
- Bluetooth is used solely for nearby device communication
- Permissions are requested only when needed

## License

Made with care for nearby communication.
