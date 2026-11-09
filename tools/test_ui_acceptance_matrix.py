import json
import tempfile
import unittest
from pathlib import Path
from PIL import Image
from ui_acceptance_matrix import analyze
from pixel_audit_batch import SCREENS


class UiAcceptanceTest(unittest.TestCase):
    def test_ranks_actual_differences_and_fails_on_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            refs=root/"reference"
            actual=root/"actual"
            for device in ("mobitel","tablet"):
                (refs/device).mkdir(parents=True)
                (actual/device).mkdir(parents=True)
                for screen in SCREENS:
                    Image.new("RGB",(48,96),(12,34,50)).save(refs/device/screen)
                    Image.new("RGB",(48,96),(12,34,50)).save(actual/device/screen)
            Image.new("RGB",(48,96),(200,100,0)).save(actual/"mobitel/16_settings.png")
            result=analyze(refs,actual,root/"audit")
            self.assertEqual(result["compared"],36)
            self.assertFalse(result["all_36_pixel_identical"])
            self.assertEqual(result["largest_deviations"][0]["screen"],"16_settings")
            self.assertTrue((root/"audit/largest_deviations.csv").exists())
            self.assertTrue((root/"audit/acceptance.json").exists())
            (actual/"tablet/19_about.png").unlink()
            result2=analyze(refs,actual,root/"audit2")
            self.assertEqual(result2["compared"],35)
            self.assertIn("19_about.png",result2["devices"]["tablet"]["missing"])


if __name__=="__main__":unittest.main()
