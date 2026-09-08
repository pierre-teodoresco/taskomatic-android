# Development

Build with JDK 17 and the Android SDK (platform 36). Gradle is pinned by the checked-in wrapper. Set `ANDROID_HOME` to the SDK or create ignored `local.properties` with `sdk.dir=...`. Android Studio can import this directory independently of the iOS checkout.

```sh
./gradlew :core:test :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease :app:bundleRelease
```

The second command requires a running Android emulator. Use an isolated emulator for tests, never a personal device's store. With several devices connected, set `ANDROID_SERIAL` to the intended emulator. Connected tests may uninstall the debug app; install `app/build/outputs/apk/debug/app-debug.apk` again with `adb install -r` for manual verification.

Debug uses `com.pierreteodoresco.taskomatic.debug`, separate from the release application's store. Release signing is intentionally unconfigured; signing credentials belong outside Git. The optimized APK is `app/build/outputs/apk/release/app-release-unsigned.apk`; the unsigned bundle is `app/build/outputs/bundle/release/app-release.aab`. Neither is a Play Store release. Both language resources remain in the base bundle so switching between English and French works offline.

Test boundaries: public task lifecycle and reminder plans with fixed dates; the SQLite repository across reopen, stale actions and imports; Compose app flows for creation, editing, completion/restoration, settings and persistence. Each behavior is developed red → green and independently reviewed before its commit.

## Manual verification

Use synthetic tasks only. Create a simple task and a recurring task; edit their titles/notes, complete, undo, restore, delete with confirmation, and relaunch to check persistence. Repeat navigation and editing in English and French, both appearances, and with large accessibility text. Check that the keyboard and scrolling leave actions reachable.

In Settings, enable reminders and handle the real Android notification permission prompt. Send a test notification without creating a task. Check the scheduled time against `adb shell dumpsys alarm` and inspect the notification on the device. Notification delivery is inexact; this is not an exact-time guarantee or a substitute for long-running battery/Doze testing on physical devices.

Export to Downloads through Android's document picker, then import the same document through the picker. Confirm the preview, ensure existing tasks are not duplicated or overwritten, and check the result. Also cancel an import and try an invalid document. Cloud-provider uploads are not part of Android's acceptance criteria: the app has no cloud integration.

## Initial delivery verification — 8 September 2026

Validation used JDK 17, SDK 36 and an isolated Pixel 7 ARM64 emulator running Android 15 (API 35). The release APK and bundle build successfully; lint reports no errors and only dependency-version update notices. Dependencies are pinned to the versions exercised here.

The final full run passed all 19 JVM domain/compatibility tests and all 47 on-device instrumentation tests, with no failures or skipped tests. Reports are generated under `core/build/reports/tests/test/` and `app/build/reports/androidTests/connected/debug/`; the lint report is `app/build/reports/lint-results-debug.html`.

Manual checks covered a real DocumentsUI export/reimport, the notification permission prompt, a registered system alarm, and French dark-mode settings/import confirmation at 200% font size. Automated regression cases include invalid/oversized document reads, atomic import rollback, stale notification actions, real SQLite write failures, a multi-megabyte Unicode/NUL note, and edited-draft restoration after activity recreation. The large-editor activity state measured 5,808 bytes after moving large text out of its Bundle.

The portable backup fixture was emitted by the actual iOS Swift encoder. The Kotlin compatibility test reads it and emits `core/build/verification/android-v1.json`; that output was also decoded by the actual Swift implementation and compared field-for-field. See [architecture](architecture.md) for the fixture provenance.

This is an emulator-validated first native implementation, not certification on every supported API/OEM. Physical-device accessibility, long-running background/battery behavior, release signing and Play distribution remain to be validated before a production release. No GitHub Actions workflow or required CI status has been introduced.
