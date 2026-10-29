---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# TutorConnect User Guide

TutorConnect is a **desktop application for freelance tutors to manage student contacts and lesson details**.
It combines typed commands with a graphical student register and details panel.

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` is installed on your computer (the version used to build and test this release).<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from the [team releases](https://github.com/AY2627S1-CS2103T-F08-3/tp/releases).

1. Copy the file to the folder you want to use as the _home folder_ for TutorConnect.

   The home folder must be writable so TutorConnect can create and update its data files.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar tutorconnect.jar`.<br>
   Double-clicking the JAR might not work on some systems; launch it from the terminal with Java 25 instead.
   On macOS, fullscreen mode can cause unexpected behavior with secondary dialogs such as Help. Exit fullscreen
   before opening Help if you encounter this issue.<br>
   The application opens with the command bar at the top, already focused, so you can start typing straight away.
   Below it are the result of your last command, counts of students and lessons, and your student list. The selected
   student's profile and the weekly schedule are on the right. In a window at least 1100 pixels wide, a read-only
   command reference also appears on the left.
   If no data file exists, the register initially contains sample students.

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all students and clears selection.

   * `add n/John Doe p/98765432 a/John street, block 123, #01-01` : Adds a student named `John Doe`.

   * `delete 3` : Permanently deletes the 3rd student shown in the current list, without confirmation.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `edit 1 n/NAME [t/TAG]` can be used as `edit 1 n/John Doe t/friend` or as `edit 1 n/John Doe`.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[t/TAG]... ` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Prefixed parameters can be in any order. If a command takes `INDEX`, put it before the prefixed parameters.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Commands and prefixes are lowercase and case-sensitive. Separate tokens with spaces; type one command per line.
  An `INDEX` is the current displayed row number, starting at 1, not a stored student ID. After `find`, use the
  filtered row numbers. Signs, decimals and leading zeroes such as `01` are not valid indices.

* Extraneous parameters for commands that take no parameters, such as `help`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Working from the keyboard

TutorConnect is designed to be used without a mouse. The command bar always shows the format of the command you are
typing, e.g. `schedule INDEX d/DAY t/TIME`, and lists the matching command words while you are still typing one.

Key | What it does
--- | ---
<kbd>Enter</kbd> | Runs the command in the command bar.
<kbd>Tab</kbd> | Completes the command word, e.g. `sch` becomes `schedule `. With several matches (`pay`), it completes as far as they agree.
<kbd>↑</kbd> / <kbd>↓</kbd> | Brings back earlier commands from this session, e.g. to fix a mistyped one. <kbd>↓</kbd> past the newest returns to what you were typing.
<kbd>Esc</kbd> | Clears the command bar.
<kbd>Ctrl</kbd>+<kbd>↑</kbd> / <kbd>↓</kbd> (<kbd>⌘</kbd> on macOS) | Selects the previous or next student in the list and shows their profile. This does not change the list or its numbering.
<kbd>Page Up</kbd> / <kbd>Page Down</kbd> | Scrolls the selected student's profile.
<kbd>Ctrl</kbd>+<kbd>L</kbd> (<kbd>⌘</kbd>+<kbd>L</kbd> on macOS) | Returns to the command bar. Typing a letter anywhere in the window also goes straight to the command bar.
<kbd>F1</kbd> | Opens the help window.

Error messages appear in red in the result area, and warnings in amber. Command history is kept only while the app is
open. Completion and hints never change what a command does; the command is checked only when you press
<kbd>Enter</kbd>.

### Viewing help: `help`

Opens a Help window displaying the address of the TutorConnect User Guide, with a **Copy URL** button.
Copy that address into your browser to read the guide. You can also use the Help menu or `F1`.

Format: `help`


### Adding a student (F01): `add`

Format: `add n/NAME p/PHONE a/ADDRESS`

All three parameters are required exactly once, in any order. Commands and prefixes are lowercase and
case-sensitive. Multiple spaces between parameters and surrounding spaces are accepted. Blank values are
invalid values, not missing parameters. Email, tags and other optional fields are not accepted by `add`.

Examples:

* `add n/Alex Tan p/81234567 a/21 Clementi Ave 3 #04-18`
* `add a/8 Jalan Besar p/+65 9234 5678 n/Nur Aisyah`

Values undergo Unicode NFKC normalization, trimming and repeated-space collapsing. Prohibited controls and
line breaks are rejected before whitespace processing. Name and address display case is preserved.

* **Name:** 1–70 Unicode code points after normalization, including at least one letter. Letters, combining
  marks, spaces, straight apostrophes (`'`), right curly apostrophes (`’`), hyphens and full stops are accepted.
  Digits are not accepted in names.
* **Phone:** eight Singapore digits starting with 6, 8 or 9. An optional `+65` prefix and spaces or hyphens
  between digit groups are accepted, e.g. `81234567`, `+65 8123 4567`, `+65-8123-4567`.
  Use a space or a single hyphen at each separator, not repeated hyphens or a mixture such as `8123 - 4567`.
  The stored/displayed form is `+6581234567`. International numbers are not supported.
* **Address:** 1–200 Unicode code points after normalization, including at least one letter or digit.
  Letters, combining marks, numbers, spaces and `# , . - / ' ( ) &` are accepted. For example, `12/3 Street`
  keeps its slash. A whitespace-delimited token of ASCII letters followed by `/` is reserved as a parameter
  boundary, not literal address text.

On success, the complete student is saved, appended to the full list, selected and shown in the details panel.
The message is `Student added: NAME.` Guardian phone, education level, subject, hourly rate and weekly slot
start unset and are shown as an em dash (`—`). This command does not set those fields.

The same normalized name (ignoring case) **and** phone is a duplicate, even if its address differs or the
existing student is hidden by a filter. A matching name with a different phone is accepted. A matching phone
with a different name is accepted with `Warning: Another student uses this phone number.` appended to success.

This is not fuzzy identity detection: spelling variants and nicknames are not matched. The same name with
a different phone is accepted without a near-name warning. Review the student list before adding a record
if you are unsure whether the student already exists. The name field is not guaranteed to support every
legal-name convention (for example, `/` and digits are not accepted). Phone support is limited to the
Singapore formats above; contacts requiring international or multiple numbers are outside current support.

F01 errors are reported in this order: unknown command; unexpected text/unknown parameter; missing/repeated
parameter (name, phone, address order); invalid name, phone, address; duplicate; save failure.

| Condition | Message |
| --- | --- |
| Unknown command | `Error: Unknown command. Type help to view available commands.` |
| Unexpected text before parameters | `Error: Unexpected text after command.` |
| Unknown parameter, e.g. `x/` | `Error: Unknown parameter: x/.` |
| Missing parameter | `Error: Missing required parameter: n/NAME.` (or `p/PHONE`, `a/ADDRESS`) |
| Repeated parameter | `Error: Parameter n/ may be specified only once.` (or `p/`, `a/`) |
| Invalid name | `Error: Invalid name. Use 1-70 letters with spaces, apostrophes, hyphens, or full stops.` |
| Invalid phone | `Error: Invalid phone. Use an 8-digit Singapore number beginning with 6, 8, or 9, optionally prefixed by +65.` |
| Invalid address | `Error: Invalid address. Use 1-200 letters, numbers, spaces, or common address punctuation.` |
| Duplicate | `Error: This student already exists with the same name and phone.` |
| Save failure | `Error: Changes could not be saved. No changes were made.` |

Every failed add leaves the file, student list, current filter, selected row and details unchanged. No partial
profile is created. If saving fails, check the data folder's permissions and available disk space before retrying.

### Listing students: `list`

Format: `list`

Reloads the complete saved register in insertion order, replaces any filtered results,
numbers rows from 1, and clears selection. If the data file does not exist, the current in-memory register is used.
The command does not save or sort records.

* With records: `Listed N students.`
* Empty register: `No students found.` and an empty-state panel.
* Extra input (including `list 1` or `list n/Amy`): `Error: List does not accept parameters.`
* Unreadable or invalid saved data: `Error: Student list could not be loaded.`

Invalid input or a load failure preserves the previous register, displayed results, and selection.
Unset optional student fields display as an em dash (—) in the details panel.

### Editing a student: `edit`

Edits an existing student's core contact details and legacy email/tags. Use the separate `guardian`, `level`,
`subject`, `rate` and `schedule` commands for those optional fields; `edit` preserves them.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]... `

* The index is from the currently displayed list, with no leading zeroes. Supply at least one field.
* Omitted fields keep their values. Name, phone and address use the same value limits as `add`.
  Each of `n/`, `p/`, `e/` and `a/` may appear at most once; blank values are invalid.
* Email is optional metadata supported by `edit`, not `add`. Use an address such as `alex.tan@example.com`.
  The local part accepts ASCII letters/digits separated by single `+`, `_`, `.` or `-` characters;
  it must start and end with a letter/digit. Domain labels use ASCII letters/digits with internal hyphens,
  separated by dots, and the final label must be at least two characters. Spaces are not accepted.
  A blank `e/` cannot clear an email. Email appears on the student card, not in the selected-student details panel.
* Tags contain ASCII letters/digits only, with no spaces or punctuation, and are case-sensitive.
  Repeated `t/` values replace the complete tag set; they do not append to existing tags. Duplicate tags collapse
  to one. Omit `t/` to preserve tags, or supply a single blank `t/` to clear all tags. Tags appear on the card.
* A real edit saves the full profile, preserves its ID and insertion position, shows the **full register**
  (clearing any search filter), and selects the edited student.
* Identical normalized values do not save or change the filter/selection. The command still reports
  `Student updated: NAME.`; there is no separate no-change message. A name or address capitalization change
  is a real edit. Phone formatting differences alone are not.
* Changing the name/phone to match both fields of another student is rejected, including students hidden by a filter.
  Sharing only a name or only a phone is accepted; unlike `add`, `edit` does not append a shared-phone warning.

On success, feedback is `Student updated: NAME.`, and the details panel shows the updated values. No fields supplied reports `At least one field to edit must be provided.`
A repeated single-value prefix reports `Multiple values specified for the following single-valued field(s): ...`.
A duplicate reports `Error: This student already exists with the same name and phone.` Invalid indices and save failures use the
standard messages shown in the other command sections.

`edit` rejects control characters and line breaks before trimming values. Unknown whitespace-delimited
ASCII-letter prefixes, such as `x/` or `wrong/`, report `Error: Unknown parameter: PREFIX.`;
extra text between the index and the first prefix reports `Error: Unexpected text after command.`
Unicode spaces are accepted between tokens. Embedded address slashes, such as `12/3 Street`, remain value text.

Structural errors are checked before repeated single-value prefixes, index syntax, and values in
name/phone/email/address/tag order. The row is then resolved and duplicate identity checked before saving.
An arbitrarily large positive index reports a missing row without overflowing. Failed edits preserve the
saved file, student list, filter, selection and details. A lone blank `t/` still clears tags; repeated `t/`
values must each contain a valid tag.

Examples:

*  `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st student to be `91234567` and `johndoe@example.com` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd student to be `Betsy Crower` and clears all existing tags.

### Setting a guardian contact (F04): `guardian`

Format: `guardian INDEX g/PHONE`

Sets or replaces one guardian phone for the student at `INDEX` in the **currently displayed list**.
The index is a positive whole number without leading zeroes: `1`, `2`, `3`, etc. After `find`, use the
filtered row number, not the student's position in the full register. The index must precede `g/PHONE`.
The lowercase command and prefix are case-sensitive. Supply `g/PHONE` exactly once; `g/` with no value
is an invalid phone, not a missing parameter.

Examples:

* `guardian 1 g/91234567`
* `guardian 3 g/+65 6777 8899`
* `guardian 1 g/+65-8123-4567`

Use eight Singapore digits starting with **6, 8 or 9**, optionally preceded by `+65`. Spaces or single
hyphens may separate digit groups. Values undergo Unicode NFKC normalization, trimming and repeated-space
collapse; controls and line breaks are not allowed. The saved and displayed form has no spaces, e.g.
`+6591234567`. International numbers, extensions, multiple guardian numbers, names and relationships are
not supported in this MVP. There is no clear-guardian command; a blank value does not remove the number.

Different students may share a guardian number (for example, siblings). A guardian number may also equal
the student's own phone number. Both cases are accepted without a duplicate warning.

After a real change, the complete profile is saved before the updated row is selected and its details are
refreshed. The student's ID, list position and other fields stay unchanged. Before assignment, the details
panel shows `Guardian phone: —`. A saved guardian contact remains available after exiting and reopening
the application in the same data folder. Equivalent input, such as `91234567` and `+65-9123-4567`, changes
nothing: no save occurs, and the current filter, selected row and details are preserved.

| Outcome | Message |
| --- | --- |
| First assignment | `Guardian contact set for NAME: PHONE.` |
| Replacement | `Guardian contact updated for NAME: OLD_PHONE -> NEW_PHONE.` |
| Equivalent number | `Guardian contact for NAME is already PHONE.` |
| Unexpected text before the parameter | `Error: Unexpected text after command.` |
| Unknown parameter, e.g. `p/` | `Error: Unknown parameter: p/.` |
| Missing parameter | `Error: Missing required parameter: g/PHONE.` |
| Repeated parameter | `Error: Parameter g/ may be specified only once.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid or blank phone | `Error: Invalid guardian phone. Use an 8-digit Singapore number beginning with 6, 8, or 9, optionally prefixed by +65.` |
| Row does not exist | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Unknown commands are reported first, then unexpected text/unknown parameters, missing/repeated parameters,
invalid index syntax, invalid guardian phone, missing row, and finally save failure. For example,
`guardian 999 g/bad` reports the invalid phone before looking up row 999. Errors leave the saved data,
student list, filter, selection and details unchanged. If saving fails, check folder permissions and disk
space before retrying; the old number (or unset value) is retained.

When manually editing JSON, use a canonical string such as `"guardianPhone": "+6591234567"`.
Missing or `null` guardian properties load as unset for compatibility with older data. Invalid guardian
values prevent the file from loading; back up the file before editing it and prefer the command above.

### Setting a student's education level (F05): `level`

Format: `level INDEX l/LEVEL`

Sets the one current education level of the student at the displayed, 1-based `INDEX`. Setting a new level
replaces the old one; levels never accumulate. Many students may share a level.

Examples:

* `level 1 l/Secondary 4`
* `level 2 l/JC2`
* `level 3 l/Primary 6`

Supply lowercase `l/` exactly once after the index. A blank `l/` is an invalid level, not a missing parameter.
Values use Unicode NFKC normalization, trimming and space collapsing; controls and line breaks in values
are rejected. There is no command to clear an existing level.

Accepted levels (case-insensitive, and the space between the word and the number is optional):

| Level | Canonical names | Also accepted |
| --- | --- | --- |
| Primary | `Primary 1` to `Primary 6` | `P1` to `P6` |
| Secondary | `Secondary 1` to `Secondary 5` | `Sec1` to `Sec5`, `S1` to `S5` |
| Junior college | `JC 1`, `JC 2` | `JC1`, `JC2` |

For example, `Sec4`, `sec 4`, `S4` and `Secondary 4` all mean `Secondary 4`. Only these forms are accepted: other
spellings such as `Sec Four`, and levels outside this table (preschool, polytechnic, ITE, IB, university), are not.
The canonical name is what is stored and shown, in the student list and in the details panel. A student with no level
shows an em dash (—). If the data file holds a spelling that `level` accepts (for example `sec4` after a manual edit),
it is shown as its canonical name; a stored value that is not a supported level is shown as an em dash and is treated
as unset by `level`, which then replaces it.

On success, the student is saved, selected and shown in the details panel. Setting a level equivalent to the current
one changes nothing: nothing is saved and the selection stays as it was.

| Outcome | Message |
| --- | --- |
| First level | `Education level set for NAME: LEVEL.` |
| Replacement | `Education level updated for NAME: OLD_LEVEL -> NEW_LEVEL.` |
| Same level | `Education level for NAME is already LEVEL.` |
| Unknown parameter | `Error: Unknown parameter: PREFIX.` |
| Repeated parameter | `Error: Parameter l/ may be specified only once.` |
| Unexpected text | `Error: Unexpected text after command.` |
| Missing parameter | `Error: Missing required parameter: l/LEVEL.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid level | `Error: Invalid level. Use Primary 1-6, Secondary 1-5, or JC 1-2.` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Unknown parameters are checked first, then repeated `l/`, unexpected pre-parameter text, and a missing parameter.
For example, `level 0 extra l/Sec4 x/` reports unknown parameter `x/`, while `level 0 extra l/Sec4 l/JC1`
reports repeated `l/`. Index syntax is checked before the level, and the level before the row is looked up,
so `level 99 l/Sec 6` reports the invalid level. A failed command
changes nothing: the saved file, the student list, the filter and the selection all stay as they were.

### Locating students by name: `find`

Finds students whose names contain any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* The search considers only names.
* Partial names match anywhere in the name: `Han` matches `Hans`, and `Anne` matches `Anne-Marie`.
  Names and keywords use Unicode NFKC normalization and locale-independent lowercase comparison.
  Full-width letters and composed/decomposed accents match; accents are not removed (`Jose` differs from `José`).
* Students matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.
* Results retain insertion order, are renumbered from 1, and clear selection. Feedback is `N student(s) listed!`.
  Each `find` searches the full in-memory register, replacing the previous query without saving or reloading
  the file; use `list` to reload/reset the view. Zero matches show `0 student(s) listed!` and `No students found.`.
* Supply at least one whitespace-separated keyword; `find` alone reports `Error: Missing required parameter: KEYWORD.`
  Unicode spaces are supported, as are existing tab/newline separators. Hidden format characters report
  `Error: Unexpected text after command.` Both errors leave the current view and selection unchanged.

Examples:

* `find John` returns `john` and `John Doe`
* With the initial sample data, `find alex david` returns `Alex Yeoh` and `David Li`, with `2 student(s) listed!`.

### Filtering students by level or subject: `filter`

Format: `filter [l/LEVEL] [s/SUBJECT]`

Supply at least one criterion, with each lowercase prefix at most once, in either order.
Level accepts the same canonical values and aliases as `level`, such as `p1`, `sec4`, `s4`, and `jc2`.
Subject matches the **entire label**, ignoring case and normalizing Unicode (NFKC) and repeated spaces:
`s/h2 math` matches `H2 Math`, while `s/Math` does not. Both criteria must match when supplied together (AND).
Students with an unset or unreadable requested field do not match.

* `filter l/sec4` lists Secondary 4 students.
* `filter s/H2 Math` lists students studying H2 Math.
* `filter l/jc2 s/H2 Math` lists only JC 2 students studying H2 Math.

Each filter starts from the full in-memory register and replaces the previous search/filter. Results retain
stored order and are renumbered from 1; selection clears. Feedback is `N student(s) listed!`; zero matches
show `0 student(s) listed!` and `No students found.`. Use `list` to reload and restore the full register.
Filtering neither changes nor saves student records and does not restrict the consolidated weekly schedule.
Use the displayed result row for subsequent indexed commands.

Unknown/repeated prefixes, bare text before criteria, blank/invalid values and control characters are rejected
without changing the view or selection. For example, `filter` reports
`Error: Supply at least one filter criterion: l/LEVEL or s/SUBJECT.`; `filter l/p1 l/p2` reports
`Error: Parameter l/ may be specified only once.`. Level and subject values use their existing validation rules.

### Deleting a student: `delete`

Format: `delete INDEX`

Deletes the complete record at the current displayed, 1-based index. For example,
`find Betsy` followed by `delete 1` deletes the first matching result.
The remaining rows are renumbered. Deleting the selected record clears selection;
otherwise the same surviving record stays selected.

Use exactly one positive whole number without signs, decimals, or leading zeroes.
Surrounding spaces and multiple spaces between the command and index are allowed.
There is no confirmation or undo.

| Outcome | Message |
| --- | --- |
| Deleted | `Student deleted: NAME.` |
| Missing index | `Error: Missing required parameter: INDEX.` |
| Multiple indices | `Error: Delete accepts exactly one index.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

The proposed register is saved before changing the displayed records. Failed deletion
preserves the profile, stored data, results, and selection.

### Setting a student's subject (F06): `subject`

Format: `subject INDEX s/SUBJECT`

Sets the one subject taught to the student at the displayed, 1-based `INDEX`. Setting a new subject replaces the
old one; a student never has more than one subject. Many students may share a subject.
After filtering (for example, with `find Alex`), use the row number in the filtered list.

Examples:

* `subject 1 s/O-Level Chemistry`
* `subject 2 s/H2 Math`
* `subject 3 s/Primary 6 English`
* `subject 1 s/English Language`
* `subject 1 s/Math/Science`
* `subject 1 s/Physics & Chemistry`

The subject is normalized before it is checked and stored: Unicode is normalized (NFKC), the text is trimmed and
repeated spaces are collapsed. It must then have:

* 2 to 50 characters (Unicode code points) after normalization, with at least one letter;
* only letters, digits, spaces and the symbols `&` `/` `-` `'` `(` `)`. Other symbols such as `@`, and control
  characters or line breaks, are rejected.

Slashes inside a subject are kept as text, e.g. `s/Math/Science`. The case you type is the case that is shown, in the
student list and in the details panel. A student with no subject shows an em dash (—).
Use lowercase `subject` and `s/`, and specify `s/` exactly once. A letter-led token ending in `/` after a
space is parsed as a parameter: `subject 1 s/Combined Math/Science` reports unknown parameter `Math/`.
Write `subject 1 s/Combined Math / Science` instead. `subject 1 s/` is an invalid empty subject;
this version has no command to clear a subject.

If a manually edited JSON file contains a valid but nonnormalized subject, both displays use its normalized
text and preserve capitalization. An unreadable subject shows an em dash and the next valid assignment uses
the first-subject message. Displaying data does not rewrite the file.

Two subjects are the same if they differ only in letter case or spacing, e.g. `H2 Math` and `h2   MATH`. Entering
the same subject again changes nothing: nothing is saved, the selection stays as it was, and the subject keeps the
display text it already had.

On a first assignment or replacement, the student is saved, selected and shown in the details panel.

| Outcome | Message |
| --- | --- |
| First subject | `Subject set for NAME: SUBJECT.` |
| Replacement | `Subject updated for NAME: OLD_SUBJECT -> NEW_SUBJECT.` |
| Same subject | `Subject for NAME is already SUBJECT.` |
| Unknown parameter | `Error: Unknown parameter: PREFIX.` |
| Repeated parameter | `Error: Parameter s/ may be specified only once.` |
| Unexpected text | `Error: Unexpected text after command.` |
| Missing parameter | `Error: Missing required parameter: s/SUBJECT.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid subject | `Error: Invalid subject. Use 2-50 letters, numbers, spaces, or the symbols & / - ' ( ).` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Structural errors take priority: unknown parameters, repeated `s/`, unexpected text before `s/`, then a missing
parameter. For example, `subject 0 extra s/@ x/` reports unknown parameter `x/`, while
`subject 0 extra s/ s/` reports repeated `s/`. Index syntax is checked next, before the subject. The subject is
checked before the row is looked up, so `subject 99 s/@` reports the invalid subject. A failed command changes
nothing: the saved file, the student list, the filter and the selection all stay as they were.

To check persistence, set a subject, close and reopen the app, then select the student. The subject should
appear unchanged on the card and in the details panel. Entering an equivalent value such as `h2   MATH`
after saving `H2 Math` reports no change and keeps `H2 Math` as the display value.

### Keeping a student's note (story 13): `note` and `clearnote`

Format: `note INDEX m/NOTE` and `clearnote INDEX`

Keeps one freeform note per student, such as `weak in calculus, short attention span`, so you can read it before
the next lesson. `note` sets the note, or replaces the current one. `clearnote` removes it. `INDEX` is the
**currently displayed**, 1-based index, so after `find Betsy` the index `1` is the first matching student.

Examples:
* `note 1 m/Weak in calculus; needs more practice on integration`
* `note 2 m/Short attention span - keep exercises under 10 minutes`
* `clearnote 1`

The note is everything after `m/`. It can hold ordinary punctuation and symbols, including slashes, so
`he/she`, `and/or`, `w/ parent` and `speed in m/s` are all fine. Only the text between the index and `m/` is
checked: anything other than the index there is an error.

* The note is normalized first: Unicode NFKC, trimmed, and repeated spaces collapsed. Compatibility characters
  are replaced by their plain forms, so `x²` is stored as `x2` and full-width letters become ordinary letters.
* It must have **1 to 500 characters** after that. Control characters, zero-width and other invisible format
  characters, and line breaks are rejected, because the note is a single line.
* Capital letters are kept as typed. Two notes are the same only if they match exactly, so `weak in calculus`
  replaces `Weak in calculus`.
* A student has at most one note. To remove it, use `clearnote`; `note 1 m/` with nothing after `m/` is an
  invalid note, not a clear.

The note appears as `Note: ...` in the selected-student details, wrapped over several lines if it is long (scroll
the details panel to read all of it). A student with no note shows an em dash (`—`). Setting or clearing a note
selects the student and refreshes the details; it does not change any other field. Deleting a student deletes
their note, and editing a student's contact details keeps it.

| Outcome | Message |
| --- | --- |
| Note set | `Note set for NAME.` |
| Note replaced | `Note updated for NAME.` |
| Same note again | `Note for NAME is already up to date.` |
| Note cleared | `Note cleared for NAME.` |
| `clearnote` on a student with no note | `NAME has no note to clear.` |
| Unexpected text before `m/`, or after the index in `clearnote` | `Error: Unexpected text after command.` |
| Another prefix before `m/`, e.g. `note 1 x/y m/text` | `Error: Unknown parameter: x/.` |
| Missing `m/`, e.g. `note 1` | `Error: Missing required parameter: m/NOTE.` |
| `clearnote` without an index | `Error: Missing required parameter: INDEX.` |
| Invalid or missing index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid note | `Error: Invalid note. Use 1-500 characters with no control characters or line breaks.` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Errors are reported in the order shown for `note`: unexpected text or an unknown prefix, then a missing `m/`,
then the index syntax, then the note, then whether the row exists. So `note 0 m/text` reports the invalid index,
while `note 99 m/` reports the invalid note. A failed command, including a failed save, changes nothing: the
saved file, the student list, the filter and the selection all stay as they were. Setting the same note again
or clearing an empty note saves nothing.

The note is saved with the student as `"academicNote": "..."` in the data file and survives a restart. Files
saved before notes existed load normally, with no note. If you edit the file by hand and the value is not a
text, `note` treats it as no note and replaces it, and `clearnote` removes it. Back up the file before editing.

### Setting a student's hourly rate (F07): `rate`

Format: `rate INDEX r/RATE`

Sets one hourly tuition rate in Singapore dollars (SGD) for the student at the **currently displayed**,
1-based `INDEX`. A new rate replaces the previous one. For example, `find Betsy` followed by `rate 1 r/80`
sets the first matching student's rate, even if that student is elsewhere in the full register.

Examples:

* `rate 1 r/80` sets S$80.00 per hour.
* `rate 1 r/80.5` replaces it with S$80.50 per hour.
* `rate 2 r/1000.00` sets the maximum rate for the second displayed student.

Supply exactly one lowercase `r/` parameter and a positive whole-number index without leading zeroes.
Separate the index and prefix with whitespace. Surrounding spaces and multiple spaces between tokens are accepted;
input uses the shared Unicode normalization (NFKC). Rates must be from **S$1.00 to S$1000.00 inclusive**,
with zero, one or two decimal places. They are stored exactly and shown with two decimal places and `S$`
in the student list card and selected-student details. Scroll the details panel if needed to see the rate.
An unset rate shows an em dash (`—`).

Use plain digits and an optional decimal point. Do not include a sign, currency symbol, commas, exponent,
leading zeroes or extra text. For example, `0`, `-40`, `1000.01`, `40.001`, `40.000`, `S$40`, `1,000`,
`1e2`, `040` and `forty` are invalid. Values are never rounded to fit. A bare `r/` is an invalid rate;
omitting `r/` entirely is a missing parameter. This command cannot clear an existing rate.

On a successful set or replacement, the saved student is selected and the details refresh. Student identity, contact details,
guardian, education level, subject and weekly slot are preserved. Equivalent amounts such as `80`, `80.0`
and `80.00` produce a no-change message, perform no save and preserve the current selection.

| Outcome | Message |
| --- | --- |
| First rate | `Hourly rate set for NAME: S$80.00.` |
| Replacement | `Hourly rate updated for NAME: S$80.00 -> S$80.50.` |
| Equivalent rate | `Hourly rate for NAME is already S$80.00.` |
| Unknown parameter, e.g. `x/` or uppercase `R/` | `Error: Unknown parameter: x/.` (or `R/.`) |
| Repeated prefix, e.g. `rate 1 r/40 r/50` | `Error: Parameter r/ may be specified only once.` |
| Extra text before the prefix, e.g. `rate 1 extra r/40` | `Error: Unexpected text after command.` |
| Missing prefix, e.g. `rate 1` | `Error: Missing required parameter: r/RATE.` |
| Invalid/missing index with a prefix, e.g. `rate 01 r/40` or `rate r/40` | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid rate | `Error: Invalid rate. Enter an SGD amount from 1.00 to 1000.00 with at most two decimal places and no currency symbol.` |
| No displayed row (including an empty list or oversized index) | `Error: No student exists at index INDEX.` |
| Save failure | `Error: Changes could not be saved. No changes were made.` |

When multiple mistakes occur, the shared parser checks unknown prefixes first, then repeated prefixes, then
unexpected text before a prefix. With no prefix, extra unprefixed text is unexpected text; otherwise it reports
the missing `r/RATE`. Index syntax is checked next, then the rate, then whether the displayed row exists.
For example, `rate 0 r/bad` reports the invalid index, while `rate 99 r/bad` reports the invalid rate.
`rate 1r/40` reports the missing parameter because whitespace is required before the prefix.

Invalid input and save failures preserve the file, student records, filter, selection and details.
If a save fails, check the data folder's permissions and free disk space before retrying.
Rates survive restart; existing JSON rates written as numbers or strings are accepted within the limits above,
and are saved as exact two-decimal strings. Missing or null rates remain unset. Back up data before manual edits.

### Recording a monthly payment status (story 11): `payment`

Format: `payment INDEX m/YYYY-MM s/pending|completed`

Records the student's manual payment status for one calendar month. `INDEX` targets the currently displayed,
1-based student list, so a preceding `find` changes which student an index selects. Both lowercase prefixes are
required exactly once and may appear in either order. Enter a real month as four year digits, a hyphen and a
two-digit month. Statuses are lowercase `pending` or `completed`.

Examples:

* `payment 1 m/2026-10 s/pending` records an explicitly pending payment for October 2026.
* `payment 1 s/completed m/2026-10` changes that month's status to completed.
* Repeating the completed command reports that the status is already completed and does not save or change selection.
* `find Casey` followed by `payment 1 m/2026-11 s/pending` targets the first displayed match.

A missing month entry means **unrecorded**; it is different from a recorded `pending` status. Each student can
have one status per month. Replacing a status affects only that month, and months and students are independent.
The command is manual status tracking only; it does not create invoices or process payments. Payment history is
stored with the student's UUID and survives edits and restart. Deleting the student deletes the attached history.
Older records without payment history load with none recorded. Malformed payment data remains untouched on load; a
payment command reports the affected student and tells you to correct or remove its `payments` entry before changing
or listing payment history.

On a real change, the student is selected and details refresh. An identical status is a successful no-change
result with no save; errors and save failures preserve the existing selection and data.

| Outcome | Message |
| --- | --- |
| First status | `Payment status set for NAME (PHONE) (YYYY-MM): pending.` |
| Replacement | `Payment status updated for NAME (PHONE) (YYYY-MM): pending -> completed.` |
| Same status | `Payment status for NAME (PHONE) (YYYY-MM) is already completed.` |
| Missing month prefix | `Error: Missing required parameter: m/MONTH.` |
| Missing status prefix | `Error: Missing required parameter: s/STATUS.` |
| Invalid month | `Error: Invalid month. Enter a valid month in YYYY-MM format.` |
| Invalid status | `Error: Invalid payment status. Use pending or completed.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| No displayed student | `Error: No student exists at index INDEX.` |
| Save failure | `Error: Changes could not be saved. No changes were made.` |

Unknown or repeated prefixes and unexpected text are reported before parameter values. The parser then checks index
syntax, month, status, and finally whether a student exists at that displayed index. For example,
`payment 99 m/2026-13 s/invalid` reports the invalid month before looking up student 99. A valid but missing
displayed index reports the no-student error after the month and status pass validation.

### Reviewing monthly payment statuses: `payments`

Format: `payments m/YYYY-MM [s/pending|completed]`

Shows recorded payments for the requested month in student insertion order. The command lists the full register even
after `find` or `filter`; it does not change the current selection or save data. Students with no recorded status
for that month are excluded, including students who have a status for another month. Add the optional status filter
to show only pending or completed records.

Each row includes the student's name and phone, the month, and its status, for example:

```text
Alex Tan (+6591234567) — 2026-10: pending
Casey Lim (+6581234567) — 2026-10: completed
```

If there are no recorded payments, or no rows match the optional status, the command displays
`No recorded payments for YYYY-MM.` The selected-student details panel shows payment months chronologically;
students with no history show an em dash.

### Scheduling a weekly lesson (F08): `schedule`

Sets one recurring weekly lesson for the student at an index in the **currently displayed list**.

Format: `schedule INDEX d/DAY t/TIME`

Examples:

* `schedule 1 d/Tuesday t/19:00`
* `schedule 2 d/Sat t/09:30`
* `schedule 2 t/09:30 d/sAT`

Supply one positive index without leading zeroes, exactly one `d/DAY`, and exactly one `t/TIME`.
The two prefixed parameters may appear in either order. Commands and prefixes are lowercase and case-sensitive.
Surrounding spaces and multiple spaces between tokens are accepted. Values use the shared Unicode normalization;
controls and line breaks are rejected.

Days accept Monday through Sunday, including weekends, or exactly `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun`.
Day names are case-insensitive and display in full. Time uses 24-hour `HH:mm` with exactly two digits in each part:
hours `00`–`23`, minutes `00`–`59`. For example, `09:00`, `18:30` and `23:59` are valid;
`9:00`, `6pm`, `18.30`, `24:00` and `12:60` are invalid.

An unset weekly slot appears as an em dash in student details. Setting or replacing a slot selects the student
and refreshes the details and weekly summary. Day and time are saved together; a failed command leaves the
complete previous slot, displayed list and selection unchanged. Equivalent normalized input preserves selection
and does not save again. Different students may share an identical slot.

The **Weekly schedule** summary shows all scheduled students, including students outside a filtered list.
It orders entries Monday–Sunday, then by time, then by student insertion order for ties. It does not reorder
the student register. Deleting a student also removes their entry from this summary and saved schedule.
This feature supports one slot per student; lesson durations, multiple slots and conflict detection are outside its scope.
Use `unschedule INDEX` to remove only the weekly slot; blank day/time values do not remove it.

Outcome | Status message
--- | ---
First slot | `Weekly lesson set for NAME: DAY TIME.`
Replacement | `Weekly lesson updated for NAME: OLD_DAY OLD_TIME -> NEW_DAY NEW_TIME.`
Equivalent slot | `Weekly lesson for NAME is already DAY TIME.`
Invalid day | `Error: Invalid day. Use Monday-Sunday or Mon-Sun.`
Invalid time | `Error: Invalid time. Use 24-hour HH:mm, for example 09:00 or 18:30.`
Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.`
No displayed student | `Error: No student exists at index INDEX.`
Save failure | `Error: Changes could not be saved. No changes were made.`

Structural errors are reported before values. For example, omitting `d/` reports
`Error: Missing required parameter: d/DAY.`, repeating it reports
`Error: Parameter d/ may be specified only once.`, and `x/value` reports
`Error: Unknown parameter: x/.` Extra unprefixed tokens report `Error: Unexpected text after command.`
Values are checked in order: index syntax, day, time, then whether the displayed row exists.

### Viewing a daily or weekly timetable: `timetable`

Use `timetable` for the full week or `timetable d/DAY` for one weekday in the existing schedule panel.
Examples: `timetable d/Mon`, `timetable d/sUNDAY`, then `timetable` to return to the week.
Day names and three-letter aliases use the same normalization as `schedule`. Prefix `d/` is lowercase
and may appear only once. Blank or invalid days report the existing invalid-day error; unknown prefixes,
repeated `d/`, unexpected leading text and control characters are rejected without changing the view.

Feedback is `Showing weekly timetable.` or `Showing timetable for Monday.` (using the chosen full day).
An empty view says `No weekly lessons scheduled.` or `No lessons scheduled for Monday.`.
The timetable includes students hidden by `find` or `filter`, orders Monday–Sunday then time, and retains insertion
order for equal slots. It does not reorder the numbered student list, change its filter/selection, reload
or save student data. Slot changes, student renaming and deletion update the current view immediately.
The selected day remains active until another timetable command; restarting opens the full week.
This is a recurring teaching timetable, not dated bookings, travel planning or conflict detection.

### Recording exam and test results (story 12): `assess`, `assessments` and `unassess`

Formats: `assess INDEX d/DATE t/TITLE g/GRADE`, `assessments INDEX` and `unassess INDEX i/RECORD`

Keeps a dated history of a student's exams and tests so you can see progress over time. `assess` adds one record,
`assessments` lists them, and `unassess` removes one, so a wrong entry can be corrected by removing it and adding
it again. `INDEX` is the **currently displayed**, 1-based index, so after `find Betsy` the index `1` is the first
matching student. Prefixes may come in any order, and each may appear once.

Examples:
- `assess 1 d/2026-10-01 t/Math Midterm g/85/100`
- `assess 1 g/A1 d/2026-12-05 t/Chemistry Final`
- `assessments 1`
- `unassess 1 i/2`

| Part | Rule |
| --- | --- |
| `DATE` | `yyyy-MM-dd`, zero-padded, and a real calendar date: `2026-10-31` is valid, `2026-02-30` and `2026-2-3` are not. It is not compared with today's date, so a past or future date is accepted. |
| `TITLE` | 1 to 60 characters: letters, digits, spaces and `& / ' ( ) - . , : #`. |
| `GRADE` | 1 to 20 characters: letters, digits, spaces and `/ . % + -`. Examples: `A1`, `B+`, `85/100`, `72.5%`, `Pass`. |
| `RECORD` | The record's number in the student's history, as shown by `assessments` and the details panel. |

Titles and grades are normalized first (Unicode NFKC, trimmed, repeated spaces collapsed) and are shown in the
case you typed. A grade is only text: TutorConnect never reads it as a number, compares two grades, or averages
them, so `A1`, `85/100` and `Grade 7` can sit side by side without implying that they are comparable. A title
may contain a slash, but a word of letters directly followed by `/` that comes after a space (for example
` Math/Science`) is read as a parameter, so write `Math / Science` or `Math-Science`.

**Order and numbering.** Records are ordered by date, oldest first. Records on the same date stay in the order
they were added. A record's number is its position in that order, from 1, so numbers can change after you add
or remove a record: check `assessments` before using `unassess`.

**Duplicates and limits.** A record with the same date and the same title as an existing one (ignoring capital
letters and repeated spaces) is a duplicate and is rejected, whatever its grade; remove the old record first to
change it. A student can have at most 100 records.

**Display.** The selected-student details show `Assessments (N):` followed by one numbered line per record, or
`Assessments: none recorded`. Long titles wrap and the panel scrolls, so a long history stays readable.
`assessments` also writes the numbered list in the result box and selects the student; it never changes any
data. Adding or removing a record selects the student and refreshes the details. Deleting a student deletes their
records, and editing a student's contact details keeps them.

| Outcome | Message |
| --- | --- |
| Record added | `Assessment added for NAME: #N DATE TITLE: GRADE.` |
| Record removed | `Assessment removed from NAME: DATE TITLE: GRADE.` |
| `assessments` | `Assessments for NAME:` and then one `N. DATE TITLE: GRADE` line per record |
| `assessments` with no records | `NAME has no assessment records.` |
| Unexpected text | `Error: Unexpected text after command.` |
| Unknown parameter | `Error: Unknown parameter: PREFIX.` |
| Missing parameter | `Error: Missing required parameter: d/DATE.` (or `t/TITLE`, `g/GRADE`, `i/RECORD`, `INDEX`) |
| Repeated parameter | `Error: Parameter d/ may be specified only once.` (or `t/`, `g/`, `i/`) |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid date | `Error: Invalid date. Use a real date in yyyy-MM-dd format, e.g. 2026-10-31.` |
| Invalid title | `Error: Invalid title. Use 1-60 letters, numbers, spaces, or the symbols & / ' ( ) - . , : #.` |
| Invalid grade | `Error: Invalid grade. Use 1-20 letters, numbers, spaces, or the symbols / . % + -.` |
| Invalid record number | `Error: Invalid record number. Enter a positive whole number without leading zeroes.` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Record number does not exist | `Error: No assessment record RECORD for NAME.` |
| Duplicate | `Error: NAME already has an assessment "TITLE" on DATE. Remove it first to change it.` |
| Too many records | `Error: A student can have at most 100 assessment records.` |
| Unreadable stored history | `Error: The stored assessment history for NAME could not be read. Correct or remove the "assessments" entry in the data file.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Errors are reported in the order shown: unexpected text or an unknown parameter, then a missing or repeated
parameter, then the index syntax, then the date, the title and the grade in that order, then whether the row
exists, whether the stored history can be read, and finally the duplicate, limit or record number. So
`assess 99 d/bad t/Quiz g/A` reports the invalid date. A failed command, including a failed save, changes nothing:
the saved file, the student list, the filter and the selection all stay as they were.

**Storage.** The history is saved with the student under `"assessments"` as a list of objects with a `"date"`, a
`"title"` and a `"grade"`, in display order, and it survives a restart. A student with no records has no
`"assessments"` entry, and files saved before this feature load normally. If you edit the file by hand and the
list can no longer be read (for example an impossible date, a duplicate, or an extra property), the commands
report it and leave the file untouched rather than overwrite your data: correct or remove the entry, then
restart. Back up the file before editing.
### Clearing all entries: `clear`

Permanently removes **all students and their optional fields**, including students hidden by a filter and their
weekly-schedule entries. There is no confirmation or undo. Back up the data file first if you may need it again.

Format: `clear`

Normal success feedback is `Address book has been cleared!`. A save failure reports
`Error: Changes could not be saved. No changes were made.`

Successful clearing also clears the selected row and details. A failed save preserves the register,
filter, selection and details. You do not need to run `list` before `clear`.

### Removing a weekly lesson: `unschedule`

Format: `unschedule INDEX`. For example, `unschedule 1` removes the weekly slot of the first student
in the currently displayed list, including after `find`. It does not delete the student or change
their name, contacts, other optional fields, internal identity or position in the register.

Success reports `Weekly lesson removed for NAME: DAY TIME.`, selects the student, clears their slot
to an em dash and removes their schedule entry immediately. The removal persists across restart.
If no slot exists, `No weekly lesson is scheduled for NAME.` is a successful no-change result:
no save and no selection change. Different students sharing a slot remain unaffected.

Supply one positive displayed index without leading zeroes. Missing, zero, negative or malformed
indices report `Error: Invalid index. Enter a positive whole number without leading zeroes.`
An oversized or out-of-range valid index reports `Error: No student exists at index INDEX.`
Unknown prefixes report `Error: Unknown parameter: PREFIX.`; extra bare text and controls report
`Error: Unexpected text after command.` Structure is checked before index syntax and existence.
On a save failure, `Error: Changes could not be saved. No changes were made.` preserves the previous
complete slot, file bytes, student list, filter, selection and details. Retry after resolving the
storage problem. Use `schedule` to assign a slot again; there is no undo history.

### Exiting the program: `exit`

Closes the application and Help window. Window size/position are saved to `preferences.json`.
Student data is saved by successful data-changing commands; `exit` is not a separate save command.

Format: `exit`

### Saving the data

TutorConnect automatically saves a complete updated student register after a successful data-changing
command. Read-only commands, rejected input, and successful no-change updates do not write the file.
`list` reloads the file without saving it. A failed save leaves stored data, the current list, and selection
unchanged and reports `Error: Changes could not be saved. No changes were made.`

### Editing the data file

TutorConnect stores the register in `data/addressbook.json` relative to the working directory from which
you start the app. If you start it in the folder containing the JAR, this is that folder's `data` directory.
The window's status bar shows the data-file path. Advanced users can edit this human-readable JSON file.

Close the app and make a backup before editing. Preserve the outer `persons` array, its order, and existing
student `id` values. Each record needs `name`, `phone`, and `address`; existing field validation still applies.
An older/newly written record without an ID receives a stable migration ID when loaded. IDs must be unique.
Do not replace a record or tag entry with `null`; use an empty `persons` array for an empty register.

For example, this complete file is valid:

```json
{
  "persons": [
    {
      "name": "Alex Tan",
      "phone": "+6581234567",
      "address": "21 Clementi Ave 3 #04-18"
    }
  ]
}
```

Optional properties can be missing or `null` for unset values. Use the documented formats for known
fields: `guardianPhone` as a canonical phone string (e.g. `+6591234567`), `educationLevel` as a canonical
level string (e.g. `Secondary 4`), `subject` as a valid subject string, `hourlyRate` as a two-decimal string
(e.g. `40.00`), and `weeklySlot` as an object such as `{"day":"MONDAY","time":"15:30"}`. Both slot components
are required when a slot is set. Unrelated optional properties are preserved when other fields change.
Prefer the application's field commands if unsure about a value's restrictions.

<box type="warning" seamless>

**Caution:**
Invalid edits can cause data loss. If the file cannot be loaded at startup, the app starts with an empty
register and shows a warning in the result box. It first copies the unreadable file beside it as
`addressbook.json.unreadable-YYYYMMDD-HHMMSS.bak`, but does not repair it. The next successful data-changing
command replaces `data/addressbook.json`, so close the app, fix the file or restore that copy (rename it back to
`addressbook.json`), and relaunch before making changes.<br>
If `list` cannot reload the file while the app is running, it reports a load error and preserves the current
in-memory list, filter, selection, and file bytes. Restore or correct the file before retrying. This is not
automatic recovery of corrupted data. Only edit values you understand and verify the loaded result.
</box>

### Features not included in this release

TutorConnect does not provide automatic backups, archiving, or undo. These capabilities are not promised
for a future release. With the app closed, copy `data/addressbook.json` to a safe location yourself.

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Close the app on both computers. Copy the original `data/addressbook.json` into the `data` folder of
the new TutorConnect working directory, creating that folder if needed. Back up any existing destination
file before replacing it, then start the app from that working directory. `preferences.json` is optional;
omitting it uses default window settings and avoids copying off-screen coordinates between computers.

**Q**: How do I clear a search or filter?<br>
**A**: Run `list`. It shows the complete student register in insertion order and clears the current selection.
Afterward, command indices refer to the full displayed list again.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.
3. **On macOS, fullscreen mode can affect secondary dialogs such as Help.** If a dialog behaves unexpectedly, leave fullscreen mode and open it again.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add student** | `add n/NAME p/PHONE a/ADDRESS` <br> e.g., `add n/James Ho p/81224444 a/123, Clementi Rd, 1234665`
**Clear**  | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... `<br> e.g.,`edit 2 n/James Lee e/jameslee@example.com`
**Set education level** | `level INDEX l/LEVEL`<br> e.g., `level 1 l/Secondary 4`
**Set guardian contact** | `guardian INDEX g/PHONE`<br> e.g., `guardian 1 g/+65 9123 4567`
**Find**   | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James Jake`
**Filter** | `filter [l/LEVEL] [s/SUBJECT]`<br> e.g., `filter l/sec4 s/Math`
**List**   | `list`
**Schedule** | `schedule INDEX d/DAY t/TIME`<br> e.g., `schedule 1 d/Tuesday t/19:00`
**Remove weekly lesson** | `unschedule INDEX`<br> e.g., `unschedule 1`
**View timetable** | `timetable [d/DAY]`<br> e.g., `timetable d/Mon`
**Add assessment** | `assess INDEX d/DATE t/TITLE g/GRADE`<br> e.g., `assess 1 d/2026-10-01 t/Math Midterm g/85/100`
**List assessments** | `assessments INDEX`<br> e.g., `assessments 1`
**Remove assessment** | `unassess INDEX i/RECORD`<br> e.g., `unassess 1 i/2`
**Set subject** | `subject INDEX s/SUBJECT`<br> e.g., `subject 1 s/H2 Math`
**Set hourly rate** | `rate INDEX r/RATE`<br> e.g., `rate 1 r/80.50`
**Record monthly payment status** | `payment INDEX m/YYYY-MM s/pending|completed`<br> e.g., `payment 1 m/2026-10 s/pending`
**List monthly payment statuses** | `payments m/YYYY-MM [s/pending|completed]`<br> e.g., `payments m/2026-10 s/completed`
**Set note** | `note INDEX m/NOTE`<br> e.g., `note 1 m/Weak in calculus`
**Clear note** | `clearnote INDEX`<br> e.g., `clearnote 1`
**Help**   | `help`
**Exit**   | `exit`
