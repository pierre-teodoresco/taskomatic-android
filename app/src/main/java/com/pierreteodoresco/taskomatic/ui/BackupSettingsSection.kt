package com.pierreteodoresco.taskomatic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierreteodoresco.taskomatic.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BackupSettingsSection(model: TaskViewModel) {
    val busy by model.busy.collectAsStateWithLifecycle()
    val preview by model.pendingImport.collectAsStateWithLifecycle()
    val tasks by model.tasks.collectAsStateWithLifecycle()
    val notice by model.backupNotice.collectAsStateWithLifecycle()
    val importDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::prepareImport) }
    val exportDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::exportBackup) }
    Text(stringResource(R.string.manual_backup), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.manual_backup_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedButton({ exportDocument.launch("Taskomatic-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.json") },
        enabled = !busy, modifier = Modifier.testTag("backup-export")) { Text(stringResource(R.string.export_backup)) }
    OutlinedButton({ importDocument.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
        enabled = !busy, modifier = Modifier.testTag("backup-import")) { Text(stringResource(R.string.import_backup)) }
    preview?.let { backup ->
        val existing = tasks.map { it.id }.toSet()
        val newCount = backup.items.count { it.id !in existing }
        AlertDialog(onDismissRequest = model::cancelImport,
            title = { DialogSystemBarAppearance(); Text(stringResource(R.string.import_backup)) },
            text = { Text(stringResource(R.string.import_preview, newCount, backup.items.size - newCount)) },
            confirmButton = { TextButton(model::confirmImport, enabled = !busy, modifier = Modifier.testTag("import-confirm")) { Text(stringResource(R.string.import_confirm)) } },
            dismissButton = { TextButton(model::cancelImport, enabled = !busy, modifier = Modifier.testTag("import-cancel")) { Text(stringResource(R.string.cancel)) } })
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
