package jp.blanktar.ruumusic;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RemoteViews;


public class PlayPauseWidget extends AppWidgetProvider{
	private boolean playing = false;


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
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
	public void onReceive(Context context, Intent intent){
		super.onReceive(context, intent);

		if(intent.getAction() == RuuService.ACTION_STATUS){
			playing = intent.getBooleanExtra("playing", false);
			AppWidgetManager awm = AppWidgetManager.getInstance(context);
			onUpdate(context, awm, awm.getAppWidgetIds(new ComponentName(context, PlayPauseWidget.class)));
		}
	}
}