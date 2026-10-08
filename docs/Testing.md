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
Each owner fixes and tests their own feature. The integrated F08 review branch is `f08-04-ui-schedule`;
temporary dependency branches are review bases and must not be merged as product branches.

Use JDK 25 as configured in `build.gradle`. On a desktop or configured virtual display, run:

```sh
env F01_UI_TESTS=true F08_UI_TESTS=true ./gradlew check shadowJar --offline --no-daemon
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
  unchanged details/summary on errors or no-change, and persisted deletion.

For an additional manual smoke run in a fresh data directory, add Zoe and Amy using
`add n/Zoe p/81234567 a/21 Clementi Ave` and `add n/Amy p/81234567 a/22 Clementi Ave`.
Schedule Zoe on Sunday 23:59 and Amy on Monday 09:00. The register stays Zoe/Amy while the summary is Amy/Zoe.
Reschedule both to Tuesday 19:00; the tie becomes Zoe/Amy. Repeat Zoe's slot with `d/tUE`; selection stays on
Amy and there is no save. Try invalid values, restart, and delete each student to check persistence and removal.

Snapshot dependencies: F01 foundation `37032dbf`, validation `e2b6bfff`, atomic persistence `025b7926`,
UI `b8b3497b`; F03 `VisibleIndex` `407c526a`; F08 type `759f6ee0`, command `28582e1f`, persistence `40272a08`,
and UI `9889f57d` in PR #86. On 8 October 2026, the desktop run on JDK 25.0.3 passed **302 tests,
zero failures, errors or skips**, including both F01 and F08 real JavaFX tests. Main/test Checkstyle,
the Shadow JAR build, repository text checks and `git diff --check` also passed.

Feature | Acceptance boundary in this snapshot
--- | ---
F01 | Add, canonical phones/IDs, duplicates/shared phones, persistence and desktop selection/details are exercised.
F02 | Ordinary list/delete and attached-slot removal are exercised; final exact conformance from PRs #81/#82 awaits integration with this snapshot.
F03 | The owner's shared `VisibleIndex` tests and F08 displayed-index use are exercised; final list behavior in #84 awaits integration.
F04–F07 | Field commands/types are absent from this snapshot. Partial F04 types in #85 are not integrated; generic field preservation does not certify their validation or commands.
F08 | Complete weekly scheduling, summary/details, reload/deletion and atomic failure behavior are exercised.

The competing shared-index PR #64 produced eight stale legacy parser/index expectations in an earlier combined
run. This snapshot uses F03's #68 interface. Consolidating shared helpers and accepting remaining owner features
must precede declaring the complete F01–F08 release accepted.

A read-only merge audit of F02/F03's final owner stack (`f9e94e96`, #82) against the tested F08 UI stack
reported conflicts in `LogicManager`, `AddressBookParser`, `JsonAddressBookStorage`, `MainWindow`,
`PersonCard` and `PersonListPanel`. The owner must reconcile list/delete with F01's staged execution,
atomic writer and selected-UUID APIs before the combined release run. These conflicts are tracked in
[issue #89](https://github.com/AY2627S1-CS2103T-F08-3/tp/issues/89), alongside #79;
the existing 302-test result applies to the explicitly recorded F01/F03-helper/F08 snapshot.
