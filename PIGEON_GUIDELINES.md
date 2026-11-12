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

## Location of Pigeon-only code

Whenever practical, put new Pigeon-only implementations under a `pigeon`
package/directory instead of keeping them next to upstream Signal code.

Examples:

- Prefer `pigeon/.../PinCreationScreen.kt` or
  `pigeon/.../PinCreationPigeonLayout.kt` over defining a large
  `PinCreationPigeonLayout` inside an upstream `org.signal...` file.
- Keep the upstream file as a thin dispatcher:

```kotlin
if (isPigeonVersion()) {
  pigeonComposableOrHelper(...)
  return
}

// Original Signal logic – keep untouched
```

This keeps upstream files close to Signal, makes Pigeon diffs easier to
review, and reduces merge conflicts when rebasing/cherry-picking future
upstream changes.

Small bridge code at the call site is fine, but the actual Pigeon-only UI,
state handling, and helpers should live under `pigeon/...` whenever possible.

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
- Commit messages are plain; do not append a `Co-authored-by: Copilot`
  trailer.

## Rebasing onto a new Signal release

Pigeon history is kept flat: every release branch (`2.0.00NN`) is the
upstream tag plus **one squashed commit** named `Rebase to Signal X.Y.Z`
that contains the whole Pigeon diff (see `1efa929207` for 8.26.4).
Follow-up fixes land as normal commits on top of it until the next
rebase, when everything is squashed again.

### Procedure

1. Fetch upstream: `git fetch signal --tags`.
2. Find the Pigeon base: the tag the current branch sits on
   (`git merge-base HEAD vOLD` / the `upon Signal X.Y.Z` suffix in
   `canonicalVersionName`).
3. Start from the new tag on a temporary branch:
   `git checkout -b rebase-X.Y.Z vX.Y.Z`.
4. Squash-merge the Pigeon branch onto it:
   `git merge --squash 2.0.00NN`. This replays the Pigeon diff
   (`vOLD..2.0.00NN`) on top of the new upstream tree.
5. Resolve conflicts following the rules in this document – keep the
   upstream code path intact, re-attach Pigeon branches at the
   equivalent points, never restore removed resources.
6. Bump versions in `app/build.gradle.kts` (see below).
7. Check the RingRTC version (see below).
8. Make sure the Pigeon build compiles.
9. Create **one** commit: `Rebase to Signal X.Y.Z`.
10. Point the release branch at it (`git branch -f 2.0.00NN`) and push.
    Steps 9–10 rewrite history and are done only by the maintainer.

### Verifying `-small` overrides after a rebase

Upstream does not touch our `-small` resource variants, so after every
rebase they may still reference views, custom view classes or resources
that Signal has since removed or renamed. The build does **not** catch
this – it surfaces at runtime on the MP02 as an `InflateException`
(`ClassNotFoundException` for a removed custom view, e.g.
`PulsingFloatingActionButton` in `layout-small/conversation_list_fragment.xml`)
or as an NPE after `findViewById()` returns `null`.

Run the checker from the repository root as part of step 8 and fix every
report before committing the rebase:

```bash
python3 scripts/pigeon/check_small_layouts.py            # all checks
python3 scripts/pigeon/check_small_layouts.py ids        # single check
```

It performs three checks:

- `classes` – every custom view class used in a `-small` layout still
  exists in a `src/main/java` source set.
- `resources` – every `@type/name` reference in a `-small` file resolves
  to a resource defined in the repository (`tools:` attributes and
  library resources are ignored).
- `ids` – every `@+id/...` declared in `layout/<name>.xml` also exists in
  `layout-small/<name>.xml`. A missing id means the Pigeon build gets
  `null` from `findViewById()`; either add the view to the `-small`
  layout (styled for the MP02) or confirm the Java/Kotlin side is
  null-safe. Also check the opposite direction by hand: ids that exist
  only in the `-small` file usually mean the view was dropped upstream
  and the Pigeon copy should drop it too.

When fixing a report, follow the rule above: mirror how the current
upstream layout handles the same spot instead of restoring the removed
class/resource. Pigeon-only layouts (`pigeon_*.xml`) are skipped by the
`ids` check because they have no upstream counterpart.

### Versions

Every rebase bumps the Pigeon build number **by exactly one**. In
`app/build.gradle.kts`:

- `pigeonVersionCode` – increment by 1 (e.g. `127547` → `127548`).
- `canonicalVersionName` – increment the Pigeon build number by 1 and
  update the Signal version, keeping the
  `"Pigeon 2.0.00NN upon Signal X.Y.Z"` format
  (e.g. `"Pigeon 2.0.0034 upon Signal 8.26.4"` →
  `"Pigeon 2.0.0035 upon Signal 8.34.2"`).
- `canonicalVersionCode` – take the value from the upstream tag as-is.

The Pigeon build number matches the name of the release branch
(`2.0.0035`).

### RingRTC

Pigeon does **not** use the upstream `libs.signal.ringrtc` Maven
artifact (that line is commented out in `app/build.gradle.kts`).
RingRTC is built separately for the MP02 and the resulting AAR is
committed to `app/libs/ringrtc-android-<version>.aar` and wired in with
`implementation(files("libs/ringrtc-android-<version>.aar"))`.

On every rebase compare the `signal-ringrtc` version in
`gradle/libs.versions.toml` with the AAR in `app/libs/`. If upstream
bumped it, RingRTC has to be rebuilt for the MP02 at the new version,
the new AAR added to `app/libs/`, the `files(...)` reference in
`app/build.gradle.kts` updated and the old AAR removed. This is a
separate task from the rebase itself.

## Pigeon main screen (home page menu)

The MP02 has no touch screen – everything is driven by a DPAD and a
small 240×320 display. There are no left/right hardware navigation
buttons, so navigation is limited to vertical DPAD movement and button
presses. Pigeon therefore replaces Signal's main screen chrome
(toolbar, navigation rail/bar, FABs, megaphones) with a simple vertical
text menu, followed by the conversation list.

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

- Pigeon must **not** use system pickers. MP02 has no touch input, so
  any picker or selector must be built inside the app.
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
- Keep upstream files thin when calling Pigeon-only code; move the real
  implementation into `pigeon/...` when practical.
- Add `layout-small/`, `values-small/`, … variants.
- Keep the original Signal code path intact and reachable.

❌ Don't
- Remove or rewrite existing Signal logic.
- Inline Pigeon-only constants into the Signal path.
- Leave large Pigeon-only composables/classes/helpers inside upstream
  `org.signal...` files when they can live under `pigeon/...` instead.
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

