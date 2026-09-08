package com.pierreteodoresco.taskomatic

import android.os.Bundle
import android.os.Parcel
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.ui.TaskViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeEditorStateTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun aLargeOriginalDoesNotTravelInsideTheActivityBundleOrGetTruncatedOnSave() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = (instrumentation.targetContext.applicationContext as TaskomaticApplication).store
        val added = store.add("Large note")
        val original = store.edit(added, added.title, "notes ".repeat(50_000), null)
        val draftNote = original.note + "Edited draft"
        try {
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].openEditor(original) }
                compose.waitUntil(15_000) { compose.onAllNodesWithTag("edit-save").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("edit-note").performScrollTo().performTextReplacement(draftNote)
                activity.onActivity { current ->
                    val state = Bundle()
                    instrumentation.callActivityOnSaveInstanceState(current, state)
                    val parcel = Parcel.obtain()
                    try {
                        parcel.writeBundle(state)
                        android.util.Log.i("TaskomaticVerification", "Saved editor state: ${parcel.dataSize()} bytes")
                        assertTrue("Saved state is ${parcel.dataSize()} bytes", parcel.dataSize() < 256 * 1024)
                    } finally { parcel.recycle() }
                }
                activity.recreate()
                compose.waitUntil(15_000) { compose.onAllNodesWithTag("edit-title").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("edit-title").performTextReplacement("Renamed large note")
                compose.onNodeWithTag("edit-save").performClick()
                compose.waitUntil(5_000) { store.tasks.value.single { it.id == original.id }.title == "Renamed large note" }
                assertEquals(draftNote, store.tasks.value.single { it.id == original.id }.note)
            }
        } finally { store.delete(original) }
    }
}
