package com.konradlapinski.fytlauncher.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings

data class MediaSnapshot(
    val packageName: String,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
)

fun isMediaAccessEnabled(context: Context): Boolean {
    val granted = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    val expected = ComponentName(context, MediaAccessService::class.java)
    return granted.split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { it == expected }
}

fun readActiveMedia(context: Context): MediaSnapshot? {
    if (!isMediaAccessEnabled(context)) return null
    return runCatching {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, MediaAccessService::class.java)
        val sessions = manager.getActiveSessions(listener)
        val controller = sessions.firstOrNull { it.metadata != null } ?: sessions.firstOrNull()
            ?: return null
        controller.toSnapshot()
    }.getOrNull()
}

fun controlMedia(context: Context, packageName: String, command: MediaCommand) {
    if (!isMediaAccessEnabled(context)) return
    runCatching {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, MediaAccessService::class.java)
        val controller = manager.getActiveSessions(listener)
            .firstOrNull { it.packageName == packageName }
            ?: return
        when (command) {
            MediaCommand.PREVIOUS -> controller.transportControls.skipToPrevious()
            MediaCommand.TOGGLE -> {
                val playing = controller.playbackState?.state in PLAYING_STATES
                if (playing) controller.transportControls.pause() else controller.transportControls.play()
            }
            MediaCommand.NEXT -> controller.transportControls.skipToNext()
        }
    }
}

enum class MediaCommand { PREVIOUS, TOGGLE, NEXT }

private fun MediaController.toSnapshot(): MediaSnapshot {
    val metadata = metadata
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: "Unknown track"
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: ""
    return MediaSnapshot(
        packageName = packageName,
        title = title,
        artist = artist,
        isPlaying = playbackState?.state in PLAYING_STATES,
    )
}

private val PLAYING_STATES = setOf(
    android.media.session.PlaybackState.STATE_PLAYING,
    android.media.session.PlaybackState.STATE_BUFFERING,
    android.media.session.PlaybackState.STATE_CONNECTING,
)
