package jp.blanktar.ruumusic.widget;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;


@UiThread
public class SkipNextWidget extends RuuWidgetProvider{
	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.skip_next_widget);

		views.setOnClickPendingIntent(R.id.widget_skip_next, makeRuuServicePendingIntent(context, RuuService.ACTION_NEXT));

		for(int id: appWidgetIds){
			appWidgetManager.updateAppWidget(id, views);
		}
	}

	@Override
	public void onAppWidgetUpdate(@NonNull Context context){
		requestWidgetUpdate(context);
	}
}
