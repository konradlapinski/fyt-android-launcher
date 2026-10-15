package com.konradlapinski.fytlauncher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class LauncherConfig(
    val favoritePackages: List<String> = emptyList(),
    val navigationPackage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showMedia: Boolean = true,
) {
    fun normalized(): LauncherConfig = copy(
        // Keep empty positions: clearing slot 2 must not shift the user's slot 3 shortcut.
        favoritePackages = favoritePackages.take(MAX_FAVORITES).map(String::trim),
        navigationPackage = navigationPackage?.takeIf(String::isNotBlank),
    )

    fun toJson(): String {
        val normalized = normalized()
        return JSONObject()
            .put("version", FORMAT_VERSION)
            .put("favoritePackages", JSONArray(normalized.favoritePackages))
            .put("navigationPackage", normalized.navigationPackage ?: JSONObject.NULL)
            .put("themeMode", normalized.themeMode.name.lowercase(Locale.ROOT))
            .put("showMedia", normalized.showMedia)
            .toString(2)
    }

    companion object {
        const val MAX_FAVORITES = 4
        private const val FORMAT_VERSION = 1

        fun fromJson(json: String): LauncherConfig {
            val root = JSONObject(json)
            val favorites = buildList {
                val array = root.optJSONArray("favoritePackages") ?: JSONArray()
                for (index in 0 until array.length()) {
                    add(array.optString(index))
                }
            }
            val navigation = root.optString("navigationPackage")
                .takeIf { it.isNotBlank() && it != "null" }
            val theme = runCatching {
                ThemeMode.valueOf(root.optString("themeMode", "system").uppercase(Locale.ROOT))
            }.getOrDefault(ThemeMode.SYSTEM)

            return LauncherConfig(
                favoritePackages = favorites,
                navigationPackage = navigation,
                themeMode = theme,
                showMedia = root.optBoolean("showMedia", true),
            ).normalized()
        }
    }
}
