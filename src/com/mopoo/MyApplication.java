package com.mopoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.mopoo.action.FetchState;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.TopicObject;

public class MyApplication extends Application {

	public static final String KEY_PRE = "PRE";
	public static final int PRE_MODE = 0;
	private static final String KEY_REMEMBER_ME_COOKIE = "RM_COOKIE";
	private static final String KEY_USER = "USER";
	private static final String KEY_PASS = "PASS";
	private TopicObject currentTopic;
	public static final int NOTI_ID_FETCH_CACHE = 1237076;
	public String getRememberMeCookie() {
		return this.getFromPreferences(KEY_REMEMBER_ME_COOKIE);
	}

	public void setCurrentTopic(TopicObject topic) {
		this.currentTopic = topic;
	}

	public TopicObject getCurrentTopic() {
		return currentTopic;
	}

	public String[] getUserAndPass() {
		String user = this.getFromPreferences(KEY_USER);
		String pass = this.getFromPreferences(KEY_PASS);
		if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(pass)) {
			return new String[] { user, pass };
		} else {
			return null;
		}
	}

	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	public void saveRememberMeCookie(String cookie) {
		this.saveToProferences(KEY_REMEMBER_ME_COOKIE, cookie);
	}

	public void saveUser(String user) {
		this.saveToProferences(KEY_USER, user);
	}

	public void savePass(String pass) {
		this.saveToProferences(KEY_PASS, pass);
	}

	private String getFromPreferences(String key) {
		SharedPreferences pre = this.getSharedPreferences(KEY_PRE, PRE_MODE);
		return pre.getString(key, null);
	}

	private boolean saveToProferences(String key, String value) {
		SharedPreferences pre = this.getSharedPreferences(KEY_PRE, PRE_MODE);
		Editor editor = pre.edit();
		editor.putString(key, value);
		return editor.commit();
	}

	public boolean isLogin() {
		if (StringUtils.isNotBlank(getRememberMeCookie())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isValidNetwork(){
	    boolean flag = false;  
        ConnectivityManager cwjManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);  
        if (cwjManager.getActiveNetworkInfo() != null)  
            flag = cwjManager.getActiveNetworkInfo().isAvailable();  
        return flag;
	}
	private boolean isUseOffline=false;
	public boolean isUseOffline(){
		return isUseOffline;
	}
	public void setUseOffline(boolean flag){
		this.isUseOffline = flag;
	}
	
	private Boolean isWifiNow = null;

	public boolean isShowNetworkImage() {
		if (this.isSettingOnlyWifiLoadImage() == true && this.isWifiNow() == false) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isWifiNow() {
		if (isWifiNow == null) {
			try {
				ConnectivityManager connectivityManager = (ConnectivityManager) this
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
				if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					isWifiNow = true;
				} else {
					isWifiNow = false;
				}
			} catch (Exception e) {
				Log.i("得到wifi信息时出错", e.getMessage());
				isWifiNow = false;
			}
		}
		return isWifiNow;
	}

	public String fixjs = null;

	public String getFixJavascript() {
		if (fixjs == null) {
			fixjs = this.getStringFromRawFile(R.raw.fixjs);
		}
		return fixjs;
	}

	public String getStringFromRawFile(int id) {
		InputStream is = null;
		StringBuilder sb = new StringBuilder();
		try {
			Log.i("mopoo", "从raw加载文件Id:" + id);
			is = this.getResources().openRawResource(id);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
			String line = null;
			while (true) {
				line = br.readLine();
				if (line == null) {
					break;
				}
				sb.append(line);
				sb.append("\n");
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("mopoo", e.getMessage());
			return "";
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
					Log.d("mopoo", e.getMessage());
				}
			}
		}
	}

	private String leftcss = null;

	public String getLeftCss() {
		if (leftcss == null) {
			leftcss = this.getStringFromRawFile(R.raw.leftcss);
			leftcss = leftcss.replace("%font_size%", this.getSettingFontSize());
			leftcss = leftcss.replace("%left_line_height%", this.getSettingLeftLineHeight());
		}
		return leftcss;
	}

	private String rightcss = null;

	public String getRigthcss() {
		if (rightcss == null) {
			rightcss = this.getStringFromRawFile(R.raw.rightcss);
			rightcss = rightcss.replace("%font_size%", this.getSettingFontSize());
			rightcss = rightcss.replace("%right_line_height%", this.getSettingRightLineHeight());
		}
		return rightcss;
	}

	private Boolean cacheIsOnlyWifiLoadImage = null;

	public boolean isSettingOnlyWifiLoadImage() {
		if (cacheIsOnlyWifiLoadImage == null) {
			cacheIsOnlyWifiLoadImage = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					"only_wifi_load_img", true);
			Log.i("mopoo", "得到配置only_wifi_load_img:" + cacheIsOnlyWifiLoadImage);
		}
		return cacheIsOnlyWifiLoadImage;
	}

	public boolean isUseRssMode() {
		boolean isUseRss = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_rss_mode", false);
		Log.i("mopoo", "得到配置use_rss_mode:" + isUseRss);
		return isUseRss;
	}

	private Boolean cacheIsUsePortrait;
	public boolean isUsePortrait() {
		if(cacheIsUsePortrait==null){
			cacheIsUsePortrait= PreferenceManager.getDefaultSharedPreferences(this).getBoolean("is_use_portrait", false);
			Log.i("mopoo", "得到配置is_use_portrait:" + cacheIsUsePortrait);
		}
		return cacheIsUsePortrait;
	}	
	public void clearCacheIsUsePortrait() {
		Log.i("mopoo", "重置cacheIsUsePortrait为null");
		cacheIsUsePortrait = null;
	}
	
	
	private Boolean cacheIsUseGesture;
	public boolean isUseGesture() {
		if(cacheIsUseGesture==null){
			cacheIsUseGesture= PreferenceManager.getDefaultSharedPreferences(this).getBoolean("is_use_gesture", false);
			Log.i("mopoo", "得到配置is_use_portrait:" + cacheIsUsePortrait);
		}
		return cacheIsUseGesture;
	}	
	public void clearCacheIsUseGesture() {
		Log.i("mopoo", "重置cacheIsUseGesture为null");
		cacheIsUseGesture = null;
	}
	
	
	public int getSettingListCount() {
		String strCount = PreferenceManager.getDefaultSharedPreferences(this).getString("list_count", "50");
		int count = 50;
		try {
			count = Integer.valueOf(strCount);
		} catch (Exception e) {
			Log.i("mopoo", "转换list_count出错:" + e.getMessage() + "，使用默认值:" + count);
		}
		Log.i("mopoo", "得到配置list_count:" + count);
		return count;
	}

	public void clearCacheIsOnlyWifiLoadImage() {
		Log.i("mopoo", "重置cacheIsOnlyWifiLoadImage为null");
		cacheIsOnlyWifiLoadImage = null;
	}

	private String cacheFontSize;

	public String getSettingFontSize() {
		if (cacheFontSize == null) {
			cacheFontSize = PreferenceManager.getDefaultSharedPreferences(this).getString("font_size", "12");
			Log.i("mopoo", "得到配置font_size:" + cacheFontSize);
		}
		return cacheFontSize;
	}

	public void clearCacheFontSize() {
		Log.i("mopoo", "重置cacheFontSize为null");
		Log.i("mopoo", "重置leftcss,rightcss为null");
		cacheFontSize = null;
		leftcss = null;
		rightcss = null;
	}
	
	private String cacheLeftLineHeight;

	public String getSettingLeftLineHeight() {
		if (cacheLeftLineHeight == null) {
			cacheLeftLineHeight = PreferenceManager.getDefaultSharedPreferences(this).getString("left_line_height", "20");
			Log.i("mopoo", "得到配置left_line_height:" + cacheLeftLineHeight);
		}
		return cacheLeftLineHeight;
	}
	
	public void clearCacheLeftLineHeight() {
		Log.i("mopoo", "重置cacheLeftLineHeight为null");
		cacheLeftLineHeight = null;
		leftcss=null;
	}

	private String cacheRightLineHeight;

	public String getSettingRightLineHeight() {
		if (cacheRightLineHeight == null) {
			cacheRightLineHeight = PreferenceManager.getDefaultSharedPreferences(this).getString("right_line_height", "20");
			Log.i("mopoo", "得到配置right_line_height:" + cacheRightLineHeight);
		}
		return cacheRightLineHeight;
	}
	
	public void clearCacheRightLineHeight() {
		Log.i("mopoo", "重置cacheRightLineHeight为null");
		cacheRightLineHeight = null;
		rightcss=null;
	}	
	
	public int getSettingFetchCount() {
		String strCount = PreferenceManager.getDefaultSharedPreferences(this).getString("fetch_count", "200");
		int count = 200;
		try {
			count = Integer.valueOf(strCount);
		} catch (Exception e) {
			Log.i("mopoo", "转换fetch_count出错:" + e.getMessage() + "，使用默认值:" + count);
		}
		Log.i("mopoo", "得到配置fetch_count:" + count);
		return count;
	}	
	
	private Boolean cacheUseCache;

	public boolean getSettingUseCache() {
		if (cacheUseCache == null) {
			cacheUseCache = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_cache", true);
			Log.i("mopoo", "得到配置cacheUseCache:" + cacheUseCache);
		}
		return cacheUseCache;
	}

	public void clearCacheUseCache() {
		Log.i("mopoo", "重置cacheUseCache为null");
		cacheUseCache = null;
	}
	
	
	private FetchState fetchState = FetchState.NOFETCH;
	public FetchState getFetchState(){
		return fetchState;
	}
	public void setFetchState(FetchState state){
		this.fetchState = state;
	}	
}
