package com.pierreteodoresco.taskomatic

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.view.WindowCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsAppTest {
    @get:Rule val compose = createEmptyComposeRule()

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
