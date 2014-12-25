package com.iflytek.voicedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

    private Toast mToast;

    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置标题栏（无标题）
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        SimpleAdapter listitemAdapter = new SimpleAdapter();
        ((ListView) findViewById(R.id.listview_main)).setAdapter(listitemAdapter);
    }

    @Override
    public void onClick(View view) {
        int tag = Integer.parseInt(view.getTag().toString());
        Intent intent = null;
        switch (tag) {
            case 0:
                // 语音转写
                intent = new Intent(this, IatDemo.class);
                break;
            case 1:
                // 语法识别
                intent = new Intent(this, AsrDemo.class);
                break;
            case 2:
                // 语义理解
                intent = new Intent(this, UnderstanderDemo.class);
                break;
            case 3:
                // 合成
                intent = new Intent(this, TtsDemo.class);
                break;
            case 4:
                // 唤醒
                showTip("请登录：http://open.voicecloud.cn/ 下载体验吧！");
                break;
            case 5:
                // 声纹
                showTip("在IsvDemo中哦，为了代码简洁，就不放在一起啦，^_^");
                break;
            case 6:
                intent = new Intent(this, OrderDemo.class);
                break;

            default:
                showTip("您要操作demo不存在!!");
                break;
        }
        if (intent != null) {
            startActivity(intent);
        }
    }


    //Menu 列表
    String items[] = {"立刻体验语音听写", "立刻体验语法识别", "立刻体验语义理解", "立刻体验语音合成", "立刻体验语音唤醒", "立刻体验声纹密码", "语音下单"};

    private class SimpleAdapter extends BaseAdapter {
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                View mView = factory.inflate(R.layout.list_items, null);
                convertView = mView;
            }
            Button btn = (Button) convertView.findViewById(R.id.btn);
            btn.setOnClickListener(MainActivity.this);
            btn.setTag(position);
            btn.setText(items[position]);
            return convertView;
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }
}
