package com.pierreteodoresco.taskomatic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import com.pierreteodoresco.taskomatic.data.Appearance
import com.pierreteodoresco.taskomatic.ui.TaskViewModel
import com.pierreteodoresco.taskomatic.ui.TaskomaticApp
import com.pierreteodoresco.taskomatic.ui.TaskomaticTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: TaskViewModel = viewModel()
            val preferences by model.preferences.collectAsStateWithLifecycle()
            val preferencesLoaded by model.preferencesLoaded.collectAsStateWithLifecycle()
            LaunchedEffect(preferencesLoaded, preferences.language, preferences.appearance) {
                if (preferencesLoaded) {
                    val nightMode = when (preferences.appearance) {
                        Appearance.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        Appearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        Appearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != nightMode) AppCompatDelegate.setDefaultNightMode(nightMode)
                    val locales = LocaleListCompat.forLanguageTags(preferences.language.tag)
                    if (AppCompatDelegate.getApplicationLocales() != locales) AppCompatDelegate.setApplicationLocales(locales)
                }
            }
            val dark = when (preferences.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            TaskomaticTheme(dark) { TaskomaticApp(model) }
        }
    }
}
