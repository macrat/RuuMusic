package jp.blanktar.ruumusic.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.client.main.MainActivity;
import jp.blanktar.ruumusic.util.PlayingStatus;
import jp.blanktar.ruumusic.util.Preference;


@UiThread
public class MusicNameWidget extends RuuWidgetProvider{
	@Nullable private String musicName = null;

	@Nullable private Preference preference = null;


	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_name_widget);

		views.setOnClickPendingIntent(R.id.widget_music_name, PendingIntent.getActivity(
				context,
				0,
				(new Intent(context, MainActivity.class)).setAction(MainActivity.ACTION_OPEN_PLAYER),
				0
		));

		if(musicName == null){
			views.setTextViewText(R.id.widget_music_name, context.getString(R.string.widget_nodata));
		}else{
			views.setTextViewText(R.id.widget_music_name, musicName);
		}

		if(preference == null){
			preference = new Preference(context);
		}
		views.setFloat(R.id.widget_music_name, "setTextSize", preference.MusicNameWidgetNameSize.get());

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
		if(status.currentMusic == null){
			musicName = null;
		}else{
			musicName = status.currentMusic.getName();
		}

		requestWidgetUpdate(context);
	}
}
