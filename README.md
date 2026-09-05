<p align="center">
  <img src="appicon2.png" alt="AirWave Logo" width="150"/>
</p>

<h1 align="center">AirWave</h1>

<p align="center">
  <strong>Chat Nearby. Stay Connected.</strong>
</p>

<p align="center">
  A session-based Bluetooth chat app for nearby device-to-device conversation — no account, no server, no internet required.
</p>

---

## What is AirWave?

AirWave is a simple, privacy-first Android app for chatting with people physically nearby over **Bluetooth RFCOMM**.

- Open AirWave
- Enter a name (no email, no password, no sign-up)
- Turn on Bluetooth
- Find nearby AirWave users
- Send a chat request
- The other person **accepts or rejects** the request
- Chat in real time
- Disconnect — and the conversation is gone

AirWave has **no chat history**. Messages exist only in memory for the duration of the active session. There is no account, no cloud, no server, no API key, and no internet connection required. Nothing is permanently saved.

## Flow

```text
Open AirWave → Enter name → Bluetooth setup → Home
   ↓
Find Nearby Users → pick a user → "Connect with Rahul?" → request
   ↓
"Meet wants to chat with you." → Accept / Reject / Block
   ↓
Chat (session-only, in memory) → Disconnect
   ↓
Conversation cleared. Nothing saved.
```

## Features

* **Pure offline Bluetooth chat** — device to device over RFCOMM, no internet
* **AirWave handshake** — a lightweight protocol confirms the peer runs AirWave before chat starts
* **Session identity** — just a nickname, remembered locally as a convenience (with a clear way to change or clear it)
* **No history, ever** — messages live in memory and are cleared on disconnect, out-of-range, or app close
* **Incoming chat requests** — accept, reject, or block before a session starts
* **Voice-to-text** — speak your message, review it, then send (never auto-sends)
* **Local notifications** — for incoming chat requests (no push, no FCM)
* **Themes** — Light, Dark, and System default
* **Accent colors** — default AirWave orange plus alternative choices
* **Language** — English and Hindi (ready for more)
* **Accessibility** — large text / contrast preferences, content descriptions, reduced-motion option
* **AirWave branding** — #082032 / #2C394B / #334756 / #FF4C29 on every screen

## Architecture

```text
UI (Fragments + XML layouts)
    ↓
BluetoothManager (single session owner, LiveData state)
    ├── discovery (AirWave users only)
    ├── handshake + connection (client / server)
    └── in-memory message list
```

The old UI → API → backend → database stack is gone. AirWave is:

```text
UI → BluetoothManager → in-memory session state
```

## Project structure

```text
com.example.airwave
├── bluetooth/          BluetoothManager, MessageThread
├── model/              AirWaveUser, ChatMessage
├── ui/
│   ├── splash/         branded splash
│   ├── welcome/        name entry (only setup step)
│   ├── home/           session home
│   ├── nearby/         find nearby AirWave users
│   ├── chat/           session chat
│   ├── profile/        Your AirWave Identity (change name)
│   ├── settings/       appearance, language, privacy…
│   └── about/          about & privacy
└── util/               PreferencesHelper, LanguageHelper
```

## Requirements

* Android 7.0 (API 24) or higher
* Bluetooth-enabled device
* Two physical Android phones to test chat between devices

## Permissions

| Permission            | Purpose                                        |
| --------------------- | ---------------------------------------------- |
| `BLUETOOTH`           | Core Bluetooth communication                   |
| `BLUETOOTH_ADMIN`     | Device discovery                               |
| `BLUETOOTH_CONNECT`   | Connect to nearby users (Android 12+)          |
| `BLUETOOTH_SCAN`      | Discover nearby users (Android 12+)            |
| `BLUETOOTH_ADVERTISE` | Be visible / accept requests (Android 12+)     |
| `ACCESS_FINE_LOCATION`| Bluetooth discovery on older Android versions  |
| `RECORD_AUDIO`        | Voice-to-text input                            |
| `POST_NOTIFICATIONS`  | Local notifications (Android 13+)              |

There is **no** `INTERNET` permission. AirWave does not use the network.

## Getting started

1. Install the app on two Android phones
2. Open AirWave and enter a name on each phone
3. Grant Bluetooth (and location on older Android) permissions
4. Keep both apps open with Bluetooth on
5. On one phone tap **Find Nearby Users**
6. Select the other phone and send a chat request
7. Accept the request on the other phone and start chatting

## Privacy

* No account — your name is not an account, and it is not registered anywhere
* No chat history — conversations exist in memory only and vanish on disconnect
* No backend — no servers, cloud, or analytics; fully offline over Bluetooth
* Blocked users are stored locally and automatically declined
* Permissions are requested only when a feature needs them

---

<p align="center">
  <strong>Made By Meet Duggar</strong>
</p>
