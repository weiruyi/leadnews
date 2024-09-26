package com.heima.utils.thread;

import com.heima.model.user.pojos.ApUser;

public class AppThreadLocalUtil {
	private final static ThreadLocal<ApUser> APP_USER_THREAD_LOCAL = new ThreadLocal<>();

	//存入线程
	public static void setUser(ApUser user) {
		APP_USER_THREAD_LOCAL.set(user);
	}

	//从线程中获取
	public static ApUser getUser() {
		return APP_USER_THREAD_LOCAL.get();
	}

	//清除
	public static void clear(){
		APP_USER_THREAD_LOCAL.remove();
	}
}
