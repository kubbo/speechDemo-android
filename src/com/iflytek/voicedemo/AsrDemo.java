package com.iflytek.voicedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.iflytek.speech.util.ApkInstaller;
import com.iflytek.speech.util.FucUtil;
import com.iflytek.speech.util.JsonParser;
import com.iflytek.sunflower.FlowerCollector;

public class AsrDemo extends Activity implements OnClickListener{
	private static String TAG = "AbnfDemo";
	// 语音识别对象
	private SpeechRecognizer mAsr;
	private Toast mToast;	
	// 缓存
	private SharedPreferences mSharedPreferences;
	// 本地语法文件
	private String mLocalGrammar = null;
	// 本地词典
	private String mLocalLexicon = null;
	// 云端语法文件
	private String mCloudGrammar = null;
		
	private static final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
	private static final String GRAMMAR_TYPE_ABNF = "abnf";
	private static final String GRAMMAR_TYPE_BNF = "bnf";

	private String mEngineType = "cloud";
	// 语音+安装助手类
	ApkInstaller mInstaller ;
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.isrdemo);
		initLayout();
		
		// 初始化识别对象
		mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);		

		// 初始化语法、命令词
		mLocalLexicon = "张海羊\n刘婧\n王锋\n";
		mLocalGrammar = FucUtil.readFile(this,"call.bnf", "utf-8");
		mCloudGrammar = FucUtil.readFile(this,"grammar_sample.abnf","utf-8");
		
		// 获取联系人，本地更新词典时使用
		ContactManager mgr = ContactManager.createManager(AsrDemo.this, mContactListener);	
		mgr.asyncQueryAllContactsName();
		mSharedPreferences = getSharedPreferences(getPackageName(),	MODE_PRIVATE);
		mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);	
		
		mInstaller = new  ApkInstaller(AsrDemo.this);
	}
	
	/**
	 * 初始化Layout。
	 */
	private void initLayout(){
		findViewById(R.id.isr_recognize).setOnClickListener(this);
		
		findViewById(R.id.isr_grammar).setOnClickListener(this);
		findViewById(R.id.isr_lexcion).setOnClickListener(this);
		
		findViewById(R.id.isr_stop).setOnClickListener(this);
		findViewById(R.id.isr_cancel).setOnClickListener(this);

		//选择云端or本地
		RadioGroup group = (RadioGroup)this.findViewById(R.id.radioGroup);
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.radioCloud)
				{
					((EditText)findViewById(R.id.isr_text)).setText(getString(R.string.text_isr_abnf_hint));
					findViewById(R.id.isr_lexcion).setEnabled(false);
					mEngineType = SpeechConstant.TYPE_CLOUD;
				}
				else
				{
					((EditText)findViewById(R.id.isr_text)).setText(mLocalGrammar);
					findViewById(R.id.isr_lexcion).setEnabled(true);
					mEngineType = SpeechConstant.TYPE_LOCAL;
					/**
					 * 选择本地合成
					 * 判断是否安装语音+,未安装则跳转到提示安装页面
					 */
					if (!SpeechUtility.getUtility().checkServiceInstalled()) {
						mInstaller.install();
					}
				}
			}
		});
	}
    
	
	String mContent;// 语法、词典临时变量
    int ret = 0;// 函数调用返回值
	@Override
	public void onClick(View view) {		
		switch(view.getId())
		{
			case R.id.isr_grammar:
				showTip("上传预设关键词/语法文件");
				// 本地-构建语法文件，生成语法id
				if (mEngineType.equals(SpeechConstant.TYPE_LOCAL)) {
					((EditText)findViewById(R.id.isr_text)).setText(mLocalGrammar);
					mContent = new String(mLocalGrammar);
					mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
					//指定引擎类型
					mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
					ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, localGrammarListener);
					if(ret != ErrorCode.SUCCESS){
						if(ret == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
							//未安装则跳转到提示安装页面
							mInstaller.install();
						}else {
							showTip("语法构建失败,错误码：" + ret);
						}
					}
				}
				// 在线-构建语法文件，生成语法id
				else {	
					((EditText)findViewById(R.id.isr_text)).setText(getString(R.string.text_isr_abnf_hint));
					mContent = new String(mCloudGrammar);
					//指定引擎类型
					mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
					mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
				    ret = mAsr.buildGrammar(GRAMMAR_TYPE_ABNF, mContent, cloudGrammarListener);
					if(ret != ErrorCode.SUCCESS)
						showTip("语法构建失败,错误码：" + ret);
				}
				
				break;
			// 本地-更新词典
			case R.id.isr_lexcion: 
				((EditText)findViewById(R.id.isr_text)).setText(mLocalLexicon);
				mContent = new String(mLocalLexicon);
				//指定引擎类型
				mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
				mAsr.setParameter(SpeechConstant.GRAMMAR_LIST, "call");
				ret = mAsr.updateLexicon("<contact>", mContent, lexiconListener);
				if(ret != ErrorCode.SUCCESS){
					if(ret == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
						//未安装则跳转到提示安装页面
						mInstaller.install();
					}else {
						showTip("更新词典失败,错误码：" + ret);
					}
				}
				break;
			// 开始识别
			case R.id.isr_recognize:
				((EditText)findViewById(R.id.isr_text)).setText(null);// 清空显示内容
				// 设置参数
				if (!setParam()) {
					showTip("请先构建语法。");
					return;
				};
				
				ret = mAsr.startListening(mRecognizerListener);
				if (ret != ErrorCode.SUCCESS) {
					if(ret == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
						//未安装则跳转到提示安装页面
						mInstaller.install();
					}else {
						showTip("识别失败,错误码: " + ret);	
					}
				}
				break;
			// 停止识别
			case R.id.isr_stop:
				mAsr.stopListening();
				showTip("停止识别");
				break;
			// 取消识别
			case R.id.isr_cancel:
				mAsr.cancel();
				showTip("取消识别");
				break;
		}
	}
	
	/**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}
		}
    };
    	
	/**
     * 更新词典监听器。
     */
	private LexiconListener lexiconListener = new LexiconListener() {
		@Override
		public void onLexiconUpdated(String lexiconId, SpeechError error) {
			if(error == null){
				showTip("词典更新成功");
			}else{
				showTip("词典更新失败,错误码："+error.getErrorCode());
			}
		}
	};
	
	/**
     * 本地构建语法监听器。
     */
	private GrammarListener localGrammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if(error == null){
				showTip("语法构建成功：" + grammarId);
			}else{
				showTip("语法构建失败,错误码：" + error.getErrorCode());
			}			
		}
	};
	/**
     * 云端构建语法监听器。
     */
	private GrammarListener cloudGrammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if(error == null){
				String grammarID = new String(grammarId);
				Editor editor = mSharedPreferences.edit();
				if(!TextUtils.isEmpty(grammarId))
					editor.putString(KEY_GRAMMAR_ABNF_ID, grammarID);
				editor.commit();
				showTip("语法构建成功：" + grammarId);
			}else{
				showTip("语法构建失败,错误码：" + error.getErrorCode());
			}			
		}
	};
	/**
	 * 获取联系人监听器。
	 */
	private ContactListener mContactListener = new ContactListener() {
		@Override
		public void onContactQueryFinish(String contactInfos, boolean changeFlag) {
			//获取联系人
			mLocalLexicon = contactInfos;
		}		
	};
	/**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        
        @Override
        public void onVolumeChanged(int volume) {
        	showTip("当前正在说话，音量大小：" + volume);
        }
        
        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
        	if (null != result) {
        		Log.d(TAG, "recognizer result：" + result.getResultString());
        		String text = JsonParser.parseGrammarResult(result.getResultString());
        		// 显示
        		((EditText)findViewById(R.id.isr_text)).setText(text);                
        	} else {
        		Log.d(TAG, "recognizer result : null");
        	}	
        }
        
        @Override
        public void onEndOfSpeech() {
        	showTip("结束说话");
        }
        
        @Override
        public void onBeginOfSpeech() {
        	showTip("开始说话");
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
    
	

    private void showTip(final String str)
    {
    	runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			mToast.setText(str);
    			mToast.show();
    		}
    	});
    }

	/**
	 * 参数设置
	 * @param param
	 * @return 
	 */
	public boolean setParam(){
		boolean result = false;
		//设置识别引擎
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		//设置返回结果为json格式
		mAsr.setParameter(SpeechConstant.RESULT_TYPE, "json");
		if("cloud".equalsIgnoreCase(mEngineType))
		{
			String grammarId = mSharedPreferences.getString(KEY_GRAMMAR_ABNF_ID, null);
			if(TextUtils.isEmpty(grammarId))
			{
				result =  false;
			}else {
				//设置云端识别使用的语法id
				mAsr.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarId);
				result =  true;
			}
		}
		else
		{
			//设置本地识别使用语法id
			mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "call");
			//设置识别的门限值
			mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
			result = true;
		}
		return result;
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出时释放连接
		mAsr.cancel();
		mAsr.destroy();
	}
	
	@Override
	protected void onResume() {
		//移动数据统计分析
		FlowerCollector.onResume(this);
		FlowerCollector.onPageStart("AsrDemo");
		super.onResume();
	}
	@Override
	protected void onPause() {
		//移动数据统计分析
		FlowerCollector.onPageEnd("AsrDemo");
		FlowerCollector.onPause(this);
		super.onPause();
	}
}
