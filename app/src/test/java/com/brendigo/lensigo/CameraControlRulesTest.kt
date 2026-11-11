package com.brendigo.lensigo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraControlRulesTest {
    @Test fun sixtyFpsRequiresAnAdvertisedCameraRange() {
        assertTrue(CameraControlRules.supports60Fps(listOf(15..60, 30..30)))
        assertTrue(CameraControlRules.supports60Fps(listOf(60..60)))
        assertFalse(CameraControlRules.supports60Fps(listOf(15..30, 24..30)))
        assertFalse(CameraControlRules.supports60Fps(emptyList()))
    }

    @Test fun audioCanBeDisabledWithoutRevokingMicrophonePermission() {
        assertFalse(CameraControlRules.recordAudioEnabled(false, true))
        assertFalse(CameraControlRules.recordAudioEnabled(true, false))
        assertTrue(CameraControlRules.recordAudioEnabled(true, true))
    }

    @Test fun shutterIsDisabledUntilCaptureUseCaseIsReady() {
        assertFalse(CameraControlRules.shutterEnabled(true,false,0,false,false,false))
        assertTrue(CameraControlRules.shutterEnabled(true,false,0,false,false,true))
        assertFalse(CameraControlRules.shutterEnabled(false,false,0,false,false,true))
        assertFalse(CameraControlRules.shutterEnabled(true,true,0,false,false,true))
        assertFalse(CameraControlRules.shutterEnabled(true,false,2,false,false,true))
    }

    @Test fun videoMayStopWhenRecordingEvenAfterUseCaseDisappears() {
        assertTrue(CameraControlRules.shutterEnabled(true,false,0,true,true,false))
        assertFalse(CameraControlRules.shutterEnabled(true,false,0,true,false,false))
        // A double-tap must not send a second stop before CameraX Finalize.
        assertFalse(CameraControlRules.shutterEnabled(
            true,false,0,true,true,false,stoppingRecording = true))
    }

    @Test fun zoomPresetsRespectHardwareAndUnusualRanges() {
        assertEquals(listOf(1f,2f), CameraControlRules.zoomPresets(1f,4f))
        assertEquals(listOf(.6f,1f,2f,5f), CameraControlRules.zoomPresets(.5f,6f))
        assertEquals(listOf(1.2f), CameraControlRules.zoomPresets(1.2f,1.8f))
        assertTrue(CameraControlRules.zoomPresets(2f,1f).isEmpty())
    }

    @Test fun zoomLabelsAreAccurate() {
        assertEquals("0.6", CameraControlRules.zoomLabel(.6f))
        assertEquals("1x", CameraControlRules.zoomLabel(1f))
        assertEquals("1.2x", CameraControlRules.zoomLabel(1.2f))
    }
}
