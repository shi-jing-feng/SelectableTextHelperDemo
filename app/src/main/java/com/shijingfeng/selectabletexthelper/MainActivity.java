package com.shijingfeng.selectabletexthelper;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.shijingfeng.library.SelectableTextHelper;

public class MainActivity extends AppCompatActivity {

    private TextView tvContent;
    private SelectableTextHelper mSelectableTextHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent = findViewById(R.id.tv_content);

        final View windowView = LayoutInflater.from(this).inflate(R.layout.layout_window_view, null);

        windowView.findViewById(R.id.tv_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectableTextHelper.close();
                Toast.makeText(MainActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                Log.e("测试", "复制的数据: " + mSelectableTextHelper.getSelectedCharSequence());
            }
        });

        windowView.findViewById(R.id.tv_select_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "全选", Toast.LENGTH_SHORT).show();
                mSelectableTextHelper.close();
            }
        });

        mSelectableTextHelper = new SelectableTextHelper.Builder(tvContent)
//                .setCursorHandleSizeInDp(20)
//                .setCursorHandleColor(Color.BLUE)
//                .setSelectedColor(Color.GRAY)
                .setWindowView(windowView)
                .build();
    }
}
