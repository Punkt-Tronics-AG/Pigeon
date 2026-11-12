#!/usr/bin/env python3
"""
Sanity checks for Pigeon's `-small` resource overrides after a rebase onto a new Signal release.

Upstream regularly removes or renames views, custom view classes and resources. The `-small`
variants are not touched by upstream, so they silently keep referencing things that no longer
exist -- which only shows up as an InflateException / NPE at runtime on the MP02.

Checks performed (run from the repository root):

  1. classes   -- every custom view class referenced from a `-small` layout exists in some
                  `*/src/main/java` source set.
  2. resources -- every `@type/name` reference in a `-small` resource file resolves to a
                  resource defined in the repository (library resources such as
                  `TextAppearance.MaterialComponents.*`, `abc_*`, `cardview_*` are ignored).
  3. ids       -- every `@+id/...` declared in the original `layout/<name>.xml` also exists in
                  `layout-small/<name>.xml`. A missing id means `findViewById()` returns null
                  for the Pigeon build; verify the Java/Kotlin side is null-safe or add the view.

Usage:
  python3 scripts/pigeon/check_small_layouts.py            # all checks
  python3 scripts/pigeon/check_small_layouts.py ids        # a single check
  python3 scripts/pigeon/check_small_layouts.py classes resources

Exit code is 1 when any check reports a problem.
"""

import glob
import os
import re
import sys

APP_RES = "app/src/main/res"
SMALL_DIRS = sorted(glob.glob(f"{APP_RES}/*-small*"))
SOURCE_ROOTS = glob.glob("*/src/main/java") + glob.glob("*/*/src/main/java")
RES_ROOTS = glob.glob("*/src/main/res") + glob.glob("*/*/src/main/res")

# Prefixes of resources that come from AndroidX / Material and are never defined in this repo.
LIBRARY_RESOURCE_PREFIXES = (
    "TextAppearance.MaterialComponents.",
    "TextAppearance.AppCompat.",
    "TextAppearance.Material3.",
    "Widget.MaterialComponents.",
    "Widget.Material3.",
    "Widget.AppCompat.",
    "ThemeOverlay.",
    "ShapeAppearance.",
    "abc_",
    "cardview_",
    "mtrl_",
    "m3_",
    "design_",
)

CUSTOM_VIEW_RE = re.compile(r"<((?:org\.thoughtcrime|org\.signal|pigeon)[A-Za-z0-9_.]+)")
RESOURCE_REF_RE = re.compile(r"(?<!tools:src=\")@(?!android:)(\w+)/([A-Za-z0-9_.]+)")
# `android:id="@id/foo"` is a valid declaration too when `foo` is defined elsewhere (ids.xml / another layout).
ID_DECL_RE = re.compile(r"(?:@\+id/|android:id=\"@id/)([A-Za-z0-9_]+)")
INCLUDE_RE = re.compile(r"<include[^>]*\blayout=\"@layout/([A-Za-z0-9_]+)\"")
VALUES_ITEM_RE = re.compile(
    r"<(string|dimen|color|style|bool|integer|attr|plurals|string-array|integer-array|array|item)[^>]*\bname=\"([^\"]+)\""
)


def read(path):
    with open(path, encoding="utf-8", errors="ignore") as f:
        return f.read()


def small_files():
    for d in SMALL_DIRS:
        for f in sorted(glob.glob(f"{d}/*.xml")):
            yield f


def check_classes():
    problems = []
    classes = set()
    for f in small_files():
        classes.update(CUSTOM_VIEW_RE.findall(read(f)))
    for cls in sorted(classes):
        rel = cls.replace(".", "/")
        if not any(os.path.exists(f"{root}/{rel}.{ext}") for root in SOURCE_ROOTS for ext in ("java", "kt")):
            problems.append(f"missing view class: {cls}")
    return problems


def collect_defined_resources():
    defined = {}

    def add(kind, name):
        defined.setdefault(kind, set()).add(name)

    for root in RES_ROOTS:
        for d in os.listdir(root):
            path = os.path.join(root, d)
            if not os.path.isdir(path):
                continue
            kind = d.split("-")[0]
            for f in os.listdir(path):
                fpath = os.path.join(path, f)
                if kind != "values":
                    add(kind, f.split(".")[0])
                    if f.endswith(".xml"):
                        for name in ID_DECL_RE.findall(read(fpath)):
                            add("id", name)
                    continue
                if not f.endswith(".xml"):
                    continue
                txt = read(fpath)
                for m in VALUES_ITEM_RE.finditer(txt):
                    tag, name = m.group(1), m.group(2)
                    if tag in ("string-array", "integer-array"):
                        tag = "array"
                    elif tag == "item":
                        tm = re.search(r'type="([^"]+)"', m.group(0))
                        if not tm:
                            continue
                        tag = tm.group(1)
                    add(tag, name)
    return defined


def check_resources():
    defined = collect_defined_resources()
    problems = []
    for f in small_files():
        txt = read(f)
        # tools:* attributes are stripped at build time, ignore them.
        txt = re.sub(r'tools:[A-Za-z_]+="[^"]*"', "", txt)
        for kind, name in RESOURCE_REF_RE.findall(txt):
            if name.startswith(LIBRARY_RESOURCE_PREFIXES):
                continue
            if kind not in defined:
                continue
            if name in defined[kind]:
                continue
            problems.append(f"{f}: @{kind}/{name} is not defined")
    return problems


def layout_ids(path, prefer_small, seen=None):
    """Ids declared in a layout, including those contributed by <include>d layouts."""
    seen = seen if seen is not None else set()
    if path in seen or not os.path.exists(path):
        return set()
    seen.add(path)
    txt = read(path)
    ids = set(ID_DECL_RE.findall(txt))
    for name in INCLUDE_RE.findall(txt):
        candidates = [f"{APP_RES}/layout-small/{name}.xml", f"{APP_RES}/layout/{name}.xml"]
        if not prefer_small:
            candidates.reverse()
        for candidate in candidates:
            if os.path.exists(candidate):
                ids |= layout_ids(candidate, prefer_small, seen)
                break
    return ids


def check_ids():
    problems = []
    for sf in sorted(glob.glob(f"{APP_RES}/layout-small/*.xml")):
        name = os.path.basename(sf)
        if name.startswith("pigeon_"):
            continue  # Pigeon-only layout, no upstream counterpart by design
        of = f"{APP_RES}/layout/{name}"
        if not os.path.exists(of):
            problems.append(f"{sf}: no original layout/{name} (removed upstream?)")
            continue
        missing = layout_ids(of, prefer_small=False) - layout_ids(sf, prefer_small=True)
        if missing:
            problems.append(f"{name}: ids missing in -small variant: {', '.join(sorted(missing))}")
    return problems


CHECKS = {
    "classes": check_classes,
    "resources": check_resources,
    "ids": check_ids,
}


def main(argv):
    if not os.path.isdir(APP_RES):
        print("Run this script from the repository root.", file=sys.stderr)
        return 2
    selected = argv[1:] or list(CHECKS)
    unknown = [s for s in selected if s not in CHECKS]
    if unknown:
        print(f"Unknown check(s): {', '.join(unknown)}. Available: {', '.join(CHECKS)}", file=sys.stderr)
        return 2

    failed = False
    for name in selected:
        problems = CHECKS[name]()
        status = "OK" if not problems else f"{len(problems)} problem(s)"
        print(f"== {name}: {status}")
        for p in problems:
            print(f"   {p}")
        failed = failed or bool(problems)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
