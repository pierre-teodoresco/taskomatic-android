package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.foundation.text.BasicTextField
import com.pierreteodoresco.taskomatic.core.TaskItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
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
    val draftFailed by model.draftFailed.collectAsStateWithLifecycle()
    val refreshFailed by model.refreshFailed.collectAsStateWithLifecycle()
    val editing by model.editing.collectAsStateWithLifecycle()
    val creating by model.creating.collectAsStateWithLifecycle()
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
    var waitingExpanded by rememberSaveable { mutableStateOf(true) }
    val focus = LocalFocusManager.current
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
    val active = tasks.filter { it.isActive(now, zone) }
    val waiting = tasks.filter { it.recurrence != null && !it.isActive(now, zone) }.sortedBy { it.nextActivation(zone) }
    val completed = tasks.filter { it.recurrence == null && it.completedAt != null }.sortedByDescending { it.completedAt }
    val visible = if (filter == TaskFilter.ACTIVE) active else completed
    val add = { model.add(); focus.clearFocus() }
    val quickLabel = stringResource(R.string.task_title)

    if (settingsOpen) SettingsScreen(model) { settingsOpen = false }
    else Scaffold(bottomBar = {
        // Reserve space for Undo, as on iOS, so it cannot cover the final task's edit action.
        Column(Modifier.background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        SnackbarHost(snackbar, Modifier.padding(horizontal = 24.dp)) { data ->
            Snackbar(data, shape = RoundedCornerShape(16.dp), containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface, actionColor = MaterialTheme.colorScheme.primary)
        }
        if (filter == TaskFilter.ACTIVE) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    .fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { focus.clearFocus(); model.openNewTask() }, enabled = !busy, modifier = Modifier.testTag("new-task")) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.new_task), tint = MaterialTheme.colorScheme.primary)
                    }
                    BasicTextField(title, model::setQuickTitle, enabled = !busy, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("quick-title").semantics { contentDescription = quickLabel },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                        decorationBox = { input -> Box(contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) Text(stringResource(R.string.task_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            input()
                        } })
                    if (title.isNotBlank()) FilledIconButton(onClick = add, enabled = !busy,
                        modifier = Modifier.testTag("quick-add"), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.add_task))
                    }
                }
            }
        }
        }
    }) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).testTag("home-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    BrandMark()
                    Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    RoundIconButton(Icons.Rounded.Tune, stringResource(R.string.settings), Modifier.testTag("settings-open")) {
                        focus.clearFocus(); settingsOpen = true
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.home_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(9.dp))
                Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(26.dp))
                Segments(listOf(Segment(stringResource(R.string.active), "filter-active", active.size),
                    Segment(stringResource(R.string.completed), "filter-completed")), filter.ordinal) { filter = TaskFilter.entries[it]; focus.clearFocus() }
                Spacer(Modifier.height(26.dp))
                if (refreshFailed) TextButton(model::refresh) { Text(stringResource(R.string.saved_refresh_failed)) }
            }
            if (visible.isEmpty()) item {
                EmptyTasks(archived = filter == TaskFilter.COMPLETED, first = tasks.isEmpty())
            } else items(visible.size, key = { visible[it].id }) { index ->
                TaskListRow(visible[index], filter == TaskFilter.COMPLETED, false, index == 0, index == visible.lastIndex,
                    busy, { if (filter == TaskFilter.ACTIVE) model.complete(visible[index]) else model.restore(visible[index]) },
                    { focus.clearFocus(); model.openEditor(visible[index]) })
            }
            if (filter == TaskFilter.ACTIVE && waiting.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = { waitingExpanded = !waitingExpanded }, modifier = Modifier.fillMaxWidth().testTag("waiting-section")) {
                        SectionCaption(stringResource(R.string.waiting), Modifier.weight(1f))
                        Text(waiting.size.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Icon(if (waitingExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (waitingExpanded) items(waiting.size, key = { waiting[it].id }) { index ->
                    TaskListRow(waiting[index], false, true, index == 0, index == waiting.lastIndex, busy,
                        { model.restore(waiting[index]) }, { focus.clearFocus(); model.openEditor(waiting[index]) })
                }
            }
        }
    }
    editing?.let { item -> TaskEditor(item, busy, model.editorStateKey, model.textStateStorage, model::reportDraftFailure,
        model::closeEditor, { model.delete(item) }, isNew = creating) { title, note, recurrence -> model.edit(item, title, note, recurrence) } }
    if (error || backgroundFailed || draftFailed) AlertDialog(onDismissRequest = model::dismissError,
        title = { DialogSystemBarAppearance(); Text(stringResource(if (draftFailed) R.string.draft_state_failed else R.string.storage_error_title)) },
        text = { Text(stringResource(if (draftFailed) R.string.draft_state_failed_body else R.string.storage_error_body)) },
        confirmButton = { TextButton(model::dismissError) { Text(stringResource(R.string.ok)) } })
}

private enum class TaskFilter { ACTIVE, COMPLETED }

@Composable
internal fun recurrenceLabel(recurrence: Recurrence): String = pluralStringResource(
    when (recurrence.unit) {
        RecurrenceUnit.DAY -> R.plurals.every_days
        RecurrenceUnit.WEEK -> R.plurals.every_weeks
        RecurrenceUnit.MONTH -> R.plurals.every_months
    }, recurrence.interval, recurrence.interval)

@Composable
private fun EmptyTasks(archived: Boolean, first: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(82.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
            Icon(if (archived) Icons.Rounded.Inventory2 else if (first) Icons.AutoMirrored.Rounded.PlaylistAdd else Icons.Rounded.Check,
                null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(if (archived) R.string.empty_archive_title else if (first) R.string.empty_first_title else R.string.empty_done_title),
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))
        Text(stringResource(if (archived) R.string.empty_archive_body else if (first) R.string.empty_first_body else R.string.empty_done_body),
            Modifier.widthIn(max = 290.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun TaskListRow(task: TaskItem, completed: Boolean, waiting: Boolean, first: Boolean, last: Boolean,
    busy: Boolean, toggle: () -> Unit, edit: () -> Unit) {
    val shape = RoundedCornerShape(topStart = if (first) 20.dp else 0.dp, topEnd = if (first) 20.dp else 0.dp,
        bottomStart = if (last) 20.dp else 0.dp, bottomEnd = if (last) 20.dp else 0.dp)
    Surface(onClick = edit, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("task-row"), shape = shape,
        color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(Modifier.padding(start = 7.dp, end = 20.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.Top) {
                IconButton(toggle, enabled = !busy) {
                    Icon(if (completed) Icons.Rounded.CheckCircle else if (waiting) Icons.Rounded.Restore else Icons.Rounded.RadioButtonUnchecked,
                        stringResource(if (completed || waiting) R.string.restore_task else R.string.complete_task, task.title),
                        tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f).padding(vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(task.title, maxLines = 3, style = MaterialTheme.typography.titleMedium,
                        color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (completed) TextDecoration.LineThrough else null)
                    if (task.note.isNotEmpty()) Text(task.note, maxLines = 2, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    task.recurrence?.let {
                        val locale = LocalConfiguration.current.locales[0]
                        val next = task.nextActivation(ZoneId.systemDefault())
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            val color = if (waiting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                            Icon(Icons.Rounded.Repeat, null, Modifier.size(14.dp), tint = color)
                            Text(if (waiting && next != null) stringResource(R.string.next_cycle, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                .withLocale(locale).format(next.atZone(ZoneId.systemDefault()))) else recurrenceLabel(it),
                                style = MaterialTheme.typography.labelMedium, color = color)
                        }
                    }
                }
            }
            if (!last) SubtleDivider(Modifier.padding(start = 55.dp))
        }
    }
}
