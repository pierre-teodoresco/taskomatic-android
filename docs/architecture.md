# Android architecture

Taskomatic has no deadlines: a task stays active until explicitly completed. Simple completed tasks are archived; recurring tasks return at the start of the local day N calendar days, weeks or months after completion. Months clamp to the last valid day and missed cycles do not accumulate.

The pure Kotlin `core` module receives an `Instant` and `ZoneId`. UUID cycle tokens protect later task cycles from old notification actions. Tests use fixed dates, including Paris daylight saving and Santiago's midnight transition.

The Android adapter persists tasks in a private SQLite database. Mutations read fresh state and only replace their owned fields. Recurrence edits preserve active state while respecting completion after the editor opened. Restores/imports rotate cycle tokens. Saves precede reminder scheduling; errors are reported without discarding the user's editor draft.

Reminders use the system alarm and notification APIs, with local weekday/time settings. Android may delay delivery for battery management. Reboot, clock/timezone changes and app startup renew scheduling. A notification action checks the current cycle before completing a task. No internet permission, account, cloud integration or automatic system backup is part of the application.

Manual JSON export/import uses the iOS version-1 format. Validate the whole file before applying an atomic, add-only import; preserve existing IDs and regenerate imported cycle tokens. Documents are limited to 10 MiB and 10,000 tasks. User-chosen file providers may offer remote locations; Taskomatic itself implements no cloud save.
