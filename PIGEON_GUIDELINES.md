# Pigeon Fork Guidelines

This document describes the rules we follow when modifying the upstream
Signal-Android codebase inside the Pigeon fork.

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

