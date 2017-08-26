package jp.blanktar.ruumusic.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.client.main.MainActivity
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.util.RuuFileBase


class MediaSessionEndpoint(val context: Context, controller: RuuService.Controller, initialStatus: PlayingStatus, initialPlaylist: Playlist?) : Endpoint {
    override val supported = true

    val preference
        get() = Preference(context)
    val mediaSession: MediaSessionCompat
    val sessionToken: MediaSessionCompat.Token
        get() = mediaSession.sessionToken
    
    var queueIndex: Long = 0

    init {
        val componentName = ComponentName(context.packageName, MediaButtonReceiver::class.java.name)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.component = componentName

        val openPlayerIntent = Intent(context, MainActivity::class.java)
        openPlayerIntent.action = MainActivity.ACTION_OPEN_PLAYER

        mediaSession = MediaSessionCompat(context, "RuuMusicService", componentName, PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0))
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setSessionActivity(PendingIntent.getActivity(context, 0, openPlayerIntent, 0))

        onStatusUpdated(initialStatus)
        if (initialPlaylist != null) {
            updateQueue(initialPlaylist)
        }

        mediaSession.isActive = true

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                controller.play()
            }

            override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                controller.play(mediaId!!)
            }

            override fun onPlayFromUri(uri: Uri, extras: Bundle) {
                controller.play(uri.path)
            }

            override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                controller.playSearch(extras!!.getCharSequence("path", preference.RootDirectory.get()).toString(), query)
            }

            override fun onSkipToQueueItem(id: Long) {
                controller.play(id)
            }

            override fun onPause() {
                controller.pause()
            }

            override fun onStop() {
                controller.pause()
            }

            override fun onSkipToNext() {
                controller.next()
            }

            override fun onSkipToPrevious() {
                controller.prev()
            }

            override fun onSeekTo(pos: Long) {
                controller.seek(pos)
            }

            override fun onSetRepeatMode(repeatMode: Int) {
                controller.setRepeatMode(when (repeatMode) {
                    PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatModeType.SINGLE
                    PlaybackStateCompat.REPEAT_MODE_ALL -> RepeatModeType.LOOP
                    else -> RepeatModeType.OFF
                })
            }

            override fun onSetShuffleModeEnabled(shuffleMode: Boolean) {
                controller.setShuffleMode(shuffleMode)
            }
        })
    }

    override fun close() {
        mediaSession.release()
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        updateMetadata(status)
        updatePlaybackState(status)
    }

    override fun onMediaStoreUpdated() {}

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onFailedPlay(status: PlayingStatus) {
        onError(context.getString(R.string.failed_play, status.currentMusic?.realPath), status)
    }

    override fun onError(message: String, status: PlayingStatus) {
        updateMetadata(status)
        updatePlaybackState(status, message)
    }

    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {
        onError(context.getString(if (isFirst) R.string.first_of_directory else R.string.last_of_directory, status.currentMusic?.realPath), status)
    }

    private val displayIcon = BitmapFactory.decodeResource(context.resources, R.drawable.display_icon)

    fun updateMetadata(status: PlayingStatus) {
        var parentPath: String
        try {
            parentPath = status.currentMusic?.parent?.ruuPath ?: ""
        } catch (e: RuuFileBase.OutOfRootDirectory) {
            parentPath = ""
        }

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, parentPath)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, status.currentMusic?.name)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, displayIcon)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, status.duration)
                .build())
    }

    fun updatePlaybackState(status: PlayingStatus, error: String? = null) {
        val state = PlaybackStateCompat.Builder()
                .setState(if (status.playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        if (status.currentMusic != null) status.receivedCurrentTime else PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f)
                .setActiveQueueItemId(queueIndex)
                .setActions(PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_STOP
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                            or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED
                            or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                            or PlaybackStateCompat.ACTION_PLAY_FROM_URI
                            or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                )

        if (error != null && error != "") {
            state.setErrorMessage(0, error)
            state.setState(PlaybackStateCompat.STATE_ERROR,
                    if (status.currentMusic != null) status.receivedCurrentTime else PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1.0f)
        }

        mediaSession.setPlaybackState(state.build())

        mediaSession.setShuffleModeEnabled(status.shuffleMode)

        mediaSession.setRepeatMode(when (status.repeatMode) {
            RepeatModeType.OFF -> PlaybackStateCompat.REPEAT_MODE_NONE
            RepeatModeType.SINGLE -> PlaybackStateCompat.REPEAT_MODE_ONE
            RepeatModeType.LOOP -> PlaybackStateCompat.REPEAT_MODE_ALL
        })
    }
    
    fun updateQueue(playlist: Playlist) {
        queueIndex = playlist.queueIndex.toLong()

        mediaSession.setQueue(playlist.mediaSessionQueue)
        mediaSession.setQueueTitle(playlist.title)
    }


    class MediaButtonReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent.action != KeyEvent.ACTION_UP) {
                    return
                }
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> sendIntent(context, RuuService.ACTION_PLAY)
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> sendIntent(context, RuuService.ACTION_PAUSE)
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> sendIntent(context, RuuService.ACTION_PLAY_PAUSE)
                    KeyEvent.KEYCODE_MEDIA_NEXT -> sendIntent(context, RuuService.ACTION_NEXT)
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> sendIntent(context, RuuService.ACTION_PREV)
                }
            }
        }

        private fun sendIntent(context: Context, event: String) {
            context.startService(Intent(context, RuuService::class.java).setAction(event))
        }
    }
}
