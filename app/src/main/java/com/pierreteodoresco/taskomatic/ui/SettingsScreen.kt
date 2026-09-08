package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierreteodoresco.taskomatic.R
import com.pierreteodoresco.taskomatic.BuildConfig
import com.pierreteodoresco.taskomatic.data.Appearance
import com.pierreteodoresco.taskomatic.data.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: TaskViewModel, onBack: () -> Unit) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) }, navigationIcon = {
            IconButton(onBack, Modifier.testTag("settings-close")) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Appearance.entries.forEach { appearance ->
                    FilterChip(preferences.appearance == appearance, { model.setAppearance(appearance) },
                        label = { Text(stringResource(when (appearance) {
                            Appearance.SYSTEM -> R.string.system
                            Appearance.LIGHT -> R.string.light
                            Appearance.DARK -> R.string.dark
                        })) }, enabled = !busy, modifier = Modifier.testTag("appearance-${appearance.name.lowercase()}"))
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(preferences.language == language, { model.setLanguage(language) },
                        label = { Text(stringResource(when (language) {
                            AppLanguage.SYSTEM -> R.string.system
                            AppLanguage.ENGLISH -> R.string.english
                            AppLanguage.FRENCH -> R.string.french
                        })) }, enabled = !busy, modifier = Modifier.testTag("language-${language.name.lowercase()}"))
                }
            }
            Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            ReminderSettingsSection(model)
            HorizontalDivider()
            Text(stringResource(R.string.local_only), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.local_only_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
