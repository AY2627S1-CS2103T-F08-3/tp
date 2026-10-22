---
  layout: default.md
  title: "Logging guide"
---

# Logging guide

* The project uses the `java.util.logging` package for logging.
* `LogsCenter` manages logging levels and destinations.
* A class obtains a `Logger` with `LogsCenter.getLogger(Class)`, which logs messages at the configured level.
* Log messages are written to the console and a `.log` file.
* The output logging level can be controlled by changing the `LOG_LEVEL` constant in the `LogsCenter` class.
* **When choosing a level for a log message**, follow the conventions given in [_[se-edu/guides] Java: Logging conventions_](https://se-education.org/guides/conventions/java/logging.html).

## What TutorConnect logs

Level | Events
--- | ---
`WARNING` | A save that failed (staged changes discarded, with the cause), a `list` reload that failed, and a temporary save file that could not be deleted
`INFO` | Startup and shutdown, each entered command, and each command that saved, with the number of students saved
`FINE` | The parsed command type, unchanged commands that skipped saving, atomic-write steps, `schedule`/`unschedule` outcomes and the timetable day shown

New outcome messages identify students by UUID, never by name, phone or address. The inherited
`[USER COMMAND]` record still contains the raw command text, so treat log files as personal data.
`LogicManagerLoggingTest` checks the save and save-failure records.
