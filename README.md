# Airtel Usage Tracker

A native Android application designed to track and manage data usage from Airtel Xstream Fiber routers. This application runs in the background to provide accurate, cumulative usage tracking aligned with your billing cycle.

## Compatibility

> **Note:** This application has been tested and verified **only** for the **Airtel ZYXEL Model PMG5617-R20B**. Compatibility with other router models (Nokia, Huawei, ZTE) is not guaranteed and may require modifications to the scraping logic.

## Key Features

*   **Billing Cycle Management:** Tracks usage based on your specific billing start date.
*   **Persistent History:** Stores usage history in a local database (Room), preventing data loss during app restarts.
*   **Automatic Background Sync:** Periodically fetches usage data uniquely when connected to WiFi to minimize battery drain.
*   **Smart Rollover:** Automatically handles month-to-month billing transitions and resets counters appropriately.
*   **Router Reboot Detection:** Intelligent logic detects router restarts to ensure cumulative usage is calculated correctly without double-counting.
*   **Dashboard:** Provides a clear overview of current cycle usage, days remaining, and FUP limit status.

## Technical Overview

*   **Architecture:** MVVM with Clean Architecture principles.
*   **UI:** Jetpack Compose (Material 3).
*   **Asynchronous Processing:** Kotlin Coroutines and Flow.
*   **Background Work:** Android WorkManager.
*   **Local Storage:** Room Database for history; DataStore/SharedPreferences for configuration.
*   **Data Acquisition:** Direct HTML scraping of the router's web interface.

## Setup Configuration

1.  **Credentials:** standard router login (default IP: `192.168.1.1`).
2.  **Billing Cycle:** Configure the "Bill Cycle Start Day" (e.g., 1st, 11th) in Settings to align the tracker with your ISP billing.
3.  **Sync Interval:** Customize how frequently the app polls the router (default: 4 hours).

## Disclaimer

This is a third-party application and is not affiliated with Airtel. Use at your own discretion.
