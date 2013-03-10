package com.mopoo.action;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import com.mopoo.MyApplication;
import com.mopoo.NotificationActionActivity;
import com.mopoo.R;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.CacheObject;

public class FetchToCacheAction {
	private MyApplication myApplication;
	private Activity activity;
	Handler handler = new Handler();

	public FetchToCacheAction(MyApplication app, Activity activity) {
		this.myApplication = app;
		this.activity = activity;
	}

	public void doFetch(final int maxCount) {
		if(FetchState.DOING == myApplication.getFetchState()){
			return;
		}
		final NotificationManager nm = (NotificationManager) myApplication.getSystemService(MyApplication.NOTIFICATION_SERVICE);
		final Notification notification = new Notification(R.drawable.icon, "开始获取离线数据...", System.currentTimeMillis());
		notification.contentView = new RemoteViews(myApplication.getPackageName(), R.layout.notification);
		// 使用notification.xml文件作VIEW
		notification.contentView.setTextViewText(R.id.noti_tip, "获取中");
		Intent notificationIntent = new Intent(myApplication, NotificationActionActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(myApplication, 0, notificationIntent, 0);
		notification.contentIntent = contentIntent;		
		new Thread(new Runnable() {
			@Override
			public void run() {
				myApplication.setFetchState(FetchState.DOING);				
				try{
					notification.contentView.setTextViewText(R.id.noti_tip, "获取帖子ID中...");
					nm.notify(MyApplication.NOTI_ID_FETCH_CACHE, notification);
					
					CacheObject<String> cacheObject = RemoteServer.getServer(myApplication).getTopicListHtml(false,true);
					String listHtml=cacheObject.data;
					ArrayList<String> topicIds = new ArrayList<String>(maxCount);
					
					//=====取id列表,取消正则，直接index较快
					int offset = 0;
					String startText = "href='info2.asp?id=";
					int startTextLength = startText.length();
					String endText="'";
					while(true){
						int start = listHtml.indexOf(startText,offset);
						if(start==-1){
							break;
						}
						int end = listHtml.indexOf(endText,start+startTextLength);
						if(end == -1){
							break;
						}
						String id = listHtml.substring(start+startTextLength, end);
						if(StringUtils.isNotBlank(id)&&!topicIds.contains(id)){
							topicIds.add(new String(id));
						}
						offset = end;
					}
					listHtml=null;
					//=====end of 取id列表
					final int total = Math.min(topicIds.size(),maxCount);
					int currIndex = 0;
					int errorCount = 0;
					
					for(final String id:topicIds){
						if(currIndex>=total){
							break;
						}
						if(FetchState.CANCEL.equals(myApplication.getFetchState())){
							break;
						}
						try{
							final int finalyIndex = currIndex;
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									notification.contentView.setTextViewText(R.id.noti_tip, "获取帖子ID["+id+"]("+(finalyIndex+1)+"/"+total+")");
									notification.contentView.setProgressBar(R.id.noti_pb, total, finalyIndex+1, false);
									nm.notify(MyApplication.NOTI_ID_FETCH_CACHE, notification);
								}
							});								
							RemoteServer.getServer(myApplication).getTopicHtml(id,false,true);						
						}catch(Exception e){
							errorCount++;
							Log.i("mopoo","得到离线帖子id:"+id+"时出错:"+e.getMessage());
						}
						currIndex++;
						Thread.sleep(1000);//休息一秒
					}
					notification.contentView.setTextViewText(R.id.noti_tip, "获取完成,得到帖子:"+currIndex+",失败:"+errorCount);
					nm.notify(MyApplication.NOTI_ID_FETCH_CACHE, notification);
				}catch(Exception e){
					Log.i("mopoo","获取离线帖子时出错:"+e.getMessage());
					notification.contentView.setTextViewText(R.id.noti_tip, "获取帖子出错:"+e.getMessage());
					nm.notify(MyApplication.NOTI_ID_FETCH_CACHE, notification);
				}finally{
					myApplication.setFetchState(FetchState.DONE);
				}
			}

		}).start();
	}
}
