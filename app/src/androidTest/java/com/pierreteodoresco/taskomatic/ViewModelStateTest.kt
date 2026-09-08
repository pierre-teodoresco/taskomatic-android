package com.pierreteodoresco.taskomatic

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.ui.TaskViewModel
import com.pierreteodoresco.taskomatic.core.TaskItem
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelStateTest {
    @Test fun theOriginalEditorSnapshotSurvivesSimulatedProcessDeath() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as TaskomaticApplication
        val original = TaskItem(title = "Original", note = "Note d’origine", createdAt = Instant.parse("2026-09-08T08:00:00Z"))
        instrumentation.runOnMainSync {
            viewModelScenario { TaskViewModel(application, createSavedStateHandle()) }.use { scenario ->
                scenario.viewModel.openEditor(original)
                scenario.recreate()
                assertEquals(original, scenario.viewModel.editing.value)
                scenario.viewModel.closeEditor()
                val other = TaskItem(title = "Autre tâche", createdAt = original.createdAt)
                scenario.viewModel.openEditor(other)
                scenario.recreate()
                assertEquals(other, scenario.viewModel.editing.value)
            }
        }
    }

    @Test fun quickDraftSurvivesSimulatedProcessDeath() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as TaskomaticApplication
        instrumentation.runOnMainSync {
            viewModelScenario { TaskViewModel(application, createSavedStateHandle()) }.use { scenario ->
                scenario.viewModel.setQuickTitle("Acheter du café")
                scenario.recreate()
                assertEquals("Acheter du café", scenario.viewModel.quickTitle.value)
            }
        }
    }
}
