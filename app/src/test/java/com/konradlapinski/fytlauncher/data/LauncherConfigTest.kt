package com.konradlapinski.fytlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherConfigTest {
    @Test
    fun normalizationCapsSlotsWithoutMovingShortcuts() {
        val config = LauncherConfig(
            favoritePackages = listOf(" com.maps ", "", "com.maps", "com.music", "com.phone", "com.extra"),
        ).normalized()

        assertEquals(listOf("com.maps", "", "com.maps", "com.music"), config.favoritePackages)
        assertEquals(LauncherConfig.MAX_FAVORITES, LauncherConfig(
            favoritePackages = listOf("a", "b", "c", "d", "e"),
        ).normalized().favoritePackages.size)
    }

    @Test
    fun blankNavigationPackageIsNotPersisted() {
        val config = LauncherConfig(navigationPackage = "  ").normalized()

        assertEquals(null, config.navigationPackage)
    }

    @Test
    fun defaultsStartWithSystemThemeAndMediaPanelEnabled() {
        val config = LauncherConfig()

        assertTrue(config.favoritePackages.isEmpty())
        assertEquals(ThemeMode.SYSTEM, config.themeMode)
        assertTrue(config.showMedia)
    }
}
