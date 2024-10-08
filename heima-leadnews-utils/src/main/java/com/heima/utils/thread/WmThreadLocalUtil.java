package com.heima.utils.thread;

import com.heima.model.wemedia.pojos.WmUser;

public class WmThreadLocalUtil {
	private final static ThreadLocal<WmUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

	//存入线程
	public static void setUser(WmUser user) {
		WM_USER_THREAD_LOCAL.set(user);
	}

	//从线程中获取
	public static WmUser getUser() {
		return WM_USER_THREAD_LOCAL.get();
	}

	//清除
	public static void clear(){
		WM_USER_THREAD_LOCAL.remove();
	}
}
