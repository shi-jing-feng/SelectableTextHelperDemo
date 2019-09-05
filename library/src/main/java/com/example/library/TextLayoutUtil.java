package com.example.library;

import android.content.Context;
import android.text.Layout;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * function: TextView文本布局工具类
 * date: 2019/8/5
 */
public class TextLayoutUtil {

    /**
     * dp转px
     * @param context Context
     * @param dp dp
     * @return px
     */
    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static float px2dp(Context ctx,int px){
        float density = ctx.getResources().getDisplayMetrics().density;
        float dp = px / density;
        return dp;
    }
}
