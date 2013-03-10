package com.mopoo.utils;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class StringUtils {
	public static boolean isBlank(String value) {
		if (value == null) {
			return true;
		}
		return value.trim().equals("") ? true : false;
	}

	public static boolean isNotBlank(String value) {
		return !isBlank(value);
	}

	public static String getMiddleString(String old, String start, String end) {
		if (StringUtils.isNotBlank(old)) {
			int firstPosi = old.indexOf(start);
			if (firstPosi >= 0) {
				int endPosi = old.indexOf(end, firstPosi);
				if (endPosi >= 0) {
					return old.substring(firstPosi + start.length(), endPosi);
				}
			}
		}
		return null;
	}
	public static String getSubStringFromStart(String old,String start){
		if (StringUtils.isNotBlank(old)) {
			int firstPosi = old.indexOf(start);
			if (firstPosi >= 0) {
				return old.substring(firstPosi + start.length(), old.length());

			}
		}
		return null;		
	}
	public static String getQueryValueByString(String url,String key){
		if(StringUtils.isBlank(url)){
			return null;
		}
		int posi = url.indexOf("?");
		if(posi != -1){
			url = url.substring(posi+1);
		}
		String [] pairs = url.split("&");
		for(String pair : pairs){
			String [] keyValue = pair.split("=");
			if ( keyValue.length ==2){
				if (keyValue[0].equals(key)){
					return keyValue[1];
				}
			}
		}
		return null;
	}
	public static List<NameValuePair> parseURL(String url,String chartset) throws Exception{
		int posi = url.indexOf("?");
		if (posi>=0){
			url = url.substring(posi+1);
		}
		String [] parms = url.split("&");
		List<NameValuePair> results = new ArrayList<NameValuePair>();
		for(String parm:parms){
			String key = substringBefore(parm,"=");
			String value = substringAfter(parm,"=");
			value = URLDecoder.decode(value,chartset);
			NameValuePair pari = createNameValuePair(key,value);
			results.add(pari);
		}
		return results;
	}
	protected static NameValuePair createNameValuePair(final String name, final String value) {
		return new BasicNameValuePair(name, value);
	}	
	public static String substringBefore(String str, String separator) {
		if (str == null || separator == null) {
			return str;
		}
		if (separator.length() == 0) {
			return "";
		}
		int pos = str.indexOf(separator);
		if (pos == -1) {
			return str;
		}
		return str.substring(0, pos);
	}
	public static String substringAfter(String str, String separator) {
		if (str == null) {
			return str;
		}
		if (separator == null) {
			return "";
		}
		int pos = str.indexOf(separator);
		if (pos == -1) {
			return "";
		}
		return str.substring(pos + separator.length());
	}	
}
