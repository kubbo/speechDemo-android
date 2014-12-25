package com.iflytek.voicedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.iflytek.cloud.*;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.util.ApkInstaller;
import com.iflytek.speech.util.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zhu on 2014/12/24.
 */
public class OrderDemo extends Activity implements View.OnClickListener,View.OnTouchListener{

    private static final String TAG = "OrderDemo";
    private SpeechRecognizer speechRecognizer;

    private SpeechSynthesizer speechSynthesizer;
    private String voicer="xiaoyan";

    private RecognizerDialog dialog;
    private ApkInstaller apkInstaller;


    private EditText text_receiver_address;
    private EditText text_receiver_phone;
    private EditText text_type;
    private EditText text_weight;
    private EditText text_remark;

    private Button btn_receiver_address;
    private Button btn_receiver_phone;
    private Button btn_type;
    private Button btn_weight;
    private Button btn_remark;
    private Button btn_submit;

    private Map<Button, EditText> buttonMap = new HashMap<Button, EditText>();
    private Lock lock = new ReentrantLock();

    private volatile CountDownLatch latch = new CountDownLatch(0);
    private Handler handler = new HandlerImpl();
    private AtomicBoolean error = new AtomicBoolean(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.orderdemo);
        initLayout();
        this.speechRecognizer = com.iflytek.cloud.SpeechRecognizer.createRecognizer(this, mInitListener);
        this.speechSynthesizer = SpeechSynthesizer.createSynthesizer(this, mInitListener);
        this.dialog = new RecognizerDialog(this, mInitListener);
        this.apkInstaller = new ApkInstaller(this);

        new Servant().start();

    }

    private class Servant extends Thread{
        @Override
        public void run() {
            while (error.get()) {
                ask_wait("欢迎下单，请输入您的的收件人地址");
                response(text_receiver_address);//wait tell and wait told
            }
            error.set(true);

            while (error.get() ) {
                ask_wait("请输入您的收件人电话");
                response(text_receiver_phone);
            }
            error.set(true);

            while (error.get()) {
                ask_wait("请输入您的物品类型");
                response(text_type);
            }
            error.set(true);


            while (error.get()) {
                ask_wait("请输入您的物品重量");
                response(text_weight);
            }
            error.set(true);
            ask_wait("您还有其他要说的么");
            response(text_remark);
            error.set(false);

            ask_wait("您的订单录入完毕，请您确认后提交");


        }
    }

    private class HandlerImpl extends Handler{


        @Override
        public void handleMessage(Message msg) {
            EditText editText = null;
            switch (msg.what){
                case 0:editText = text_receiver_address;break;
                case 1:editText = text_receiver_phone;break;
                case 2:editText = text_type;break;
                case 3:editText = text_weight;break;
                case 4:
                    editText = text_remark;
                    break;
            }
            editText.setText("");

            setParam();
            dialog.setListener(new RecognizerDialogListenerImpl(editText));
            dialog.show();
            showTip("请开始说话");


        }
    }

    private void initLayout() {

        //address
        this.text_receiver_address = ((EditText) findViewById(R.id.text_receiver_address));
        this.btn_receiver_address = ((Button) findViewById(R.id.btn_receiver_address));
        this.btn_receiver_address.setOnTouchListener(this);
        this.buttonMap.put(this.btn_receiver_address, this.text_receiver_address);


        //phone
        this.text_receiver_phone = ((EditText) findViewById(R.id.text_receiver_phone));
        this.btn_receiver_phone = ((Button) findViewById(R.id.btn_receiver_phone));
        this.btn_receiver_phone.setOnTouchListener(this);
        buttonMap.put(btn_receiver_phone, text_receiver_phone);

        //type
        this.text_type = ((EditText) findViewById(R.id.text_type));
        this.btn_type = ((Button) findViewById(R.id.btn_type));
        this.btn_type.setOnTouchListener(this);
        buttonMap.put(btn_type, text_type);

        //weight
        this.text_weight = ((EditText) findViewById(R.id.text_weight));
        this.btn_weight = ((Button) findViewById(R.id.btn_weight));
        this.btn_weight.setOnTouchListener(this);
        buttonMap.put(btn_weight, text_weight);

        //remark
        this.text_remark = ((EditText) findViewById(R.id.text_remark));
        this.btn_remark = ((Button) findViewById(R.id.btn_remark));
        this.btn_remark.setOnTouchListener(this);
        buttonMap.put(btn_remark, text_remark);


        //submit
        this.btn_submit = ((Button) findViewById(R.id.order_submit));
        this.btn_submit.setOnClickListener(this);



    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            EditText editText = buttonMap.get(view);
            editText.setText("");
            setParam();
            dialog.setListener(new RecognizerDialogListenerImpl(editText));
            dialog.show();
            showTip("请开始说话");
        }else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            speechRecognizer.stopListening();
        }
        return true;
    }




    private void response(EditText editText) {
        //
        latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (Exception e) {

        }
        Message message = new Message();
        if (editText == text_receiver_address) {
            message.what = 0;
        } else if (editText == text_receiver_phone) {
            message.what = 1;
        } else if (editText == text_type) {
            message.what = 2;
        } else if (editText == text_weight) {
            message.what = 3;
        } else if (editText == text_remark) {
            message.what = 4;
        }
        handler.sendMessage(message);
        //等待消息录入
        latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    @SuppressLint("WrongViewCast")
    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.order_submit) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);

            TextView address = new TextView(this);
            address.setText("收件人地址："+this.text_receiver_address.getText().toString());

            TextView phone = new TextView(this);
            phone.setText("收件人电话："+this.text_receiver_phone.getText().toString());

            TextView type = new TextView(this);
            type.setText("物品类型："+this.text_type.getText().toString());

            TextView weight = new TextView(this);
            weight.setText("物品重量："+this.text_weight.getText().toString());

            TextView remark = new TextView(this);
            remark.setText("备注："+this.text_remark.getText().toString());

            layout.addView(address);
            layout.addView(phone);
            layout.addView(type);
            layout.addView(weight);
            layout.addView(remark);

            new AlertDialog.Builder(this).setTitle("订单详情")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(layout).setPositiveButton("确定", null)
                    .setNegativeButton("取消", null).show();

        }


    }

    private void showTip(final String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT);
    }

    private void setParam() {
        speechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        speechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        speechRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin");
        speechRecognizer.setParameter(SpeechConstant.VAD_BOS, "4000");
        speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "1000");
        speechRecognizer.setParameter(SpeechConstant.ASR_PTT, "0");
        speechRecognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/iflytek/wavaudio.pcm");
    }

    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            }
        }
    };

    private void ask_wait(String text) {
        if (text != null) {
            this.speechSynthesizer.startSpeaking(text, new SynthesizerListener() {

                @Override
                public void onSpeakBegin() {

                }

                @Override
                public void onBufferProgress(int i, int i1, int i2, String s) {

                }

                @Override
                public void onSpeakPaused() {

                }

                @Override
                public void onSpeakResumed() {

                }

                @Override
                public void onSpeakProgress(int i, int i1, int i2) {

                }

                @Override
                public void onCompleted(SpeechError speechError) {
                    latch.countDown();
                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {

                }
            });
        }

    }






    /**
     * 听写UI监听器
     */
    private  class RecognizerDialogListenerImpl implements RecognizerDialogListener{
        private EditText editText;
        public RecognizerDialogListenerImpl(EditText editText) {
            this.editText = editText;
        }
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            String text = JsonParser.parseIatResult(results.getResultString());
            this.editText.append(text);
            this.editText.setSelection(text.length());
            if (isLast) {
                if(error.compareAndSet(true, false)){
                    latch.countDown();
                }

            }

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
            if(OrderDemo.this.error.compareAndSet(true,true)){
                latch.countDown();;
            }


        }

    };


}
