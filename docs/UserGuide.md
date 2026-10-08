---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# AB-3 User Guide

AddressBook Level 3 (AB3) is a **desktop application for managing contacts, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). If you type quickly, AB3 can help you manage contacts faster than traditional GUI applications.

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from [here](https://github.com/se-edu/addressbook-level3/releases).

1. Copy the file to the folder you want to use as the _home folder_ for your AddressBook.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar addressbook.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all contacts.

   * `add n/John Doe p/98765432 a/John street, block 123, #01-01` : Adds a student named `John Doe`.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `clear` : Deletes all contacts.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[t/TAG]... ` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

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

* **Name:** 1–70 Unicode characters after normalization, including at least one letter. Letters, combining
  marks, spaces, apostrophes, hyphens and full stops are accepted. Digits are not accepted in names.
* **Phone:** eight Singapore digits starting with 6, 8 or 9. An optional `+65` prefix and spaces or hyphens
  between digit groups are accepted, e.g. `81234567`, `+65 8123 4567`, `+65-8123-4567`.
  The stored/displayed form is `+6581234567`. International numbers are not supported.
* **Address:** 1–200 Unicode characters after normalization, including at least one letter or digit.
  Letters, combining marks, digits, spaces and `# , . - / ' ( ) &` are accepted. For example, `12/3 Street`
  keeps its slash. A whitespace-delimited token of ASCII letters followed by `/` is reserved as a parameter
  boundary, not literal address text.

On success, the complete student is saved, appended to the full list, selected and shown in the details panel.
The message is `Student added: NAME.` Guardian phone, education level, subject, hourly rate and weekly slot
start unset and are shown as an em dash (`—`). This command does not set those fields.

The same normalized name (ignoring case) **and** phone is a duplicate, even if its address differs or the
existing student is hidden by a filter. A matching name with a different phone is accepted. A matching phone
with a different name is accepted with `Warning: Another student uses this phone number.` appended to success.

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
numbers rows from 1, and clears selection. Before the first save, the in-memory register is used.
The command does not save or sort records.

* With records: `Listed N students.`
* Empty register: `No students found.` and an empty-state panel.
* Extra input (including `list 1` or `list n/Amy`): `Error: List does not accept parameters.`
* Unreadable or invalid saved data: `Error: Student list could not be loaded.`

Invalid input or a load failure preserves the previous register, displayed results, and selection.
Unset optional student fields display as an em dash (—) in the details panel.

### Editing a person: `edit`

Edits an existing person in the address book.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]... `

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, ...
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.

Examples:
*  `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@example.com` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Setting a student's education level (F05): `level`

Format: `level INDEX l/LEVEL`

Sets the one current education level of the student at the displayed, 1-based `INDEX`. Setting a new level
replaces the old one; levels never accumulate. Many students may share a level.

Examples:
* `level 1 l/Secondary 4`
* `level 2 l/JC2`
* `level 3 l/Primary 6`

Accepted levels (case-insensitive, and the space between the word and the number is optional):

| Level | Canonical names | Also accepted |
| --- | --- | --- |
| Primary | `Primary 1` to `Primary 6` | `P1` to `P6` |
| Secondary | `Secondary 1` to `Secondary 5` | `Sec1` to `Sec5`, `S1` to `S5` |
| Junior college | `JC 1`, `JC 2` | `JC1`, `JC2` |

For example, `Sec4`, `sec 4`, `S4` and `Secondary 4` all mean `Secondary 4`. Only these forms are accepted: other
spellings such as `Sec Four`, and levels outside this table (preschool, polytechnic, ITE, IB, university), are not.
The canonical name is what is stored and shown, in the student list and in the details panel. A student with no level
shows an em dash (—).

On success, the student is saved, selected and shown in the details panel. Setting a level equivalent to the current
one changes nothing: nothing is saved and the selection stays as it was.

| Outcome | Message |
| --- | --- |
| First level | `Education level set for NAME: LEVEL.` |
| Replacement | `Education level updated for NAME: OLD_LEVEL -> NEW_LEVEL.` |
| Same level | `Education level for NAME is already LEVEL.` |
| Unexpected text | `Error: Unexpected text after command.` |
| Unknown parameter | `Error: Unknown parameter: PREFIX.` |
| Missing parameter | `Error: Missing required parameter: l/LEVEL.` |
| Repeated parameter | `Error: Parameter l/ may be specified only once.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid level | `Error: Invalid level. Use Primary 1-6, Secondary 1-5, or JC 1-2.` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Errors are reported in the order shown above, and the index syntax is checked before the level. The level is
checked before the row is looked up, so `level 99 l/Sec 6` reports the invalid level. A failed command
changes nothing: the saved file, the student list, the filter and the selection all stay as they were.

### Locating persons by name: `find`

Finds persons whose names contain any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* The search considers only names.
* Only full words match; for example, `Han` does not match `Hans`.
* Persons matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.

Examples:
* `find John` returns `john` and `John Doe`
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

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

Examples:
* `subject 1 s/O-Level Chemistry`
* `subject 2 s/H2 Math`
* `subject 3 s/Primary 6 English`

The subject is normalized before it is checked and stored: Unicode is normalized (NFKC), the text is trimmed and
repeated spaces are collapsed. It must then have:

* 2 to 50 characters, with at least one letter;
* only letters, digits, spaces and the symbols `&` `/` `-` `'` `(` `)`. Other symbols such as `@`, and control
  characters or line breaks, are rejected.

Slashes inside a subject are kept as text, e.g. `s/Math/Science`. The case you type is the case that is shown, in the
student list and in the details panel. A student with no subject shows an em dash (—).

Two subjects are the same if they differ only in letter case or spacing, e.g. `H2 Math` and `h2   MATH`. Entering
the same subject again changes nothing: nothing is saved, the selection stays as it was, and the subject keeps the
display text it already had.

On success, the student is saved, selected and shown in the details panel.

| Outcome | Message |
| --- | --- |
| First subject | `Subject set for NAME: SUBJECT.` |
| Replacement | `Subject updated for NAME: OLD_SUBJECT -> NEW_SUBJECT.` |
| Same subject | `Subject for NAME is already SUBJECT.` |
| Unexpected text | `Error: Unexpected text after command.` |
| Unknown parameter | `Error: Unknown parameter: PREFIX.` |
| Missing parameter | `Error: Missing required parameter: s/SUBJECT.` |
| Repeated parameter | `Error: Parameter s/ may be specified only once.` |
| Invalid index syntax | `Error: Invalid index. Enter a positive whole number without leading zeroes.` |
| Invalid subject | `Error: Invalid subject. Use 2-50 letters, numbers, spaces, or the symbols & / - ' ( ).` |
| Row does not exist (including oversized integers) | `Error: No student exists at index INDEX.` |
| Save failed | `Error: Changes could not be saved. No changes were made.` |

Errors are reported in the order shown above, and the index syntax is checked before the subject. The subject is
checked before the row is looked up, so `subject 99 s/@` reports the invalid subject. A failed command changes
nothing: the saved file, the student list, the filter and the selection all stay as they were.

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

### Clearing all entries: `clear`

Clears all entries from the address book.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

AddressBook automatically saves data after every command. You do not need to save manually.

### Editing the data file

AddressBook data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

<box type="warning" seamless>

**Caution:**
If your changes make the data file invalid, AddressBook starts with an empty address book at the next run. The invalid file remains on disk until you run a command (AddressBook saves after every command). Still, we recommend backing up the file before editing it.<br>
Furthermore, certain edits can cause the AddressBook to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</box>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with the data file from your previous AddressBook home folder.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add student** | `add n/NAME p/PHONE a/ADDRESS` <br> e.g., `add n/James Ho p/81224444 a/123, Clementi Rd, 1234665`
**Clear**  | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... `<br> e.g.,`edit 2 n/James Lee e/jameslee@example.com`
**Set education level** | `level INDEX l/LEVEL`<br> e.g., `level 1 l/Secondary 4`
**Find**   | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James Jake`
**List**   | `list`
**Schedule** | `schedule INDEX d/DAY t/TIME`<br> e.g., `schedule 1 d/Tuesday t/19:00`
**Set subject** | `subject INDEX s/SUBJECT`<br> e.g., `subject 1 s/H2 Math`
**Help**   | `help`
