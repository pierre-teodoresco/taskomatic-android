package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaskSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(content = content)
    }
}

@Composable
fun SectionCaption(title: String, modifier: Modifier = Modifier) {
    Text(title.uppercase(), modifier.semantics { heading() }, style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.3.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun BrandMark(size: Int = 28) {
    Box(Modifier.size(size.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape((size * .29f).dp)),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Check, null, Modifier.size((size * .65f).dp), tint = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        IconButton(onClick, modifier.size(48.dp), enabled = enabled) {
            Icon(icon, label, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

data class Segment(val title: String, val tag: String, val count: Int? = null)

@Composable
fun Segments(options: List<Segment>, selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    val vertical = LocalDensity.current.fontScale >= 1.5f
    val container = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(4.dp).selectableGroup()
    val content: @Composable (Int, Segment, Modifier) -> Unit = { index, option, modifier ->
        Row(modifier.clip(RoundedCornerShape(11.dp))
            .background(if (index == selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
            .selectable(index == selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(index) })
            .testTag(option.tag).heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(option.title, color = if (index == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, fill = false))
            option.count?.let {
                Spacer(Modifier.width(7.dp))
                Text(it.toString(), Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 7.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (vertical) Column(container, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, option -> content(index, option, Modifier.fillMaxWidth()) }
    } else Row(container, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, option -> content(index, option, Modifier.weight(1f)) }
    }
}

@Composable
fun SelectionRow(title: String, selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().background(if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
        .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        .heightIn(min = 54.dp).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsAction(title: String, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .heightIn(min = 52.dp).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .38f)
        Icon(icon, null, Modifier.size(22.dp), tint = color)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = color)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(18.dp), tint = color)
    }
}

@Composable
fun PrimaryButton(title: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SubtleDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun TaskSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(checked, onChange, modifier, enabled = enabled, colors = SwitchDefaults.colors(
        checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
        uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f),
        uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent))
}

/** Keep navigation and confirmation reachable without squeezing a large title between them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String, navigation: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    if (LocalDensity.current.fontScale >= 1.5f) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                navigation()
                Spacer(Modifier.weight(1f))
                actions()
            }
            Text(title, Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleMedium)
        }
    } else CenterAlignedTopAppBar(title = {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }, navigationIcon = navigation, actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
}
