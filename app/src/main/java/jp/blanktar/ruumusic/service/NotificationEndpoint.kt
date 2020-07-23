package jp.blanktar.ruumusic.service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.client.main.MainActivity
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.util.RuuFileBase


class NotificationEndpoint(val service: Service, private val mediaSession: MediaSessionCompat) : Endpoint {
    override val supported = true

    val context = service.applicationContext!!
    private var firstCall = true

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_playing_status_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_playing_status_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun close() {
        removeNotification()
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        service.startForeground(1, makeNotification(status))
        firstCall = false

        if (!status.playing) {
            service.stopForeground(status.currentMusic != null && !firstCall)
        }
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onFailedPlay(status: PlayingStatus) {
        service.startForeground(
            1,
            NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_play_notification)
                setContentTitle(status.currentMusic?.name)
                setContentText(status.currentMusic?.name)
                setPriority(NotificationCompat.PRIORITY_LOW)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            }.build()
        )
        removeNotification()
    }

    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}

    private fun removeNotification() {
        service.stopForeground(true)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
    }

    private fun makeNotification(status: PlayingStatus): Notification {
        val playPauseIcon = if (status.playing) R.drawable.ic_pause_for_notif else R.drawable.ic_play_for_notif
        val playPauseText = if (status.playing) "pause" else "play"
        val playPausePI = PendingIntent.getService(
                context,
                0,
                Intent(context, RuuService::class.java).setAction(if (status.playing) RuuService.ACTION_PAUSE else RuuService.ACTION_PLAY),
                0)

        val prevPI = PendingIntent.getService(context, 0, Intent(context, RuuService::class.java).setAction(RuuService.ACTION_PREV), 0)
        val nextPI = PendingIntent.getService(context, 0, Intent(context, RuuService::class.java).setAction(RuuService.ACTION_NEXT), 0)

        val shuffleIcon = if (status.shuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
        val shufflePI = PendingIntent.getService(
                context,
                0,
                Intent(context, RuuService::class.java).setAction(RuuService.ACTION_SHUFFLE).putExtra("mode", !status.shuffleMode),
                PendingIntent.FLAG_CANCEL_CURRENT)

        val repeatIcon = when (status.repeatMode) {
                              RepeatModeType.OFF ->  R.drawable.ic_repeat_off
                              RepeatModeType.SINGLE ->  R.drawable.ic_repeat_one
                              RepeatModeType.LOOP ->  R.drawable.ic_repeat_all
                          }
        val repeatPI = PendingIntent.getService(
                context,
                0,
                Intent(context, RuuService::class.java).setAction(RuuService.ACTION_REPEAT).putExtra("mode", status.repeatMode.next.name),
                PendingIntent.FLAG_CANCEL_CURRENT)

        val intent = Intent(context, MainActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_PLAYER
        val contentIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val parentPath = try {
            status.currentMusic?.parent?.ruuPath ?: ""
        } catch (e: RuuFileBase.OutOfRootDirectory) {
            ""
        }

        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_play_notification)
            setTicker(status.currentMusic?.name)
            setContentTitle(status.currentMusic?.name)
            setContentText(parentPath)
            setContentIntent(contentIntent)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            addAction(shuffleIcon, "shuffle", shufflePI)
            addAction(R.drawable.ic_prev_for_notif, "prev", prevPI)
            addAction(playPauseIcon, playPauseText, playPausePI)
            addAction(R.drawable.ic_next_for_notif, "next", nextPI)
            addAction(repeatIcon, "repeat", repeatPI)

            setStyle(MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3)
            )
        }.build()
    }

    companion object {
        const val CHANNEL_ID = "playing_status"
    }
}
