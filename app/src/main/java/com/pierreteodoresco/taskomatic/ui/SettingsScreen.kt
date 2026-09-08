package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.rounded.Smartphone
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
        ScreenHeader(title = stringResource(R.string.settings), navigation = {
            IconButton(onBack, Modifier.testTag("settings-close")) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)) {
            ReminderSettingsSection(model)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionCaption(stringResource(R.string.appearance))
                TaskSurface {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Segments(Appearance.entries.map { option -> Segment(stringResource(when (option) {
                            Appearance.SYSTEM -> R.string.system
                            Appearance.LIGHT -> R.string.light
                            Appearance.DARK -> R.string.dark
                        }), "appearance-${option.name.lowercase()}") }, preferences.appearance.ordinal, !busy) {
                            model.setAppearance(Appearance.entries[it])
                        }
                        SubtleDivider()
                        Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyMedium)
                        Segments(AppLanguage.entries.map { option -> Segment(stringResource(when (option) {
                            AppLanguage.SYSTEM -> R.string.system
                            AppLanguage.ENGLISH -> R.string.english
                            AppLanguage.FRENCH -> R.string.french
                        }), "language-${option.name.lowercase()}") }, preferences.language.ordinal, !busy) {
                            model.setLanguage(AppLanguage.entries[it])
                        }
                        Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            BackupSettingsSection(model)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionCaption(stringResource(R.string.storage))
                TaskSurface {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Smartphone, null, Modifier.size(22.dp))
                            Text(stringResource(R.string.local_only), style = MaterialTheme.typography.labelLarge)
                        }
                        Text(stringResource(R.string.local_only_body), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandMark(32)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
