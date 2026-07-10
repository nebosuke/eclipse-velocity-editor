#!/usr/bin/env python3
"""Increment the patch version across all Tycho project files and print the new version."""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

MANIFEST = ROOT / "io.github.nebosuke.velocityeditor/META-INF/MANIFEST.MF"
FEATURE_XML = ROOT / "io.github.nebosuke.velocityeditor.feature/feature.xml"
CATEGORY_XML = ROOT / "io.github.nebosuke.velocityeditor.updatesite/category.xml"
POM_FILES = [
    ROOT / "pom.xml",
    ROOT / "io.github.nebosuke.velocityeditor/pom.xml",
    ROOT / "io.github.nebosuke.velocityeditor.feature/pom.xml",
    ROOT / "io.github.nebosuke.velocityeditor.updatesite/pom.xml",
]


def read_current_version():
    text = MANIFEST.read_text()
    match = re.search(r"Bundle-Version:\s*(\d+)\.(\d+)\.(\d+)\.qualifier", text)
    if not match:
        sys.exit("Could not find Bundle-Version in MANIFEST.MF")
    return tuple(int(x) for x in match.groups())


def format_version(version):
    return "{}.{}.{}".format(*version)


def replace_in_file(path, pattern, replacement, count=0):
    text = path.read_text()
    new_text, n = re.subn(pattern, replacement, text, count=count)
    if n == 0:
        sys.exit(f"Pattern not found in {path}: {pattern}")
    path.write_text(new_text)


def main():
    major, minor, patch = read_current_version()
    old_str = format_version((major, minor, patch))
    new_str = format_version((major, minor, patch + 1))

    replace_in_file(
        MANIFEST,
        rf"Bundle-Version: {re.escape(old_str)}\.qualifier",
        f"Bundle-Version: {new_str}.qualifier",
    )
    replace_in_file(
        FEATURE_XML,
        rf'version="{re.escape(old_str)}\.qualifier"',
        f'version="{new_str}.qualifier"',
        count=1,
    )
    replace_in_file(
        CATEGORY_XML,
        rf"{re.escape(old_str)}\.qualifier",
        f"{new_str}.qualifier",
    )
    for pom in POM_FILES:
        replace_in_file(
            pom,
            rf"<version>{re.escape(old_str)}-SNAPSHOT</version>",
            f"<version>{new_str}-SNAPSHOT</version>",
            count=1,
        )

    print(new_str)


if __name__ == "__main__":
    main()
