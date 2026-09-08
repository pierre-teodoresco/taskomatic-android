# Development

Build with JDK 17 and the Android SDK (platform 36). Gradle is pinned by the checked-in wrapper. Set `ANDROID_HOME` to the SDK or create ignored `local.properties` with `sdk.dir=...`. Android Studio can import this directory independently of the iOS checkout.

```sh
./gradlew :core:test :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The second command requires a running Android emulator. Use an isolated emulator for tests, never a personal device's store. Install `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`. Release signing is intentionally unconfigured; signing credentials belong outside Git.

Test boundaries: public task lifecycle and reminder plans with fixed dates; the SQLite repository across reopen, stale actions and imports; Compose app flows for creation, editing, completion/restoration, settings and persistence. Each behavior is developed red → green and independently reviewed before its commit.
