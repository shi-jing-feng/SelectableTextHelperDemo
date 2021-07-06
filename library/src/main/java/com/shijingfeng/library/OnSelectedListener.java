package com.shijingfeng.library;

import androidx.annotation.NonNull;

/**
 * function: 选择区域动态变化监听器
 * date: 2019/8/5
 */
public interface OnSelectedListener {

    /**
     * 选择区域变化 回调
     * @param content 内容
     */
    void onSelected(@NonNull CharSequence content);

}
