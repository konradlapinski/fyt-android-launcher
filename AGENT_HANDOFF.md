# Agent handoff: FYT Android Launcher

Read this file together with `AGENTS.md` before continuing work.

## Project goal

Build a safe, customizable Android HOME launcher for the user's FR9 car head unit. The reported hardware is a Unisoc UIS7862S, ARM64, 1280×720 display, and Android API 29 (Android 10); the FYT platform identification is still an assumption. Preserve the factory launcher and provide a reliable way to return to it.

The user's model preference for this project is Luna xhigh or Luna max for all tasks and agents. Do not use Sol or Astra models.

## Original project brief

The target is a Chinese head unit installed in a Hyundai i30 II (2014). The user reports these device details, verified with CPU-Z: FR9 head unit; FYT platform likely but not definitively confirmed; Unisoc UIS7862S with 2× Cortex-A75 and 6× Cortex-A55; Mali-G52; 8 GB RAM; 128 GB advertised storage; ARM64; 1280×720 display; Android 10/API 29; Linux 4.14.133; no root; KYD/L6315-family MCU. Android 14 is an advertised/misleading system string. Use API 29 as the compatibility target and verify device properties directly.

The product vision includes a customizable dashboard, app shortcuts, external navigation, optional FM radio and Bluetooth/media controls where supported, vehicle information where available, widgets, day/night themes, and settings backup/restore. The first release must prioritize a safe, ordinary launcher, navigation access, media, shortcuts, customization, and a dependable route back to the factory launcher. Treat radio, vehicle, and vendor features as optional research until documented APIs and physical-device tests support them.

The factory launcher reportedly embeds Google Maps but shows distracting recommendation panels (for example, “Białystok / Dinner picks”). Investigate supported intents, Google Maps SDK/navigation options, and alternative map providers if navigation UI work is requested. Do not assume unrestricted interactive embedding is technically supported or licensed.

Reference projects from the original brief (research only; do not treat as dependencies without review):

- https://github.com/vasyl91/FYT-Launcher-Mod
- https://github.com/PimpinPumpkin/fyt7862-mods
- https://carwebguru.com/

Review licenses before reusing code. The initial brief asked to compare Kotlin/Java, Compose/Views, modular architecture, Android 10 support, launcher HOME intents, persistence, performance, integrations, and safe rollback. The current MVP uses Kotlin, Jetpack Compose, DataStore, and Android HOME role support.

The original plan said to begin in planning mode. Later user direction proceeded to MVP implementation; continue from the existing implementation and follow the latest user request instead of restarting the planning phase.

## Current implementation

The repository contains an initial Kotlin/Jetpack Compose MVP with:

- Android HOME role support and a route to Android Home settings.
- A configurable dashboard with one external navigation shortcut and four app shortcuts.
- Optional media controls backed by Android MediaSession data and notification-listener access.
- Day/night appearance and locally persisted settings using DataStore.
- JSON settings export/import through Android's document picker.
- GitHub Actions for Android checks and APK artifact output.

Navigation launches another app; it is not embedded. FYT-specific controls, FM radio APIs, CAN/vehicle data, and proprietary head-unit integrations are not implemented or verified.

## Safety and scope

- Keep the launcher an ordinary, unprivileged Android HOME app.
- Do not require root, flash firmware, modify the MCU, or disable/replace factory system apps.
- Do not assume the FYT platform or undocumented vendor APIs are available.
- Do not embed a map provider unless its technical support and license permit it.
- Keep driver interactions readable and usable at 1280×720; avoid distracting motion and driving-time text entry.
- Do not include proprietary APKs, assets, or API keys.

`AGENTS.md` contains the persistent project rules. Check for project-provided `SKILL.md` files before each task; none were present when this handoff was written. Do not overwrite skills supplied later by the user.

## Build and checks

Use the Gradle wrapper from the repository root. The project expects JDK 21 and Android SDK platform 36/build tools 36; the minimum Android version is API 29 and target SDK is 35.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

The instrumentation command requires an API 29 emulator or device. Cloud checks previously passed for unit tests, lint, debug APK, and Android test APK. API 29 emulator instrumentation passed before the final backup-rules manifest adjustment; rerun it after relevant changes. Lint reported an `OldTargetApi` warning because SDK 36 is newer than the current target SDK 35.

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk` after building.

## Next useful work

1. Inspect the actual head unit and verify it accepts a third-party HOME app.
2. Confirm the factory launcher can still be selected after FYT Launcher is chosen.
3. Check touch layout, contrast, text scaling, and screen behavior at 1280×720.
4. Test external navigation launch, app discovery/shortcuts, media access, and backup/restore on the device.
5. Keep any FYT, radio, CAN, or vehicle-data investigation separate from verified behavior until a supported interface and physical-device test exist.

Do not report emulator checks as physical head-unit validation.

## GitHub/local continuation

The current implementation and this handoff are on branch `agent-handoff`; `main` was left unchanged. The branch is connected to `origin` at `https://github.com/konradlapinski/fyt-android-launcher.git`.

From `C:\Users\Konrad\OneDrive\Codex`, the user can get the project with:

```powershell
git clone --branch agent-handoff https://github.com/konradlapinski/fyt-android-launcher.git fyt-android-launcher
```

For a new local chat, first read `AGENTS.md`, this file, and `README.md`, then inspect the code before proposing or making changes. Keep work on `agent-handoff` unless the user asks to merge or change branches.
