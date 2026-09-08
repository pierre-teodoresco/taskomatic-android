package com.pierreteodoresco.taskomatic.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pierreteodoresco.taskomatic.core.ReminderSettings
import java.time.DayOfWeek
import java.time.LocalTime

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val tag: String) { SYSTEM(""), ENGLISH("en"), FRENCH("fr") }
data class AppPreferences(
    val appearance: Appearance = Appearance.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val reminders: ReminderSettings = ReminderSettings(),
)

class PreferencesRepository(context: Context, name: String = "taskomatic-preferences.db") : AutoCloseable {
    private val helper = object : SQLiteOpenHelper(context.applicationContext, name, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE preferences (
                id INTEGER PRIMARY KEY CHECK (id = 1), appearance TEXT NOT NULL, language TEXT NOT NULL,
                reminders_enabled INTEGER NOT NULL, reminder_time TEXT NOT NULL, reminder_weekdays TEXT NOT NULL
            )""")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Missing preferences migration from $oldVersion to $newVersion")
        }
    }

    fun read(): AppPreferences = helper.readableDatabase.query("preferences", null, "id = 1", null, null, null, null).use { cursor ->
        if (!cursor.moveToFirst()) return AppPreferences()
        fun string(key: String) = cursor.getString(cursor.getColumnIndexOrThrow(key))
        AppPreferences(Appearance.valueOf(string("appearance")), AppLanguage.valueOf(string("language")),
            ReminderSettings(cursor.getInt(cursor.getColumnIndexOrThrow("reminders_enabled")) != 0,
                LocalTime.parse(string("reminder_time")), string("reminder_weekdays").split(',')
                    .filter { it.isNotEmpty() }.map(DayOfWeek::valueOf).toSet()))
    }

    fun save(value: AppPreferences) {
        val values = ContentValues().apply {
            put("id", 1)
            put("appearance", value.appearance.name)
            put("language", value.language.name)
            put("reminders_enabled", if (value.reminders.enabled) 1 else 0)
            put("reminder_time", value.reminders.time.toString())
            put("reminder_weekdays", value.reminders.weekdays.sortedBy { it.value }.joinToString(",") { it.name })
        }
        helper.writableDatabase.replaceOrThrow("preferences", null, values)
    }

    override fun close() = helper.close()
}
