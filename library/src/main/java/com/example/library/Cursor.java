package com.example.library;

import android.graphics.PointF;

/**
 * function: 游标信息
 * date: 2019/8/6
 */
public class Cursor {

    /** 全局字符索引 */
    public int offset = 0;
    /** 行索引 */
    public int line = 0;
    /** 相对坐标 (去掉TextView的Padding后的相对坐标 (加上滑动位置)) */
    public PointF coord = new PointF();

    public Cursor() {}

    public void clear() {
        offset = 0;
        line = 0;
        coord.x = 0F;
        coord.y = 0F;
    }

}
