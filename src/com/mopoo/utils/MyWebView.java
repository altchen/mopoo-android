package com.mopoo.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;

public class MyWebView extends WebView {
	private GestureDetector gestureDetector;

	public MyWebView(Context context) {
		super(context);
	}

	public MyWebView(Context context, AttributeSet set) {
		super(context, set);
	}

	public void setGestureDetector(GestureDetector gestureDetector) {
		this.gestureDetector = gestureDetector;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		boolean superResult = super.onTouchEvent(event);
		if (gestureDetector != null) {
			gestureDetector.onTouchEvent(event);
		}
		return superResult;
	}
}
