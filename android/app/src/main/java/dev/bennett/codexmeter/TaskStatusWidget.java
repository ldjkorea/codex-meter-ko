package dev.bennett.codexmeter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

/** Independent provider; never touches IDs/options belonging to the usage/tier widgets. */
public final class TaskStatusWidget extends AppWidgetProvider {
    static void update(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);for(int id:manager.getAppWidgetIds(new ComponentName(c,TaskStatusWidget.class)))draw(c,manager,id);}
    private static void draw(Context c,AppWidgetManager manager,int id){Bundle size=manager.getAppWidgetOptions(id);int count=size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0)>=250&&size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0)>=300?3:1;
        RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.task_status_widget);views.setTextViewText(R.id.task_status_copy,TaskStatusStore.widgetText(c,count));
        views.setOnClickPendingIntent(R.id.task_status_root,PendingIntent.getActivity(c,id,new Intent(c,TaskStatusActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));manager.updateAppWidget(id,views);
    }
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)draw(c,manager,id);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,Bundle options){draw(c,manager,id);}
}
