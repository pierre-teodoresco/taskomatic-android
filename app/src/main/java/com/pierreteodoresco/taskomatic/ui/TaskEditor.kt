package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.DeleteOutline
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
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
    onClose: () -> Unit, onDelete: () -> Unit, isNew: Boolean = false, onSave: (String, String, Recurrence?) -> Unit) {
    val titleSaver = remember(stateKey) { textStateSaver(storage, "$stateKey-draft-title", onDraftFailure) }
    val noteSaver = remember(stateKey) { textStateSaver(storage, "$stateKey-draft-note", onDraftFailure) }
    val intervalSaver = remember(stateKey) { textStateSaver(storage, "$stateKey-draft-interval", onDraftFailure) }
    var title by rememberSaveable(item.id.toString(), stateSaver = titleSaver) { mutableStateOf(item.title) }
    var note by rememberSaveable(item.id.toString(), stateSaver = noteSaver) { mutableStateOf(item.note) }
    var recurring by rememberSaveable(item.id.toString()) { mutableStateOf(item.recurrence != null) }
    var interval by rememberSaveable(item.id.toString(), stateSaver = intervalSaver) { mutableStateOf((item.recurrence?.interval ?: 1).toString()) }
    var unit by rememberSaveable(item.id.toString()) { mutableStateOf(item.recurrence?.unit ?: RecurrenceUnit.DAY) }
    val validInterval = interval.toIntOrNull()?.takeIf { it in 1..99 }
    var recurrenceOpen by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val dirty = title != item.title || note != item.note || recurring != (item.recurrence != null) ||
        (recurring && (validInterval != item.recurrence?.interval || unit != item.recurrence?.unit))
    val close = { if (!busy) { if (dirty) confirmDiscard = true else onClose() } }
    Dialog(onDismissRequest = { if (recurrenceOpen) recurrenceOpen = false else close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogSystemBarAppearance()
        BackHandler(recurrenceOpen) { recurrenceOpen = false }
        if (recurrenceOpen) {
            RecurrenceScreen(if (recurring) Recurrence(validInterval ?: 1, unit) else null,
                onBack = { recurrenceOpen = false }) { value ->
                recurring = value != null
                interval = (value?.interval ?: 1).toString()
                unit = value?.unit ?: RecurrenceUnit.DAY
                recurrenceOpen = false
            }
        } else Scaffold(topBar = {
            ScreenHeader(title = stringResource(if (isNew) R.string.new_task else R.string.edit_task), navigation = { IconButton(close, enabled = !busy) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) } },
                actions = { TextButton({ onSave(title, note, if (recurring) Recurrence(validInterval!!, unit) else null) },
                    enabled = !busy && title.isNotBlank() && (!recurring || validInterval != null), modifier = Modifier.testTag("edit-save")) {
                    Text(stringResource(if (isNew) R.string.add_short else R.string.save))
                } })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TaskSurface {
                    EditorField(title, { title = it }, stringResource(R.string.task_title), !busy,
                        Modifier.testTag("edit-title"), titleField = true)
                    SubtleDivider(Modifier.padding(horizontal = 20.dp))
                    EditorField(note, { note = it }, stringResource(R.string.note), !busy, Modifier.testTag("edit-note"))
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionCaption(stringResource(R.string.repeat_title))
                    TaskSurface {
                        SettingsAction(if (recurring) recurrenceLabel(Recurrence(validInterval ?: 1, unit)) else stringResource(R.string.repeat_never),
                            Icons.Rounded.Repeat, Modifier.testTag("recurrence-open"), !busy) { focus.clearFocus(); recurrenceOpen = true }
                    }
                    if (recurring) Text(stringResource(R.string.recurrence_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.completedAt?.let { completed ->
                    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0])
                    Text(stringResource(R.string.last_completed, formatter.format(completed.atZone(ZoneId.systemDefault()))),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (recurring) {
                        val next = Recurrence(validInterval ?: 1, unit).nextActivation(completed, ZoneId.systemDefault())
                        Text(stringResource(R.string.next_cycle, formatter.format(next.atZone(ZoneId.systemDefault()))),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!isNew) TextButton({ focus.clearFocus(); confirmDelete = true }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("edit-delete"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
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

@Composable
private fun EditorField(value: String, onChange: (String) -> Unit, placeholder: String, enabled: Boolean,
    modifier: Modifier = Modifier, titleField: Boolean = false) {
    BasicTextField(value, onChange, enabled = enabled, modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder }.padding(20.dp),
        textStyle = (if (titleField) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
            .copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        minLines = if (titleField) 1 else 4, maxLines = if (titleField) 5 else 8,
        decorationBox = { input -> Box {
            if (value.isEmpty()) Text(placeholder, style = if (titleField) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            input()
        } })
}
