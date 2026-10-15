package com.konradlapinski.fytlauncher.ui

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.konradlapinski.fytlauncher.MainActivity
import com.konradlapinski.fytlauncher.apps.LaunchableApp
import com.konradlapinski.fytlauncher.apps.loadLaunchableApps
import com.konradlapinski.fytlauncher.data.LauncherConfig
import com.konradlapinski.fytlauncher.data.LauncherConfigStore
import com.konradlapinski.fytlauncher.data.ThemeMode
import com.konradlapinski.fytlauncher.media.MediaCommand
import com.konradlapinski.fytlauncher.media.MediaSnapshot
import com.konradlapinski.fytlauncher.media.controlMedia
import com.konradlapinski.fytlauncher.media.isMediaAccessEnabled
import com.konradlapinski.fytlauncher.media.readActiveMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF355C70),
    background = Color(0xFFF3F5F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3E9ED),
    onSurfaceVariant = Color(0xFF40515C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF042F2E),
    secondary = Color(0xFF9CC9D7),
    background = Color(0xFF08111A),
    surface = Color(0xFF111E29),
    surfaceVariant = Color(0xFF233440),
    onSurfaceVariant = Color(0xFFC0CDD5),
)

private enum class Screen { DASHBOARD, SETTINGS }

private sealed interface PickerTarget {
    data object Navigation : PickerTarget
    data class Favorite(val slot: Int) : PickerTarget
}

@Composable
fun LauncherApp(activity: MainActivity, configStore: LauncherConfigStore) {
    val config by configStore.config.collectAsStateWithLifecycle(initialValue = LauncherConfig())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var screen by remember { mutableStateOf(Screen.DASHBOARD) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refreshState by remember { mutableLongStateOf(0L) }
    val currentConfig by rememberUpdatedState(config)

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            loadLaunchableApps(activity.packageManager)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshState += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            message = null
        }
    }

    LaunchedEffect(screen) {
        if (screen == Screen.DASHBOARD) {
            activity.window.decorView.keepScreenOn = false
        }
    }

    val roleRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        refreshState += 1
        message = if (result.resultCode == Activity.RESULT_OK) {
            "FYT Launcher is now the default Home app."
        } else {
            "Home app selection was not changed."
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(currentConfig.toJson())
                    } ?: error("Unable to open the selected file")
                }
            }.onSuccess { message = "Configuration backup saved." }
                .onFailure { message = "Could not save backup: ${it.localizedMessage ?: "file error"}" }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    } ?: error("Unable to open the selected file")
                    LauncherConfig.fromJson(json)
                }
            }.onSuccess { restored ->
                configStore.save(restored)
                message = "Configuration restored."
            }.onFailure { message = "Could not restore backup: ${it.localizedMessage ?: "invalid file"}" }
        }
    }

    BackHandler(enabled = screen == Screen.SETTINGS) {
        screen = Screen.DASHBOARD
    }

    val isDark = when (config.themeMode) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (isDark) DarkColors else LightColors

    SideEffect {
        activity.window.statusBarColor = colors.background.toArgb()
        activity.window.navigationBarColor = colors.background.toArgb()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (screen == Screen.DASHBOARD) {
                    DashboardScreen(
                        config = config,
                        apps = apps,
                        refreshState = refreshState,
                        onSettings = { screen = Screen.SETTINGS },
                        onNavigation = {
                            val target = apps.firstOrNull { it.packageName == config.navigationPackage }
                            if (target == null) {
                                pickerTarget = PickerTarget.Navigation
                            } else {
                                launchApp(activity, target.packageName, onFailure = { message = it })
                            }
                        },
                        onPickFavorite = { pickerTarget = PickerTarget.Favorite(it) },
                        onOpenFavorite = { packageName ->
                            launchApp(activity, packageName, onFailure = { message = it })
                        },
                        onEnableMediaAccess = { openNotificationListenerSettings(activity) },
                    )
                } else {
                    SettingsScreen(
                        config = config,
                        apps = apps,
                        isHomeRoleHeld = isHomeRoleHeld(activity),
                        mediaAccessEnabled = isMediaAccessEnabled(context),
                        onBack = { screen = Screen.DASHBOARD },
                        onPickNavigation = { pickerTarget = PickerTarget.Navigation },
                        onPickFavorite = { pickerTarget = PickerTarget.Favorite(it) },
                        onClearFavorite = { index ->
                            updateConfig(configStore, scope, config) { old ->
                                old.copy(favoritePackages = old.favoritePackages.withSlot(index, null))
                            }
                        },
                        onMediaToggle = { enabled ->
                            updateConfig(configStore, scope, config) { it.copy(showMedia = enabled) }
                        },
                        onThemeSelected = { theme ->
                            updateConfig(configStore, scope, config) { it.copy(themeMode = theme) }
                        },
                        onSetAsHome = {
                            requestHomeRole(activity, roleRequest, onMessage = { message = it })
                        },
                        onChooseOtherHome = { openHomeSettings(activity, onFailure = { message = it }) },
                        onEnableMediaAccess = { openNotificationListenerSettings(activity) },
                        onBackup = { backupLauncher.launch("fyt-launcher-backup.json") },
                        onRestore = {
                            restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                        },
                    )
                }

                SnackbarHost(
                    hostState = snackbarHost,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                )
            }
        }
    }

    pickerTarget?.let { target ->
        val currentlySelected = when (target) {
            PickerTarget.Navigation -> config.navigationPackage
            is PickerTarget.Favorite -> config.favoritePackages.getOrNull(target.slot)
        }
        AppPickerDialog(
            title = if (target == PickerTarget.Navigation) "Choose navigation app" else "Choose an app",
            apps = apps,
            selectedPackage = currentlySelected,
            onDismiss = { pickerTarget = null },
            onChoose = { selected ->
                when (target) {
                    PickerTarget.Navigation -> updateConfig(configStore, scope, config) {
                        it.copy(navigationPackage = selected?.packageName)
                    }
                    is PickerTarget.Favorite -> updateConfig(configStore, scope, config) {
                        it.copy(favoritePackages = it.favoritePackages.withSlot(target.slot, selected?.packageName))
                    }
                }
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun DashboardScreen(
    config: LauncherConfig,
    apps: List<LaunchableApp>,
    refreshState: Long,
    onSettings: () -> Unit,
    onNavigation: () -> Unit,
    onPickFavorite: (Int) -> Unit,
    onOpenFavorite: (String) -> Unit,
    onEnableMediaAccess: () -> Unit,
) {
    val appByPackage = remember(apps) { apps.associateBy(LaunchableApp::packageName) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val now = remember(nowMillis) { Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(76.dp)) {
            val headerWidth = maxWidth
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FYT Launcher", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(dateFormatter.format(now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (headerWidth >= 420.dp) {
                    Text(timeFormatter.format(now), fontSize = if (headerWidth >= 760.dp) 34.sp else 26.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.width(if (headerWidth >= 760.dp) 18.dp else 8.dp))
                }
                OutlinedButton(onClick = onSettings, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("SETTINGS")
                }
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= 760.dp) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    NavigationCard(
                        app = config.navigationPackage?.let(appByPackage::get),
                        hasSelection = config.navigationPackage != null,
                        onClick = onNavigation,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FavoriteGrid(
                        favorites = config.favoritePackages,
                        appByPackage = appByPackage,
                        onPick = onPickFavorite,
                        onOpen = onOpenFavorite,
                        modifier = Modifier.weight(1.12f).fillMaxHeight(),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    NavigationCard(
                        app = config.navigationPackage?.let(appByPackage::get),
                        hasSelection = config.navigationPackage != null,
                        onClick = onNavigation,
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                    )
                    FavoriteGrid(
                        favorites = config.favoritePackages,
                        appByPackage = appByPackage,
                        onPick = onPickFavorite,
                        onOpen = onOpenFavorite,
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                    )
                }
            }
        }

        if (config.showMedia) {
            MediaPanel(
                refreshState = refreshState,
                onEnableAccess = onEnableMediaAccess,
                modifier = Modifier.fillMaxWidth().heightIn(min = 108.dp, max = 128.dp),
            )
        }
    }
}

@Composable
private fun NavigationCard(
    app: LaunchableApp?,
    hasSelection: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "Navigation app: ${app?.label ?: if (hasSelection) "unavailable" else "choose app"}" },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(26.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("NAVIGATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f), fontWeight = FontWeight.Bold)
            Column {
                Text(app?.label ?: if (hasSelection) "App unavailable" else "Choose navigation app", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(if (app == null) "Tap to select an installed app" else "Open ${app.label}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("OPEN APP  ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FavoriteGrid(
    favorites: List<String>,
    appByPackage: Map<String, LaunchableApp>,
    onPick: (Int) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) { rowIndex ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { columnIndex ->
                    val index = rowIndex * 2 + columnIndex
                    val app = favorites.getOrNull(index)?.let(appByPackage::get)
                    AppTile(
                        app = app,
                        missingSelection = favorites.getOrNull(index) != null && app == null,
                        onClick = { if (app == null) onPick(index) else onOpen(app.packageName) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: LaunchableApp?,
    missingSelection: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = when {
                app != null -> "Open ${app.label}"
                missingSelection -> "Choose replacement app"
                else -> "Add app shortcut"
            }
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (app != null) AppGlyph(app, Modifier.size(52.dp)) else {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (missingSelection) "!" else "+", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = app?.label ?: if (missingSelection) "Select replacement" else "Add shortcut",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppGlyph(app: LaunchableApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(app.packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(app.packageName)
            val size = 112
            createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "${app.label} icon", modifier = modifier.clip(RoundedCornerShape(18.dp)))
    } else {
        Box(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun MediaPanel(
    refreshState: Long,
    onEnableAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var accessEnabled by remember { mutableStateOf(isMediaAccessEnabled(context)) }
    var snapshot by remember { mutableStateOf<MediaSnapshot?>(null) }

    LaunchedEffect(refreshState) {
        accessEnabled = isMediaAccessEnabled(context)
    }
    LaunchedEffect(accessEnabled, refreshState) {
        snapshot = null
        if (accessEnabled) {
            while (true) {
                snapshot = withContext(Dispatchers.IO) { readActiveMedia(context) }
                delay(2_000)
            }
        }
    }

    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("MEDIA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                when {
                    !accessEnabled -> Text("Allow notification access to show playback controls", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    snapshot == null -> Text("No active media session", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        Text(snapshot?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(snapshot?.artist.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (!accessEnabled) {
                OutlinedButton(onClick = onEnableAccess, modifier = Modifier.heightIn(min = 52.dp)) { Text("ENABLE") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { snapshot?.let { controlMedia(context, it.packageName, MediaCommand.PREVIOUS) } },
                        enabled = snapshot != null,
                        modifier = Modifier.heightIn(min = 52.dp),
                    ) { Text("PREV") }
                    Button(
                        onClick = { snapshot?.let { controlMedia(context, it.packageName, MediaCommand.TOGGLE) } },
                        enabled = snapshot != null,
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) { Text(if (snapshot?.isPlaying == true) "PAUSE" else "PLAY") }
                    TextButton(
                        onClick = { snapshot?.let { controlMedia(context, it.packageName, MediaCommand.NEXT) } },
                        enabled = snapshot != null,
                        modifier = Modifier.heightIn(min = 52.dp),
                    ) { Text("NEXT") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    config: LauncherConfig,
    apps: List<LaunchableApp>,
    isHomeRoleHeld: Boolean,
    mediaAccessEnabled: Boolean,
    onBack: () -> Unit,
    onPickNavigation: () -> Unit,
    onPickFavorite: (Int) -> Unit,
    onClearFavorite: (Int) -> Unit,
    onMediaToggle: (Boolean) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onSetAsHome: () -> Unit,
    onChooseOtherHome: () -> Unit,
    onEnableMediaAccess: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val appByPackage = remember(apps) { apps.associateBy(LaunchableApp::packageName) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) { Text("‹  BACK") }
            Spacer(Modifier.width(12.dp))
            Text("Launcher settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("settingsList"),
            contentPadding = PaddingValues(top = 6.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsCard(title = "Default Home app", description = if (isHomeRoleHeld) "FYT Launcher currently handles the Home button." else "Choose FYT Launcher as your Android Home app.") {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth < 560.dp) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onSetAsHome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                    Text(if (isHomeRoleHeld) "CURRENT HOME APP" else "SET AS HOME APP")
                                }
                                OutlinedButton(onClick = onChooseOtherHome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                    Text("CHOOSE OTHER HOME APP")
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onSetAsHome, modifier = Modifier.heightIn(min = 52.dp)) {
                                    Text(if (isHomeRoleHeld) "CURRENT HOME APP" else "SET AS HOME APP")
                                }
                                OutlinedButton(onClick = onChooseOtherHome, modifier = Modifier.heightIn(min = 52.dp)) {
                                    Text("CHOOSE OTHER HOME APP")
                                }
                            }
                        }
                    }
                }
            }
            item {
                SettingsCard(title = "App shortcuts", description = "Choose up to four large dashboard shortcuts.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(LauncherConfig.MAX_FAVORITES) { index ->
                            val selected = config.favoritePackages.getOrNull(index)?.let(appByPackage::get)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Slot ${index + 1}", modifier = Modifier.width(68.dp), style = MaterialTheme.typography.bodyLarge)
                                OutlinedButton(onClick = { onPickFavorite(index) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                                    Text(selected?.label ?: "Choose app", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(
                                    onClick = { onClearFavorite(index) },
                                    enabled = config.favoritePackages.getOrNull(index) != null,
                                    modifier = Modifier.heightIn(min = 52.dp),
                                ) { Text("CLEAR") }
                            }
                        }
                    }
                }
            }
            item {
                val selectedNavigation = config.navigationPackage?.let(appByPackage::get)
                SettingsCard(title = "Navigation app", description = "This opens a separate navigation app. Maps are not embedded in this launcher.") {
                    OutlinedButton(onClick = onPickNavigation, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(selectedNavigation?.label ?: if (config.navigationPackage == null) "Choose installed app" else "Choose replacement app")
                    }
                }
            }
            item {
                SettingsCard(title = "Media controls", description = "MediaSession controls need optional notification-listener access. No notification text is read or stored.") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (config.showMedia) "Show dashboard media panel" else "Media panel hidden", style = MaterialTheme.typography.titleMedium)
                            Text(if (mediaAccessEnabled) "Access enabled" else "Access not enabled", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = config.showMedia, onCheckedChange = onMediaToggle)
                    }
                    if (config.showMedia) {
                        OutlinedButton(onClick = onEnableMediaAccess, modifier = Modifier.heightIn(min = 52.dp)) {
                            Text(if (mediaAccessEnabled) "OPEN ACCESS SETTINGS" else "ENABLE MEDIA ACCESS")
                        }
                    }
                }
            }
            item {
                SettingsCard(title = "Appearance", description = "Select a theme for the dashboard.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.themeMode == mode,
                                onClick = { onThemeSelected(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard(title = "Backup and restore", description = "Export your shortcut and appearance settings to a JSON file.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onBackup, modifier = Modifier.heightIn(min = 52.dp)) { Text("SAVE BACKUP") }
                        OutlinedButton(onClick = onRestore, modifier = Modifier.heightIn(min = 52.dp)) { Text("RESTORE BACKUP") }
                    }
                }
            }
            item {
                Text(
                    "FM radio, vehicle data, and factory-specific controls are not enabled because this unit's supported APIs have not been verified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        })
    }
}

@Composable
private fun AppPickerDialog(
    title: String,
    apps: List<LaunchableApp>,
    selectedPackage: String?,
    onDismiss: () -> Unit,
    onChoose: (LaunchableApp?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.widthIn(min = 280.dp, max = 620.dp)) {
                if (apps.isEmpty()) {
                    Text("No launchable apps were found on this device.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(apps, key = LaunchableApp::packageName) { app ->
                            TextButton(
                                onClick = { onChoose(app) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).semantics {
                                    contentDescription = "Select ${app.label}"
                                },
                            ) {
                                AppGlyph(app, Modifier.size(38.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (app.packageName == selectedPackage) Text("SELECTED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onChoose(null) }) { Text("CLEAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

private fun updateConfig(
    store: LauncherConfigStore,
    scope: kotlinx.coroutines.CoroutineScope,
    config: LauncherConfig,
    transform: (LauncherConfig) -> LauncherConfig,
) {
    scope.launch { store.save(transform(config)) }
}

private fun List<String>.withSlot(index: Int, packageName: String?): List<String> {
    val slots = MutableList(LauncherConfig.MAX_FAVORITES) { this.getOrNull(it).orEmpty() }
    slots[index] = packageName.orEmpty()
    return slots
}

private fun launchApp(context: Context, packageName: String, onFailure: (String) -> Unit) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent == null) {
        onFailure("That app is no longer available. Choose another app in Settings.")
        return
    }
    runCatching { context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { onFailure("Could not open that app: ${it.localizedMessage ?: "launch failed"}") }
}

private fun isHomeRoleHeld(activity: Activity): Boolean {
    val manager = activity.getSystemService(RoleManager::class.java) ?: return false
    return manager.isRoleAvailable(RoleManager.ROLE_HOME) && manager.isRoleHeld(RoleManager.ROLE_HOME)
}

private fun requestHomeRole(
    activity: Activity,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onMessage: (String) -> Unit,
) {
    val manager = activity.getSystemService(RoleManager::class.java)
    if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        if (manager.isRoleHeld(RoleManager.ROLE_HOME)) {
            onMessage("FYT Launcher is already the default Home app.")
        } else {
            launcher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
        return
    }
    openHomeSettings(activity, onFailure = onMessage)
}

private fun openHomeSettings(context: Context, onFailure: (String) -> Unit) {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { onFailure("Open Android Settings and select Home app to return to the factory launcher.") }
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
