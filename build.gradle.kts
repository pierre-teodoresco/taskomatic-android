plugins {
    id("com.android.application") version "9.4.0" apply false
    kotlin("jvm") version "2.4.20" apply false
    kotlin("plugin.compose") version "2.4.20" apply false
    kotlin("plugin.serialization") version "2.4.20" apply false
}

// Provision native JDKs only for the supported desktop build platforms.
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    toolchainPlatforms.set(listOf(
        org.gradle.platform.BuildPlatformFactory.of(org.gradle.platform.Architecture.AARCH64, org.gradle.platform.OperatingSystem.MAC_OS),
        org.gradle.platform.BuildPlatformFactory.of(org.gradle.platform.Architecture.X86_64, org.gradle.platform.OperatingSystem.MAC_OS),
        org.gradle.platform.BuildPlatformFactory.of(org.gradle.platform.Architecture.AARCH64, org.gradle.platform.OperatingSystem.LINUX),
        org.gradle.platform.BuildPlatformFactory.of(org.gradle.platform.Architecture.X86_64, org.gradle.platform.OperatingSystem.LINUX),
        org.gradle.platform.BuildPlatformFactory.of(org.gradle.platform.Architecture.X86_64, org.gradle.platform.OperatingSystem.WINDOWS),
    ))
}
