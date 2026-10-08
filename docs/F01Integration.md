# F01 shared integration contract

Status: implementation contract for coordination; owner acknowledgement is pending.
F01 implements Add Student only. This document does not allocate or implement other owners' commands.

## Student model

The existing `Person` aggregate represents a student, avoiding a repository-wide rename while owners work in parallel.
`getId()` returns a stable UUID independent of displayed indices. New students use `Person(Name, Phone, Address)`;
legacy email/tags remain available for old files but are empty for new students. Never invent optional values.
`withDetails(...)` and `withStudentFields(...)` preserve identity and unrelated fields. Equality compares content;
use the UUID explicitly for selection and identity, and normalized name plus phone for duplicate detection.

`StudentFields` is an immutable, defensively copied extension envelope. Optional-field owners implement
`StudentField<T>` (`key`, `encode`, `decode`) to supply their own validated types and JSON mappings. Missing/null
values mean unset; unrelated fields are retained, including those written by a newer owner implementation.
Reserved keys: `guardianPhone`, `educationLevel`, `subject`, `hourlyRate`, `weeklySlot`.
Field owners must use canonical phones and levels, BigDecimal rates with two decimal places,
and weekday plus LocalTime slots. F01 does not invent these field types or implement their commands.

## Normalization and parsing

`StudentText.normalize` rejects controls, format characters and line separators before NFKC, trimming and
collapsing Unicode spaces. It preserves case. Name/address limits count Unicode code points.
`Phone` supplies shared Singapore validation/canonicalization; guardian-phone owners should reuse it.
Commands/prefixes stay ASCII lowercase and case-sensitive; do not NFKC-normalize command syntax.

The shared `StudentParameters` parser preserves raw values until validation. A boundary is a token beginning
with a single ASCII letter followed by `/`. Thus `a/12/3 Street` is one address, while `x/value` is an
unknown parameter. Slashes embedded in tokens (e.g. `Math/Science`) remain data. Prefix-like literal tokens
are reserved syntax and cannot be escaped. Owners should raise any disagreement with this boundary rule.
Unknown parameter/unexpected preamble checks precede required/duplicate parameter checks, then values are
validated in command documentation order. Empty values are invalid values, not missing prefixes.
The F02/F03 owner supplies index syntax/resolution and list/delete selection policy; F01 does not replace them.

## Atomic execution and view handoff

`LogicManager` executes commands against `Model.copyForCommand()`, saves the complete proposed state only
when data changed, and calls `Model.publish()` only after successful storage. Errors leave the live model,
filter and selected UUID untouched. Read-only/no-change commands must not write storage.
`AtomicJsonFile` writes and forces a sibling temporary file before atomic replacement. Unsupported atomic
moves fail safely; there is no truncate-in-place fallback. JSON keeps the existing `persons` array in order.

The model exposes a selected UUID and current predicate. Add shows the full list and selects its new UUID.
F02/F03 can use this state for list (clear), delete (preserve unless selected target removed) and displayed-index
resolution. Field owners should select their target only after a real successful change, preserving selection
and display formatting for equivalent normalized no-change input. All command effects remain staged until save.

UI selection is synchronized only after successful execution. Optional values display an em dash when unset.
Please coordinate changes to Model, LogicManager, Person, JsonAdaptedPerson and selection APIs before merging.

## Review slices and owner handoff

F01 is split into four PRs: foundation (#57), validation/parsing (#62), atomic persistence (#63), and UI/tests/docs.
Each dependent PR is initially based on the preceding slice, not `master`, to keep its diff focused. Retarget to
`master` after the prerequisite has landed there; merging into a dependency branch does not itself deliver to
`master`. No PR in this chain implements another owner's command.

* F02/F03: reuse `getPersonPredicate`, `getSelectedPersonId`, `selectPerson`, `copyForCommand` and `publish`.
  Shared visible-index work is tracked in #64. Never interpret UUIDs as displayed indices. The existing legacy
  command behaviour is not the final F02/F03 contract; implement exact index/list/delete messages there.
* Guardian phone owner: reuse `Phone`; use a `StudentField<Phone>` codec with key `guardianPhone`.
* Education/subject/rate owners: supply their own validated types and codecs; use `withStudentFields` and keep
  unrelated values. Canonical levels and two-decimal BigDecimal rates remain those owners' responsibility.
* F08: the weekly slot value is being introduced in #60. Its owner supplies the `weeklySlot` codec and final
  display formatting; F01 preserves that JSON but does not impose its internal schema.

Owners of no-change updates must return without changing their staged model or selection. Data equality skips
saving; it is not a substitute for command-specific no-change messages or display-format preservation.
The fallback details renderer shows stored values; each optional-field owner should add its final formatter.
