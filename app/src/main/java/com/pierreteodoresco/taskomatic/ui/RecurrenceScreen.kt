package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pierreteodoresco.taskomatic.R
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.core.RecurrenceUnit

private val presets = listOf(
    Triple(R.string.repeat_never, "repeat-never", null),
    Triple(R.string.repeat_daily, "repeat-daily", Recurrence(1, RecurrenceUnit.DAY)),
    Triple(R.string.repeat_weekly, "repeat-weekly", Recurrence(1, RecurrenceUnit.WEEK)),
    Triple(R.string.repeat_fortnightly, "repeat-fortnightly", Recurrence(2, RecurrenceUnit.WEEK)),
    Triple(R.string.repeat_monthly, "repeat-monthly", Recurrence(1, RecurrenceUnit.MONTH)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceScreen(recurrence: Recurrence?, onBack: () -> Unit, onSelect: (Recurrence?) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(recurrence != null && presets.none { it.third == recurrence }) }
    var interval by rememberSaveable { mutableStateOf((recurrence?.interval ?: 1).toString()) }
    var unit by rememberSaveable { mutableStateOf(recurrence?.unit ?: RecurrenceUnit.WEEK) }
    val valid = interval.toIntOrNull()?.takeIf { it in 1..99 }
    Scaffold(topBar = {
        ScreenHeader(title = stringResource(R.string.repeat_title), navigation = { IconButton(onBack, Modifier.testTag("recurrence-back")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TaskSurface {
                presets.forEachIndexed { index, (label, tag, value) ->
                    SelectionRow(stringResource(label), recurrence == value, Modifier.testTag(tag)) { onSelect(value) }
                    if (index != presets.lastIndex) SubtleDivider(Modifier.padding(start = 18.dp))
                }
            }
            TaskSurface {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth().toggleable(custom, role = Role.Switch, onValueChange = { custom = it })
                        .testTag("recurrence-enabled"), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.repeat_custom), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TaskSwitch(custom, null)
                    }
                    if (custom) {
                        OutlinedTextField(interval, { value -> if (value.length <= 2 && value.all(Char::isDigit)) interval = value },
                            label = { Text(stringResource(R.string.interval)) }, singleLine = true, isError = valid == null,
                            supportingText = { Text(stringResource(R.string.interval_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("recurrence-interval"), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        Segments(RecurrenceUnit.entries.map { option -> Segment(stringResource(when (option) {
                            RecurrenceUnit.DAY -> R.string.days
                            RecurrenceUnit.WEEK -> R.string.weeks
                            RecurrenceUnit.MONTH -> R.string.months
                        }), "recurrence-${option.name.lowercase()}") }, unit.ordinal) { unit = RecurrenceUnit.entries[it] }
                        PrimaryButton(stringResource(R.string.done), Modifier.testTag("recurrence-done"), valid != null) {
                            valid?.let { onSelect(Recurrence(it, unit)) }
                        }
                    }
                }
            }
            Text(stringResource(R.string.recurrence_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
