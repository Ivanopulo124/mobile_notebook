# Planner

**English** · [Русский](README.ru.md)

A paper-style weekly planner for Android. One screen is one week: swipe left or right to move between weeks, tap a line to write a task, tick it off when it is done. It works fully offline and can optionally sync through a file on your Google Drive.

<p>
  <img src="docs/screenshots/en/week.png" width="200" alt="Week view with today highlighted">
  <img src="docs/screenshots/en/menu.png" width="200" alt="Task menu with color tags">
  <img src="docs/screenshots/en/dark.png" width="200" alt="Dark theme">
</p>
<p>
  <img src="docs/screenshots/en/tablet.png" width="420" alt="Tablet layout">
</p>

## Features

- **One week per screen.** Swipe to change weeks; the round calendar button jumps back to the current week. Today's day is highlighted.
- **Write on the lines.** Tap an empty line to add a task and press Enter to keep adding. Tap a task to edit it; clear its text to delete it.
- **Tick tasks off.** Done tasks are greyed out and struck through.
- **Other.** A list for tasks without a date, shown after Sunday.
- **Long-press menu.** Color highlight (five colors), move to the next day, move to any date, move to Other, delete with undo.
- **Phones and tablets.** Portrait layout with one column on phones and two on tablets.
- **Light and dark themes.** Follows the system or can be forced in Settings.
- **Optional Google Drive sync.** Tasks are kept in a hidden app folder on your Drive; the app cannot see your other files.
- **English and Russian**, picked from the system language.

## Building

Requirements: Android SDK with platform 37 (Android Gradle Plugin downloads it automatically when the SDK licenses are accepted) and JDK 17 or newer to run Gradle.

```bash
./gradlew :app:installDebug
```

The app targets Android 16 (API 36) and runs on Android 8.0 (API 26) and newer.

## Google Drive sync setup

Sync is optional; without it the app works entirely offline. Google identifies Android apps by package name plus signing-key fingerprint, so there is no client ID in the code, but each build signature needs an OAuth client:

1. In [Google Cloud Console](https://console.cloud.google.com/) create a project and enable the **Google Drive API**.
2. In **Google Auth Platform**, configure the consent screen: audience *External*, keep it in *Testing* and add your account as a test user, and add the `https://www.googleapis.com/auth/drive.appdata` scope.
3. Create an **Android** OAuth client with package name `com.ivanopulo.planner` and the SHA-1 of the signing key. For the debug key:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
   ```
   A release build needs a second Android client with the release key's SHA-1.
4. In the app open **Settings → Sync → Sign in with Google**.

### How sync works

- All tasks, including deletion markers, are stored in one `planner.json` in the Drive `appDataFolder`.
- Each task carries a last-changed timestamp; when devices disagree the newer version wins, and a sync never overwrites a task that was edited locally while the sync was running.
- Sync runs when the app comes to the foreground, five seconds after the last edit, every 12 hours in the background and on demand, and only with a network connection.
- Signing out keeps all tasks on the device and revokes the app's Drive access.

## Architecture

Single-activity Kotlin app with Jetpack Compose and Material 3, no DI framework.

| Layer | Contents |
| --- | --- |
| `domain` | `Task`, `TaskColor`, week arithmetic (`Weeks`) |
| `data` | Room database (`TaskEntity`, `TaskDao`), `TaskRepository`, DataStore settings |
| `sync` | `SyncEngine` and `TaskMerge` (pure logic), `DriveRemoteStore` (Drive v3 REST), `GoogleAuth` (`AuthorizationClient`), `SyncManager` and `SyncWorker` (WorkManager) |
| `ui` | Week pager, day blocks and inline editing, task menu, settings |

Weeks are pages of a `HorizontalPager` counted from the week the app was started in. Deleted tasks are soft-deleted so the deletion can reach other devices through sync.

```
app/src/main/java/com/ivanopulo/planner/
├── domain/
├── data/
├── sync/
└── ui/
    ├── components/
    ├── settings/
    ├── theme/
    └── week/
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Unit tests cover week arithmetic, the task repository (against an in-memory DAO), the merge rules, the sync file format and full sync cycles against a fake remote.
