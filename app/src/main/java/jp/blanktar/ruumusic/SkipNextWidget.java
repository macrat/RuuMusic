package jp.blanktar.ruumusic;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;


public class SkipNextWidget extends AppWidgetProvider{
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.skip_next_widget);

		views.setOnClickPendingIntent(R.id.widget_skip_next, PendingIntent.getService(
				context,
				0,
				(new Intent(context, RuuService.class)).setAction(RuuService.ACTION_NEXT),
				0
		));

		for(int id: appWidgetIds){
			appWidgetManager.updateAppWidget(id, views);
		}
	}
}
