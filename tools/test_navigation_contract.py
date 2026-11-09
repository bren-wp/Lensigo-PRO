"""App navigation regression: no dedicated Privacy screen or dead link.

This checks source wiring, not physical Android UI operation.
"""
import unittest
from pathlib import Path
from pixel_audit_batch import SCREENS

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/brendigo/lensigo"


class NavigationContractTest(unittest.TestCase):
    def test_privacy_page_and_routes_are_removed(self):
        main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        info = (SRC / "InfoScreens.kt").read_text(encoding="utf-8")
        settings = (SRC / "SettingsScreens.kt").read_text(encoding="utf-8")
        self.assertNotIn("Screen.PRIVACY", main)
        self.assertNotIn("LensigoPrivacyScreen", info)
        self.assertNotIn("onPrivacy:", info)
        self.assertNotIn("onPrivacy:", settings)
        self.assertNotIn("18_privacy.png", SCREENS)
        self.assertIn("Screen.ABOUT", main)
        self.assertIn("Screen.CAMERA_SETTINGS", main)

    def test_real_privacy_protections_and_about_version_remain(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        info = (SRC / "InfoScreens.kt").read_text()
        settings = (SRC / "SettingsScreens.kt").read_text()
        self.assertIn('android:allowBackup="false"', manifest)
        self.assertNotIn("android.permission.INTERNET", manifest)
        self.assertIn("Lokacija u EXIF-u", settings)
        self.assertIn("Verzija $version", info)
        self.assertIn("onLicences: () -> Unit", info)


if __name__ == "__main__":
    unittest.main()
