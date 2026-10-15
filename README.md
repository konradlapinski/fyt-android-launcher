# FYT Launcher

FYT Launcher is an unprivileged Android HOME app for landscape car head units. The initial MVP focuses on a configurable dashboard, large app shortcuts, external navigation handoff, optional media controls, day/night themes, and portable settings backups.

## Current scope

- Minimum Android version: Android 10 (API 29), matching the reported head-unit API level.
- Build SDK: API 36; target SDK: API 35.
- The dashboard has one configurable navigation app and four configurable app shortcuts.
- Navigation opens the selected app as a separate application. It does not embed Google Maps or another map provider.
- Media controls use Android `MediaSession` data. The user must enable the app's notification-listener access in Android Settings; the launcher does not store notification contents.
- Configuration is stored locally with DataStore and can be exported/imported as JSON using Android's document picker.
- The app requests Android's HOME role and provides a shortcut to Home settings for choosing another launcher, including the factory launcher.

FYT platform identification, FM radio APIs, CAN/vehicle data, and any proprietary head-unit controls are still unverified. They are not implemented. The reported unit is an FR9 with UIS7862S, Android API 29, 1280×720 display, ARM64, and no root; FYT compatibility remains an assumption until checked on the device.

This project does not require root, flash firmware, change the MCU, replace system apps, bundle factory APKs, or require a Google Maps API key. The original launcher remains installed and selectable through Android Home settings.

## Build

Install JDK 21 and Android SDK packages `platforms;android-36`, `build-tools;36.0.0`, and `platform-tools`. Then, from the repository root:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Android Studio is optional; the Gradle wrapper is the supported build entry point.

## Checks

Run the local checks with:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On an API 29 emulator or device, run the Compose smoke and JSON backup tests with:

```sh
./gradlew connectedDebugAndroidTest
```

GitHub Actions runs unit tests, Android lint, a debug APK build, and instrumentation tests on an API 29 emulator. It uploads the debug APK as a workflow artifact. These checks do not validate behavior on the target head unit.

## First launch and recovery

1. Install the debug or release APK without uninstalling the factory launcher.
2. Open FYT Launcher and use **Settings → Set as Home app**. Android presents its HOME-role selection UI.
3. Choose the navigation app and dashboard shortcuts in Launcher settings.
4. Media controls can remain off; to enable them, switch on the dashboard media panel and grant notification-listener access from Android Settings.
5. To return to the factory launcher, open **Settings → Choose other Home app** and select the factory launcher. This app does not disable or remove it.

The exact labels and availability of HOME-role and notification-listener settings can vary on vendor Android builds. Confirm those flows on the head unit before daily use.

## Physical head-unit checks still required

- Verify the Android API level and that the unit permits a third-party HOME role.
- Confirm the original launcher is listed and can be selected again after FYT Launcher becomes default.
- Check layout, contrast, touch target size, screen-on behavior, and text scaling at 1280×720.
- Select the installed navigation app and verify external launch behavior.
- Grant/revoke media access, then verify playback metadata and transport controls for the installed media apps.
- Confirm package visibility exposes the apps the user wants to pin.
- Test backup export/import using storage locations offered by the vendor build.
- Confirm no FYT radio, CAN, or vehicle-data integration is assumed to work until a supported interface is documented.
