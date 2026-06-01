# Copilot Instructions

This is the **Pigeon** fork of Signal-Android.

Before suggesting or applying any change, **read and follow** the rules in
[`PIGEON_GUIDELINES.md`](../PIGEON_GUIDELINES.md) at the repository root.

## TL;DR

- **Never change the original Signal logic.** Pigeon-specific behavior must
  be added as an extra branch next to the original code, gated by
  `isPigeonVersion()` / `isSignalVersion()` (Kotlin) or
  `pigeon.extensions.BuildExtensionsKt.isPigeonVersion()` /
  `isSignalVersion()` (Java).
- Keep the Signal code path intact and reachable so upstream merges stay
  conflict-free.
- For resource changes prefer adding qualified variants (e.g.
  `res/layout-small/…`, `res/values-small/…`) instead of editing the
  original resource files.
- Do not remove or rewrite Signal code unless absolutely necessary.

## Canonical pattern

```kotlin
if (isPigeonVersion()) {
  // Pigeon-only behavior
  return
}
// Original Signal logic – DO NOT modify
```

```java
if (pigeon.extensions.BuildExtensionsKt.isPigeonVersion()) {
  // Pigeon-only behavior
  return;
}
// Original Signal logic – DO NOT modify
```

When in doubt, ask:
> "If we re-applied an upstream commit on top of this change, would Signal
> still compile and behave the same?"

If the answer is no, refactor the change so it is guarded by a build-variant
check.

