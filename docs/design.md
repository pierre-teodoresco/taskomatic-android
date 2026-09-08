# Taskomatic visual design

The native iOS application is the visual reference for Taskomatic. Android implements that identity in Jetpack Compose, using shared local components rather than the stock Material screen composition. SwiftUI and Compose remain independent implementations.

## Reference and tokens

The reference is the sibling iOS checkout's `App/Design/Theme.swift`, asset colors and feature views. `ui/TaskomaticTheme.kt` mirrors the light/dark semantic palette; `ui/TaskomaticComponents.kt` owns surfaces, segmented choices, selection rows, action rows, headers, switches and the brand mark.

| Role | Light | Dark |
| --- | --- | --- |
| Canvas | `#F7F8FC` | `#101117` |
| Surface | `#FFFFFF` | `#1A1B24` |
| Raised | `#EFF1F7` | `#242631` |
| Ink | `#202330` | `#F2F3FA` |
| Secondary | `#686E82` | `#A2A8BC` |
| Accent | `#5757DC` | `#9290FF` |
| Soft accent | `#ECEBFF` | `#2B2849` |
| Divider | `#E5E7EF` | `#323442` |

Page margins are 24 dp, surfaces have 20 dp corners, and primary buttons have 16 dp corners. Android retains its native font with a hierarchy modeled on iOS: 34 sp page titles, 22 sp editor titles, 17 sp task titles/body, 15 sp secondary text and 13 sp captions. Touch targets remain at least 48 dp. At a font scale of 1.5 or greater, segmented choices stack and screen headers separate the title from navigation/actions.

## Screen composition

- Home has the brand header, page title/subtitle, a two-way active/archive selector and grouped task rows. Waiting recurring tasks form a collapsible section below active tasks, ordered by their next activation. Completed tasks are ordered by completion time.
- Quick entry stays at the bottom. The plus opens a new unsaved task with the current quick title; the submit arrow or keyboard action creates a simple task. Undo reserves its own space above quick entry so it cannot obstruct a task row.
- The editor groups title and note in one surface, with a separate recurrence row. The recurrence screen offers one-off, daily, weekly, fortnightly and monthly choices, plus custom intervals from 1 to 99. Custom input is applied only with Done. Returning from that screen leaves the editor open.
- Settings groups reminders, appearance/language, manual backups and local storage information, followed by the brand/version footer. Import preview uses a full-screen summary and a bottom confirmation action.

Native Android back navigation, keyboard, permission prompts, time picker and document picker are retained. The visual redesign does not add cloud synchronization or automatic backup. Labels remain localized in English and French; populated text inputs retain persistent accessible names.

## Visual verification

Compare the same synthetic tasks across platforms in light/dark appearance and both languages. Check 200% text, keyboard visibility, long notes, recurrence selection, discard/delete confirmations and the import preview. Keep screenshots and emulator data outside Git, as required by the repository rules.
