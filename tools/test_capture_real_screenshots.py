"""Live screenshot contract tests without faking Android screen captures."""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from capture_real_screenshots import (
    SCREENS, centered_mode_present, png_dimensions, find_label, publish_gallery
)


class RealScreenshotContractTest(unittest.TestCase):
    def test_standalone_permissions_screen_never_returns(self):
        names = [screen for screen, _ in SCREENS]
        self.assertNotIn("03_permissions", names)
        self.assertNotIn("18_privacy", names)
        self.assertEqual(len(names), len(set(names)))
        self.assertIn("05_photo", names)
        self.assertIn("13_gallery", names)

    def test_mode_needs_exact_centered_text(self):
        middle = ET.fromstring(
            '<hierarchy><node text="PRO" bounds="[495,2000][583,2040]"/>'
            '<node text="VIDEO" bounds="[160,2000][240,2040]"/></hierarchy>')
        self.assertTrue(centered_mode_present(middle, "PRO"))
        self.assertFalse(centered_mode_present(middle, "VIDEO"))
        self.assertFalse(centered_mode_present(middle, "PORTRET"))
        self.assertFalse(centered_mode_present(middle, "PRO CAMERA"))

    def test_rejects_unverified_or_missing_real_png_files(self):
        with self.assertRaises(ValueError):
            png_dimensions(b"not a PNG")
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "README.md").write_text(
                "<!-- BEGIN REAL LENSIGO SCREENSHOTS -->\n"
                "Pending\n<!-- END REAL LENSIGO SCREENSHOTS -->"
            )
            output = root / "docs/screenshots/real"
            output.mkdir(parents=True)
            # No captured bytes, so README cannot be overwritten with marketing claims.
            with self.assertRaises(RuntimeError):
                publish_gallery(root, output, "test-run")
            self.assertIn("Pending", (root / "README.md").read_text())


if __name__ == "__main__":
    unittest.main()
