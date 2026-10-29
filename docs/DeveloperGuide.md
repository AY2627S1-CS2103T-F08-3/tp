---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TutorConnect Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* TutorConnect is based on [AddressBook-Level3](https://se-education.org/addressbook-level3/) (AB3) by the [SE-EDU initiative](https://se-education.org). Its architecture, documentation structure and parts of its code and diagrams are adapted from AB3.
* Libraries: [JavaFX](https://openjfx.io/) (GUI), [Jackson](https://github.com/FasterXML/jackson) (JSON storage), [JUnit 5](https://junit.org/junit5/) (testing).
* Tools: [Gradle](https://gradle.org/) with the [Shadow plugin](https://gradleup.com/shadow/), [Checkstyle](https://checkstyle.org/), [JaCoCo](https://www.jacoco.org/), [PlantUML](https://plantuml.com/) and [MarkBind](https://markbind.org/).

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`TutorConnectLauncher`** (consisting of classes [`TutorConnectLauncher`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/TutorConnectLauncher.java) and [`TutorConnectApp`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/TutorConnectApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`. `Logic` runs every command on a staged copy of the model, saves the staged data, and only then publishes it, so a failed save leaves the live data and the screen unchanged.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts: `CommandBox`, `ResultDisplay`, `StudentListPanel` (with one `StudentCard` per row), `StudentDetailsPanel`, `WeeklySchedulePanel`, `CommandReferencePanel` and `StatusBarFooter`. `MainWindow` and the FXML-backed parts inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts. `StudentDetailsPanel`, `WeeklySchedulePanel` and `CommandReferencePanel` are code-built JavaFX `VBox` subclasses without FXML, so they do not extend `UiPart`.

TutorConnect is type-first, so the UI adds no mouse-only controls:

* `CommandBox` keeps a `CommandHistory` for <kbd>↑</kbd>/<kbd>↓</kbd>, completes command words with <kbd>Tab</kbd> and shows the typed command's format below the field. Both use `CommandGuide`, a read-only table of command words and formats. `CommandGuide` never parses or validates input, so `TutorConnectParser` remains the only authority; a command missing from the guide simply gets no hint. `CommandReferencePanel` renders the same table as the window's left rail.
* `MainWindow` adds window-wide key filters. <kbd>Shift</kbd>+<kbd>Tab</kbd> moves focus through a fixed list of panels (command bar, command rail when shown, student list, profile, timetable), and the panel holding focus gets the `panel-focused` style class through a listener on the scene's focus owner. Arrow and Page keys scroll the focused `ScrollPane` or, for the timetable, whose rows are not selectable, `WeeklySchedulePanel#scrollBy`; the student list keeps its own arrow-key selection. <kbd>Tab</kbd> or <kbd>Esc</kbd> in another panel, printable keys and <kbd>Ctrl/⌘</kbd>+<kbd>L</kbd> return focus to the command field, and <kbd>Ctrl/⌘</kbd>+<kbd>↑</kbd>/<kbd>↓</kbd> move the list selection through `StudentListPanel#moveSelection`. The filters are on the window, so they run before the command field's own <kbd>Tab</kbd> completion, which is why <kbd>Tab</kbd> is left to the field while it has focus. `MainWindow` also hides the rail and the count cards in narrow windows.
* Each profile line is a `DetailRow`, a `Label` whose text stays the plain `Key: value` line that tests and owners rely on, while its own skin draws the styled key, value and section caption. On `StudentCard`, the prefixed labels (`Level: ...`, `Weekly lesson: ...`) likewise keep their text but display only the value through a `Text` graphic.
* Styling lives in `TutorConnectTheme.css`, whose colours are defined once as looked-up colours on `.root`.

`MainWindow` wires three views of the same register. `StudentListPanel` shows `Logic#getFilteredStudentList()`, which `find`/`filter` narrow and whose order defines visible indices. `StudentDetailsPanel` shows the student whose UUID is `Logic#getSelectedStudentId()`. `WeeklySchedulePanel` shows `Logic#getWeeklySchedule()`, the separately sorted list of every scheduled student. After each command, `MainWindow` passes any `TimetableView` carried by the `CommandResult` to `WeeklySchedulePanel#showDay`, then refreshes selection and details.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.
* keeps the timetable day as UI-only state: `timetable` never reaches the model, storage or the displayed-list predicate.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `TutorConnectParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a student).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="100%" alt="Contact parsers and their validation helpers"/>

How contact parsing works:

* `TutorConnectParser` dispatches `add`, `guardian`, `delete` and `edit` to the corresponding concrete
  `Parser<T>`. Each returns its matching command; parsing does not save or mutate the model.
* `AddCommandParser` and `GuardianCommandParser` use `StudentParameters` to validate parameter structure
  while retaining raw values. `ParserUtil` creates validated contact values and arbitrary-size `Index` objects.
* `DeleteCommandParser` normalizes its single positional argument through `StudentText`, checks missing
  or multiple indices, then uses `ParserUtil.parseIndex`. Index syntax and row existence are separate checks:
  command execution resolves the index against the currently displayed list using `VisibleIndexResolver`.
* `EditCommandParser` checks raw controls before trimming, recognizes Unicode spaces, and rejects unknown
  ASCII-letter prefixes and extra positional text. It retains `ArgumentTokenizer` / `ArgumentMultimap`
  for optional fields and repeated tags. Repeated single-value prefixes precede index and field validation;
  a lone blank `t/` clears tags. Prefix constants and the other command parsers are omitted for readability.
* Once parsing succeeds, `LogicManager` executes the command on `Model.copyForCommand()`, saves changed
  data, then calls `Model.publish()`. A parse, execution or save error leaves the live model and view intact.
  Contact updates retain the student's UUID, insertion position and unrelated `StudentFields`, including
  unknown nested JSON. Deletion removes the entire target record. These are execution/storage guarantees,
  not responsibilities of the parser classes.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores the student register, i.e. all `Student` objects in insertion order, in a `UniqueStudentList` inside `StudentDirectory`.
* stores the students matched by the current `find`/`filter` predicate in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Student>` that the UI observes; its order defines the displayed indices that commands use.
* stores the selected student by UUID (`ModelManager#selectedStudentId`), so selection survives renumbering.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* provides `copyForCommand()` and `publish(Model)` so that `LogicManager` can run a command on a staged copy and only publish it after a successful save.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

Each `Student` has a stable `UUID`, the required `Name`, `Phone` and `Address`, and one immutable `StudentFields`
envelope for optional values. Each optional feature owns a `StudentField<T>` codec (for example `EducationLevel.FIELD`
or `WeeklySlotField.INSTANCE`) that validates, decodes and encodes its value under a stable JSON key. Updating one
field replaces only that key, so features never overwrite each other's data. `Student` still carries the legacy AB3
`Email` and `Tag` values so that older data files and `edit e/... t/...` keep working; TutorConnect does not
collect them in `add`. `WeeklySchedule` is a sorted read-only view of every scheduled student (by day, time, then
insertion order); it ignores the filtered list, so the timetable always shows the full register.


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-F08-3/tp/tree/master/src/main/java/tutorconnect/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both the student register and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonStudentDirectoryStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

Student records keep owner-defined optional values in `StudentFields`, a JSON-node envelope keyed by each
`StudentField` codec's stable key. `JsonAdaptedStudent` captures these values through Jackson's any-field mapping,
so adding `payments` does not require a new top-level JSON property and edits preserve fields owned by other features.
`PaymentHistory.FIELD` owns the `payments` key and represents one student's month-to-status map. Older records without
the key remain unset; malformed payment JSON stays in the envelope until a payment command reads it, which avoids
loading the directory as empty and accidentally overwriting the original file.

The `payment INDEX m/YYYY-MM s/pending|completed` command changes one recorded month. The companion
`payments m/YYYY-MM [s/pending|completed]` command lists recorded entries from the full register, omits unrecorded
students, and does not change selection. Both commands use the same typed history codec and storage key.

The payment mutation uses the same transaction boundary as other updates: the command edits a staged model, the
complete student directory is encoded and atomically saved, and only then does `LogicManager` publish the staged
state. A no-change result skips the save; a save error leaves the live model and UI untouched.

<puml src="diagrams/PaymentPersistenceSequenceDiagram.puml" width="900" alt="Monthly payment persistence transaction" />

### Common classes

Classes used by multiple components are in the `tutorconnect.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Adding a student

`TutorConnectParser` recognizes the case-sensitive command, then `StudentParameters` checks parameter
boundaries, unknown/unexpected input and required/duplicate parameters. It retains raw values so controls
cannot disappear during trimming. `AddCommandParser` validates `Name`, `Phone`, `Address` in that order and
constructs one complete `Student` (the existing aggregate now represents a student). The three-argument
constructor assigns a UUID and leaves all optional data unset. `StudentText` owns normalization; `Phone`
owns the reusable Singapore-number contract. `Student.isSameStudent` compares normalized name ignoring case
plus canonical phone; full content equality remains separate from UUID identity.

`LogicManager` executes on `Model.copyForCommand()`. `AddCommand` checks duplicates and shared phones against
the complete register, then stages the append, full-list predicate and new selected UUID. The logic saves
changed data before `Model.publish()` updates the observable model. `JsonStudentDirectoryStorage` serializes the
complete state first; `AtomicJsonFile` writes and forces a sibling temporary file and atomically replaces the
destination. There is no unsafe truncate-in-place fallback. An I/O failure reports the standard save error and
does not publish. Read-only or identical-data operations do not save.

The JSON `persons` array retains insertion order. Each record now stores `id`; older valid records missing IDs
receive deterministic migration UUIDs from normalized name and phone, persisted on the next successful save.
Missing/null optional properties load unset. Existing email/tags and owner-defined JSON properties survive
round trips. `StudentField<T>` lets each owner provide a validated type/codec; `StudentFields` is immutable and
defensively copied, and prevents optional keys from overwriting core fields. F01 does not define other owners'
commands, education/rate/slot types or their final serialization schemas.

After successful execution, `MainWindow` selects the model's UUID through `StudentListPanel`, which scrolls to
the new row. `StudentDetailsPanel` displays the complete record and em dashes for unset optional fields. Errors
leave the list and details untouched. The F02/F03 owner remains responsible for final index/list/delete policy;
see [the shared interface and owner handoff contract](F01Integration.md), whose owner acknowledgement is pending.

Verification:

* `./gradlew check coverage` runs validation, parser, command, persistence and end-to-end tests.
* `F01_UI_TESTS=true ./gradlew test --rerun-tasks` also runs the real JavaFX command-box/selection/details test.
  It requires a desktop display (or a configured virtual display on Linux) and the project's prescribed JDK+FX.
  The GUI test is explicitly disabled without that environment variable so headless local checks remain usable.
  Linux CI enables it under `xvfb-run` using software rendering, and uploads that run's coverage to Codecov.
  The test opt-in is a Gradle task input so enabling it cannot reuse results from a run that skipped the UI test.
* `AtomicStudentCommandTest` injects write/replacement failures and checks old file bytes, filter and selection,
  as well as save-before-publish ordering. `StudentPersistenceTest` checks old records, stable IDs and owner data.
* `AddStudentEndToEndTest` verifies invalid commands leave state unchanged, exact error precedence, both user
  examples, shared-phone warnings, duplicate rules and insertion order after reload.

### Shared single-field updates

Guardian, rate, level and subject commands delegate their staged mutation to the package-private
`StudentFieldUpdate` helper. Each command retains its public constructor, messages and owner-supplied codec.
The helper resolves the displayed index, preserves the stored formatting and selection on equivalent input,
and otherwise replaces only the requested optional field. Saving remains exclusively in `LogicManager`.
Unreadable in-memory level/subject values retain the existing repair-as-unset behavior; file loading still
validates stored fields before publication. `ClearCommand` clears its staged selection before publication,
so an already-saved empty register never attempts to select a removed UUID.

### Monthly payment status

`PaymentHistory` is the optional `StudentField` value stored under the `payments` JSON key. Each student record
already carries a stable UUID, so its sorted `YYYY-MM` to `pending|completed` map represents one status per
student and month. An absent month is unrecorded; pending is an explicit value. The typed codec validates the
whole object during JSON load, leaving older records without `payments` unset and rejecting malformed months,
types or statuses safely. Editing preserves the UUID and optional-field envelope; deleting the student removes
the history with it.

`PaymentCommandParser` uses the shared structural parser and `VisibleIndex`. It validates the displayed index
syntax before the month and status, then `PaymentCommand` resolves the row against the currently filtered list.
Setting an identical typed status returns a no-change result without staging a mutation. A new or replacement
status updates only its month, selects the student and leaves all other fields untouched. The existing
`LogicManager` copy-for-command, save-before-publish flow makes storage failure atomic for the saved file, model,
view and selection. The storage adapter retains payment JSON without decoding it during startup, so malformed history
cannot cause the whole directory to load empty and be overwritten later. Payment commands decode on access and
report the affected student without saving. Tracking remains manual; there is no invoice generation or payment processing.

`PaymentsCommandParser` validates the required month and optional status with the shared structural parser.
`PaymentsCommand` walks the complete student register in insertion order, so active filters do not hide recorded
payments. It excludes unrecorded months, optionally filters by status, and leaves storage and selection untouched.
`PaymentHistory.detailLines` renders chronological months in the selected-student details panel, with an em dash
for no history and a safe message for malformed in-memory values. Tests cover filtered-view independence, status
filtering, empty results, no-save behavior and details rendering.

The User Guide documents both commands, error precedence, transitions, no-change behavior, listing behavior and
restart examples. Tests cover strict month/status parsing, reordered prefixes, index errors, malformed persisted
maps, legacy records, reload, unrelated-field preservation, deletion and save failure.

### Weekly lesson scheduling

`WeeklySlot` is immutable: it requires a `DayOfWeek` and a minute-precision `LocalTime` together.
`WeeklySlotField` implements F01's `StudentField<WeeklySlot>` with the reserved key `weeklySlot`.
It encodes one JSON object, for example `{"day":"TUESDAY","time":"19:00"}`. The optional object is either
unset or complete. `JsonAdaptedStudent` invokes the codec before returning a loaded student, rejecting partial
objects and invalid values. The shared `StudentFields` envelope retains unrelated owner fields and stable IDs.

`ScheduleCommandParser` uses `StudentParameters.parseIndexed` for structure and `StudentText` for normalization.
Command/prefix syntax stays case-sensitive. Structural unknown/unexpected errors precede required/repeated
parameters; values are checked as index syntax, day, then time. F03's `VisibleIndex` keeps arbitrary-length
decimal indexes and resolves against the displayed list only after value validation.

`ScheduleCommand` compares the typed normalized slot before updating. A real change uses `withStudentFields`
to replace the complete pair and selects the target UUID; no-change returns without touching the staged model.
F01's `copyForCommand`/save/`publish` protocol and `AtomicJsonFile` preserve file bytes, live state, predicate,
selection and both components on failure. Overlaps are accepted; there is no conflict or duration model.

`WeeklySchedule` observes the complete register through a filtered/sorted view, ordering by weekday, time and
source insertion position. Its unmodifiable entries drive `WeeklySchedulePanel`; it does not change register
order or follow the displayed-list filter. Removal from the register automatically removes the schedule entry.
The details/card formatters use full weekday names and HH:mm, with an em dash for an unset slot.

#### Daily and weekly timetable (`timetable [d/DAY]`)

`TimetableCommandParser` uses `StudentParameters.parseSingleValues` with only `d/`, then the same day
normalizer as `schedule`, so aliases, case-insensitivity and the invalid-day message are shared.
`TimetableCommand` touches no model state: it returns `CommandResult.timetable(message, TimetableView)`, where
`TimetableView` wraps an `Optional<DayOfWeek>` (empty means the whole week). `LogicManager` therefore performs
no staging or save for it. `MainWindow` hands the day to `WeeklySchedulePanel#showDay`, which sets the predicate of
a JavaFX `FilteredList` wrapped around `Logic#getWeeklySchedule()` and updates the heading and empty message.
Filtering preserves the underlying Monday–Sunday/time/insertion order, and because the panel observes the
live sorted list, scheduling, renaming and deletion refresh the current day or week immediately. A parse error
returns before the result reaches the panel, so the previous view stays. The day is not persisted; restart
shows the week. A second calendar model was rejected to avoid duplicating ordering and refresh logic.

#### Removing a weekly slot (`unschedule INDEX`)

`UnscheduleCommandParser` reuses `StudentParameters.parseIndexed` and `VisibleIndex`, so structure, index and
oversized-index errors match the other indexed commands. `UnscheduleCommand` resolves the target in the displayed
list. Without a slot, it returns a success message without touching the staged model, so nothing is saved and
selection stays. Otherwise it calls `StudentFields#without(WeeklySlotField.INSTANCE)` and `Model#setStudent`,
which preserves the UUID, register position, unrelated fields and unknown stored fields, and selects the target.
The usual staged save/publish protocol applies, so a write failure leaves the previous slot, file bytes, filter
and selection intact. `delete` remains the way to remove a student together with their slot.
`MainWindow` ignores transient selection events while publishing, then refreshes the selected immutable record.
Repeated equivalent commands preserve selection, scrolling and existing details nodes.

Verification covers field validation, codec/parser/command behavior, persistence and staged failures,
summary ordering, and the actual JavaFX command box. See [the acceptance run and commands](Testing.md).

### Academic records: notes and assessments

<puml src="diagrams/AcademicRecordsClassDiagram.puml" width="450" />

A student can carry one freeform **note** and a dated **assessment history**. Both are optional fields in the
`StudentFields` envelope, so they follow the same rules as the other owned fields: a typed `StudentField` codec
supplies the key, validation and JSON encoding, `Student#withStudentFields` keeps the UUID and every unrelated
field, and an unset value is simply absent from the file.

**Model classes**

* `AcademicNote` (key `academicNote`) is immutable text of 1-500 Unicode code points after NFKC normalization,
  trimming and space collapsing. Control, format and line-break characters are rejected. Equality is exact, so a
  case-only change is a different note.
* `AssessmentRecord` is an immutable `LocalDate`, a title (1-60 characters) and a grade (1-20 characters). The
  date must be a zero-padded, real `yyyy-MM-dd` date and is not compared with the current date. The grade is kept
  as text and never parsed, so grading systems are never compared or averaged. Two records are the *same
  assessment* if they share a date and a title, ignoring case and spacing.
* `AssessmentHistory` (key `assessments`) is an immutable list of at most 100 records ordered by date, with
  records on the same date kept in the order they were added. It never contains two records that are the same
  assessment. A record's *number* is its 1-based position in this order, which the commands, the result box and
  the details panel all use.

**Storage.** The note is a JSON string and the history is a JSON list of `{"date", "title", "grade"}` objects in
display order. A student with no note or no records has no key at all, so files written before these features
load unchanged. Decoding the history is strict: anything that is not a list of exactly those three text
properties, an impossible date, a duplicate or more than 100 records makes the value *unreadable*. The two
fields treat unreadable data differently on purpose:

| Field | Unreadable stored value |
| --- | --- |
| `academicNote` (one replaceable value) | Shown as an em dash; `note` treats it as unset and replaces it; `clearnote` removes it. |
| `assessments` (a list a user may have built up) | Shown as "stored data could not be read"; `assess`, `assessments` and `unassess` report an error and never overwrite it. |

**Commands.** `note` reuses the shared `StudentFieldUpdate` helper. `clearnote`, `assess`, `assessments` and
`unassess` resolve the displayed index with `VisibleIndexResolver`, then build the updated `Student` and hand it
to `Model#setStudent`; `AssessmentUpdate` reads the history (reporting an unreadable one) and writes it back,
removing the key when the history becomes empty. As for every other command, `LogicManager` runs the command on
a staged copy of the model, saves the complete proposed directory atomically only if it changed, and then
publishes it, so a failed command or a failed save leaves the file, the list, the filter and the selection
unchanged. Equivalent `note` input, `clearnote` on an empty note and `assessments` therefore never write the file.
`assessments` only selects the student.

| Command | Parser | Command class | Effect on `StudentFields` |
| --- | --- | --- | --- |
| `note INDEX m/NOTE` | `NoteCommandParser` (everything after `m/` is the note) | `NoteCommand` | sets or replaces `academicNote` |
| `clearnote INDEX` | `ClearNoteCommandParser` | `ClearNoteCommand` | removes `academicNote` |
| `assess INDEX d/DATE t/TITLE g/GRADE` | `AssessCommandParser` (`StudentParameters.parseIndexedValues`) | `AssessCommand` | adds one record to `assessments` |
| `assessments INDEX` | `AssessmentsCommandParser` | `AssessmentsCommand` | none (selects the student) |
| `unassess INDEX i/RECORD` | `UnassessCommandParser` (`IndexedParameterParserUtil`) | `UnassessCommand` | removes one record by number |

**Interaction with other features.** `delete` removes the whole `Student`, so its note and records leave the file
with it. `edit` uses `Student#withDetails`, which keeps the `StudentFields`, so contact edits never touch them.
The level, subject, guardian, rate and schedule commands each replace only their own key.

**Design decisions and alternatives**

* *One note, exact comparison.* A note is free text, so unlike a subject it is not folded for case; a re-typed
  note with different capitals is an update.
* *Positional record numbers.* Records are identified by their number in date order rather than by a stored
  identifier. This keeps the file and the commands simple and the numbers visible next to each record, at the cost
  that numbers shift after an add or remove, which the User Guide tells users to check with `assessments`. A
  stable record identifier is the alternative if corrections by number ever prove error-prone.
* *Duplicates are rejected, not replaced.* Replacing the grade of an existing date and title would need a third
  command or an overwrite rule; instead the user removes the old record and adds the new one.
* *Strict history decoding.* Refusing to touch an unreadable history trades convenience for never silently
  losing a hand-built list.

### Student discovery: find and filter

`find` uses `NameContainsKeywordsPredicate`: NFKC normalization via `StudentText`, followed by
`Locale.ROOT` lowercase substring comparison. Whitespace-separated keywords are OR alternatives;
accents are preserved. `filter` uses `StudentCriteriaPredicate` with optional typed `EducationLevel`
and `Subject` criteria combined with AND. Their existing constructors/equality define alias and label
normalization. Unset or unreadable requested fields do not match. `StudentParameters.parseOptional`
reuses structural checks without relaxing any existing command's required parameters.

<puml src="diagrams/StudentDiscoverySequenceDiagram.puml" width="900" />

Both commands replace the model predicate over the full register, retain stored order, clear selection,
and report the result count (including zero). Queries do not change records, so `LogicManager` does not
save. `list` remains the reload/reset operation. Parse errors never publish a proposed model.
`WeeklySchedule` observes the full register independently of the student-list predicate.

Indexed commands resolve the displayed row to its UUID before mutation. If a successful level/subject
update removes that row from the filter, `StudentFieldUpdate` clears selection and returns no row index;
otherwise its existing selection policy is retained. This avoids publishing a hidden selected UUID or
selecting the next student after the row disappears. Existing edit/reset and deletion behavior is retained.

`StudentDiscoveryIntegrationTest` covers query replacement, no-save queries, JSON reload, every indexed
mutation's target UUID, error preservation and schedule independence. `StudentDiscoveryUiTest` checks
rendered numbering, empty results and selection through the actual command box (`F01_UI_TESTS=true`).

### Delete and list

- `VisibleIndex.parse` validates decimal syntax and `resolve` targets the current displayed list.
  Decimal text is compared against list size before integer conversion, safely handling oversized indices.
- Commands execute on F01's `Model.copyForCommand()` snapshot. Deletion removes the entire student,
  including the optional-field envelope. The shared logic saves before `Model.publish()` updates the UI.
  Storage delegates to `AtomicJsonFile`; a failed save preserves data, filter, and selected UUID.
- List reads and validates saved records into the proposed model, resets the filter, and clears selection.
  It publishes only after a successful load and never saves. Before the first save it uses the in-memory register.
- The shared model holds the selected UUID. `MainWindow` synchronizes the list and details after success.
  `StudentListPanel.selectStudent` refreshes cells so row numbering follows the updated positions.
  The empty register shows a placeholder; clearing selection resets the details panel.
- Optional student values render as em dashes when unset in `StudentDetailsPanel`. Legacy empty email
  metadata remains hidden as specified by F01. Field owners retain responsibility for typed codecs/formatters.

Verification covers duplicate-name targeting, stored order, entire optional envelopes after reload,
first/middle/last and filtered deletion, malformed/oversized indices, failure preservation, and real JavaFX
selection/details/renumbering. The optional-field envelope fixtures verify preservation without replacing
field owners' typed validation tests. JavaFX command tests run under `F01_UI_TESTS=true` (Linux CI uses Xvfb).

### \[Proposed\] Undo/redo feature

TutorConnect v1.5 has no `undo` or `redo` command (the User Guide says so); this section records the planned design,
adapted from AB3.

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedStudentDirectory`. It extends `StudentDirectory` with an undo/redo history, stored internally as a `studentDirectoryStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedStudentDirectory#commit()` -- Saves the current student directory state in its history.
* `VersionedStudentDirectory#undo()` -- Restores the previous student directory state from its history.
* `VersionedStudentDirectory#redo()` -- Restores a previously undone student directory state from its history.

These operations are exposed in the `Model` interface as `Model#commitStudentDirectory()`, `Model#undoStudentDirectory()` and `Model#redoStudentDirectory()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedStudentDirectory` will be initialized with the initial student directory state, and the `currentStatePointer` pointing to that single student directory state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes `delete 5` command to delete the 5th student in the displayed list. The `delete` command calls `Model#commitStudentDirectory()`, causing the modified state of the student register after the `delete 5` command executes to be saved in the `studentDirectoryStateList`, and the `currentStatePointer` is shifted to the newly inserted student directory state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add n/David …​` to add a new student. The `add` command also calls `Model#commitStudentDirectory()`, causing another modified student directory state to be saved into the `studentDirectoryStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitStudentDirectory()`, so the student directory state will not be saved into the `studentDirectoryStateList`.
</box>

Step 4. The user now decides that adding the student was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoStudentDirectory()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous student directory state, and restores the student register to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial StudentDirectory state, then there are no previous StudentDirectory states to restore. The `undo` command uses `Model#canUndoStudentDirectory()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoStudentDirectory()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the student register to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `studentDirectoryStateList.size() - 1`, pointing to the latest student directory state, then there are no undone StudentDirectory states to restore. The `redo` command uses `Model#canRedoStudentDirectory()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the student register, such as `list`, will usually not call `Model#commitStudentDirectory()`, `Model#undoStudentDirectory()` or `Model#redoStudentDirectory()`. Thus, the `studentDirectoryStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitStudentDirectory()`. Since the `currentStatePointer` is not pointing at the end of the `studentDirectoryStateList`, all student directory states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire student directory.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the student being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

**Aspect: Interaction with the staged command flow:**

* Commit the history only after `LogicManager` has saved and published the staged model, so a failed save never
  creates an undo step. Undo and redo themselves must save the restored state atomically before publishing it.

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

TutorConnect targets freelance private home tutors (persona: a self-employed tutor teaching 15-20 students) who:

* work independently, without administrative staff, and manage multiple students across Primary, Secondary and Junior College levels;
* need to keep student and parent contact details organised alongside lesson schedules, subject syllabi and academic notes;
* regularly look up or update student information when planning lessons or contacting parents;
* find it slow and error-prone to cross-reference messaging apps, paper planners and spreadsheets between lessons;
* are comfortable using a laptop and keyboard shortcuts, and prefer fast keyboard commands for frequent tasks; and
* need access to their tutoring records while travelling between students' homes, without relying on an internet connection or server.

**Value proposition**:

TutorConnect helps independent home tutors manage student and parent contact details, lesson schedules and subject
records in one keyboard-driven desktop application, so they no longer cross-reference messaging apps, calendars and
spreadsheets.

In v1.5 this means one offline register in which a tutor can:

* record each student's contacts, guardian phone, education level, subject and hourly rate;
* keep one recurring weekly lesson slot per student and view the whole week or one day as a timetable;
* find students by partial name, or filter them by level and subject;
* keep a freeform note and a dated history of test and exam results; and
* mark each month's payment as pending or completed and list who still owes fees.

**Out of scope**: generating invoices or processing payments, syncing between several tutors, and replacing a full
calendar or learning-management system. Lesson durations, clash detection and more than one slot per student are
deferred.


### User stories

The following stories describe TutorConnect's requirements for a freelance tutor, including ideas that may not be implemented in the final product.
Priorities follow the [team user-story register](https://docs.google.com/spreadsheets/d/1GyZpU64-x8papgY5nPyfu2VwB_hmP-6wA3xbr1CX16M/edit): High (must have) - `* * *`, Medium (nice to have) - `* *`.
These are product priorities, not release commitments. The MVP (release v1.3) delivered stories 1-8; v1.5 implements stories 1-15
(edit, filter, payments, assessments, notes, timetable and partial-name search). Stories 16-31 are not implemented and
are kept for future consideration.

| ID | Priority | As a ... | I can ... | So that I can ... |
|----|----------|----------|-----------|-------------------|
| 1 | `* * *` | freelance tutor | add a student contact with their name, phone number, and address | keep their primary logistical details in one location |
| 2 | `* * *` | freelance tutor | delete a student profile when they stop taking lessons | keep my contact list relevant and clutter-free |
| 3 | `* * *` | freelance tutor | list all my registered students | get an overview of my current client base |
| 4 | `* * *` | freelance tutor | associate a parent's contact number with a student | reach out to the bill-payer directly regarding tuition matters |
| 5 | `* * *` | freelance tutor | tag students by educational level (e.g., Primary, Sec 4, JC 2) | tailor my materials and syllabus planning accurately |
| 6 | `* * *` | freelance tutor | tag students by subject taught (e.g., H2 Math, O-Level Chem) | group students by preparation requirements |
| 7 | `* * *` | freelance tutor | record an agreed hourly rate for each student | calculate tuition fees accurately at the end of each billing cycle |
| 8 | `* * *` | freelance tutor | record weekly recurring lesson slots (day and time) for each student | plan the teaching week alongside student details |
| 9 | `* * *` | freelance tutor | edit an existing student's contact or academic details | keep information up to date without re-creating the profile |
| 10 | `* * *` | freelance tutor | filter students by subject or level | view subsets of my cohort during syllabus revisions |
| 11 | `* * *` | freelance tutor | mark a monthly invoice or payment as pending or completed | track which parents owe me fees without manual spreadsheet checking |
| 12 | `* * *` | freelance tutor | record student exam grades and test scores over time | monitor individual academic progress across terms |
| 13 | `* * *` | freelance tutor | add freeform notes to a student profile (e.g., weak in calculus, short attention span) | adjust my pedagogy prior to each session |
| 14 | `* * *` | freelance tutor | view a consolidated daily or weekly teaching schedule | plan my daily commute between home tuition sessions |
| 15 | `* * *` | freelance tutor | search for a student by partial name match | pull up their file rapidly during an unscheduled parent phone call |
| 16 | `* *` | freelance tutor | filter or sort students by postal district or neighbourhood | cluster back-to-back home visits within the same area |
| 17 | `* *` | freelance tutor | track cumulative unpaid lesson balances across multiple weeks | send exact outstanding tallies to parents without manual re-calculation |
| 18 | `* *` | freelance tutor | export payment reminder summaries or invoice text | copy-paste them straight into WhatsApp or SMS to parents |
| 19 | `* *` | freelance tutor | log lesson attendance per session (e.g., Present, Cancelled, Student MC) | charge accurately for billable hours and track makeup classes owed |
| 20 | `* *` | freelance tutor | view an aggregated monthly revenue report | understand my monthly freelance income without external accounting tools |
| 21 | `* *` | freelance tutor | tag students with upcoming national examination years (e.g., 2026 O-Levels) | prioritise intensive revision slots for graduating cohorts |
| 22 | `* *` | freelance tutor | assign homework tasks and flag submission statuses per student | hold students accountable for work due before the next lesson |
| 23 | `* *` | freelance tutor | record whether lessons are conducted Online or In-Student | prepare meeting links or commute routes accordingly |
| 24 | `* *` | freelance tutor | archive graduated or inactive students instead of hard deleting them | retain historical records and past exam outcomes for referrals |
| 25 | `* *` | freelance tutor | view students who have not had a lesson logged in over two weeks | follow up on prolonged unexplained absences or paused arrangements |
| 26 | `* *` | freelance tutor | record school syllabus streams (e.g., IP, IB, Express, Normal Academic) | align lesson pace with school-specific examination formats |
| 27 | `* *` | freelance tutor | store emergency secondary contact details | reach an alternative guardian if the primary contact is unreachable |
| 28 | `* *` | freelance tutor | set lesson duration in minutes per student (e.g., 90 mins vs 120 mins) | calculate fees precisely when lesson lengths vary |
| 29 | `* *` | freelance tutor | export the student contact and progress list into CSV/JSON format | maintain independent backups or prepare tax records |
| 30 | `* *` | freelance tutor | record textbook editions and curriculum materials lent out to a student | retrieve loaned resources before the student concludes their tuition |
| 31 | `* *` | freelance tutor | record attendance or cancelled lessons and the reason for cancelling | avoid charging students for lessons they did not attend |

### Use cases

(For all use cases below, the **System** is `TutorConnect` and the **Actor** is the `user`, unless specified otherwise.
"The list" means the student list currently displayed, and INDEX is a student's number in it.)

The following extensions apply to every use case that changes data, and are not repeated below:

* *a. TutorConnect cannot save the data file.
    * *a1. TutorConnect shows `Error: Changes could not be saved. No changes were made.` and keeps the previous data,
      list, filter and selection.

      Use case ends.

* *b. The given INDEX is not a positive whole number, or no student has that number in the list.
    * *b1. TutorConnect shows an invalid-index or missing-student error and changes nothing.

      Use case resumes at the step where the user gave the index.

**Use case: UC01 - Add a student**

**MSS**

1. User requests to add a student, providing name, phone and address.
2. TutorConnect adds the student to the end of the register, selects the new student and shows their details.

   Use case ends.

**Extensions**

* 1a. A required field is missing, repeated or invalid (e.g. a phone number that is not a Singapore number).

    * 1a1. TutorConnect shows an error message naming the problem.

      Use case resumes at step 1.

* 1b. A student with the same name (ignoring case) and phone already exists.

    * 1b1. TutorConnect shows a duplicate-student error message.

      Use case resumes at step 1.

* 1c. Another student with a different name already uses the phone number (e.g. a sibling).

    * 1c1. TutorConnect adds the student and adds a shared-phone warning to the success message.

      Use case ends.

**Use case: UC02 - Find or filter students**

**MSS**

1. User requests to find students by part of a name, or to filter them by education level and/or subject.
2. TutorConnect shows the matching students from the whole register, numbered from 1, and the number found.

   Use case ends.

**Extensions**

* 1a. No keyword or criterion is given, or a criterion is invalid.

    * 1a1. TutorConnect shows an error message and keeps the current list.

      Use case resumes at step 1.

* 2a. No student matches.

    * 2a1. TutorConnect shows an empty list and reports that 0 students were found.

      Use case ends.

* 2b. User later wants the full register again and requests to list all students.

    * 2b1. TutorConnect reloads the full register from the data file and clears the filter and selection.

      Use case ends.

**Use case: UC03 - Delete a student**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to delete a specific student in the list.
3. TutorConnect deletes the student with all their details, slot, note, results and payments, and renumbers the list.

   Use case ends.

**Extensions**

* 1a. The list is empty.

  Use case ends.

**Use case: UC04 - Record a student's guardian, level, subject or rate**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to set one detail (guardian phone, education level, subject or hourly rate) for a student in the list.
3. TutorConnect saves the new value, selects the student and shows the value in their details.

   Use case ends.

**Extensions**

* 2a. The value is invalid (e.g. a level outside Primary 1 to JC 2, or a rate above S$1000.00).

    * 2a1. TutorConnect shows an error message describing the accepted values.

      Use case resumes at step 2.

* 2b. The value is equivalent to the stored one (e.g. `sec 4` when the level is already Secondary 4).

    * 2b1. TutorConnect reports that the value is already set and does not save.

      Use case ends.

**Use case: UC05 - Schedule a weekly lesson**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to set a weekly lesson day and start time for a student in the list.
3. TutorConnect records the slot, replacing any previous slot of that student, and adds it to the timetable.

   Use case ends.

**Extensions**

* 2a. The day or time is missing or invalid (e.g. `t/24:00` or `d/Mo`).

    * 2a1. TutorConnect shows an error message describing the accepted format.

      Use case resumes at step 2.

* 2b. The slot equals the student's current slot.

    * 2b1. TutorConnect reports that the lesson is already at that time and does not save.

      Use case ends.

* 3a. Another student already has a lesson in the same slot.

    * 3a1. TutorConnect records the slot anyway; both lessons appear in the timetable. Clash detection is not provided.

      Use case ends.

**Use case: UC06 - View the timetable**

**MSS**

1. User requests to view the timetable for the whole week or for one day.
2. TutorConnect shows every scheduled student in the timetable panel, ordered by day and time, regardless of the
   list's filter. The student list and selection do not change.

   Use case ends.

**Extensions**

* 1a. The day is invalid or given more than once.

    * 1a1. TutorConnect shows an error message and keeps the previous timetable view.

      Use case resumes at step 1.

* 2a. No lessons are scheduled for the chosen day or week.

    * 2a1. TutorConnect shows an empty timetable with a message saying no lessons are scheduled.

      Use case ends.

**Use case: UC07 - Remove a weekly lesson**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to remove the weekly lesson of a student in the list.
3. TutorConnect removes the slot only, keeps all other details of the student, and updates the timetable.

   Use case ends.

**Extensions**

* 2a. The student has no weekly lesson.

    * 2a1. TutorConnect reports that no lesson is scheduled and does not save.

      Use case ends.

**Use case: UC08 - Keep a note about a student**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to set a note for a student in the list.
3. TutorConnect saves the note, replacing any earlier note, and shows it in the student's details.

   Use case ends.

**Extensions**

* 2a. The note is empty, longer than 500 characters or contains line breaks.

    * 2a1. TutorConnect shows an error message.

      Use case resumes at step 2.

* 2b. User requests to clear the student's note instead.

    * 2b1. TutorConnect removes the note, or reports that there is no note to clear.

      Use case ends.

**Use case: UC09 - Record and review assessment results**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to add an assessment result (date, title and grade) for a student in the list.
3. TutorConnect adds the result to the student's history in date order and shows its number.
4. User requests to view the student's assessment history.
5. TutorConnect lists the numbered results.

   Use case ends.

**Extensions**

* 2a. The date is not a real `yyyy-MM-dd` date, or the title or grade is invalid.

    * 2a1. TutorConnect shows an error message.

      Use case resumes at step 2.

* 2b. The student already has a result with the same date and title.

    * 2b1. TutorConnect rejects the duplicate and tells the user to remove the old result first.

      Use case resumes at step 2.

* 5a. User requests to remove a result by its number.

    * 5a1. TutorConnect removes that result and renumbers the rest, or shows an error if the number does not exist.

      Use case ends.

**Use case: UC10 - Track monthly payments**

**MSS**

1. User lists, finds or filters students (UC02).
2. User requests to mark a month's payment for a student in the list as pending or completed.
3. TutorConnect records the status for that month.
4. User requests the payment statuses for a month, optionally only the pending ones.
5. TutorConnect lists every student with a recorded status for that month, from the whole register.

   Use case ends.

**Extensions**

* 2a. The month is not `YYYY-MM` or the status is not `pending` or `completed`.

    * 2a1. TutorConnect shows an error message.

      Use case resumes at step 2.

* 2b. The status is already recorded for that month.

    * 2b1. TutorConnect reports no change and does not save.

      Use case ends.

* 5a. No student has a recorded status matching the request.

    * 5a1. TutorConnect reports that there are no recorded payments for that month.

      Use case ends.

**Use case: UC11 - Edit a student's details**

**MSS**

1. User requests to list students.
2. TutorConnect shows a list of students.
3. User requests to edit the details of a specific student in the list.
4. TutorConnect updates the student's details.

   Use case ends.

**Extensions**

* 2a. The list is empty.

  Use case ends.

* 3a. The given index is invalid.

    * 3a1. TutorConnect shows an error message.

      Use case resumes at step 2.

* 3b. A given field is invalid (e.g. malformed phone number).

    * 3b1. TutorConnect shows an error message describing the invalid field.

      Use case resumes at step 2.

* 3c. The edit would result in a duplicate student (same normalized name and student phone as an existing student).

    * 3c1. TutorConnect shows a duplicate-student error message.

      Use case resumes at step 2.

### Non-Functional Requirements

**Operating environment and portability**

1. Should work on any _mainstream OS_ (Windows, macOS, Linux) that has Java `25` installed. Only Java 25 is
   supported; the packaged classes target Java 25 and cannot run on older runtimes.
2. Should run as a single executable JAR (`tutorconnect.jar`, under 100 MB) launched with `java -jar`, without an
   installer, administrator rights or other files.
3. Should work from any folder the user can write to. The data file (`data/addressbook.json`), preferences and log
   are created relative to the folder the app is launched from; a write-protected folder is not supported.
4. Must not depend on a remote server, cloud service or network connection; all features work offline.

**Performance and capacity**

5. Should hold at least 1,000 students, each with a weekly slot, note, 100 assessment records and 24 months of
   payment statuses, with each command responding within 1 second on a typical laptop.
6. Should show an interactive window within 5 seconds of launch on a typical laptop with an SSD.

**Usability**

7. A user who types at 60 words per minute or more should complete common tasks (add, find, schedule, note)
   faster with commands than with a mouse.
8. Every failed command should show one error message that names the problem and leave the data, list, filter
   and selection unchanged.
9. The main window should remain usable, without overlapping or clipped text, at resolutions from 1280×720 to
   1920×1080 and at 100% to 150% display scaling.

**Data integrity and storage**

10. Data is stored locally in one human-editable JSON file without a database. Each change is written to a
    temporary file and atomically replaces the previous file, so a crash or failed save never leaves a partly
    written file.
11. A command that does not change data (a query or a no-change update) must not rewrite the data file.
12. Files written by earlier TutorConnect versions must still load, and fields a version does not recognise must
    be kept when it saves.

**Privacy**

13. Student and guardian contact details, rates and academic records never leave the user's computer; the app
    sends no telemetry or network requests.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing. Never test with your real data file: use a new empty folder.
</box>

### Launch and shutdown

1. Initial launch

   1. Run `java -version` and check that it reports Java 25.

   1. Copy `tutorconnect.jar` into a new empty folder that you can write to (not a write-protected or synced folder).

   1. Open a terminal in that folder and run `java -jar "tutorconnect.jar"`.<br>
      Expected: The `TutorConnect` window opens with six sample students. The folder now contains `preferences.json`
      and a log file; `data/addressbook.json` is created on the first change. Double-clicking the JAR may also work,
      but is not the supported way to launch.

1. Saving window preferences

   1. Resize the window and move it. Close it with the window's close button.

   1. Launch again with `java -jar "tutorconnect.jar"` from the same folder.<br>
      Expected: The window reopens at the last size and position.

1. Shutdown

   1. Enter `exit`, or press <kbd>F1</kbd> to open the help window and then enter `exit`.<br>
      Expected: The main window and the help window both close.

### Setting up test data

1. With the app closed, replace the contents of `data/addressbook.json` with `{"persons":[]}` (create the
   `data` folder if needed) and launch the app. The list is empty.

1. Enter, in order:
   `add n/Alex Tan p/81234567 a/21 Clementi Ave`,
   `add n/Nur Aisyah p/92345678 a/8 Jalan Besar`,
   `add n/Ben Lim p/81234567 a/3 Bishan St`.<br>
   Expected: Each is added and selected. The third reports `Student added: Ben Lim. Warning: Another student uses
   this phone number.` because Alex has the same phone.

The remaining sections start from these three students, numbered 1-3, unless stated otherwise.

### Adding a student

1. Test case: `add n/alex tan p/+65 8123 4567 a/1 Road`<br>
   Expected: `Error: This student already exists with the same name and phone.` Name case and phone formatting
   do not matter for duplicates. Nothing is added.

1. Test case: `add n/Alex Tan p/12345678 a/1 Road`<br>
   Expected: `Error: Invalid phone. ...` (phones must be Singapore numbers starting with 6, 8 or 9).

1. Test case: `add n/Alex Tan p/91234567`<br>
   Expected: `Error: Missing required parameter: a/ADDRESS.`

### Setting guardian, level, subject and rate

1. Test case: `guardian 1 g/91234567`, `level 1 l/Sec4`, `subject 1 s/H2 Math`, `rate 1 r/80`<br>
   Expected: Each succeeds and selects Alex. The details panel shows `Guardian phone: +6591234567`,
   `Education level: Secondary 4`, `Subject: H2 Math` and `Hourly rate: S$80.00`.

1. Test case: `rate 1 r/80.00`<br>
   Expected: `Hourly rate for Alex Tan is already S$80.00.` The data file is not rewritten.

1. Test case: `level 2 l/P7`, `rate 1 r/1000.01`<br>
   Expected: Invalid-level and invalid-rate errors; nothing changes.

### Finding and filtering students

1. Prerequisite: `level 2 l/sec 3` and `subject 2 s/Chemistry`.

1. Test case: `find nur`<br>
   Expected: Only Nur Aisyah is listed, as number 1, with `1 student(s) listed!`.

1. Test case: `find xyz`<br>
   Expected: `0 student(s) listed!` and an empty list with `No students found.`.

1. Test case: `filter l/sec4`, then `filter s/chemistry`<br>
   Expected: Only Alex, then only Nur. `filter` alone reports that at least one criterion is needed.

1. Test case: `list`<br>
   Expected: `Listed 3 students.` with the full register and no selection.

### Weekly scheduling and timetable

1. Prerequisite: `list`, then `schedule 1 d/Mon t/09:00`, `schedule 2 d/mon t/09:00` and `schedule 3 d/Sunday t/23:59`.<br>
   Expected: All three succeed; Alex and Nur may share the same slot.

1. Test case: `schedule 1 d/Mon t/09:00`<br>
   Expected: `Weekly lesson for Alex Tan is already Monday 09:00.` (no save).

1. Test case: `schedule 1 d/Mon t/9am`<br>
   Expected: `Error: Invalid time. Use 24-hour HH:mm, for example 09:00 or 18:30.`

1. Test case: `timetable`, then `timetable d/sun`<br>
   Expected: The schedule panel lists Alex and Nur (Monday 09:00, in the order they were added), then Ben
   (Sunday 23:59); with `d/sun` its heading is `Sunday schedule` and it lists only Ben. The student list does not change.

1. Test case: `find ben`, then `timetable`<br>
   Expected: The student list shows only Ben, but the timetable still shows all three lessons.

1. Test case: `timetable d/Wed`, then `timetable d/Funday`<br>
   Expected: An empty panel saying `No lessons scheduled for Wednesday.`, then
   `Error: Invalid day. Use Monday-Sunday or Mon-Sun.` with the Wednesday view kept.

1. Test case: `list`, then `unschedule 2`<br>
   Expected: `Weekly lesson removed for Nur Aisyah: Monday 09:00.` Nur's details show `Weekly slot: —` and she
   leaves the timetable; Alex keeps the Monday slot. Repeating it reports `No weekly lesson is scheduled for Nur Aisyah.`

### Notes and assessment results

1. Test case: `note 1 m/Weak in calculus`<br>
   Expected: `Note set for Alex Tan.` Repeating it reports `Note for Alex Tan is already up to date.`

1. Test case: `clearnote 1` twice<br>
   Expected: `Note cleared for Alex Tan.`, then `Alex Tan has no note to clear.`

1. Test case: `assess 1 d/2026-10-01 t/Mid-year g/B+`, then `assess 1 d/2026-09-15 t/Quiz 1 g/72%`, then `assessments 1`<br>
   Expected: Results are kept in date order: `1. 2026-09-15 Quiz 1: 72%` and `2. 2026-10-01 Mid-year: B+`.

1. Test case: `assess 1 d/2026-10-01 t/mid-year g/A`, then `assess 1 d/2026-02-30 t/Quiz g/A`<br>
   Expected: A duplicate error (same date and title, ignoring case), then `Error: Invalid date. ...`.

1. Test case: `unassess 1 i/1`, then `unassess 1 i/5`<br>
   Expected: The quiz is removed and the mid-year result becomes number 1; then `Error: No assessment record 5 for Alex Tan.`

### Monthly payments

1. Test case: `payment 1 m/2026-10 s/pending`, then `payment 2 m/2026-10 s/completed`<br>
   Expected: Both are recorded. Repeating the first reports that the status is already pending.

1. Test case: `payments m/2026-10`, then `payments m/2026-10 s/pending`<br>
   Expected: Both students with their status, then only Alex. Run `find nur` first: the list is still from the whole register.

1. Test case: `payment 1 m/2026-13 s/pending`<br>
   Expected: `Error: Invalid month. Enter a valid month in YYYY-MM format.`

### Deleting a student

1. Test case: `list`, then `delete 3`<br>
   Expected: `Student deleted: Ben Lim.` Ben and his Sunday lesson disappear from the list and the timetable.

1. Test case: `delete 3` (now beyond the list), `delete 0`, `delete x`, `delete`<br>
   Expected: `Error: No student exists at index 3.`, then `Error: Invalid index. Enter a positive whole number
   without leading zeroes.` (for `0` and `x`), then `Error: Missing required parameter: INDEX.` Nothing is deleted.

1. Test case: `find nur`, then `delete 1`<br>
   Expected: Nur (number 1 in the filtered list) is deleted, not Alex.

### Saving data

1. Data persists after restart

   1. Close the app and launch it again from the same folder.<br>
      Expected: The same students, fields, lessons, results and payments are shown.

1. Missing data file

   1. With the app closed, delete `data/addressbook.json` and launch.<br>
      Expected: The six sample students appear again.

1. Corrupted data file

   1. With the app closed, change one student's `"phone"` value in `data/addressbook.json` to `"12"` and launch.<br>
      Expected: The app starts with an empty list, and the result box shows a warning that the file could not be
      read. The `data` folder now also holds an exact copy named `addressbook.json.unreadable-YYYYMMDD-HHMMSS.bak`.
      `list` reports `Error: Student list could not be loaded.`

   1. Test case: `add n/Cara p/83456789 a/3 St`, then close the app.<br>
      Expected: `data/addressbook.json` now holds only Cara, and the `.bak` copy is unchanged. Rename the copy to
      `addressbook.json`, fix the phone value and relaunch: the original students are back.

### Clearing all data

1. Test case: `clear`<br>
   Expected: Every student and the whole timetable are removed, and the change is saved.

## **Appendix: Planned Enhancements**

Team size: 5.

There are no planned enhancements at present. The feature flaws found in alpha testing (#191 to #196), such as the
silent replacement of an unreadable data file and the remaining AB3 messages, were fixed in v1.5.

--------------------------------------------------------------------------------------------------------------------

## **Glossary**

* **AB3**: AddressBook-Level3, the SE-EDU starter project that TutorConnect extends.
* **Assessment record**: One dated test or exam result (date, title and grade) in a student's assessment history.
* **Brownfield project**: A project that extends an existing codebase rather than starting from scratch.
* **CLI (Command-Line Interface)**: A text-based interface where users interact with the application by entering commands.
* **DevOps**: Development and operational activities such as repository management, build setup, and project tooling.
* **Data file**: The JSON file (`data/addressbook.json` in the launch folder) that stores all students.
* **Displayed index**: The number shown beside a student in the current list. Commands use it to pick a student; it
  changes after `find`, `filter`, `list` or a deletion.
* **Feature specification**: A description of a feature's purpose, commands, inputs, validation, errors, outputs, and expected behaviour.
* **Greenfield project**: A project developed from new requirements without relying on an existing product design.
* **Lesson slot** (weekly slot): A student's one recurring weekly lesson, stored as a day of the week and a 24-hour
  start time. Different students may share a slot.
* **Local storage**: Data stored on the user's own computer rather than on an external server.
* **Mainstream OS**: Windows, Linux or macOS.
* **Logic component**: The part of the application that processes commands and applies application rules.
* **Model component**: The part of the application that represents and manages domain data such as students and lesson details.
* **MVP (Minimum Viable Product)**: The smallest usable version containing the essential features needed by the target user.
* **No-change result**: A successful command whose value already matches the stored one; it does not save the file.
* **Note**: One freeform text remark (up to 500 characters) attached to a student.
* **Optional field**: A student value other than name, phone and address (guardian phone, level, subject, rate,
  weekly slot, note, assessments, payments). An unset optional field is shown as an em dash (—).
* **Parent contact**: The parent or guardian's contact information associated with a student.
* **Payment status**: A manually recorded pending or completed state for one student and calendar month; no entry means unrecorded.
* **Persona**: A representative user profile used to guide product and feature decisions.
* **Recurring lesson slot**: A lesson slot that repeats on a regular schedule, such as every Monday at 4 p.m.
* **Storage component**: The part of the application that saves and loads data.
* **Timetable**: The schedule panel listing every scheduled student by day and time, for the whole week or one day.
* **UI component**: The part of the application responsible for user interaction and displayed output.
* **User story**: A requirement written from the user's perspective, commonly in the format “As a..., I can..., so that...”.
