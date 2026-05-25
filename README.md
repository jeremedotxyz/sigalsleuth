[README.md](https://github.com/user-attachments/files/28202578/README.md)
# Cell Tracker

A dark-themed Android app for monitoring nearby cell towers, signal strength, and radio metrics in real time.

![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Features

- **Live signal readout** — dBm value with color-coded quality indicator (excellent → poor)
- **Multi-generation support** — 5G NR, 4G LTE, 3G WCDMA/UMTS, 2G GSM, CDMA
- **Per-tower details** — Cell ID, LAC/TAC, PCI, ARFCN/EARFCN, MCC, MNC, operator name
- **LTE/NR radio metrics** — RSRP, RSRQ, RSSNR/SINR, CQI, channel bandwidth
- **Serving vs neighbor cells** — clearly labeled, sorted by signal strength
- **GPS location** — lat/lon + accuracy stamped on each scan
- **Background service** — optional foreground service keeps scanning with a persistent notification
- **Tap for detail** — bottom sheet with full raw values for any visible tower

---

## Screenshots

> Dark signal-intelligence aesthetic: `#0D0F14` background, monospace readouts, green/blue/amber signal color coding.

---

## Requirements

| Item | Version |
|---|---|
| Android Studio | Hedgehog 2023.1+ |
| Gradle | 8.4 |
| Android SDK | API 26 (Oreo) min, API 34 target |
| Kotlin | 1.9.22 |

---

## Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Required by Android 9+ for `getAllCellInfo()` |
| `READ_PHONE_STATE` | Access telephony state and cell info |
| `FOREGROUND_SERVICE` | Background scanning notification |
| `POST_NOTIFICATIONS` | Android 13+ notification permission |

> All data is processed **on-device only**. Nothing is uploaded or transmitted.

---

## Build & Run

```bash
git clone https://github.com/YOUR_USERNAME/CellTracker
cd CellTracker
./gradlew assembleDebug
```

Or open in Android Studio → **Run** (Shift+F10).

> **Note:** Cell info APIs require a real physical device. Emulators return empty or simulated data.

---

## Project Structure

```
CellTracker/
├── app/src/main/
│   ├── java/com/celltracker/
│   │   ├── data/
│   │   │   └── CellTowerData.kt      # Data models (tower, network type, signal quality)
│   │   ├── scanner/
│   │   │   ├── CellScanner.kt        # TelephonyManager wrapper, parses all cell types
│   │   │   └── ScannerService.kt     # Foreground service for background scanning
│   │   └── ui/
│   │       ├── MainActivity.kt       # Main screen, scan loop, location, animations
│   │       ├── CellTowerAdapter.kt   # RecyclerView adapter for tower list
│   │       └── TowerDetailDialog.kt  # Bottom sheet with full tower details
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml     # Main screen layout
│       │   ├── item_cell_tower.xml   # Tower list item
│       │   └── dialog_tower_detail.xml
│       ├── drawable/                 # Signal progress bar, badges, icons
│       └── values/                  # Colors, strings, themes
```

---

## Signal Reference

| dBm Range | Quality |
|---|---|
| ≥ -65 | Excellent |
| -66 to -80 | Good |
| -81 to -95 | Fair |
| -96 to -110 | Poor |
| < -110 | No Signal |

---

## License

MIT — free to use, modify, redistribute.
