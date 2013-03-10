package com.mopoo.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static Date getDateByInt(long va){
    	Date result=new Date();
    	result.setTime(va);
    	return result;
    }
    public static long getDayStartTime(long time){
    	Calendar cal=Calendar.getInstance();
    	cal.setTimeInMillis(time);
    	SimpleDateFormat sf=new SimpleDateFormat("yyyy-MM-dd");
    	String strDay=sf.format(cal.getTime());
   		long result;
		try {
			result = sf.parse(strDay).getTime();
		} catch (ParseException e) {
			result=time;
		}
    	return result; 
    }
    public static long oneDay=1000*60*60*24;
    public static SimpleDateFormat sf=new SimpleDateFormat("yyyy-MM-dd");
    public static String getSmartDateString(long time,String extString,String zeroDayString){
    	long theTime=getDayStartTime(time);
    	long nowTime=getDayStartTime(System.currentTimeMillis());
    	long moreTime=nowTime-theTime;
    	if(moreTime<=0L){
    		return zeroDayString;
    	}
    	if(moreTime<oneDay*7){//7天内
    		return (moreTime/oneDay)+" "+extString;
    	}else{
    		return sf.format(getDateByInt(time));
    	}
    }
    

    public static final long MILLIS_PER_SECOND = 1000;
    public static final long MILLIS_PER_MINUTE = 60000;
    public static final long MILLIS_PER_HOUR = 3600000;    
    public static String getFriendlyDate(Date createOn) {
        long past = createOn.getTime();
        long now = System.currentTimeMillis();
        String ret;
        if (past > now - 24 * 60 * 60 * 1000) {
            long d = now - past;
            if (d / DateUtils.MILLIS_PER_HOUR > 0) {
                ret = "约 " + String.valueOf( d / DateUtils.MILLIS_PER_HOUR) + " 小时前";
            } else if (d / DateUtils.MILLIS_PER_MINUTE > 0) {
                ret = String.valueOf(d/DateUtils.MILLIS_PER_MINUTE)+" 分钟前";
            } else {
                ret = String.valueOf(d/DateUtils.MILLIS_PER_SECOND)+" 秒钟前";
            }
        } else {
			SimpleDateFormat sf =new SimpleDateFormat("yyyy/MM/dd HH:mm");
            ret = sf.format(createOn);
        }
        return ret;
    }

    
    public static void main(String[] args) {
		System.out.println(System.currentTimeMillis());
		System.out.println(new Date().getTime());
	}
}
