#!/usr/bin/env python3
"""Rebuild the user-approved 512×512 LENSIGO launch icon, pixel-for-pixel.

This is the ORIGINAL brand generator geometry, not a screenshot approximation.
It is run before Gradle and never sends art or user data to an external API.
Dependencies are pinned in .github/workflows/android-preview.yml.
"""
import argparse
import hashlib
import io
from pathlib import Path
import cairosvg
import numpy as np
from PIL import Image, ImageDraw

SYMBOL = (
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">'
    '<defs><linearGradient id="lens" x1="0" y1="0" x2="1" y2="1">'
    '<stop stop-color="#08E7FD"/><stop offset="1" stop-color="#2679FF"/>'
    '</linearGradient></defs>'
    '<circle cx="50" cy="50" r="42" fill="#061829" '
    'stroke="url(#lens)" stroke-width="5"/>'
    + ''.join(
        '<path d="M50 50 L33 19 Q52 9 68 19 L66 48 Z" fill="url(#lens)" '
        'stroke="#071626" stroke-width="2" transform="rotate(' + str(a) +
        ' 50 50)"/>' for a in range(0,360,60)
    )
    + '<circle cx="50" cy="50" r="15" fill="#0B1018" '
    'stroke="#00D4FF" stroke-width="3"/>'
    '<circle cx="50" cy="50" r="6" fill="#00D4FF"/></svg>'
)
EXPECTED_PIXELS_SHA256 = "96538ced13ae75393f3feed7a06866a5d9f72f59e35d359c10dc67cca958e2df"

def rgb(value):
    return tuple(int(value.lstrip("#")[i:i+2], 16) for i in (0,2,4))

def render(path):
    size = 512
    dark = np.array(rgb("#071120"),dtype=float)
    light = np.array(rgb("#153853"),dtype=float)
    y = np.linspace(0,1,size,dtype=float)
    t = (np.sin(y*np.pi)*.22+(1-y)*.50)[:,None,None]
    bands = np.uint8(dark[None,None,:]*(1-t)+light[None,None,:]*t)
    background = Image.fromarray(
        np.broadcast_to(bands,(size,size,3)).copy(),"RGB")
    draw = ImageDraw.Draw(background)
    draw.rounded_rectangle((24,24,488,488),radius=112,fill="#081C32",
        outline="#3F9FC3",width=5)
    logo = Image.open(io.BytesIO(cairosvg.svg2png(
        bytestring=SYMBOL.encode(),output_width=size,output_height=size
    ))).convert("RGBA")
    background.paste(logo,(0,0),logo)
    digest = hashlib.sha256(background.tobytes()).hexdigest()
    if digest != EXPECTED_PIXELS_SHA256:
        raise RuntimeError("Icon pixels differ from approved 512px original: " + digest)
    path.parent.mkdir(parents=True,exist_ok=True)
    background.save(path)
    print("LENSIGO legacy launcher OK: original 512px pixels, " + digest)


def render_adaptive_foreground(path):
    """The exact approved aperture, centered inside Android's safe zone.

    Android 8+ launcher masks differ (circle, squircle, OEM), so the foreground
    must be transparent and contained within the central 66.7% safe zone.
    The full icon background is supplied by adaptive-icon XML, never a white
    OEM fallback plate around the legacy rectangle.
    """
    size = 512
    symbol_size = 352  # 68.75% canvas; aperture itself occupies 84%.
    raster = Image.open(io.BytesIO(cairosvg.svg2png(
        bytestring=SYMBOL.encode(), output_width=symbol_size,
        output_height=symbol_size
    ))).convert("RGBA")
    foreground = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    foreground.alpha_composite(raster, ((size-symbol_size)//2, (size-symbol_size)//2))
    box = foreground.getbbox()
    if box is None or min(box[0], box[1]) < 86 or max(box[2], box[3]) > 426:
        raise RuntimeError("Adaptive icon aperture exceeds the launcher safe zone: " + str(box))
    path.parent.mkdir(parents=True,exist_ok=True)
    foreground.save(path)
    print("LENSIGO adaptive foreground OK, bounds", box)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("output", type=Path)
    ap.add_argument("--adaptive-foreground", type=Path)
    args = ap.parse_args()
    render(args.output)
    if args.adaptive_foreground:
        render_adaptive_foreground(args.adaptive_foreground)
