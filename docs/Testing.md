---
  layout: default.md
  title: "Testing guide"
  pageNav: 3
---

# Testing guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Running tests

You can run tests in two ways.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose `Run 'ABC'`
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)

<box type="info" seamless>

**Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</box>

--------------------------------------------------------------------------------------------------------------------

## Types of tests

This project has three types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `seedu.address.commons.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `seedu.address.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `seedu.address.logic.LogicManagerTest`

## F01–F08 acceptance

Coordination is tracked in [acceptance issue #79](https://github.com/AY2627S1-CS2103T-F08-3/tp/issues/79).
Each owner remains responsible for their feature's tests and fixes. The final integrated acceptance snapshot
is `f08-04-ui-schedule` at `93ad5cf0`, combining master `7b6cc481`, scheduling integration `9f7146e6`
and persistence integration `324be3a6`. The review PRs are retargeted to master in order;
temporary dependency branches are review bases and must not be merged as product branches.

Use JDK 25 as configured in `build.gradle`. On a desktop or configured virtual display, run:

```sh
env F01_UI_TESTS=true F08_UI_TESTS=true ./gradlew check shadowJar coverage --offline --no-daemon
sh .github/run-checks.sh
git diff --check
```

Omit `--offline` if dependencies are not cached. Without the environment flags, desktop UI tests are skipped;
that run alone does not certify details/selection/summary behavior.

The F08 automated acceptance includes:

* `WeeklySlotTest`: all seven days and exact aliases in all cases, Unicode normalization, strict valid time
  boundaries, invalid formats/ranges/controls, required components and minute precision.
* `ScheduleCommandParserTest`/`ScheduleCommandTest`: both prefix orders, structural/value precedence,
  missing/repeated/unknown parameters, invalid/oversized/displayed indexes, set/replace/no-change and overlaps.
* `WeeklySlotFieldTest`/`WeeklySlotPersistenceTest`: complete canonical pairs, missing/null legacy slots,
  invalid partial records, all-weekday reloads, stable identity and unrelated optional data.
* `WeeklySchedulingIntegrationTest`: save-before-publish, byte/object/filter/selection preservation on
  partial-write and atomic-move failures, both unset or both old components, no-save normalized input,
  filtered overlaps and removal of attached slots by delete.
* `WeeklyScheduleTest`/`WeeklyScheduleUiTest`: independent weekday/time ordering, insertion-order ties,
  immediate replacement/deletion refresh, em-dash/full-name formatting, actual command-box status/selection,
  unchanged details/summary on errors or no-change, and persisted deletion. The real window test also runs
  guardian, level, subject and rate updates on a scheduled student, checks all details, reloads through `list`,
  and verifies slot replacement preserves every unrelated optional field.

For an additional manual smoke run in a fresh data directory, add Zoe and Amy using
`add n/Zoe p/81234567 a/21 Clementi Ave` and `add n/Amy p/81234567 a/22 Clementi Ave`.
Schedule Zoe on Sunday 23:59 and Amy on Monday 09:00. The register stays Zoe/Amy while the summary is Amy/Zoe.
Reschedule both to Tuesday 19:00; the tie becomes Zoe/Amy. Repeat Zoe's slot with `d/tUE`; selection stays on
Amy and there is no save. Try invalid values, restart, and delete each student to check persistence and removal.

On 8 October 2026, the combined desktop run on JDK 25.0.3 passed **397 tests, zero failures, errors or skips**,
including both F01 and F08 real JavaFX tests. Main/test Checkstyle, the Shadow JAR build, coverage generation,
repository text checks and `git diff --check` also passed. The earlier 302-test F01/F03-helper/F08 snapshot
is superseded by this combined run, which includes the merged list/delete and all optional field commands.

Linux CI runs both desktop test classes under Xvfb with software rendering and uploads their coverage;
macOS and Windows CI run the remaining checks without desktop opt-in. Local desktop flags are required
to reproduce the complete no-skip acceptance result.

Feature | Automated acceptance in the integrated snapshot
--- | ---
F01 | Add, required-field normalization, canonical phones/IDs, duplicate/shared-phone handling, persistence, atomic rollback and desktop selection/details.
F02/F03 | Final list/delete and shared index tests, strict messages, arbitrary-size displayed indexes, reload/load failure, insertion order, selection/renumbering, attached-slot removal and atomic delete failure.
F04/F07 | Guardian/rate parsing and execution, typed canonical storage, unrelated-field preservation, reload, no-change, filtered/oversized indexes and save rollback; real contact details in the window.
F05/F06 | Level/subject owner tests for aliases/normalization, validation/precedence, single-field replacement, identity/order, reload/no-change/rollback and filtered/oversized indexes; real details/cards alongside weekly scheduling.
F08 | All weekday/alias and time boundaries, command precedence, complete set/replace/no-change, accepted overlaps, independent summary order, details/selection, reload/deletion and atomic failure preservation.

The earlier list/delete merge conflicts in [issue #89](https://github.com/AY2627S1-CS2103T-F08-3/tp/issues/89)
are reconciled by master repair #92 and the F08 integration commits above. All commands retain F01's staged
execution and atomic writer, list loads without saving, and selection uses stable UUIDs. Cross-feature fixtures
now use the complete typed weekly-slot codec rather than placeholder strings. The UI merge retains subject/level
cards and the latest owner regressions while adding the separately sorted summary and publication guard.

This records the automated combined acceptance run. Each feature owner retains responsibility for additional
feature-specific acceptance or product sign-off; the run does not replace their documented requirements.
