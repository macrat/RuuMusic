package jp.blanktar.ruumusic.service


import android.content.Context
import androidx.core.app.NotificationCompat


fun makeNotificationBuilder(context: Context) = NotificationCompat.Builder(context, NotificationEndpoint.CHANNEL_ID)
