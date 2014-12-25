package com.iflytek.voicedemo;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

public class SpeechApp extends Application{
  
	@Override
	public void onCreate() {
		// 应用程序入口处调用,避免手机内存过小，杀死后台进程,造成SpeechUtility对象为null
		// 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true" 参数间使用“,”分隔。
		// 设置你申请的应用appid
		SpeechUtility.createUtility(SpeechApp.this, "appid="+getString(R.string.app_id));
		super.onCreate();
	}
}
