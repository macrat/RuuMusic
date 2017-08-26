package jp.blanktar.ruumusic.widget;

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
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;
import jp.blanktar.ruumusic.client.main.MainActivity;


@UiThread
public class UnifiedWidget extends AppWidgetProvider{
	private boolean playing = false;
	@Nullable private String musicName = null;
	@Nullable private String musicPath = null;

	@Nullable private Preference preference = null;


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.unified_widget);

		views.setOnClickPendingIntent(R.id.widget_unified, PendingIntent.getActivity(
				context,
				0,
				(new Intent(context, MainActivity.class)).setAction(MainActivity.ACTION_OPEN_PLAYER),
				0
		));

		views.setOnClickPendingIntent(R.id.play_button, PendingIntent.getService(
				context,
				0,
				(new Intent(context, RuuService.class)).setAction(RuuService.ACTION_PLAY_PAUSE),
				0
		));

		views.setOnClickPendingIntent(R.id.prev_button, PendingIntent.getService(
				context,
				0,
				(new Intent(context, RuuService.class)).setAction(RuuService.ACTION_PREV),
				0
		));

		views.setOnClickPendingIntent(R.id.next_button, PendingIntent.getService(
				context,
				0,
				(new Intent(context, RuuService.class)).setAction(RuuService.ACTION_NEXT),
				0
		));

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
	public void onReceive(@NonNull Context context, @NonNull Intent intent){
		super.onReceive(context, intent);

		switch(intent.getAction()){
			case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
				context.startService((new Intent(context, RuuService.class)).setAction(RuuService.ACTION_PING));
				break;
			case RuuService.ACTION_STATUS:
				playing = intent.getBooleanExtra("playing", false);

				try{
					RuuFile file = RuuFile.getInstance(context, intent.getStringExtra("path"));
					musicName = file.getName();
					musicPath = file.getParent().getRuuPath();
				}catch(RuuFileBase.NotFound | RuuFileBase.OutOfRootDirectory | NullPointerException e){
					musicName = null;
					musicPath = null;
				}

				AppWidgetManager awm = AppWidgetManager.getInstance(context);
				onUpdate(context, awm, awm.getAppWidgetIds(new ComponentName(context, UnifiedWidget.class)));
				break;
		}
	}
}

