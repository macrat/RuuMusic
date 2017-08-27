package jp.blanktar.ruumusic.widget;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.client.main.MainActivity;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.PlayingStatus;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuFileBase;


@UiThread
public class UnifiedWidget extends RuuWidgetProvider{
	private boolean playing = false;
	@Nullable private String musicName = null;
	@Nullable private String musicPath = null;

	@Nullable private Preference preference = null;


	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.unified_widget);

		views.setOnClickPendingIntent(R.id.widget_unified, PendingIntent.getActivity(
				context,
				0,
				(new Intent(context, MainActivity.class)).setAction(MainActivity.ACTION_OPEN_PLAYER),
				0
		));

		views.setOnClickPendingIntent(R.id.play_button, makeRuuServicePendingIntent(context, RuuService.ACTION_PLAY_PAUSE));
		views.setOnClickPendingIntent(R.id.prev_button, makeRuuServicePendingIntent(context, RuuService.ACTION_PREV));
		views.setOnClickPendingIntent(R.id.next_button, makeRuuServicePendingIntent(context, RuuService.ACTION_NEXT));

		views.setImageViewResource(R.id.play_button, playing ? R.drawable.ic_pause : R.drawable.ic_play);

		views.setTextViewText(R.id.music_name, musicName != null ? musicName : context.getString(R.string.widget_nodata));
		views.setTextViewText(R.id.music_path, musicPath != null ? musicPath : "");

		if(preference == null){
			preference = new Preference(context);
		}
		views.setFloat(R.id.music_path, "setTextSize", preference.UnifiedWidgetMusicPathSize.get());
		views.setFloat(R.id.music_name, "setTextSize", preference.UnifiedWidgetMusicNameSize.get());

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
		
		if(status.currentMusic == null){
			musicName = null;
			musicPath = null;
		}else{
			try{
				musicName = status.currentMusic.getName();
				musicPath = status.currentMusic.getParent().getRuuPath();
			}catch(RuuFileBase.OutOfRootDirectory | NullPointerException e){
				musicName = null;
				musicPath = null;
			}
		}

		requestWidgetUpdate(context);
	}
}
