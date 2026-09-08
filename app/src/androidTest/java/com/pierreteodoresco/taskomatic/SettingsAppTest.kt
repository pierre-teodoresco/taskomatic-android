package com.pierreteodoresco.taskomatic

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.view.WindowCompat
import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule
import androidx.test.platform.app.InstrumentationRegistry
import android.app.NotificationManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val permission: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray())

    @Test fun aTestNotificationDoesNotCreateOrCompleteAnyTask() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = (context.applicationContext as TaskomaticApplication).store
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("settings-open").performClick()
            val before = store.tasks.value
            compose.onNodeWithTag("test-notification").performScrollTo().performClick()
            compose.waitUntil(5_000) { manager.activeNotifications.isNotEmpty() }
            org.junit.Assert.assertEquals(before, store.tasks.value)
            assertTrue(manager.activeNotifications.single().notification.actions.isNullOrEmpty())
        }
        manager.cancelAll()
    }

    @Test fun remindersAndSelectedWeekdaysPersistAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            compose.onNodeWithTag("settings-open").performClick()
            compose.onNodeWithTag("reminders-enabled").performScrollTo().performClick()
            compose.onNodeWithTag("reminders-enabled").assertIsOn()
            compose.onNodeWithTag("reminders-enabled").assertContentDescriptionEquals(
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.reminders_enabled))
            compose.onNodeWithTag("weekday-monday").performScrollTo().performClick()
            compose.onNodeWithTag("weekday-monday").assertIsNotSelected()
            activity.recreate()
            compose.onNodeWithTag("weekday-monday").performScrollTo().assertIsNotSelected()
            compose.onNodeWithTag("weekday-monday").performClick()
            compose.onNodeWithTag("reminders-enabled").performScrollTo().assertIsOn().performClick()
            compose.onNodeWithTag("reminders-enabled").assertIsOff()
        }
    }

    @Test fun changingLanguageLocalizesTheInterfaceAndPersistsAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            compose.onNodeWithTag("settings-open").performClick()
            compose.onNodeWithTag("language-french").performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Réglages").fetchSemanticsNodes().isNotEmpty() }
            activity.recreate()
            compose.onNodeWithText("Réglages").assertIsDisplayed()
            compose.onNodeWithTag("language-french").assertIsSelected()
            compose.onNodeWithTag("language-english").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("language-system").performClick()
        }
    }

    @Test fun appearanceChoicePersistsWhenSettingsReopen() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            compose.onNodeWithTag("settings-open").performClick()
            compose.onNodeWithTag("appearance-dark").performScrollTo().performClick()
            compose.onNodeWithTag("appearance-dark").assertIsSelected()
            activity.onActivity { current ->
                val bars = WindowCompat.getInsetsController(current.window, current.window.decorView)
                assertFalse(bars.isAppearanceLightStatusBars)
                assertFalse(bars.isAppearanceLightNavigationBars)
            }
            activity.recreate()
            compose.onNodeWithTag("appearance-dark").assertIsSelected()
            compose.onNodeWithTag("settings-close").performClick()
            compose.onNodeWithTag("settings-open").performClick()
            compose.onNodeWithTag("appearance-dark").assertIsSelected()
            compose.onNodeWithTag("appearance-light").performClick()
            compose.onNodeWithTag("appearance-light").assertIsSelected()
            activity.onActivity { current ->
                val bars = WindowCompat.getInsetsController(current.window, current.window.decorView)
                assertTrue(bars.isAppearanceLightStatusBars)
                assertTrue(bars.isAppearanceLightNavigationBars)
            }
            compose.onNodeWithTag("appearance-system").performClick()
        }
    }
}
