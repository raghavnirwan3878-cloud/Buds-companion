package com.budscompanion.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class BudsWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, BudsWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(provider);
        for (int id : ids) {
            updateWidget(context, mgr, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager mgr, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(BudsConnectionService.PREFS, Context.MODE_PRIVATE);
        int left = prefs.getInt(BudsConnectionService.PREF_LEFT, -1);
        int right = prefs.getInt(BudsConnectionService.PREF_RIGHT, -1);
        int caseLevel = prefs.getInt(BudsConnectionService.PREF_CASE, -1);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_buds);
        views.setTextViewText(R.id.widget_left, left >= 0 ? left + "%" : "--");
        views.setTextViewText(R.id.widget_right, right >= 0 ? right + "%" : "--");
        views.setTextViewText(R.id.widget_case, caseLevel >= 0 ? caseLevel + "%" : "--");

        mgr.updateAppWidget(widgetId, views);
    }
}
