"""Tests verify the static privacy audit detects regressions, not just passes."""
import tempfile
import unittest
from pathlib import Path
from security_audit import audit

MANIFEST = """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.CAMERA"/>
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
      android:maxSdkVersion="28"/>
  <application android:allowBackup="false" android:usesCleartextTraffic="false">
    <activity android:name=".MainActivity" android:exported="true"/>
  </application>
</manifest>"""
MEDIA = """// OWNER_PACKAGE_NAME
if (cursor.getString(ownerIndex) != context.packageName) continue
quality.coerceIn(70, 100)
IS_PENDING, 0
IS_PENDING, 0
if (published != 1) error("failed")
"""


class SecurityContractTest(unittest.TestCase):
    def project(self, root, manifest=MANIFEST, media=MEDIA):
        p = root/"app/src/main/java/com/brendigo/lensigo"
        p.mkdir(parents=True)
        (root/"app/src/main").mkdir(exist_ok=True, parents=True)
        (root/"app/src/main/AndroidManifest.xml").write_text(manifest)
        (p/"MediaLibrary.kt").write_text(media)

    def test_minimal_secure_manifest_and_media_pass(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); self.project(root)
            self.assertTrue(audit(root)["passed"])

    def test_internet_location_and_backup_regressions_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            unsafe=MANIFEST.replace("<application",
                '<uses-permission android:name="android.permission.INTERNET"/>' +
                '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>' +
                '<application')
            unsafe=unsafe.replace('android:allowBackup="false"',
                                  'android:allowBackup="true"')
            self.project(root,manifest=unsafe)
            report=audit(root)
            self.assertFalse(report["passed"])
            self.assertGreaterEqual(len(report["problems"]),3)

    def test_other_exported_components_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            unsafe=MANIFEST.replace("</application>",
                '<provider android:name=".Data" android:exported="true"/>' +
                '</application>')
            self.project(root,manifest=unsafe)
            self.assertFalse(audit(root)["passed"])

    def test_media_spoofing_and_raw_exif_copy_detected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            unsafe=MEDIA.replace('cursor.getString(ownerIndex) != context.packageName',
                                 'true') + 'FileInputStream(file)'
            self.project(root,media=unsafe)
            report=audit(root)
            self.assertFalse(report["passed"])
            self.assertTrue(any("EXIF" in message for message in report["problems"]))
            self.assertTrue(any("owner" in message for message in report["problems"]))


if __name__=="__main__":
    unittest.main()
