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
`INFO` | Startup and shutdown, and each command that saved, with the number of students saved
`FINE` | Command receipt, parse/execution rejection, the parsed command type, unchanged commands that skipped saving, completed publication, UI completion/rejection, atomic-write steps, `schedule`/`unschedule` outcomes and the timetable day shown

Command traces identify the command class after parsing succeeds. They do not echo raw input,
arguments, user-facing feedback or validation exception messages. A parse rejection leaves the live
model unchanged; an execution rejection discards the staged copy. The publication message is emitted
only after `Model.publish` succeeds. Routine invalid input uses `FINE`; storage failures use `WARNING`.
This applies to contact commands (`add`, `edit`, `guardian`, `delete`) and other commands using the same pipeline.

Logs can still contain UUIDs, file paths and storage exception causes, and older log files may contain
raw commands. Treat them as personal data. `LogicManagerLoggingTest` covers saves, save failures,
contact command completion, validation rejection and omission of contact values from command traces.
