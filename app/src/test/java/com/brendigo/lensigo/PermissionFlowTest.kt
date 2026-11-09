package com.brendigo.lensigo

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionFlowTest {
    @Test fun cameraIsAlwaysRequiredBeforeEnteringCapture() {
        assertEquals(PermissionNextStep.CAMERA,
            nextPermissionStep(cameraGranted = false,
                storageGranted = true, storageRequired = false))
        assertEquals(PermissionNextStep.CAMERA,
            nextPermissionStep(cameraGranted = false,
                storageGranted = false, storageRequired = true))
    }

    @Test fun legacyStoragePermissionIsRequiredOnAndroidSevenToNine() {
        assertEquals(PermissionNextStep.STORAGE,
            nextPermissionStep(cameraGranted = true,
                storageGranted = false, storageRequired = true))
    }

    @Test fun noExtraStoragePermissionOnScopedStorageDevices() {
        assertEquals(PermissionNextStep.CONTINUE,
            nextPermissionStep(cameraGranted = true,
                storageGranted = false, storageRequired = false))
        assertEquals(PermissionNextStep.CONTINUE,
            nextPermissionStep(cameraGranted = true,
                storageGranted = true, storageRequired = true))
    }
}
