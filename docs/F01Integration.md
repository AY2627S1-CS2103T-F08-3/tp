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

The shared named-parameter parser will preserve raw values until validation. A boundary is a token beginning
with an ASCII letter-only prefix followed by `/`. Thus `a/12/3 Street` is one address, while `x/value` is an
unknown parameter. Slashes embedded in tokens (e.g. `Math/Science`) remain data. Prefix-like literal tokens
are reserved syntax and cannot be escaped. Owners should raise any disagreement with this boundary rule.
Unknown parameter/unexpected preamble checks precede required/duplicate parameter checks, then values are
validated in command documentation order. Empty values are invalid values, not missing prefixes.
The F02/F03 owner supplies index syntax/resolution and list/delete selection policy; F01 does not replace them.

## Atomic execution and view handoff

`LogicManager` will execute commands against `Model.copyForCommand()`, save the complete proposed state only
when data changed, and call `Model.publish()` only after successful storage. Errors leave the live model,
filter and selected UUID untouched. Read-only/no-change commands must not write storage.
`AtomicJsonFile` writes and forces a sibling temporary file before atomic replacement. Unsupported atomic
moves fail safely; there is no truncate-in-place fallback. JSON keeps the existing `persons` array in order.

The model exposes a selected UUID and current predicate. Add shows the full list and selects its new UUID.
F02/F03 can use this state for list (clear), delete (preserve unless selected target removed) and displayed-index
resolution. Field owners should select their target only after a real successful change, preserving selection
and display formatting for equivalent normalized no-change input. All command effects remain staged until save.

UI selection is synchronized only after successful execution. Optional values display an em dash when unset.
Please coordinate changes to Model, LogicManager, Person, JsonAdaptedPerson and selection APIs before merging.
