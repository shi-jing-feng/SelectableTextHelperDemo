package com.example.library;

import android.graphics.PointF;

/**
 * function: 选择操作的状态信息
 * date: 2019/8/5
 */
public class SelectionInfo {

    /** 开始的左边的游标 */
    public Cursor startCursor = new Cursor();
    /** 结束的右边的游标 */
    public Cursor endCursor = new Cursor();
    /** 选择的内容 */
    public CharSequence content = "";

    public void clear() {
        startCursor.clear();
        endCursor.clear();
        content = "";
    }
}
