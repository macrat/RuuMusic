package jp.blanktar.ruumusic.widget;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.PlayingStatus;


@UiThread
public class PlayPauseWidget extends RuuWidgetProvider{
	private boolean playing = false;


	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.play_pause_widget);

		views.setOnClickPendingIntent(R.id.widget_play_pause, makeRuuServicePendingIntent(context, RuuService.ACTION_PLAY_PAUSE));

		views.setImageViewResource(R.id.widget_play_pause, playing ? R.drawable.ic_pause : R.drawable.ic_play);

		for(int id: appWidgetIds){
			appWidgetManager.updateAppWidget(id, views);
		}
	}

	@Override
	public void onAppWidgetUpdate(@NonNull Context context){
		requestStatusUpdate(context);
	}

	@Override
	public void onPlayingStatus(@NonNull Context context, @NonNull PlayingStatus status){
		playing = status.playing;
		requestWidgetUpdate(context);
	}
}
