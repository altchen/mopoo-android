package com.mopoo.utils;

import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;


public class MyGestureDetector extends SimpleOnGestureListener {
    private static final int SWIPE_MIN_DISTANCE = 80;
    private static final int SWIPE_MAX_OFF_PATH = 30;
	private static final int SWIPE_THRESHOLD_VELOCITY = 50;
	private MoveLeftRightInterface leftRight;
	public MyGestureDetector(MoveLeftRightInterface leftRight){
		super();
		this.leftRight=leftRight;
	}
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            // right to left swipe
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            	leftRight.rightToLeft();
            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            	leftRight.leftToRight();
            }
        } catch (Exception e){
        }
        return false;
    }
}
