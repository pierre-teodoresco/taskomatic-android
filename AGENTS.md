# Working on Taskomatic Android

Native Kotlin / Jetpack Compose application. The repository is independent of the sibling iOS checkout. Android stores data locally; automatic backup and cloud synchronization are outside the current product scope.

- `core` owns task lifecycle, recurrence, reminder planning and the portable backup format. Inject time and time zone; keep Android APIs out of this module.
- `app` adapts SQLite, preferences, notifications and Compose. Commit each mutation before scheduling reminders; surface persistence errors.
- Put user-facing text in matching English and French resources. Preserve the language of task titles and notes. Use semantic colors, accessible targets and layouts that support large fonts.

Read `docs/architecture.md` before changes to storage, backups, recurrence or notifications, and `docs/development.md` before build, emulator or device work. Read `docs/git-workflow.md` before commits, pushes, PRs or changes to GitHub policy.

The user delegated testing decisions and requested TDD with independent review. Test the public domain, repository and backup interfaces plus real app flows. Use one failing behavior and its implementation per cycle, with independently specified calendar examples. Before committing, request an independent review, address findings, rerun affected checks and obtain follow-up review. Keep keys, local SDK paths, generated builds and personal task data out of Git.
