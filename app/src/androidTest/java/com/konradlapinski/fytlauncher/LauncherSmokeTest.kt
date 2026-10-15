package com.konradlapinski.fytlauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.konradlapinski.fytlauncher.data.LauncherConfig
import com.konradlapinski.fytlauncher.data.ThemeMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardLoadsWithNavigationAndShortcutSlots() {
        compose.onNodeWithText("FYT Launcher").assertIsDisplayed()
        compose.onNodeWithText("Choose navigation app").assertIsDisplayed()
    }

    @Test
    fun settingsExposeHomeRecoveryAndBackup() {
        compose.onNodeWithText("SETTINGS").performClick()
        compose.onNodeWithText("Launcher settings").assertIsDisplayed()
        compose.onNodeWithText("CHOOSE OTHER HOME APP").assertIsDisplayed()
        compose.onNodeWithTag("settingsList").performScrollToIndex(5)
        compose.onNodeWithText("SAVE BACKUP").assertIsDisplayed()
        compose.onNodeWithText("RESTORE BACKUP").assertIsDisplayed()
    }

    @Test
    fun jsonBackupRoundTripPreservesSettingsAndRejectsMalformedFiles() {
        val original = LauncherConfig(
            favoritePackages = listOf("com.maps", "", "com.music"),
            navigationPackage = "com.maps",
            themeMode = ThemeMode.DARK,
            showMedia = false,
        )
        assertEquals(original, LauncherConfig.fromJson(original.toJson()))
        assertTrue(runCatching { LauncherConfig.fromJson("not a backup") }.isFailure)
    }
}
