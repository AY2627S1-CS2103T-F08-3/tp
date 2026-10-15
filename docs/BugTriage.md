---
  layout: default.md
  title: "Bug triage and prevention"
  pageNav: 3
---

# Bug triage and prevention

Use this checklist for the affected parts of each PR and for reported bugs. It turns the team's supplied
functionality/feature-flaw guidance into review and verification steps; it is not a guarantee of a bug-free
product or a substitute for the course's current scope rules. Record unverified checks honestly.

## Classify by user impact

1. Reproduce with synthetic data, recording the commit, Java/OS, window size if relevant, exact input,
   expected versus actual behavior, and the affected task. Do not attach real student contact data.
2. Decide what should change. Broken implementation is `type.FunctionalityBug`; a missing or inconvenient
   design is `type.FeatureFlaw`; an incorrect guide is `type.DocumentationBug` if behavior is appropriate.
   A guide change alone does not cure an unusable design. Related symptoms may share one root cause.
3. Propose severity from frequency, affected users, task obstruction, recoverability, and data loss.
   Minor clipping can be `severity.VeryLow`; obscuring information needed to distinguish students can
   warrant `Low` or higher. Do not classify a crash/data-loss risk as cosmetic merely because input is unusual.
4. Claim `NotInScope` only with the applicable course rule and a user-impact rationale. Do not use it for
   a flaw essential to reasonable usefulness, or an unnecessarily worse design with a low-effort improvement.
   Low severity is not the same as out of scope. Disputed cases need team/course review.
5. Assign the affected feature owner, agree shared-interface changes, add a reproducing regression test,
   then fix and verify. Record remaining product decisions separately from completed repairs.

## Review matrix

| Area | Prevention and evidence required when affected |
| --- | --- |
| Accidental malformed input | Try a missing space between prefixes, copy/paste line breaks, blank values, repeated/unknown prefixes, and concatenated values. Assert a controlled error, no partial profile, unchanged bytes/model/filter/selection, and a usable next command. |
| Extreme input / overflow | Deliberate sabotage alone need not be a course bug; assess whether ordinary mistakes can reach the same path. Exercise values beyond the machine integer range and reject/resolve them before narrowing. Do not spend unbounded effort on artificial resource exhaustion. |
| Long values | Test at the supported maximum and one beyond it, including Unicode code points and long unbroken words. Inspect the real window at minimum size and a normal size. Important values must remain accessible through wrapping, scrolling, or an equivalent usable detail view. |
| Limits and symbols | Justify restrictions against actual users, not parser convenience alone. Try realistic apostrophes, hyphens, combining marks, unit numbers, embedded slashes, and legal-name conventions such as `s/o`. A documented restriction can still be a design flaw. |
| Duplicate detection | Normalize case/Unicode/spaces and canonical phones consistently. Cover duplicate students hidden by a filter. Explain which matches block, warn, or pass; never imply fuzzy identity detection when it is not implemented. |
| Overzealous validation | Prefer warning/acceptance when safe and within the agreed product contract. Review legitimate exceptions before blocking; validate harmful values. Do not silently introduce multiple phones, international support, or other excluded features. |
| Error specificity | Identify the offending parameter and corrective action where practical. Keep missing, blank, repeated, format, invalid value, missing index, duplicate, and save errors distinct and test precedence. Do not relabel a value error as a format error. Propose shared-message changes for owner agreement. |
| Command usability | Assess typing burden and discoverability; consider aliases when useful. Preserve existing case-sensitive command/prefix contracts until an agreed change updates parsers, tests, and UG together. |
| Case and search | Preserve display case but compare names/search keywords case-insensitively. Exercise supported Unicode/space normalization. Confirm documented full-word versus substring behavior and OR versus AND matching; assess usefulness, not only implementation consistency. |
| UG/behavior mismatch | Execute documented examples and compare every relevant limit, message, save/reload promise, and field format with the current branch. Fix either behavior or docs based on intended user value, not whichever change is easiest. |
| Terminal output | Inspect logs during startup, valid input, ordinary errors, and load/save failures. Routine input must not look like an application crash. Avoid echoing raw control characters or contact data; distinguish diagnostic output from the single GUI status. Terminal-only issues can be lower severity, not automatically out of scope. |
| Human-edited data | Verify correct edits load, old records with missing optional fields load, and UUID/order/unrelated fields survive a save/reload. Check malformed syntax, null root/records/tags, duplicate IDs, and invalid known fields. Fail loading cleanly rather than publishing part of the file. |
| Storage safety | Exercise injected write and atomic-replace failures. Check file bytes, in-memory objects, filter, and selection. No-change/read-only commands must not write; do not add a truncate-in-place fallback. |

The baseline for manual editing is the [AB3 User Guide](https://se-education.org/addressbook-level3/UserGuide.html#editing-the-data-file):
valid edits work; invalid edits may lose data as documented. TutorConnect must not promise recovery/backup
features it does not provide. The [human-editable-file constraint](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-constraints.html#constraint-human-editable-file)
also applies: keep persistence usable as text, with documented fields and formats.

## TutorConnect decisions that remain open to review

The following are current requirements, not exemptions from feature-flaw triage:

- Phones are restricted to one Singapore number per field. The team should confirm the intended users'
  contact needs; do not claim all freelance tutors have only Singapore contacts. International or multiple
  numbers require an explicit product decision and compatible validation/storage changes.
- Names do not accept `/` or digits, so some legal-name conventions are unsupported. The field must not
  be represented as guaranteed legal-name support. Review impact with the team rather than recommending
  falsified names or silently expanding a shared grammar.
- Whitespace-delimited letter-plus-slash tokens are reserved prefixes. Embedded slashes such as `12/3`
  or `Math/Science` are data, but a value containing a standalone prefix-like token has a limitation.
- Commands/prefixes are lowercase and case-sensitive. Search is case-insensitive, OR-based, and full-word,
  not fuzzy. Canonical-equivalence/Unicode search behavior needs explicit verification when changed.
- Duplicate blocking uses normalized name **and** phone. Different names sharing a phone warn but are
  accepted; the same name with a different phone is accepted without a fuzzy-name warning. Spelling
  variants, nicknames, and real-world identity are not resolved automatically.

Changing these decisions requires owner agreement, examples, tests, and corresponding UG changes.
Documenting a limitation helps set expectations but does not make every limitation acceptable.

## Evidence in this hardening change

- `DataFileSafetyTest` reproduces malformed JSON load paths and checks controlled storage/list errors,
  no partial publication, unchanged selection/filter/objects/file bytes, and valid manual-edit round trips.
- `JsonAdaptedTagTest` verifies a null tag name yields validation failure rather than a null-pointer crash.
- `AddStudentEndToEndTest` covers missed prefix separators, accidentally concatenated phones, and values
  just beyond name/address limits while checking unchanged state and bytes. Existing tests cover error
  precedence, normalized duplicates, shared-phone warnings, and save failures.
- Existing `StudentFieldIntegrationTest`, field-owner tests, and JavaFX acceptance tests cover large
  displayed indices, cross-feature preservation, no-change, atomic rollback, and selection/details.

These tests do not certify every visual layout, legal-name use case, locale, or console message. Record
manual evidence or follow-up decisions for those areas before declaring them verified.

## PR completion record

For relevant risks, record: scenario; classification and impact; owner/decision; automated test or manual
evidence; result; remaining limitations. Use **passed**, **failed**, **not run**, or **not applicable with reason**.
Run Java 25 `check coverage shadowJar` with `F01_UI_TESTS=true` and `F08_UI_TESTS=true` on a real/virtual
display, repository-wide checks, and whitespace checks. A skipped desktop test is not GUI acceptance.
Do not lower coverage gates or hide failures to complete the checklist. See [Testing](Testing.md).
