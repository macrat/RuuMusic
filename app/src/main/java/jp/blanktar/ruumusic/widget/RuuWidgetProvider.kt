package jp.blanktar.ruumusic.widget


import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

import jp.blanktar.ruumusic.service.RuuService
import jp.blanktar.ruumusic.util.PlayingStatus


abstract class RuuWidgetProvider : AppWidgetProvider() {
    fun makeRuuServicePendingIntent(context: Context, action: String)
            = if (Build.VERSION.SDK_INT >= 26) PendingIntent.getForegroundService(context, 0, Intent(context, RuuService::class.java).setAction(action), 0);
              else PendingIntent.getService(context, 0, Intent(context, RuuService::class.java).setAction(action), 0);

    fun requestStatusUpdate(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(Intent(context, RuuService::class.java).setAction(RuuService.ACTION_PING));
        } else {
            context.startService(Intent(context, RuuService::class.java).setAction(RuuService.ACTION_PING));
        }
    }

    fun requestWidgetUpdate(context: Context) {
        val awm = AppWidgetManager.getInstance(context)
        onUpdate(context, awm, awm.getAppWidgetIds(ComponentName(context, this::class.java)))
    }

    open fun onAppWidgetUpdate(context: Context) {}

    open fun onPlayingStatus(context: Context, status: PlayingStatus) {}

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> onAppWidgetUpdate(context)
            RuuService.ACTION_STATUS -> onPlayingStatus(context, PlayingStatus(context, intent))
            else -> super.onReceive(context, intent)
        }
    }
}
