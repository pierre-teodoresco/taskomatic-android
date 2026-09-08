package com.pierreteodoresco.taskomatic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.ui.platform.LocalConfiguration
import java.time.ZoneId
import java.time.format.FormatStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierreteodoresco.taskomatic.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsSection(model: TaskViewModel) {
    val busy by model.busy.collectAsStateWithLifecycle()
    val preview by model.pendingImport.collectAsStateWithLifecycle()
    val tasks by model.tasks.collectAsStateWithLifecycle()
    val notice by model.backupNotice.collectAsStateWithLifecycle()
    val importDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::prepareImport) }
    val exportDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::exportBackup) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCaption(stringResource(R.string.manual_backup))
        TaskSurface {
            Text(stringResource(R.string.manual_backup_hint), Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SubtleDivider()
            SettingsAction(stringResource(R.string.export_backup), Icons.Rounded.Upload,
                Modifier.testTag("backup-export"), !busy) {
                exportDocument.launch("Taskomatic-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.json")
            }
            SubtleDivider(Modifier.padding(start = 20.dp))
            SettingsAction(stringResource(R.string.import_backup), Icons.Rounded.Download,
                Modifier.testTag("backup-import"), !busy) {
                importDocument.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    preview?.let { backup ->
        val existing = tasks.map { it.id }.toSet()
        val newCount = backup.items.count { it.id !in existing }
        Dialog(onDismissRequest = model::cancelImport, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogSystemBarAppearance()
            Scaffold(topBar = {
                ScreenHeader(title = stringResource(R.string.import_backup), navigation = { TextButton(model::cancelImport, enabled = !busy, modifier = Modifier.testTag("import-cancel")) {
                        Text(stringResource(R.string.cancel))
                    } })
            }, bottomBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PrimaryButton(stringResource(if (newCount == 0) R.string.done else R.string.import_confirm),
                        Modifier.navigationBarsPadding().padding(24.dp).testTag("import-confirm"), !busy) {
                        if (newCount == 0) model.cancelImport() else model.confirmImport()
                    }
                }
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(Icons.Rounded.Restore, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.import_preview, newCount, backup.items.size - newCount), style = MaterialTheme.typography.bodyLarge)
                    TaskSurface {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val locale = LocalConfiguration.current.locales[0]
                            Text(stringResource(R.string.backup_created, backup.createdAt.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            backup.items.filter { it.id !in existing }.take(3).forEach { task ->
                                Text(task.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }

    notice?.let { result ->
        AlertDialog(onDismissRequest = model::dismissBackupNotice,
            title = { DialogSystemBarAppearance(); Text(stringResource(if (result == BackupNotice.Failed) R.string.backup_failed else R.string.backup_done)) },
            text = { Text(when (result) {
                is BackupNotice.Imported -> stringResource(R.string.import_result, result.inserted, result.skipped)
                BackupNotice.Failed -> stringResource(R.string.backup_failed_body)
                BackupNotice.Exported -> stringResource(R.string.export_result)
            }) },
            confirmButton = { TextButton(model::dismissBackupNotice, modifier = Modifier.testTag("backup-notice-close")) { Text(stringResource(R.string.ok)) } })
    }
}
