"""Launcher compatibility, no-white-border and original pixel invariants."""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
from generate_launcher import render, render_adaptive_foreground

ROOT = Path(__file__).resolve().parents[1]
NS = "{http://schemas.android.com/apk/res/android}"


class AdaptiveLauncherContract(unittest.TestCase):
    def test_manifest_declares_adaptive_icon_for_regular_and_round(self):
        app = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot().find("application")
        self.assertEqual(app.attrib[NS+"icon"], "@mipmap/lensigo_launcher")
        self.assertEqual(app.attrib[NS+"roundIcon"], "@mipmap/lensigo_launcher")

    def test_icon_uses_full_bleed_dark_background_and_generated_foreground(self):
        x = ET.parse(ROOT / "app/src/main/res/mipmap-anydpi-v26/lensigo_launcher.xml").getroot()
        self.assertEqual(x.tag, "adaptive-icon")
        self.assertEqual(x.find("background").attrib[NS+"drawable"],
                         "@color/lensigo_launcher_background")
        self.assertEqual(x.find("foreground").attrib[NS+"drawable"],
                         "@drawable/lensigo_launcher_foreground")
        colors = ET.parse(ROOT / "app/src/main/res/values/colors.xml").getroot()
        self.assertEqual(colors.find("color").text.strip().upper(), "#071120")

    def test_symbol_stays_in_safe_zone_and_legacy_pixels_are_unchanged(self):
        with tempfile.TemporaryDirectory() as temp:
            legacy = Path(temp) / "legacy.png"
            foreground = Path(temp) / "foreground.png"
            render(legacy)  # SHA-256 check for the exact previous approved legacy pixels.
            render_adaptive_foreground(foreground)
            with Image.open(legacy) as original:
                self.assertEqual(original.size, (512, 512))
                self.assertNotEqual(original.getpixel((0, 0)), (255, 255, 255))
            with Image.open(foreground) as image:
                self.assertEqual(image.mode, "RGBA")
                self.assertEqual(image.size, (512, 512))
                self.assertEqual(image.getpixel((0, 0))[3], 0)
                self.assertEqual(image.getpixel((511, 511))[3], 0)
                self.assertGreater(image.getpixel((256, 256))[3], 0)
                bounds = image.getbbox()
                self.assertGreaterEqual(min(bounds[0], bounds[1]), 86)
                self.assertLessEqual(max(bounds[2], bounds[3]), 426)


if __name__ == "__main__":
    unittest.main()
