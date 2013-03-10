package com.mopoo;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MyPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	MyApplication myApplication;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		myApplication = (MyApplication) this.getApplication();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if ("only_wifi_load_img".equals(key)) {
			myApplication.clearCacheIsOnlyWifiLoadImage();
		}
		if ("font_size".equals(key)) {
			myApplication.clearCacheFontSize();
		}
		
		if ("left_line_height".equals(key)) {
			myApplication.clearCacheLeftLineHeight();
		}
		
		if ("right_line_height".equals(key)) {
			myApplication.clearCacheRightLineHeight();
		}		
		
		if("is_use_portrait".equals(key)) {
			myApplication.clearCacheIsUsePortrait();
		}
		
		if("is_use_gesture".equals(key)) {
			myApplication.clearCacheIsUseGesture();
		}	
		
		if("use_cache".equals(key)) {
			myApplication.clearCacheUseCache();
		}
	}
}
