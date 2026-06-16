# Pigeon Fork Guidelines

This document describes the rules we follow when modifying the upstream
Signal-Android codebase inside the Pigeon fork.

## Language

All code, identifiers, comments, commit messages, log messages and
documentation **must be written in English**. This matches the upstream
Signal codebase and keeps diffs/merges clean. Do not introduce Polish
(or any other non-English) text into source files, XML resources or
Markdown docs.

## Golden Rule

**Do not change the original Signal logic.**
Pigeon-specific behavior must always be added as an *additional* branch next
to the original one, gated by a build-variant check. Never delete, rewrite or
replace existing Signal code paths unless absolutely necessary.

The goal is to keep diffs against upstream as small as possible so that we
can keep rebasing/merging new Signal releases with minimal conflicts.

## How to add Pigeon-specific behavior

Always wrap the new behavior in an `if`/`when` that checks the build variant.

### Kotlin

```kotlin
import pigeon.extensions.isPigeonVersion
import pigeon.extensions.isSignalVersion

if (isPigeonVersion()) {
  // Pigeon-only behavior
} else {
  // Original Signal behavior – leave untouched
}
```

or, when the Signal path is the default:

```kotlin
if (isSignalVersion()) {
  // Original Signal behavior
} else {
  // Pigeon-only behavior
}
```

### Java

```java
import static pigeon.extensions.BuildExtensionsKt.isPigeonVersion;
import static pigeon.extensions.BuildExtensionsKt.isSignalVersion;

if (isPigeonVersion()) {
  // Pigeon-only behavior
} else {
  // Original Signal behavior – leave untouched
}
```

### Compose

```kotlin
if (isPigeonVersion()) {
  PigeonComponent()
} else {
  SignalComponent()
}
```

## Resource overrides

For resources (layouts, drawables, strings, dimens, colors, …) prefer
adding a `-small` (or other appropriate qualifier) variant instead of
editing the original file. The MP02 device is the primary Pigeon target
and uses the `small` qualifier.

If a resource must be edited in place, double-check that the Signal
build still works on phones and tablets.

## Backups

In Pigeon we use **only local (on-device) backups**. The remote
Signal-cloud backup flow does not apply to MP02 hardware.

Concretely, in any backup-related UI or flow:

- Do **not** show the "Choose backup folder" / "I saved my backup as a
  single file" picker. The user does not select files or folders by
  hand on MP02.
- The Pigeon code path must use the fixed local backup directory and
  skip system file pickers (`ACTION_OPEN_DOCUMENT_TREE` /
  `ACTION_OPEN_DOCUMENT`).
- Restore flows must read the most recent local backup automatically
  and proceed straight to the recovery-key entry step.
- Any UI element that exists only to choose a backup destination or
  source file must be guarded by `isSignalVersion()` so the Signal
  build is unaffected.

When in doubt, prefer wiring the Pigeon path so the user is taken
directly from "Restore backup" to "Enter recovery key" with no folder
or file picker in between.

## Do / Don't

✅ Do
- Add `if (isPigeonVersion()) { … }` branches.
- Add new files under `pigeon/` packages.
- Add `layout-small/`, `values-small/`, … variants.
- Keep the original Signal code path intact and reachable.

❌ Don't
- Remove or rewrite existing Signal logic.
- Inline Pigeon-only constants into the Signal path.
- Drop original Signal resources – override them via qualifiers instead.
- Change public method signatures used by upstream code.

## When in doubt

If you are unsure whether a change touches the Signal path, ask yourself:

> "If we re-applied the upstream commit on top of this change, would it
> still compile and behave the same for Signal users?"

If the answer is no, refactor the change to be guarded by
`isPigeonVersion()` / `isSignalVersion()`.

