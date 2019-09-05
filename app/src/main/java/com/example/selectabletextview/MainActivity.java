package com.example.selectabletextview;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.library.SelectableTextHelper;

public class MainActivity extends AppCompatActivity {

    private TextView tvContent;
//    private EditText etContent;
    private SelectableTextHelper mSelectableTextHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent = findViewById(R.id.tv_content);
//        etContent = findViewById(R.id.et_content);

        final View windowView = LayoutInflater.from(this).inflate(R.layout.layout_window_view, null);

        mSelectableTextHelper = new SelectableTextHelper.Builder(tvContent)
                .setCursorHandleColor(Color.BLUE)
                .setCursorHandleSizeInDp(20)
                .setSelectedColor(Color.GREEN)
                .setWindowView(windowView)
                .build();
    }
}
