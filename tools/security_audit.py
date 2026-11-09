#!/usr/bin/env python3
"""Fail-closed static security/privacy contracts for LENSIGO Android.

A passing report is NOT a penetration test, dependency CVE assessment, or
an OEM device compatibility certificate.
"""
from __future__ import annotations
import argparse
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

NS = "{http://schemas.android.com/apk/res/android}"


def audit(root: Path) -> dict:
    problems = []
    warnings = []
    manifest = root / "app/src/main/AndroidManifest.xml"
    main = root / "app/src/main/java/com/brendigo/lensigo"
    try:
        doc = ET.parse(manifest).getroot()
    except (OSError, ET.ParseError) as error:
        return {"passed": False, "problems": [f"Manifest error: {error}"],
                "warnings": warnings}
    uses = {n.attrib.get(NS + "name"): n for n in doc.findall("uses-permission")}
    for permission in ("android.permission.INTERNET",
                       "android.permission.ACCESS_FINE_LOCATION",
                       "android.permission.ACCESS_COARSE_LOCATION",
                       "android.permission.READ_EXTERNAL_STORAGE",
                       "android.permission.READ_MEDIA_IMAGES",
                       "android.permission.READ_MEDIA_VIDEO",
                       "android.permission.ACCESS_MEDIA_LOCATION",
                       "android.permission.MANAGE_EXTERNAL_STORAGE"):
        if permission in uses:
            problems.append("Overbroad permission: " + permission)
    storage = uses.get("android.permission.WRITE_EXTERNAL_STORAGE")
    if storage is not None and storage.attrib.get(NS+"maxSdkVersion") != "28":
        problems.append("Legacy WRITE_EXTERNAL_STORAGE must be capped at API 28")
    app = doc.find("application")
    if app is None:
        problems.append("Application element missing")
    else:
        if app.attrib.get(NS+"allowBackup") != "false":
            problems.append("Application backup is not explicitly disabled")
        if app.attrib.get(NS+"usesCleartextTraffic") != "false":
            problems.append("Cleartext traffic is not explicitly disabled")
        if app.attrib.get(NS+"debuggable") == "true":
            problems.append("Production manifest enables debuggable")
        for child in app:
            if child.tag in ("activity","service","receiver","provider") and (
                    child.attrib.get(NS+"exported") == "true"):
                if not (child.tag == "activity" and
                        child.attrib.get(NS+"name") == ".MainActivity"):
                    problems.append("Unexpected exported component: " +
                                    child.attrib.get(NS+"name",child.tag))
    sources = {p.name: p.read_text(encoding="utf-8") for p in main.glob("*.kt")}
    media = sources.get("MediaLibrary.kt","")
    if ("OWNER_PACKAGE_NAME" not in media or
            "cursor.getString(ownerIndex) != context.packageName" not in media):
        problems.append("MediaStore gallery must validate owner on API 29+")
    if "FileInputStream(file)" in media or "input.copyTo(output)" in media:
        problems.append("Camera JPEG must be re-encoded, not copied with untrusted EXIF")
    if "quality.coerceIn(70, 100)" not in media:
        problems.append("JPEG quality not bounded before saving")
    if media.count("IS_PENDING, 0") < 2 or "!= 1" not in media:
        problems.append("MediaStore photo publish is not checked")
    for name, body in sources.items():
        for banned in ("Runtime.getRuntime().exec(", "ProcessBuilder(",
                       "WebView(", "HttpURLConnection(", "Socket("):
            if banned in body:
                problems.append(f"Unexpected execution/network primitive in {name}: {banned}")
    if "android.permission.INTERNET" not in uses:
        warnings.append("App has no INTERNET permission, but external ACTION_VIEW may open a browser")
    warnings.append("No real device, dynamic security, dependency vulnerability or screenshot audit")
    return {"passed": not problems, "files_scanned": len(sources),
            "problems": problems, "warnings": warnings}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--root", type=Path,
                   default=Path(__file__).resolve().parents[1])
    args = p.parse_args()
    report = audit(args.root)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if not report["passed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
