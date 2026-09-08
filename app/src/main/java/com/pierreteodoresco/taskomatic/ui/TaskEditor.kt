package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pierreteodoresco.taskomatic.R
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.core.RecurrenceUnit
import com.pierreteodoresco.taskomatic.data.TextStateStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditor(item: TaskItem, busy: Boolean, stateKey: String, storage: TextStateStorage, onDraftFailure: () -> Unit,
    onClose: () -> Unit, onDelete: () -> Unit, onSave: (String, String, Recurrence?) -> Unit) {
    val titleSaver = remember(stateKey) { textStateSaver(storage, "$stateKey-draft-title", onDraftFailure) }
    val noteSaver = remember(stateKey) { textStateSaver(storage, "$stateKey-draft-note", onDraftFailure) }
    val intervalSaver = remember(stateKey) { textStateSaver(storage, "$stateKey-draft-interval", onDraftFailure) }
    var title by rememberSaveable(item.id.toString(), stateSaver = titleSaver) { mutableStateOf(item.title) }
    var note by rememberSaveable(item.id.toString(), stateSaver = noteSaver) { mutableStateOf(item.note) }
    var recurring by rememberSaveable(item.id.toString()) { mutableStateOf(item.recurrence != null) }
    var interval by rememberSaveable(item.id.toString(), stateSaver = intervalSaver) { mutableStateOf((item.recurrence?.interval ?: 1).toString()) }
    var unit by rememberSaveable(item.id.toString()) { mutableStateOf(item.recurrence?.unit ?: RecurrenceUnit.DAY) }
    val validInterval = interval.toIntOrNull()?.takeIf { it in 1..99 }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val dirty = title != item.title || note != item.note || recurring != (item.recurrence != null) ||
        (recurring && (validInterval != item.recurrence?.interval || unit != item.recurrence?.unit))
    val close = { if (!busy) { if (dirty) confirmDiscard = true else onClose() } }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogSystemBarAppearance()
        Scaffold(topBar = {
            TopAppBar(title = { Text(stringResource(R.string.edit_task)) },
                navigationIcon = { IconButton(close, enabled = !busy) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) } },
                actions = { TextButton({ onSave(title, note, if (recurring) Recurrence(validInterval!!, unit) else null) },
                    enabled = !busy && title.isNotBlank() && (!recurring || validInterval != null), modifier = Modifier.testTag("edit-save")) {
                    Text(stringResource(R.string.save))
                } })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.task_title)) },
                    enabled = !busy, maxLines = 4, modifier = Modifier.fillMaxWidth().testTag("edit-title"))
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) },
                    enabled = !busy, minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth().testTag("edit-note"))
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().toggleable(recurring, enabled = !busy, role = Role.Switch,
                    onValueChange = { recurring = it }).testTag("recurrence-enabled"), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.recurring), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(recurring, null, enabled = !busy)
                }
                Text(stringResource(R.string.recurrence_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (recurring) {
                    OutlinedTextField(interval, { interval = it }, label = { Text(stringResource(R.string.interval)) },
                        singleLine = true, enabled = !busy, isError = validInterval == null,
                        supportingText = { Text(stringResource(R.string.interval_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("recurrence-interval"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurrenceUnit.entries.forEach { option ->
                            FilterChip(unit == option, { unit = option }, { Text(stringResource(when (option) {
                                RecurrenceUnit.DAY -> R.string.days
                                RecurrenceUnit.WEEK -> R.string.weeks
                                RecurrenceUnit.MONTH -> R.string.months
                            })) }, enabled = !busy, modifier = Modifier.testTag("recurrence-${option.name.lowercase()}"))
                        }
                    }
                }
                HorizontalDivider()
                TextButton({ confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("edit-delete"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete_task))
                }
            }
        }
        if (confirmDelete) AlertDialog(onDismissRequest = { if (!busy) confirmDelete = false },
            title = { DialogSystemBarAppearance(); Text(stringResource(R.string.delete_task)) },
            text = { Text(stringResource(R.string.delete_warning)) },
            confirmButton = { TextButton(onDelete, enabled = !busy, modifier = Modifier.testTag("delete-confirm")) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            } }, dismissButton = { TextButton({ confirmDelete = false }, enabled = !busy, modifier = Modifier.testTag("delete-cancel")) {
                Text(stringResource(R.string.cancel))
            } })
        if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
            title = { DialogSystemBarAppearance(); Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = { TextButton(onClose, modifier = Modifier.testTag("discard-confirm")) { Text(stringResource(R.string.discard)) } },
            dismissButton = { TextButton({ confirmDiscard = false }, modifier = Modifier.testTag("discard-cancel")) { Text(stringResource(R.string.keep_editing)) } })
    }
}
