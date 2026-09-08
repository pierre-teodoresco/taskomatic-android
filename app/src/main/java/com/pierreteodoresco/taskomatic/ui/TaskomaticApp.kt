package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.core.RecurrenceUnit
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.pierreteodoresco.taskomatic.R
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskomaticApp(model: TaskViewModel) {
    val tasks by model.tasks.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val backgroundFailed by model.backgroundFailed.collectAsStateWithLifecycle()
    val refreshFailed by model.refreshFailed.collectAsStateWithLifecycle()
    val editing by model.editing.collectAsStateWithLifecycle()
    val undo by model.undo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val completedMessage = stringResource(R.string.task_completed)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(undo) {
        undo?.let { completion ->
            if (snackbar.showSnackbar(completedMessage, undoLabel, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                model.undo(completion)
            }
            model.dismissUndo(completion)
        }
    }
    val title by model.quickTitle.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(TaskFilter.ACTIVE) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(settingsOpen) { settingsOpen = false }
    val listState = rememberLazyListState()
    val added by model.added.collectAsStateWithLifecycle()
    LaunchedEffect(added) { added?.let { id ->
        filter = TaskFilter.ACTIVE
        listState.scrollToItem(0)
        model.acknowledgeAdded(id)
    } }
    var now by remember { mutableStateOf(Instant.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = Instant.now(); model.refresh() }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = Instant.now() } }
    val zone = ZoneId.systemDefault()
    val visible = tasks.filter { when (filter) {
        TaskFilter.ACTIVE -> it.isActive(now, zone)
        TaskFilter.WAITING -> it.recurrence != null && !it.isActive(now, zone)
        TaskFilter.COMPLETED -> it.completedAt != null && it.recurrence == null
    } }
    val add = model::add

    if (settingsOpen) SettingsScreen(model) { settingsOpen = false }
    else Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = {
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, actions = { IconButton({ settingsOpen = true }, Modifier.testTag("settings-open")) {
            Icon(Icons.Rounded.Settings, stringResource(R.string.settings))
        } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskFilter.entries.forEach { option ->
                    FilterChip(filter == option, { filter = option }, { Text(stringResource(option.label)) },
                        Modifier.testTag("filter-${option.name.lowercase()}"))
                }
            }
            if (refreshFailed) TextButton(model::refresh) { Text(stringResource(R.string.saved_refresh_failed)) }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, model::setQuickTitle, label = { Text(stringResource(R.string.task_title)) },
                    singleLine = true, enabled = !busy, modifier = Modifier.weight(1f).testTag("quick-title"),
                    shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }))
                FilledIconButton(onClick = add, enabled = !busy && title.isNotBlank(),
                    modifier = Modifier.size(52.dp).testTag("quick-add")) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.add_task))
                }
            }
            if (visible.isEmpty()) {
                Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.CheckCircleOutline, null, Modifier.size(56.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(when (filter) {
                        TaskFilter.ACTIVE -> R.string.empty_active
                        TaskFilter.WAITING -> R.string.empty_waiting
                        TaskFilter.COMPLETED -> R.string.empty_completed
                    }), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.empty_active_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.id }) { task ->
                    Card(onClick = { model.openEditor(task) }, modifier = Modifier.fillMaxWidth().testTag("task-row"), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (filter != TaskFilter.ACTIVE) model.restore(task) else model.complete(task) }, enabled = !busy) {
                                Icon(if (filter != TaskFilter.ACTIVE) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.RadioButtonUnchecked,
                                    stringResource(if (filter != TaskFilter.ACTIVE) R.string.restore_task else R.string.complete_task, task.title),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(Modifier.weight(1f).padding(8.dp)) {
                                Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                if (task.note.isNotBlank()) Text(task.note, maxLines = 2,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                task.recurrence?.let { recurrence ->
                                    Text(recurrenceLabel(recurrence), style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                if (filter == TaskFilter.WAITING) task.nextActivation(zone)?.let { next ->
                                    val locale = LocalConfiguration.current.locales[0]
                                    Text(stringResource(R.string.next_cycle, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                        .withLocale(locale).format(next.atZone(zone))), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { item -> TaskEditor(item, busy, model::closeEditor, { model.delete(item) }) { title, note, recurrence -> model.edit(item, title, note, recurrence) } }
    if (error || backgroundFailed) AlertDialog(onDismissRequest = model::dismissError,
        title = { DialogSystemBarAppearance(); Text(stringResource(R.string.storage_error_title)) },
        text = { Text(stringResource(R.string.storage_error_body)) },
        confirmButton = { TextButton(model::dismissError) { Text(stringResource(R.string.ok)) } })
}

private enum class TaskFilter(val label: Int) { ACTIVE(R.string.active), WAITING(R.string.waiting), COMPLETED(R.string.completed) }

@Composable
private fun recurrenceLabel(recurrence: Recurrence): String = pluralStringResource(
    when (recurrence.unit) {
        RecurrenceUnit.DAY -> R.plurals.every_days
        RecurrenceUnit.WEEK -> R.plurals.every_weeks
        RecurrenceUnit.MONTH -> R.plurals.every_months
    }, recurrence.interval, recurrence.interval)
