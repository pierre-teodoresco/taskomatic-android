package com.pierreteodoresco.taskomatic

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskAppTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun createRecurringTaskWithDetailsBeforeSaving() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            val title = "Détails ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            compose.onNodeWithTag("quick-title").assertContentDescriptionEquals(context.getString(R.string.task_title))
            compose.onNodeWithTag("new-task").performClick()
            compose.onNodeWithTag("edit-title").assertTextContains(title)
            compose.onNodeWithTag("edit-note").performTextInput("Un chapitre chaque semaine")
            compose.onNodeWithTag("edit-title").assertContentDescriptionEquals(context.getString(R.string.task_title))
            compose.onNodeWithTag("edit-note").assertContentDescriptionEquals(context.getString(R.string.note))
            compose.onNodeWithTag("recurrence-open").performScrollTo().performClick()
            compose.onNodeWithTag("repeat-weekly").performClick()
            activity.recreate()
            compose.onNodeWithTag("edit-note").assertTextContains("Un chapitre chaque semaine")
            compose.onNodeWithTag("edit-save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("edit-save").fetchSemanticsNodes().isEmpty() }
            activity.recreate()
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("edit-note").assertTextContains("Un chapitre chaque semaine")
            compose.onNodeWithTag("recurrence-open").performScrollTo().performClick()
            compose.onNodeWithTag("repeat-weekly").assertIsSelected()
        }
    }

    @Test fun rotatingDuringASlowAddClearsTheCurrentDraftExactlyOnce() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val warmup = "Warmup ${UUID.randomUUID()}"
            compose.onNodeWithTag("quick-title").performTextInput(warmup)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(context.getString(R.string.complete_task, warmup)).fetchSemanticsNodes().isNotEmpty() }
            val title = "Lent ${UUID.randomUUID().toString().take(6)}"
            context.openOrCreateDatabase("taskomatic.db", 0, null).use { database ->
                database.beginTransaction()
                try {
                    compose.onNodeWithTag("quick-title").performTextInput(title)
                    compose.onNodeWithTag("quick-add").performClick()
                    compose.onNodeWithTag("quick-title").assertIsNotEnabled()
                    activity.recreate()
                    compose.onNodeWithTag("quick-title").assertIsNotEnabled()
                } finally { database.endTransaction() }
            }
            compose.waitUntil(5_000) {
                compose.onNodeWithTag("quick-title").fetchSemanticsNode().config[SemanticsProperties.EditableText].text.isEmpty()
            }
            compose.onAllNodesWithContentDescription(context.getString(R.string.complete_task, title)).assertCountEquals(1)
        }
    }

    @Test fun failedSaveKeepsTheEditorDraftAndAllowsRetry() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val title = "Échec ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("edit-note").performTextInput("Brouillon important")
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.openOrCreateDatabase("taskomatic.db", 0, null).use { database ->
                database.execSQL("""CREATE TRIGGER fail_edit BEFORE UPDATE ON tasks
                    BEGIN SELECT RAISE(ABORT, 'Simulated storage failure'); END""")
                try {
                    compose.onNodeWithTag("edit-save").performClick()
                    compose.onNodeWithText(context.getString(R.string.storage_error_title)).assertIsDisplayed()
                    compose.onNodeWithText(context.getString(R.string.ok)).performClick()
                    compose.onNodeWithTag("edit-note").assertTextContains("Brouillon important")
                } finally { database.execSQL("DROP TRIGGER fail_edit") }
            }
            compose.onNodeWithTag("edit-save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("edit-save").fetchSemanticsNodes().isEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("edit-note").assertTextContains("Brouillon important")
        }
    }

    @Test fun closingAnEditedTaskRequiresAnExplicitDiscard() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val title = "Brouillon ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("edit-note").performTextInput("À conserver")
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            compose.onNodeWithContentDescription(context.getString(R.string.cancel)).performClick()
            compose.onNodeWithTag("discard-cancel").performClick()
            compose.onNodeWithTag("edit-note").assertTextContains("À conserver")
            compose.onNodeWithContentDescription(context.getString(R.string.cancel)).performClick()
            compose.onNodeWithTag("discard-confirm").performClick()
            compose.onNodeWithTag("edit-save").assertDoesNotExist()
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithText("À conserver").assertDoesNotExist()
        }
    }

    @Test fun deletionRequiresConfirmationAndSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            val title = "Supprimer ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("edit-delete").performScrollTo().performClick()
            compose.onNodeWithTag("delete-cancel").performClick()
            compose.onNodeWithTag("edit-title").performScrollTo().assertTextContains(title)
            compose.onNodeWithTag("edit-delete").performScrollTo().performClick()
            compose.onNodeWithTag("delete-confirm").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("edit-save").fetchSemanticsNodes().isEmpty() }
            activity.recreate()
            compose.onNode(hasTestTag("task-row") and hasText(title)).assertDoesNotExist()
        }
    }

    @Test fun undoCompletionReturnsTheTaskToTheActiveList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val title = "Annuler ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            compose.onNodeWithContentDescription(context.getString(R.string.complete_task, title)).performClick()
            compose.onNodeWithText(context.getString(R.string.undo)).performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).assertIsDisplayed()
        }
    }

    @Test fun recurringTaskWaitsForItsNextCycleAfterCompletion() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            val title = "Arroser ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("recurrence-open").performScrollTo().performClick()
            compose.onNodeWithTag("recurrence-enabled").performScrollTo().performClick()
            compose.onNodeWithTag("recurrence-interval").performScrollTo().performTextReplacement("3")
            compose.onNodeWithTag("recurrence-week").performScrollTo().performClick()
            compose.onNodeWithTag("recurrence-done").performScrollTo().performClick()
            compose.onNodeWithTag("edit-save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("edit-save").fetchSemanticsNodes().isEmpty() }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            compose.onNodeWithContentDescription(context.getString(R.string.complete_task, title)).performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(context.getString(R.string.complete_task, title)).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("waiting-section"))
            compose.onNodeWithTag("waiting-section").assertIsDisplayed()
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("task-row") and hasText(title))
            compose.onNode(hasTestTag("task-row") and hasText(title)).assertIsDisplayed()
            activity.recreate()
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("task-row") and hasText(title))
            compose.onNode(hasTestTag("task-row") and hasText(title)).performScrollTo().performClick()
            compose.onNodeWithTag("recurrence-open").performScrollTo().performClick()
            compose.onNodeWithTag("recurrence-interval").performScrollTo().assertTextContains("3")
            compose.onNodeWithTag("recurrence-week").performScrollTo().assertIsSelected()
        }
    }

    @Test fun editTitleAndNoteAndKeepTheDraftDuringRotation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            val title = "Lire ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasTestTag("task-row") and hasText(title)).performClick()
            compose.onNodeWithTag("edit-title").performTextReplacement("$title demain")
            compose.onNodeWithTag("edit-note").performTextInput("Chapitre deux — en français")
            activity.recreate()
            compose.onNodeWithTag("edit-note").assertTextContains("Chapitre deux — en français")
            compose.onNodeWithTag("edit-save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("edit-save").fetchSemanticsNodes().isEmpty() }
            activity.recreate()
            compose.onNodeWithText("$title demain").performClick()
            compose.onNodeWithTag("edit-note").assertTextContains("Chapitre deux — en français")
        }
    }

    @Test fun completeAndRestoreTaskFromTheArchive() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val title = "Courses ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            compose.onNodeWithContentDescription(context.getString(R.string.complete_task, title)).performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("filter-completed").performClick()
            compose.onNode(hasTestTag("task-row") and hasText(title)).assertIsDisplayed()
            compose.onNodeWithContentDescription(context.getString(R.string.restore_task, title)).performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("filter-active").performClick()
            compose.onNode(hasTestTag("task-row") and hasText(title)).assertIsDisplayed()
        }
    }

    @Test fun createTaskAndKeepItAfterActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            val title = "Café ${UUID.randomUUID().toString().take(6)}"
            compose.onNodeWithTag("quick-title").performTextInput(title)
            compose.onNodeWithTag("quick-add").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("task-row") and hasText(title)).fetchSemanticsNodes().isNotEmpty() }
            activity.recreate()
            compose.onNode(hasTestTag("task-row") and hasText(title)).assertIsDisplayed()
        }
    }
}
