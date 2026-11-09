#!/usr/bin/env python3
"""Deterministic 18-screen mobile/tablet audit; never substitutes concept art for real screenshots."""
import argparse
import json
from pathlib import Path
from pixel_audit import inspect

SCREENS = (
    "01_splash.png", "02_onboarding.png", "03_permissions.png",
    "04_home.png", "05_photo.png", "06_video.png", "07_pro.png",
    "08_night.png", "09_portrait.png", "10_panorama.png", "11_dual.png",
    "12_filters.png", "13_gallery.png", "14_viewer.png", "15_editor.png",
    "16_settings.png", "17_camera-settings.png",
    "19_about.png"
)

def run(reference: Path, actual: Path, output: Path) -> dict:
    output.mkdir(parents=True, exist_ok=True)
    reports, missing, failures = [], [], []
    for screen in SCREENS:
        ref_file, real_file = reference / screen, actual / screen
        if not ref_file.is_file() or not real_file.is_file():
            missing.append(screen)
            continue
        try:
            report = inspect(ref_file, real_file, output / screen.removesuffix(".png"))
            reports.append({"screen": screen, **report})
        except (ValueError, OSError) as exc:
            failures.append({"screen": screen, "error": str(exc)})
    identical = sum(bool(x["strictly_identical"]) for x in reports)
    summary = {
        "expected": len(SCREENS),
        "checked": len(reports),
        "identical": identical,
        "missing": missing,
        "errors": failures,
        "all_screens_identical": identical == len(SCREENS) and
                                 not missing and not failures,
        "warning": "Live camera image content and OEM system bars must be matched "
                   "or masked before geometric UI comparisons."
    }
    (output / "summary.json").write_text(
        json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")
    return summary

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reference", required=True, type=Path)
    parser.add_argument("--actual", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    result = run(args.reference, args.actual, args.output)
    print(json.dumps(result, indent=2, ensure_ascii=False))
    if not result["all_screens_identical"]:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
