#!/usr/bin/env python3
"""Acquire genuine Android UI screenshots from a running LENSIGO APK.

No design sources, fake device frames or generated UI are used. Failure to
reach an actual screen means that screen is not published.
"""
from __future__ import annotations

import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

APP = "com.brendigo.lensigo"
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
SIZE = (1080, 2400)
START = "<!-- BEGIN REAL LENSIGO SCREENSHOTS -->"
END = "<!-- END REAL LENSIGO SCREENSHOTS -->"
SCREENS = (
    ("02_onboarding", "Dobrodošlica"),
    ("05_photo", "Foto način"),
    ("04_home", "Početna"),
    ("16_settings", "Postavke"),
    ("13_gallery", "Galerija"),
)


def adb(*args: str, timeout: int = 35) -> bytes:
    completed = subprocess.run(["adb", *args], stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE, timeout=timeout, check=True)
    return completed.stdout


def find_label(root: ET.Element, label: str) -> ET.Element | None:
    for node in root.iter("node"):
        if any(label.casefold() in node.attrib.get(field, "").casefold()
               for field in ("text", "content-desc")):
            return node
    return None


def ui_tree() -> ET.Element:
    adb("shell", "uiautomator", "dump", "/sdcard/lensigo-ui.xml", timeout=25)
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/lensigo-ui.xml"))


def visible(label: str, timeout_seconds: int = 25) -> ET.Element:
    end = time.monotonic() + timeout_seconds
    while time.monotonic() < end:
        try:
            root = ui_tree()
            if find_label(root, label) is not None:
                return root
        except (ET.ParseError, subprocess.SubprocessError, OSError):
            pass
        time.sleep(1.0)
    raise RuntimeError("Real Android screen control not found: " + label)


def tap(label: str, timeout_seconds: int = 25) -> None:
    root = visible(label, timeout_seconds)
    node = find_label(root, label)
    if node is None:
        raise RuntimeError("Missing target: " + label)
    bounds = node.attrib.get("bounds", "")
    found = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not found:
        raise RuntimeError("Android node has no tappable bounds: " + label)
    left, top, right, bottom = map(int, found.groups())
    if right <= left or bottom <= top:
        raise RuntimeError("Invalid Android target bounds: " + label)
    adb("shell", "input", "tap", str((left + right) // 2),
        str((top + bottom) // 2))
    time.sleep(1.2)


def png_dimensions(data: bytes) -> tuple[int, int]:
    if len(data) < 45 or not data.startswith(PNG_MAGIC) or data[12:16] != b"IHDR":
        raise ValueError("Not a complete Android PNG screenshot")
    return struct.unpack(">II", data[16:24])


def capture(path: Path, expected: str) -> None:
    tree = visible(expected)
    if not any(node.attrib.get("package") == APP for node in tree.iter("node")):
        raise RuntimeError("Screenshot foreground is not the LENSIGO application")
    data = adb("exec-out", "screencap", "-p", timeout=30)
    if png_dimensions(data) != SIZE:
        raise ValueError("Unexpected screenshot dimensions: " +
                         str(png_dimensions(data)))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    print("Captured genuine Android screen:", path, flush=True)


def publish_gallery(repo_root: Path, output: Path, source_run_id: str) -> None:
    files = [(name, label) for name, label in SCREENS
             if (output / (name + ".png")).is_file()]
    if len(files) < 2:
        raise RuntimeError("At least two verified real captures are required")
    preview = "\n".join(
        '<img src="docs/screenshots/real/' + name + '.png" '
        'alt="Stvarna Android emulator snimka — ' + label +
        '" width="190">'
        for name, label in files
    )
    block = (
        "**Stvarne snimke pokrenute aplikacije** — Android 15 emulator, "
        "1080 × 2400 px, gustoća 480 dpi; stvarna kamera na fizičkom uređaju "
        "nije ovdje verificirana. Snimke nisu dizajnerski mockupovi.\n\n"
        '<p align="center">\n' + preview + '\n</p>\n\n'
        "Izvor: `adb exec-out screencap -p`, GitHub Actions run "
        "`" + source_run_id + "`. "
        "[Matrica za 1:1 mjerenje](docs/UI-ACCEPTANCE-MATRIX.md).\n"
    )
    readme = repo_root / "README.md"
    data = readme.read_text(encoding="utf-8")
    if data.count(START) != 1 or data.count(END) != 1:
        raise RuntimeError("README screenshots markers missing")
    a = data.index(START) + len(START)
    b = data.index(END)
    if a >= b:
        raise RuntimeError("README screenshot markers out of order")
    readme.write_text(data[:a] + "\n" + block + data[b:],
                      encoding="utf-8")
    (output / "CAPTURE-INFO.md").write_text(
        "# LENSIGO — autentične emulator snimke\n\n"
        "Izvor: stvarni instalirani Android APK, ne dizajnerski koncept.\n"
        "Capture: `adb exec-out screencap -p`.\n"
        "Emulator: Android API 35, 1080 × 2400 px, 480 dpi.\n"
        "GitHub Actions source run: `" + source_run_id + "`.\n"
        "Snimke ne dokazuju 1:1 podudarnost ili performanse fizičke kamere.\n",
        encoding="utf-8"
    )


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: capture_real_screenshots.py preview-debug.apk docs/screenshots/real")
        return 2
    root = Path(__file__).resolve().parents[1]
    output = Path(sys.argv[2])
    adb("wait-for-device")
    adb("shell", "wm", "size", f"{SIZE[0]}x{SIZE[1]}")
    adb("shell", "wm", "density", "480")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("install", "-r", sys.argv[1], timeout=180)
    adb("shell", "pm", "clear", APP)
    adb("shell", "am", "start", "-n", APP + "/.MainActivity")

    # Remove screenshots from older APKs; never publish stale permission UI.
    for stale in output.glob("*.png"):
        stale.unlink()
    # The emulator grants the native CAMERA runtime permission before the
    # Welcome action; there is no custom permissions page in the current app.
    adb("shell", "pm", "grant", APP, "android.permission.CAMERA")
    capture(output / "02_onboarding.png", "Započni")
    tap("Započni")
    if find_label(ui_tree(), "Spremno za snimanje?") is not None:
        raise RuntimeError("Removed permissions page is still present")
    capture(output / "05_photo.png", "FOTO")
    try:
        adb("shell", "input", "keyevent", "4")
        capture(output / "04_home.png", "Načini snimanja")
        tap("Postavke")
        capture(output / "16_settings.png", "Postavke")
        adb("shell", "input", "keyevent", "4")
        tap("Otvori galeriju")
        capture(output / "13_gallery.png", "Galerija")
    except (RuntimeError, subprocess.SubprocessError, ValueError) as exc:
        print("Optional Android screenshot sequence stopped:", exc, file=sys.stderr)
        # Only verified real screenshots are published.
    
    publish_gallery(root, output, os.environ.get("SOURCE_RUN_ID", "unknown"))
    print("README updated with verified emulator screenshots only.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
