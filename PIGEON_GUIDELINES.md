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

When upstream removes a resource (drawable, dimen, string, …) that a
`-small` variant still references, **do not bring the old resource back**.
Look at how the current original Signal layout handles the same spot and
adapt the `-small` variant accordingly (drop the reference, use the
replacement resource, etc.).

## Git workflow

- **No rebase, amend, squash, reset or force-push without explicit
  approval from the maintainer.** History rewriting is always a
  conscious decision made by a human.
- **Do not create commits or push on your own initiative.** Leave changes
  in the working tree and describe them; the maintainer decides when and
  how they are committed.

## Pigeon main screen (home page menu)

The MP02 has no touch screen – everything is driven by a DPAD and a
small 240×320 display. Pigeon therefore replaces Signal's main screen
chrome (toolbar, navigation rail/bar, FABs, megaphones) with a simple
vertical text menu, followed by the conversation list.

Structure (`MainActivity.kt`):

- `setContent` first shows `pigeon.compose.PreLoader` for ~5 s
  (`pigeonShowSplashScreen`) and only then the real content.
- `MainListPaneChrome` on Pigeon delegates to `PigeonListPaneChrome`.
  It wraps the whole list pane in a `NestedScrollView` hosting a
  `ComposeView` so the menu and the list scroll together with the DPAD.
  The `NestedScrollView` must be `isFocusable = false` with
  `FOCUS_AFTER_DESCENDANTS`, otherwise DPAD_DOWN past the last item
  leaves focus stuck in an empty area.
- Inside it: `pigeon.fragments.HomePageFragment` (the menu) and, when
  `_pigeonShowConversation` is `true`, the upstream list `content()`
  (`ConversationListFragment` / archive / calls / stories – whatever
  `ListDetailNavDisplay` provides for the current tab).
- Signal's `MainToolbar`, `MainNavigationRail`, `MainNavigationBar`,
  `MainBottomChrome` and `Material3OnScrollHelper` are not used on
  Pigeon.

The menu itself is `pigeon.compose.HomePageScreen`, a `Column` of
`HomePageButton`s (plain focusable `Text`, 24 sp unfocused / 40 sp
focused, white vs. 50 % white, start padding 30 dp → 5 dp on focus; the
focused item is scrolled to the centre of the `NestedScrollView`):

1. **New message** – `NewConversationActivity`
2. **New group** – `CreateGroupActivity`
3. **Mark all read** – marks all threads read, cancels notifications
4. **Settings** – `AppSettingsActivity.home()` (result
   `REQUEST_CONFIG_CHANGES` triggers `recreate()`)
5. **Search** – shown only while the list is hidden
   (`HomePageFragment.setupSearchButtonState(true)`); calls
   `MainActivity.collapseHomePage()`

State helpers in `MainActivity` (all `PIGEON-ONLY`):

- `collapseHomePage()` – show the conversation list, hide the *Search*
  item. Called at the end of `onCreate`, from the *Search* item and from
  `ConversationListArchiveFragment`.
- `expandHomePage()` / `hideArchivedConversations()` – hide the list,
  show the *Search* item again.
- `onBackPressed()` – with the list visible, Back finishes the task
  (`finishAffinity()`); with the list hidden on the task root, Back
  re-expands the home page instead of leaving the app.

When upstream restructures `MainActivity`, keep the Signal composition
untouched and re-attach these pieces at the equivalent points; do not
try to merge the old Pigeon layout code (`AppScaffold`,
`MainNavigationListLocation`, …) into the new one.

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
- Restore resources that upstream has removed just to satisfy a `-small`
  variant.
- Rebase, amend, force-push or commit without the maintainer's approval.

## When in doubt

If you are unsure whether a change touches the Signal path, ask yourself:

> "If we re-applied the upstream commit on top of this change, would it
> still compile and behave the same for Signal users?"

If the answer is no, refactor the change to be guarded by
`isPigeonVersion()` / `isSignalVersion()`.

