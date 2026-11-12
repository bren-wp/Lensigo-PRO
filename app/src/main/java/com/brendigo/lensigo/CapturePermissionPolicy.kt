package com.brendigo.lensigo

/** No in-app permission page: request Android-native permissions at point of use. */
internal enum class CapturePermission { CAMERA, STORAGE, NONE }

internal object CapturePermissionPolicy {
    fun nextMissing(
        cameraGranted: Boolean,
        storageGranted: Boolean,
        legacyStorageRequired: Boolean
    ): CapturePermission = when {
        !cameraGranted -> CapturePermission.CAMERA
        legacyStorageRequired && !storageGranted -> CapturePermission.STORAGE
        else -> CapturePermission.NONE
    }
}
