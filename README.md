# AdaptiveStudyTracker

## Task Creation / Editing Flow

This project now includes a working task creation and editing flow in `Schedule`:

- Add a task from the floating action button.
- Edit an existing task by tapping it in the list.
- Mark tasks as done directly in the list; completed tasks are removed automatically.
- Delete active tasks directly from the list.
- Fields: title, category (`study`, `life`, `sleep`, `exercise`), due time, daily reminder toggle, importance.
- Importance labels now clarify `1: least important` and `5: most important`.
- Validation is enforced before save.
- Data is persisted locally using `SharedPreferences` JSON storage.

## App Bars and History

- Main screen now uses a top app bar.
- Add/Edit task screen has an app bar menu:
  - `Task history` opens completed-task history.
  - `Delete task` removes the current task when editing.
- History screen supports deleting a specific completed task or deleting all completed tasks.

## Key Files

- `app/src/main/java/com/example/adaptivestudytracker/ScheduleFragment.java`
- `app/src/main/java/com/example/adaptivestudytracker/EditTaskActivity.java`
- `app/src/main/java/com/example/adaptivestudytracker/Task.java`
- `app/src/main/java/com/example/adaptivestudytracker/TaskStorage.java`
- `app/src/main/java/com/example/adaptivestudytracker/TaskFormValidator.java`
- `app/src/main/java/com/example/adaptivestudytracker/TaskAdapter.java`
- `app/src/main/res/layout/activity_edit_task.xml`
- `app/src/main/res/layout/item_task.xml`

## Quick Try

```zsh
cd /Users/andy0332hk/AndroidStudioProjects/AdaptiveStudyTracker
./gradlew test
./gradlew assembleDebug
```

