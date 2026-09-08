package com.pierreteodoresco.taskomatic

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import androidx.lifecycle.viewmodel.testing.ViewModelScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.ui.TaskViewModel
import com.pierreteodoresco.taskomatic.core.TaskItem
import java.time.Instant
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelStateTest {
    @Test fun addingARecoveredQuickDraftRemovesItsPrivateRecoveryFiles() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as TaskomaticApplication
        val title = "Quick ${UUID.randomUUID()} " + "x".repeat(20_000)
        val directory = File(application.filesDir, "editor-state")
        val existingFiles = directory.listFiles().orEmpty().toSet()
        lateinit var scenario: ViewModelScenario<TaskViewModel>
        instrumentation.runOnMainSync {
            scenario = viewModelScenario { TaskViewModel(application, createSavedStateHandle()) }
            scenario.viewModel.setQuickTitle(title)
            scenario.recreate()
            assertEquals(title, scenario.viewModel.quickTitle.value)
        }
        try {
            val files = directory.listFiles()!!.filter { it !in existingFiles && it.name.endsWith("-draft-title") }
            assertTrue(files.isNotEmpty())
            instrumentation.runOnMainSync { scenario.viewModel.add() }
            withTimeout(5_000) { while (scenario.viewModel.quickTitle.value.isNotEmpty()) delay(10) }
            withTimeout(5_000) { while (files.any { it.exists() }) delay(10) }
            assertEquals(1, application.store.tasks.value.count { it.title == title })
        } finally {
            instrumentation.runOnMainSync { scenario.close() }
            application.store.tasks.value.filter { it.title == title }.forEach { application.store.delete(it) }
        }
    }

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
