package jp.blanktar.ruumusic.widget;

import java.io.File;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.client.MainActivity;
import jp.blanktar.ruumusic.service.RuuService;


@UiThread
public class MusicNameWidget extends AppWidgetProvider{
	@Nullable private String musicName = null;


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
				String path = intent.getStringExtra("path");
				if(path == null){
					musicName = null;
				}else{
					musicName = (new File(path)).getName();
				}

				AppWidgetManager awm = AppWidgetManager.getInstance(context);
				onUpdate(context, awm, awm.getAppWidgetIds(new ComponentName(context, MusicNameWidget.class)));
				break;
		}
	}
}
