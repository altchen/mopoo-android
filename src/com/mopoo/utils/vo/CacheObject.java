package com.mopoo.utils.vo;

import java.util.Date;

public class CacheObject<T> {
	public boolean isCache;
	public Date dataDateTime;
	public T data;
	public String errorMsg;
}
