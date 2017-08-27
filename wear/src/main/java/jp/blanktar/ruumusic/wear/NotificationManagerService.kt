package jp.blanktar.ruumusic.wear


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import android.support.v4.app.NotificationManagerCompat

import jp.blanktar.ruumusic.wear.R


const val CHANNEL_ID = "playing_status"


fun makeNotificationChannel(context: Context) {
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


class NotificationManagerService : WearableListenerService() {
    var client: RuuClient? = null

    override fun onCreate() {
        super.onCreate()
        
        println("")

        client = RuuClient(applicationContext)
        client?.connect()
        client?.onStatusUpdated = {
            sendNotification()
        }

        makeNotificationChannel(applicationContext)
    }

    override fun onDestroy() {
        client?.disconnect()
    }

    fun sendNotification() {
        if (client?.status?.playing == true) {
            println("send notification")
            NotificationManagerCompat.from(applicationContext).notify(2, makeNotification())
        } else {
            println("remove notification")
            NotificationManagerCompat.from(applicationContext).cancel(2)
        }
    }

    fun makeNotification(): Notification {
        val playpause_icon = if (client!!.status.playing) R.drawable.ic_pause_for_notif else R.drawable.ic_play_for_notif
        val playpause_text = if (client!!.status.playing) "pause" else "play"
        val playpause_pi = PendingIntent.getService(
                applicationContext,
                0,
                Intent(applicationContext, ControlIntentBridgeService::class.java).setAction(if (client!!.status.playing) ControlIntentBridgeService.ACTION_PAUSE else ControlIntentBridgeService.ACTION_PLAY),
                0)

        val prev_pi = PendingIntent.getService(applicationContext, 0, Intent(applicationContext, ControlIntentBridgeService::class.java).setAction(ControlIntentBridgeService.ACTION_PREV), 0)
        val next_pi = PendingIntent.getService(applicationContext, 0, Intent(applicationContext, ControlIntentBridgeService::class.java).setAction(ControlIntentBridgeService.ACTION_NEXT), 0)

        val contentIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_PLAYER), 0)

        return NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_play_notification)
                .setTicker(client!!.status.musicName)
                .setContentTitle(client!!.status.musicName)
                .setContentText(client!!.status.musicDir)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(R.drawable.ic_prev_for_notif, "prev", prev_pi)
                .addAction(playpause_icon, playpause_text, playpause_pi)
                .addAction(R.drawable.ic_next_for_notif, "next", next_pi)
                .build()
    }
}
