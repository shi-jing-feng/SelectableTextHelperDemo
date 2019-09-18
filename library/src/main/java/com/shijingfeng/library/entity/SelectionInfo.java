package com.shijingfeng.library.entity;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

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

    public boolean isClear() {
        return startCursor.isClear() && endCursor.isClear() && TextUtils.isEmpty(content);
    }

    public SelectionInfo clone() {
        final SelectionInfo newSelectionInfo = new SelectionInfo();

        newSelectionInfo.startCursor = this.startCursor.clone();
        newSelectionInfo.endCursor = this.endCursor.clone();

        if (this.content instanceof Spanned) {
            newSelectionInfo.content = new SpannableString(this.content);
        } else {
            newSelectionInfo.content = this.content;
        }

        return newSelectionInfo;
    }
}
