package jp.blanktar.ruumusic.widget;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;


@UiThread
public class SkipPrevWidget extends RuuWidgetProvider{
	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int[] appWidgetIds){
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.skip_prev_widget);

		views.setOnClickPendingIntent(R.id.widget_skip_prev, makeRuuServicePendingIntent(context, RuuService.ACTION_PREV));

		for(int id: appWidgetIds){
			appWidgetManager.updateAppWidget(id, views);
		}
	}

	@Override
	public void onAppWidgetUpdate(@NonNull Context context){
		requestWidgetUpdate(context);
	}
}
