package com.brendigo.lensigo

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionFlowTest {
    @Test fun cameraIsAlwaysTheFirstRequiredPermission() {
        assertEquals(CapturePermission.CAMERA,
            CapturePermissionPolicy.nextMissing(false, true, false))
        assertEquals(CapturePermission.CAMERA,
            CapturePermissionPolicy.nextMissing(false, false, true))
    }

    @Test fun legacyStorageOnlyWhenCapturingOnAndroidSevenToNine() {
        assertEquals(CapturePermission.STORAGE,
            CapturePermissionPolicy.nextMissing(true, false, true))
        assertEquals(CapturePermission.NONE,
            CapturePermissionPolicy.nextMissing(true, false, false))
        assertEquals(CapturePermission.NONE,
            CapturePermissionPolicy.nextMissing(true, true, true))
    }
}
