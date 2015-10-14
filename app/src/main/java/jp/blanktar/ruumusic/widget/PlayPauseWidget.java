package jp.blanktar.ruumusic.widget;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;


@UiThread
public class PlayPauseWidget extends AppWidgetProvider{
	private boolean playing = false;


	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.play_pause_widget);

		views.setOnClickPendingIntent(R.id.widget_play_pause, PendingIntent.getService(
				context,
				0,
				(new Intent(context, RuuService.class)).setAction(RuuService.ACTION_PLAY_PAUSE),
				0
		));

		views.setImageViewResource(R.id.widget_play_pause, playing ? R.drawable.ic_pause : R.drawable.ic_play);

		for(int id: appWidgetIds){
			appWidgetManager.updateAppWidget(id, views);
		}
	}

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent){
		super.onReceive(context, intent);

		switch(intent.getAction()){
			case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
				context.startService((new Intent(context, RuuService.class)).setAction(RuuService.ACTION_PING));
				break;
			case RuuService.ACTION_STATUS:
				playing = intent.getBooleanExtra("playing", false);
				AppWidgetManager awm = AppWidgetManager.getInstance(context);
				onUpdate(context, awm, awm.getAppWidgetIds(new ComponentName(context, PlayPauseWidget.class)));
				break;
		}
	}
}
