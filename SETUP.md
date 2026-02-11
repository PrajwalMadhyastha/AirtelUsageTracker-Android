# Airtel Usage Tracker - Setup Guide

This guide explains how to configure the app with your router credentials.

## Prerequisites

- Android Studio installed
- Android device or emulator
- Access to your Airtel router's web interface

## Configuration

The app requires router credentials to function. These are stored in `local.properties` (which is gitignored for security).

### 1. Configure Router Credentials

Create or edit `local.properties` in the project root and add:

```properties
# Router default credentials
router.default.ip=192.168.1.1
router.default.username=admin
router.default.password=admin
```

**Replace the values** with your actual router settings:
- `router.default.ip`: Your router's IP address (usually `192.168.1.1`)
- `router.default.username`: Your router's admin username
- `router.default.password`: Your router's admin password

### 2. Build and Run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and click Run.

### 3. Deploy to Device

The app is designed to run 24/7 on an Android phone connected to your WiFi network. **Note**: The emulator cannot access your local router.

## Security Notes

- ✅ `local.properties` is gitignored - your credentials won't be committed
- ✅ Credentials are injected at build time into BuildConfig
- ✅ Users can change credentials in the app's settings after installation

## How It Works

1. Build system reads `local.properties`
2. Credentials are injected into `BuildConfig` as constants
3. App uses these as default values on first launch
4. Users can override via in-app settings (stored in SharedPreferences)

---

**Important**: Never commit `local.properties` to version control!
