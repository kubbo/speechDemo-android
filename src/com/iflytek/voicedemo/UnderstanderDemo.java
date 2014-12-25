package com.iflytek.voicedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.speech.setting.UnderstanderSettings;
import com.iflytek.sunflower.FlowerCollector;

public class UnderstanderDemo extends Activity implements OnClickListener{
	private static String TAG = "UnderstanderDemo";
	// 语义理解对象（语音到语义）。
	private SpeechUnderstander mSpeechUnderstander;
	// 语义理解对象（文本到语义）。
	private TextUnderstander   mTextUnderstander;	
	private Toast mToast;	
	private EditText mUnderstanderText;
	
	private SharedPreferences mSharedPreferences;
	
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.understander);
		initLayout();
		
		// 初始化对象
		mSpeechUnderstander = SpeechUnderstander.createUnderstander(this, speechUnderstanderListener);
		mTextUnderstander = TextUnderstander.createTextUnderstander(this, textUnderstanderListener);
		
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	}
	
	/**
	 * 初始化Layout。
	 */
	private void initLayout(){
		findViewById(R.id.text_understander).setOnClickListener(this);
		findViewById(R.id.start_understander).setOnClickListener(this);
		
		mUnderstanderText = (EditText)findViewById(R.id.understander_text);
		
		findViewById(R.id.understander_stop).setOnClickListener(this);
		findViewById(R.id.understander_cancel).setOnClickListener(this);
		findViewById(R.id.image_understander_set).setOnClickListener(this);
		
		mSharedPreferences = getSharedPreferences(UnderstanderSettings.PREFER_NAME, Activity.MODE_PRIVATE);
	}
	
    /**
     * 初始化监听器（语音到语义）。
     */
    private InitListener speechUnderstanderListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "speechUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}			
		}
    };
    
    /**
     * 初始化监听器（文本到语义）。
     */
    private InitListener textUnderstanderListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "textUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}
		}
    };
	
    
	int ret = 0;// 函数调用返回值
	@Override
	public void onClick(View view) 
	{				
		switch (view.getId()) {
		// 进入参数设置页面
		case R.id.image_understander_set:
			Intent intent = new Intent(UnderstanderDemo.this, UnderstanderSettings.class);
			startActivity(intent);
			break;
		// 开始文本理解
		case R.id.text_understander:
			mUnderstanderText.setText("");
			String text = "合肥明天的天气怎么样？";	
			showTip(text);
			
			if(mTextUnderstander.isUnderstanding()){
				mTextUnderstander.cancel();
				showTip("取消");
			}else {
				ret = mTextUnderstander.understandText(text, textListener);
				if(ret != 0)
				{
					showTip("语义理解失败,错误码:"+ ret);
				}
			}
			break;
		// 开始语音理解
		case R.id.start_understander:
			mUnderstanderText.setText("");
			// 设置参数
			setParam();
	
			if(mSpeechUnderstander.isUnderstanding()){// 开始前检查状态
				mSpeechUnderstander.stopUnderstanding();
				showTip("停止录音");
			}else {
				ret = mSpeechUnderstander.startUnderstanding(mRecognizerListener);
				if(ret != 0){
					showTip("语义理解失败,错误码:"	+ ret);
				}else {
					showTip(getString(R.string.text_begin));
				}
			}
			break;
		// 停止语音理解
		case R.id.understander_stop:
			mSpeechUnderstander.stopUnderstanding();
			showTip("停止语义理解");
			break;
		// 取消语音理解
		case R.id.understander_cancel:
			mSpeechUnderstander.cancel();
			showTip("取消语义理解");
			break;
		default:
			break;
		}
	}
	
	private TextUnderstanderListener textListener = new TextUnderstanderListener() {
		
		@Override
		public void onResult(final UnderstanderResult result) {
	       	runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (null != result) {
			            	// 显示
							Log.d(TAG, "understander result：" + result.getResultString());
							String text = result.getResultString();
							if (!TextUtils.isEmpty(text)) {
								mUnderstanderText.setText(text);
							}
			            } else {
			                Log.d(TAG, "understander result:null");
			                showTip("识别结果不正确。");
			            }
					}
				});
		}
		
		@Override
		public void onError(SpeechError error) {
			showTip("onError Code："	+ error.getErrorCode());
			
		}
	};
	
    /**
     * 识别回调。
     */
    private SpeechUnderstanderListener mRecognizerListener = new SpeechUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
	       	runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (null != result) {
			            	// 显示
							String text = result.getResultString();
							if (!TextUtils.isEmpty(text)) {
								mUnderstanderText.setText(text);
							}
			            } else {
			                showTip("识别结果不正确。");
			            }	
					}
				});
		}
    	
        @Override
        public void onVolumeChanged(int v) {
            showTip("onVolumeChanged："	+ v);
        }
        
        @Override
        public void onEndOfSpeech() {
			showTip("onEndOfSpeech");
        }
        
        @Override
        public void onBeginOfSpeech() {
			showTip("onBeginOfSpeech");
        }

		@Override
		public void onError(SpeechError error) {
			showTip("onError Code："	+ error.getErrorCode());
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// TODO Auto-generated method stub
			
		}
    };
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
        // 退出时释放连接
    	mSpeechUnderstander.cancel();
    	mSpeechUnderstander.destroy();
    	if(mTextUnderstander.isUnderstanding())
    		mTextUnderstander.cancel();
    	mTextUnderstander.destroy();    
    }
	
    private void showTip(final String str)
	{
		mToast.setText(str);
		mToast.show();
	}
	
	/**
	 * 参数设置
	 * @param param
	 * @return 
	 */
	public void setParam(){
		String lag = mSharedPreferences.getString("understander_language_preference", "mandarin");
		if (lag.equals("en_us")) {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "en_us");
		}else {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mSpeechUnderstander.setParameter(SpeechConstant.ACCENT,lag);
		}
		// 设置语音前端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("understander_vadbos_preference", "4000"));
		// 设置语音后端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("understander_vadeos_preference", "1000"));
		// 设置标点符号
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("understander_punc_preference", "1"));
		// 设置音频保存路径
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/iflytek/wavaudio.pcm");
	}	
	
	@Override
	protected void onResume() {
		//移动数据统计分析
		FlowerCollector.onResume(this);
		FlowerCollector.onPageStart("UnderstanderDemo");
		super.onResume();
	}
	@Override
	protected void onPause() {
		//移动数据统计分析
		FlowerCollector.onPageEnd("UnderstanderDemo");
		FlowerCollector.onPause(this);
		super.onPause();
	}
}
