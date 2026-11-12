package com.brendigo.lensigo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsOriginNavigationTest {
    @Test fun settingsReturnsToScreenThatOpenedIt() {
        for (origin in listOf(
            Screen.WELCOME, Screen.HOME, Screen.CAMERA,
            Screen.GALLERY, Screen.VIEWER, Screen.ABOUT, Screen.LICENCES
        )) {
            assertTrue(shouldRememberSettingsOrigin(origin, Screen.SETTINGS))
        }
    }

    @Test fun nestedSettingsDoesNotOverwritePreviousOrigin() {
        assertFalse(shouldRememberSettingsOrigin(Screen.SETTINGS, Screen.SETTINGS))
        assertFalse(shouldRememberSettingsOrigin(Screen.CAMERA_SETTINGS, Screen.SETTINGS))
    }

    @Test fun otherNavigationsMustNeverChangeSettingsBackDestination() {
        for (next in Screen.entries.filter { it != Screen.SETTINGS }) {
            assertFalse(shouldRememberSettingsOrigin(Screen.ABOUT, next))
        }
    }
}
