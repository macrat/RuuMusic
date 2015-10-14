package jp.blanktar.ruumusic.widget;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;


@UiThread
public class SkipPrevWidget extends AppWidgetProvider{
	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.skip_prev_widget);

		views.setOnClickPendingIntent(R.id.widget_skip_prev, PendingIntent.getService(
				context,
				0,
				(new Intent(context, RuuService.class)).setAction(RuuService.ACTION_PREV),
				0
		));

		for(int id: appWidgetIds){
			appWidgetManager.updateAppWidget(id, views);
		}
	}
}

