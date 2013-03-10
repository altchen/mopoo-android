package com.mopoo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.mopoo.server.HttpClientHelper;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.vo.TopicObject;

public class MyAppWidgetProvider extends AppWidgetProvider {
	private final static String TAG = "mopoo";
	public final static int[] TEXT_IDS = { R.id.task_1, R.id.task_2, R.id.task_3, R.id.task_4, R.id.task_5 };
	public final static int[] SEPARATOR_IDS = { R.id.separator_1, R.id.separator_2, R.id.separator_3, R.id.separator_4 };


	private static boolean eventUpdate=false;
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.e(TAG, "MyAppWidgetProvider onUpdate()");
		RemoteViews views = null;
		ComponentName thisWidget = new ComponentName(context, MyAppWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		views = new RemoteViews(context.getPackageName(), R.layout.widget_initialized);
        Intent intentClick = new Intent("com.mopoo.widget.refresh");
        PendingIntent refreshIntent = PendingIntent.getBroadcast(context, 0,
                intentClick, 0);
		views.setOnClickPendingIntent(R.id.btn_refresh, refreshIntent);	
		manager.updateAppWidget(thisWidget, views);
		if(!eventUpdate){
			eventUpdate = true;
			reloadData(context);
		} 
	}

	


	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);	
        if (intent.getAction().equals("com.mopoo.widget.refresh")) {
        	reloadData(context);
        }
	}




	private static final String KEY_REMEMBER_ME_COOKIE = "RM_COOKIE";
	public String getRememberMeCookie(Context context) {
		return getFromPreferences(context,KEY_REMEMBER_ME_COOKIE);
	}	
	public static final String KEY_PRE = "PRE";
	public static final int PRE_MODE = 0;	
	private String getFromPreferences(Context context,String key) {
		SharedPreferences pre = context.getSharedPreferences(KEY_PRE, PRE_MODE);
		return pre.getString(key, null);
	}
	
	private void reloadData(Context context){
		Log.e(TAG, "MyAppWidgetProvider reloadData()");
		MyApplication myApplication = (MyApplication)context.getApplicationContext();
		HttpClientHelper.getInstance(myApplication).addCookies(myApplication.getRememberMeCookie());	
		RemoteViews views = null;
		ComponentName thisWidget = new ComponentName(context, MyAppWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		views = new RemoteViews(context.getPackageName(), R.layout.widget_initialized);
		Intent listIntent = new Intent(context, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, listIntent, 0);
		views.setOnClickPendingIntent(R.id.taskbody, pendingIntent);
        Intent intentClick = new Intent("com.mopoo.widget.refresh");
        PendingIntent refreshIntent = PendingIntent.getBroadcast(context, 0,
                intentClick, 0);
		views.setOnClickPendingIntent(R.id.btn_refresh, refreshIntent);	
		
		int[] textIDs = TEXT_IDS;
		int[] separatorIDs = SEPARATOR_IDS;
		int numberOfTasks = 5;
		List<TopicObject> topicList = new ArrayList<TopicObject>();
		try {
			views.setTextViewText(R.id.refresh_tv, "loading...");
			manager.updateAppWidget(thisWidget, views);
			topicList = RemoteServer.getTopTopicListForWidget(numberOfTasks);
			
			for (int i = 0; i < textIDs.length; i++) {
				TopicObject topic = (i < topicList.size()) ? topicList.get(i) : null;
				String textContent = "";
				if (topic != null) {
					textContent = "◆ "+topic.subjct;
					if (i < separatorIDs.length) {
						if (i < topicList.size() - 1 && topicList.get(i + 1) != null) {
							views.setViewVisibility(separatorIDs[i], View.VISIBLE);
						} else {
							views.setViewVisibility(separatorIDs[i], View.INVISIBLE);
						}
					}
					views.setTextViewText(textIDs[i], textContent);
				}

			}		
			SimpleDateFormat sf =new SimpleDateFormat("HH:mm:ss");
			views.setTextViewText(R.id.refresh_tv, "最后刷新:"+sf.format(new Date()));
		} catch (Exception e) {
			e.printStackTrace();
			views.setTextViewText(R.id.refresh_tv, "失败请重新刷新");
			Log.e(TAG, e.getMessage()==null?"":e.getMessage());
		}
		manager.updateAppWidget(thisWidget, views);		
		
	}
}
