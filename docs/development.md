# Development

Build with JDK 26 and the Android SDK (platform 36). The checked-in wrapper pins Gradle 9.7.1; the build uses Android Gradle Plugin 9.4.0 and Kotlin 2.4.20. Use Android Studio Quail 4 (2026.1.4) or newer for AGP 9.4 support. Set `ANDROID_HOME` to the SDK or create ignored `local.properties` with `sdk.dir=...`. Android Studio can import this directory independently of the iOS checkout.

`gradle/gradle-daemon-jvm.properties` selects an Eclipse Temurin Java 26 build daemon, even when the launcher uses Android Studio's bundled Java 25 or a different `JAVA_HOME`. Gradle detects an installed matching JDK or provisions one from the versioned download URLs. First-time provisioning requires network access. The Foojay settings plugin resolves compilation toolchains and regenerates those URLs for macOS/Linux ARM64/x64 and Windows x64. No personal JDK path is committed.

In Android Studio, sync the project with Gradle files after updating. If an old incompatible-JVM prompt is still open, dismiss it and sync again so the IDE rereads the wrapper and daemon criteria. Keep the IDE's own JetBrains Runtime unchanged: the IDE runtime and the Gradle daemon are different processes. `./gradlew --version` shows the launcher JVM and the daemon criteria.

Both modules compile with a Java 26 toolchain but emit Java 17 bytecode. `compileSdk`, `targetSdk`, and `minSdk` are unchanged; updating the desktop build JVM does not require Java 26 on an Android device. The app now uses AGP's built-in Kotlin support instead of the obsolete standalone `kotlin("android")` application.

To refresh the pinned distribution and daemon download URLs deliberately:

```sh
./gradlew wrapper --gradle-version=9.7.1 --distribution-type=bin
./gradlew updateDaemonJvm --jvm-version=26 --jvm-vendor=adoptium
```

Verify the distribution and wrapper JAR checksums against [Gradle's published checksums](https://gradle.org/release-checksums/) before committing a wrapper update. See the official [JVM compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html), [daemon criteria](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:daemon_jvm_criteria), and [AGP/Studio compatibility](https://developer.android.com/build/releases/about-agp).

These are stable releases, not previews. Kotlin 2.4.20's published full-compatibility range currently ends at Gradle 9.7.0 and AGP 9.3.1; newer stable versions are allowed but are not yet fully covered by that upstream matrix. Keep the local build, instrumentation and IDE-sync checks when updating this combination. See [Kotlin's compatibility guidance](https://kotlinlang.org/docs/gradle-configure-project.html).

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

## Toolchain upgrade verification — 8 September 2026

The original Gradle 8.13 build failed on `help` when launched by Android Studio's JBR 25.0.3. After migration, the same launcher successfully selects the Temurin 26.0.2.1+1 daemon through the checked-in criteria. Gradle distribution/wrapper and the installed JDK archive checksums were verified against their official publishers.

With Gradle 9.7.1, AGP 9.4.0 and Kotlin 2.4.20, all 19 JVM tests and all 47 instrumentation tests passed (zero failures/skips). Debug APK, optimized unsigned release APK and unsigned release bundle built successfully. Lint has no errors; remaining warnings concern dependency updates, the unchanged target API 36 and optional resource shrinking. The test infrastructure emits an upstream Protobuf `sun.misc.Unsafe` deprecation warning on Java 26; it does not fail the tests.

Android Studio Quail 4 (2026.1.4) completed a real Gradle sync, recognized both modules and enabled the app run configuration. Class inspection confirmed Java 17 bytecode (major version 61) in both modules. No application behavior or supported Android API level changed.

Do not deploy from Android Studio to the test emulator while instrumentation is running: a concurrent IDE deployment force-stopped the first test run. A separate full rerun after the builds completed passed all 47 tests without changing application code or test timeouts.
