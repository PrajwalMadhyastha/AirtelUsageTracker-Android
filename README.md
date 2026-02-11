# Airtel Usage Tracker - Android App

A native Android app to track Airtel router data usage, designed to run 24/7 on an old Android phone.

## Features

- ✅ **Automatic Background Tracking**: Polls router every 15 minutes via WorkManager
- ✅ **Reboot-Safe**: Uptime-based detection prevents data loss during router reboots
- ✅ **Material 3 UI**: Clean, modern dashboard with usage stats and progress bar
- ✅ **WebView Scraping**: No external dependencies, uses built-in WebView
- ✅ **Persistent Storage**: SharedPreferences for lightweight data storage

## Setup Instructions

### 1. Open in Android Studio
1. Copy the project to `/Users/prajw/StudioProjects/AirtelUsageTracker`
2. Open Android Studio
3. Select "Open an Existing Project"
4. Navigate to the project folder

### 2. Configure Router Credentials
The app uses default credentials:
- **Router IP**: 192.168.1.1
- **Username**: admin
- **Password**: admin
- **FUP Limit**: 3333 GB

To change these, you can either:
- Modify the defaults in `RouterConfig` data class
- Or add a Settings screen (future enhancement)

### 3. Build & Install
1. Connect your old Android phone via USB
2. Enable Developer Options and USB Debugging on the phone
3. In Android Studio, click **Run** (green play button)
4. Select your device
5. The app will install and launch

### 4. Keep Phone Plugged In
- Keep the phone plugged in 24/7
- The app will run in the background
- WorkManager ensures periodic updates even if the app is closed

## How It Works

### Architecture
```
MainActivity (Compose UI)
    ↓
UsageViewModel (State Management)
    ↓
UsageRepository (Business Logic)
    ↓
RouterScraper (WebView Scraping)
    ↓
SharedPreferences (Storage)

UsageWorker (Background) → UsageRepository
```

### Reboot Detection
The app tracks router uptime. When uptime decreases:
1. Detects router reboot
2. Does NOT add previous counters (already counted)
3. Resets baseline to new counter values
4. Prevents double-counting

### Background Service
- **WorkManager** schedules periodic tasks every 15 minutes
- Survives app restarts and phone reboots
- Requires network connectivity

## Project Structure

```
app/
├── src/main/
│   ├── java/com/airtel/usagetracker/
│   │   ├── data/
│   │   │   ├── models/Models.kt          # Data classes
│   │   │   ├── RouterScraper.kt          # WebView scraping
│   │   │   └── UsageRepository.kt        # Business logic
│   │   ├── workers/
│   │   │   └── UsageWorker.kt            # Background task
│   │   ├── ui/
│   │   │   ├── MainActivity.kt           # Main screen
│   │   │   ├── UsageViewModel.kt         # ViewModel
│   │   │   └── theme/                    # Material theme
│   │   └── UsageTrackerApp.kt            # Application class
│   ├── AndroidManifest.xml
│   └── res/                              # Resources
└── build.gradle.kts
```

## Troubleshooting

### App Not Updating in Background
1. Go to Settings → Apps → Airtel Usage Tracker
2. Battery → Unrestricted
3. Disable battery optimization for this app

### WebView Not Loading
- Ensure `usesCleartextTraffic="true"` in AndroidManifest (already set)
- Check router IP is correct
- Verify phone is on same WiFi network as router

### Data Not Persisting
- Check SharedPreferences in Device File Explorer
- Path: `/data/data/com.airtel.usagetracker/shared_prefs/usage_data.xml`

## Future Enhancements

- [ ] Settings screen for router config
- [ ] Notifications when approaching FUP limit
- [ ] Home screen widget
- [ ] Export usage history to CSV
- [ ] Charts/graphs for usage trends

## License

MIT License - Feel free to modify and use!
