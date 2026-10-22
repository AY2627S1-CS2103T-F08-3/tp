---
  layout: default.md
  title: "Project minutes and v1.3 retrospective"
  pageNav: 3
---

# Project minutes and v1.3 retrospective

Prepared on 22 October 2026 (Singapore time) to supplement the
[shared project notes](https://docs.google.com/document/d/16jVZgsgFMVfAl_6DctChEAP5Lj49X11AKTatE1yRDP4/edit).
The existing notes contain meetings dated 3, 10 and 17 September. This supplement covers the missing
teaching Weeks 7–10 and a process-focused v1.3 postmortem.

**Record status: reconstructed draft for team confirmation.** Course pages establish the required work;
dated PRs and releases establish recorded project activity. They do not establish that a meeting took
place, who attended, or what the team said or agreed. Actual meeting dates, attendance and decisions
remain unconfirmed. Proposed actions below must not be treated as completed or agreed commitments.
After the team reviews the draft, record its corrections and confirmation date and copy the confirmed
minutes into the shared project notes for the tutor. This repository copy alone does not establish
completion of the required team discussion.

## Coverage and dates

| Period | Record |
| --- | --- |
| Week 6, 14–20 September | Existing meeting of 17 September; retain the original entry |
| 21–27 September | Recess gap between teaching Weeks 6 and 7; no meeting is inferred |
| Week 7, 28 September–4 October | Draft below: workflow practice and v1.1 documentation |
| Week 8, 5–11 October | Draft below: first functional increment, v1.2 |
| Week 9, 12–18 October | Draft below: MVP delivery, v1.3 |
| Week 10, 19–25 October | Draft below: evidence through 22 October only; v1.3 retrospective and v1.4 |

The course's [Week 7 page](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week7/project.html)
starts on 28 September. Do not create an extra numbered teaching week for 21–27 September.
The known team roster is Eugene, Kiefer, Anton, Jimi and Pratiksha; this is not an attendance list.

## Week 7 — Workflow practice and documentation

**Meeting date/time and attendees:** to be confirmed.

**Agenda reconstructed from the course requirements:** practise issue/branch/PR collaboration, adapt the
README and website, and document user stories, glossary, non-functional requirements and use cases.
Reference: [Week 7 project requirements](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week7/project.html).

**Recorded progress:**

- Individual profile/photo PRs merged on 1 October, including Jimi's
  [photo #9](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/9) and
  [profile #22](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/22).
- The team added [user stories #44](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/44),
  [glossary #45](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/45),
  [non-functional requirements #47](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/47) and
  [use cases #48](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/48).
- [PR #43](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/43) corrected a README newline that broke CI.
  [Website adaptation #49](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/49) merged on 2 October.

**Discussion prompts and proposed follow-up:**

Confirm whether everyone could independently create an assigned issue, work on a separate fork branch,
review a teammate's PR and resolve conflicts. The newline repair is evidence that documentation changes
also need repository checks. Before functional implementation, have each feature owner identify the
shared model/parser/storage interfaces they will touch. Assign a reviewer before implementation starts.
Actual meeting decisions and acceptance of these actions remain to be recorded.

## Week 8 — First functional increment

**Meeting date/time and attendees:** to be confirmed.

**Agenda reconstructed from the course requirements:** divide the minimum viable features among members
and deliver a first increment through small PRs. The course did not require a new product release at the
end of this iteration. Reference:
[Week 8 project requirements](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week8/project.html).

**Recorded progress:**

- The student foundation and atomic persistence arrived through
  [#57](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/57) and
  [#63](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/63).
- Jimi's displayed-index, delete and list work merged through
  [#68](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/68),
  [#81](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/81),
  [#82](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/82) and
  [#84](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/84).
- Guardian, education-level, subject and scheduling increments also merged on 8 October; representative
  PRs are [#91](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/91),
  [#95](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/95),
  [#96](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/96) and
  [#86](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/86).
- [Integration repair #92](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/92) reconciled overlapping
  transaction implementations, index handling and field/UUID preservation. This is a concrete example
  of coordination risk around shared code, not evidence that any individual failed to contribute.

**Discussion prompts and proposed follow-up:**

Review how separate feature branches interacted at merge time. Document one agreed save-before-publish
contract and require cross-owner review for shared model or storage changes. Maintain a visible dependency
order so UI and command PRs do not assume infrastructure that has not landed. Before the MVP release,
test the combined branch, including reload, failed saves and commands on filtered rows.

## Week 9 — MVP delivery, v1.3

**Meeting date/time and attendees:** to be confirmed.

**Agenda reconstructed from the course requirements:** plan remaining MVP tasks, dependencies and owners
using milestone issues, review each other's PRs, and deliver a working release by 15 October, 23:59.
Reference: [Week 9 project requirements](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week9/project.html).

**Recorded progress and outcome:**

- Acceptance work covered field preservation, persistence and displays, including
  [level preservation #103](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/103),
  [rate rollback #116](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/116) and
  [subject restart compatibility #124](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/124).
- Jimi's [schedule acceptance #108](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/108) and
  [desktop evidence/release handoff #115](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/115)
  provided reproducible validation and release material.
- [The v1.3 release](https://github.com/AY2627S1-CS2103T-F08-3/tp/releases/tag/v1.3) was published on
  15 October at 17:05 Singapore time. Its release notes report 467 tests with no failures, errors or
  skips, all three desktop flags enabled on Windows/JDK 25, and executable startup verification.
  They explicitly do not claim independent macOS/Linux execution during that release run.
- The naming refactor was kept outside the MVP release and merged subsequently in
  [#128](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/128), on 16 October.

**Discussion prompts and proposed follow-up:**

Confirm which practices helped deliver the release and where review or integration work accumulated.
Retain reproducible acceptance evidence and an explicit release commit. Review the process observations
below before planning the next iteration; do not infer that the retrospective happened during this week.

## v1.3 postmortem — Process review for team discussion

**Prepared:** 22 October 2026. **Discussion date, attendees and approval:** not yet confirmed.
The [Week 10 requirement](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week10/project.html)
asks for a team discussion of successes, difficulties and process improvements, recorded in the shared
notes. The observations below are grounded in project records; the recommendations are proposed actions.

### What worked well

1. **Small review units made responsibilities traceable.** Separate persistence, parser, UI and acceptance
   PRs made it possible to review individual concerns. The level and subject acceptance PRs above provide
   concrete examples of targeted validation rather than a single undifferentiated feature submission.
2. **Release evidence went beyond unit tests.** The v1.3 notes identify the source commit, desktop test
   configuration, packaged executable, screenshots and platform limits. This makes the release repeatable
   and prevents a passing developer environment from being mistaken for universal verification.
3. **Scope boundaries were visible.** The naming refactor was explicitly excluded from the published MVP.
   Keeping that change separate reduced the amount of simultaneous release work needing verification.

### What did not work well, or needs improvement

1. **Integration risk was concentrated late.** Many acceptance PRs merged on 15 October, the release day.
   The timestamps do not prove rushed reviews, but they show limited time for another complete validation
   cycle if a late merge had failed. Aim for an integrated candidate before release day.
2. **Shared contracts needed reconciliation.** The earlier repair in #92 shows the cost of independently
   changing transactions, optional fields and indices. v1.3 testing recovered confidence, but the process
   should catch conflicting assumptions before implementation and review.
3. **Guide and setup maintenance lagged implementation.** Contributor/setup corrections landed in
   [#127](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/127) on 16 October; further User Guide alignment
   followed in [#129](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/129) on 19 October. Require a guide
   review alongside behavior changes instead of leaving documentation reconciliation to a later pass.
4. **Meeting records were not kept current.** The shared document inspected on 22 October stopped at the
   17 September minutes. Repository activity cannot replace a record of actual discussion and decisions.
5. **Platform coverage was limited.** The v1.3 evidence states its Windows coverage clearly, but the team
   should distinguish local evidence from hosted CI results and arrange a second-platform smoke check.

### Proposed process actions

These are recommendations for confirmation, not retroactively recorded commitments. Because this draft
was prepared on release day, missed v1.4 checkpoints must not be backdated; carry them into v1.5 planning.

| Improvement | Proposed responsibility | Timing | Evidence of completion |
| --- | --- | --- | --- |
| Agree shared interfaces before dependent implementation | Feature owner plus one affected owner | At issue planning | Issue records the contract, affected files and dependency order |
| Review small PRs promptly | A named teammate other than the author | Aim for first review within one working day | Review discusses behavior/tests and blockers are resolved explicitly |
| Build an integrated candidate earlier | Release coordinator, nominated each iteration | Target the day before the release deadline | Full build, desktop test flags and smoke result recorded against one commit |
| Maintain guides with code | Each feature owner and reviewer | Before each behavior PR is ready | UG examples and relevant DG text match actual commands |
| Keep notes current | Rotating meeting recorder | Within 24 hours of the actual discussion | Shared notes contain date, attendees, decisions and action owners |
| Check release portability and provenance | Release coordinator plus a second-platform volunteer | Before publication, or document the gap | Artifact source/checksum and platform-specific results recorded |
| Review whether these changes helped | Whole team | Next retrospective | Compare late merges, review delays and unresolved handoffs with v1.3 |

### Confirmation record

- Actual discussion date/time and attendees: to be supplied by the team.
- Corrections to the evidence-based observations: to be recorded.
- Accepted actions, named owners and due dates: to be recorded.
- Date the confirmed minutes were added to the shared project notes: pending.

## Week 10 — Alpha delivery and retrospective

**Meeting date/time and attendees:** to be confirmed. **Evidence cutoff:** 22 October 2026.

**Agenda reconstructed from the course requirements:** review v1.3 process, plan and deliver v1.4,
practise concurrent PRs, and begin bringing DG UML diagrams up to date. The alpha deadline is
22 October, 23:59. Reference:
[Week 10 project requirements](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week10/project.html).

**Recorded progress:**

- Student discovery merged through [#136](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/136),
  [#143](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/143) and
  [#149](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/149): partial names, subject/level filtering,
  and integration/UI acceptance. The first two were open concurrently; the dependent acceptance PR
  was initially draft, then marked ready after its prerequisites merged.
- Contact input and preservation work merged in
  [#146](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/146) and
  [#150](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/150); scheduling work included
  [timetable #148](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/148) and
  [slot removal #155](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/155).
- Academic notes and assessments merged through
  [#151](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/151) and
  [#153](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/153). Updated diagrams followed in
  [#152](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/152),
  [#158](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/158) and
  [#159](https://github.com/AY2627S1-CS2103T-F08-3/tp/pull/159).
- [The v1.4 alpha release](https://github.com/AY2627S1-CS2103T-F08-3/tp/releases/tag/v1.4)
  was published on 22 October at 17:45 Singapore time. A published release is not evidence that this
  retrospective discussion has already taken place.

**Discussion prompts and proposed follow-up:**

Review the postmortem above, confirm process actions and assign their owners. Check that milestone and
dependency metadata match the intended iteration. Preserve the release evidence separately from later
changes to master. Complete the meeting record in the shared document, and carry remaining process
actions into the next iteration rather than claiming they were completed before today's deadline.
