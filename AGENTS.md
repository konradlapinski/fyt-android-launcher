# Project guidance

- Keep the first release as a regular, unprivileged Android HOME app. Do not add root requirements, firmware flashing, MCU changes, or replacement of factory system apps.
- Preserve a clear route back to Android Home settings so the owner can select the factory launcher.
- Treat FYT, radio, CAN, and other vendor integrations as unsupported until a documented API, license review, and physical-unit test establish otherwise.
- Keep navigation external to this app unless a provider's embedding terms and technical support are verified. Do not include API keys or proprietary app code/assets.
- Keep driver interactions readable and large enough for a 1280×720 landscape display. Do not add distracting motion or driving-time text entry.
- Before each task, inspect repository skills and read applicable `SKILL.md` files. Do not overwrite user-provided skills.
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug`; run `./gradlew connectedDebugAndroidTest` on API 29 when an emulator is available. Track physical head-unit checks separately from emulator results.
