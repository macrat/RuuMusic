package jp.blanktar.ruumusic.service


import android.content.Context
import android.support.v4.app.NotificationCompat


fun makeNotificationBuilder(context: Context) = NotificationCompat.Builder(context, NotificationEndpoint.CHANNEL_ID)
