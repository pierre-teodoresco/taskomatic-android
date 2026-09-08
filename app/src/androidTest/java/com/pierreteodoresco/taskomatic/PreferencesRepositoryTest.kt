package com.pierreteodoresco.taskomatic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.data.*
import com.pierreteodoresco.taskomatic.core.ReminderSettings
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "preferences-test-${UUID.randomUUID()}"
    @After fun cleanup() { context.deleteDatabase(name) }

    @Test fun failedSaveDoesNotExposeUncommittedPreferences() {
        val initial = AppPreferences(Appearance.LIGHT, AppLanguage.ENGLISH)
        PreferencesRepository(context, name).use { repository ->
            repository.save(initial)
            context.openOrCreateDatabase(name, 0, null).use { database ->
                database.execSQL("""CREATE TRIGGER fail_preferences BEFORE INSERT ON preferences
                    WHEN NEW.language = 'FRENCH' BEGIN SELECT RAISE(ABORT, 'Simulated write failure'); END""")
            }
            assertThrows(android.database.sqlite.SQLiteException::class.java) { repository.save(initial.copy(language = AppLanguage.FRENCH)) }
            assertEquals(initial, repository.read())
        }
        PreferencesRepository(context, name).use { assertEquals(initial, it.read()) }
    }

    @Test fun languageAppearanceAndReminderChoicesSurviveReopening() {
        val choices = AppPreferences(Appearance.DARK, AppLanguage.FRENCH,
            ReminderSettings(true, LocalTime.of(7, 35), setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)))
        PreferencesRepository(context, name).use { repository ->
            assertFalse(repository.read().reminders.enabled)
            repository.save(choices)
        }
        PreferencesRepository(context, name).use { assertEquals(choices, it.read()) }
    }
}
