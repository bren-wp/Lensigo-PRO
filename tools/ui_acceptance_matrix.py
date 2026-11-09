#!/usr/bin/env python3
"""Rank pixel deviations for all 19 phone + 19 tablet screenshots.

This does not fabricate emulator captures, silently resize them, or mask
active UI. Missing/invalid images remain failures, not zero-percent errors.
"""
import argparse
import csv
import json
from pathlib import Path
from pixel_audit_batch import run


def analyze(reference_root: Path, actual_root: Path, output_root: Path) -> dict:
    output_root.mkdir(parents=True,exist_ok=True)
    per_device={}
    rankings=[]
    for device in ("mobitel","tablet"):
        device_out=output_root/device
        summary=run(reference_root/device,actual_root/device,device_out)
        per_device[device]=summary
        for path in sorted(device_out.glob("*.json")):
            if path.name=="summary.json":continue
            report=json.loads(path.read_text(encoding="utf-8"))
            rankings.append({
                "device":device,
                "screen":path.stem,
                "mae_rgb":report["mae_rgb"],
                "similar_pixels_tolerance_12":report["similar_pixels_tolerance_12"],
                "strictly_identical":report["strictly_identical"],
                "actual":report["actual"],
                "reference":report["reference"],
            })
    rankings.sort(key=lambda row:-row["mae_rgb"])
    with (output_root/"largest_deviations.csv").open("w",newline="",encoding="utf-8") as out:
        writer=csv.DictWriter(out,fieldnames=[
            "device","screen","mae_rgb","similar_pixels_tolerance_12",
            "strictly_identical","actual","reference"])
        writer.writeheader()
        writer.writerows(rankings)
    verified=all(x["all_screens_identical"] for x in per_device.values())
    result={
        "expected":38,
        "compared":len(rankings),
        "all_38_pixel_identical":verified,
        "devices":per_device,
        "largest_deviations":rankings[:10],
        "note":"Camera scene pixels differ naturally. Compare deterministic UI regions "
               "separately without masking HUD, icons, text or controls. "
               "No missing screenshot counts as 0% deviation."
    }
    (output_root/"acceptance.json").write_text(
        json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
    return result


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--reference-root",type=Path,required=True)
    p.add_argument("--actual-root",type=Path,required=True)
    p.add_argument("--output-root",type=Path,required=True)
    a=p.parse_args()
    result=analyze(a.reference_root,a.actual_root,a.output_root)
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if not result["all_38_pixel_identical"]:raise SystemExit(1)


if __name__=="__main__":main()
