package com.pierreteodoresco.taskomatic.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierreteodoresco.taskomatic.R
import com.pierreteodoresco.taskomatic.notifications.ReminderNotifications
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun ReminderSettingsSection(model: TaskViewModel) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val failed by model.schedulingFailed.collectAsStateWithLifecycle()
    val next by model.nextReminder.collectAsStateWithLifecycle()
    val reminders = preferences.reminders
    val notifications = remember(context) { ReminderNotifications(context) }
    var allowed by remember { mutableStateOf(notifications.canPost()) }
    var pendingTest by rememberSaveable { mutableStateOf(false) }
    var testFailed by rememberSaveable { mutableStateOf(false) }
    val showTest = { testFailed = !runCatching { notifications.showTest(preferences.language) }.getOrDefault(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = notifications.canPost()
        if (pendingTest) { pendingTest = false; showTest() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = notifications.canPost() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionCaption(stringResource(R.string.reminders))
    TaskSurface {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    val enabledLabel = stringResource(R.string.reminders_enabled)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(enabledLabel, Modifier.weight(1f))
        TaskSwitch(reminders.enabled, { enabled ->
            model.setRemindersEnabled(enabled)
            if (enabled && !allowed && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }, enabled = !busy, modifier = Modifier.testTag("reminders-enabled").semantics { contentDescription = enabledLabel })
    }
    if (reminders.enabled) {
        SettingsAction(title = stringResource(R.string.reminder_time, reminders.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))), icon = Icons.Rounded.Schedule, onClick = {
            TimePickerDialog(context, { _, hour, minute -> model.setReminderTime(LocalTime.of(hour, minute)) },
                reminders.time.hour, reminders.time.minute, DateFormat.is24HourFormat(context)).show()
        }, enabled = !busy, modifier = Modifier.testTag("reminder-time"))
        Text(stringResource(R.string.reminder_days), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                val selected = day in reminders.weekdays
                Box(Modifier.widthIn(min = 64.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .selectable(selected, enabled = !busy, role = Role.Checkbox, onClick = { model.toggleReminderDay(day) })
                    .testTag("weekday-${day.name.lowercase()}").semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) }
                    .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(day.getDisplayName(TextStyle.SHORT, locale).uppercase(locale), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(stringResource(R.string.reminder_timing_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!allowed) {
            Text(stringResource(R.string.notifications_blocked), color = MaterialTheme.colorScheme.error)
            OutlinedButton({ context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) {
                Text(stringResource(R.string.notification_settings))
            }
        }
        if (failed) TextButton(model::refresh) { Text(stringResource(R.string.reminder_scheduling_failed)) }
        else if (allowed) Text(next?.let {
            stringResource(R.string.next_reminder, it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale)))
        } ?: stringResource(R.string.no_reminder), style = MaterialTheme.typography.bodySmall)
    }
    SettingsAction(title = stringResource(R.string.test_notification), icon = Icons.Rounded.Notifications, onClick = {
        if (!allowed && Build.VERSION.SDK_INT >= 33) {
            pendingTest = true
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else showTest()
    }, modifier = Modifier.testTag("test-notification"))
    }
    }
    }
    if (testFailed) AlertDialog(onDismissRequest = { testFailed = false },
        title = { DialogSystemBarAppearance(); Text(stringResource(R.string.test_notification_failed)) },
        text = { Text(stringResource(R.string.notifications_blocked)) },
        confirmButton = { TextButton({
            testFailed = false
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text(stringResource(R.string.notification_settings)) } },
        dismissButton = { TextButton({ testFailed = false }) { Text(stringResource(R.string.cancel)) } })
}
