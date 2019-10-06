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
import androidx.media.app.NotificationCompat.MediaStyle

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.client.main.MainActivity
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.util.RuuFileBase


class NotificationEndpoint(val service: Service, val mediaSession: MediaSessionCompat) : Endpoint {
    override val supported = true

    val context = service.applicationContext!!
    var first = true

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID,
                                              context.getString(R.string.notification_channel_playing_status_name),
                                              NotificationManager.IMPORTANCE_LOW)
            channel.setDescription(context.getString(R.string.notification_channel_playing_status_description))
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            channel.setShowBadge(false)
            channel.enableLights(false)
            channel.enableVibration(false)

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun close() {
        removeNotification()
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        if (status.playing) {
            service.startForeground(1, makeNotification(status))
            first = false
        } else {
            service.stopForeground(true)

            if(Build.VERSION.SDK_INT >= 16 && status.currentMusic != null && !first){
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, makeNotification(status))
            }
        }
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onFailedPlay(status: PlayingStatus) {
        removeNotification()
    }

    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}

    fun removeNotification() {
        service.stopForeground(true)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
    }

    fun makeNotification(status: PlayingStatus): Notification {
        val playpause_icon = if (status.playing) R.drawable.ic_pause_for_notif else R.drawable.ic_play_for_notif
        val playpause_text = if (status.playing) "pause" else "play"
        val playpause_pi = PendingIntent.getService(
                context,
                0,
                Intent(context, RuuService::class.java).setAction(if (status.playing) RuuService.ACTION_PAUSE else RuuService.ACTION_PLAY),
                0)

        val prev_pi = PendingIntent.getService(context, 0, Intent(context, RuuService::class.java).setAction(RuuService.ACTION_PREV), 0)
        val next_pi = PendingIntent.getService(context, 0, Intent(context, RuuService::class.java).setAction(RuuService.ACTION_NEXT), 0)

        val shuffle_icon = if (status.shuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
        val shuffle_pi = PendingIntent.getService(
                context,
                0,
                Intent(context, RuuService::class.java).setAction(RuuService.ACTION_SHUFFLE).putExtra("mode", !status.shuffleMode),
                PendingIntent.FLAG_CANCEL_CURRENT)

        val repeat_icon = when (status.repeatMode) {
                              RepeatModeType.OFF ->  R.drawable.ic_repeat_off
                              RepeatModeType.SINGLE ->  R.drawable.ic_repeat_one
                              RepeatModeType.LOOP ->  R.drawable.ic_repeat_all
                          }
        val repeat_pi = PendingIntent.getService(
                context,
                0,
                Intent(context, RuuService::class.java).setAction(RuuService.ACTION_REPEAT).putExtra("mode", status.repeatMode.next.name),
                PendingIntent.FLAG_CANCEL_CURRENT)

        val intent = Intent(context, MainActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_PLAYER
        val contentIntent = PendingIntent.getActivity(context, 0, intent, 0)

        var parentPath: String
        try {
            parentPath = status.currentMusic?.parent?.ruuPath ?: ""
        } catch (e: RuuFileBase.OutOfRootDirectory) {
            parentPath = ""
        }

        return makeNotificationBuilder(context)
                .setSmallIcon(R.drawable.ic_play_notification)
                .setTicker(status.currentMusic?.name)
                .setContentTitle(status.currentMusic?.name)
                .setContentText(parentPath)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(shuffle_icon, "shuffle", shuffle_pi)
                .addAction(R.drawable.ic_prev_for_notif, "prev", prev_pi)
                .addAction(playpause_icon, playpause_text, playpause_pi)
                .addAction(R.drawable.ic_next_for_notif, "next", next_pi)
                .addAction(repeat_icon, "repeat", repeat_pi)
                .setStyle(MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(1, 2, 3))
                .build()
    }

    companion object {
        const val CHANNEL_ID = "playing_status"
    }
}
