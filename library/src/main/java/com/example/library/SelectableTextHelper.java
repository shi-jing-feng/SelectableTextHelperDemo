package com.example.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.library.util.ScreenUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * function: 可自由选择TextView文本的帮助类
 * date: 2019/8/5
 *
 * 踩坑记录
 *  1. getPrimaryHorizontal和getSecondaryHorizontal坐标值一样, 是字符开始的位置, 且都是以索引开始(从0开始), 不是以offset开始(从1开始)
 *  2. Line从0开始, Offset从1开始 (Offset针对所有文本的，不是针对某一行的)
 *  3.
 */
public class SelectableTextHelper {

    /** 默认选择长度 */
    private static final int DEFAULT_SELECTION_LENGTH = 1;
    /** 默认延迟时间 */
    private static final int DEFAULT_SHOW_DURATION = 100;

    /** 开始游标, 操作框, 结束游标 全部是关闭状态 (默认状态) */
    private static final int ALL_CLOSED = 0;
    /** 开始游标, 操作框, 结束游标 全部是隐藏状态 */
    private static final int ALL_HIDED = 1;
    /** 开始游标, 操作框 是显示状态; 结束游标 是隐藏状态 */
    private static final int START_CURSOR_SHOWED = 2;
    /** 开始游标, 操作框 是隐藏状态; 结束游标 是显示状态 */
    private static final int END_CURSOR_SHOWED = 3;
    /** 开始游标, 操作框, 结束游标 全部是显示状态 */
    private static final int ALL_SHOWED = 4;

    /** 左边游标 */
    private static final int START = 1;
    /** 右边游标 */
    private static final int END = 2;

    private Context mContext;
    private TextView mTextView;
    /** 操作框View */
    private View mWindowView;
    /** 游标颜色 */
    private final int mCursorHandleColor;
    /** 游标尺寸 (宽和高一样) */
    private final int mCursorHandleSize;
    /** 被选择的文本背景色 */
    private final int mSelectedColor;
    /** 开始和结束游标信息 和 选择文本信息 */
    private final SelectionInfo mSelectionInfo;
    /** 开始和结束游标信息 和 选择文本信息 清除后的缓存 */
    private SelectionInfo mSelectionInfoCache;

    /** 是否正在滑动 */
    private boolean mIsScrolling;
    /** 是否开启功能 */
    private boolean mEnable;
    /** 游标和操作框的状态 {@link #ALL_CLOSED} */
    private int mStatus;
    /** mWindowView width */
    private int mWindowViewWidth;
    /** mWindowView height */
    private int mWindowViewHeight;
    /** 游标Padding */
    private int mCursorPadding = 0;
    /** X轴滑动距离 (手指向左滑动 增加值，手指向右滑动 减少值) */
    private int mScrollX;
    /** Y轴滑动距离 (手指向上滑动 增加值，手指向下滑动 减少值) */
    private int mScrollY;
    /** 外层TextView坐标  0: x  1: y */
    private int[] mTextViewCoord = new int[2];
    /** TextView ACTION_DOWN事件 相对坐标 */
    private PointF mTextViewDownPointF = new PointF();
    /** 操作框 PopupWindow */
    private OperatePopupWindow mOperatePopupWindow;
    /** 左边开始的游标 */
    private CursorHandle mStartCursorHandle;
    /** 右边结束的游标 */
    private CursorHandle mEndCursorHandle;
    /** Spannable 字符串 */
    private Spannable mSpannable;
    /** 背景颜色 Span */
    private BackgroundColorSpan mBackgroundColorSpan;
    /** TextView 窗口聚焦监听器 */
    private ViewTreeObserver.OnWindowFocusChangeListener mOnWindowFocusChangeListener;
    /** TextView 滚动监听器 */
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;
    /** 视图绘制前调用的监听器 */
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    /** TextView 布局改变监听器 */
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    /** 设置文本选择监听器 */
    private OnSelectedListener mOnSelectedListener;
    private Runnable mShowSelectViewRunnable;

    private SelectableTextHelper(@NonNull Builder builder) {
        this.mTextView = builder.mTextView;
        this.mContext = mTextView.getContext();
        this.mWindowView = builder.mWindowView;
        this.mCursorHandleColor = builder.mCursorHandleColor;
        this.mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
        this.mSelectedColor = builder.mSelectedColor;
        this.mSelectionInfo = new SelectionInfo();
        this.mSelectionInfoCache = new SelectionInfo();
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        initData();
        initListener();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(1 << 30 - 1, View.MeasureSpec.AT_MOST);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(1 << 30 - 1, View.MeasureSpec.AT_MOST);

        //测量 WindowView 宽高
        mWindowView.measure(widthMeasureSpec, heightMeasureSpec);

        mWindowViewWidth = mWindowView.getMeasuredWidth();
        mWindowViewHeight = mWindowView.getMeasuredHeight();

        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);

        mTextView.post(new Runnable() {
            @Override
            public void run() {
                mEnable = true;
            }
        });
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        mShowSelectViewRunnable = new Runnable() {
            @Override
            public void run() {
                if (mStatus == ALL_HIDED) {
                    mIsScrolling = false;
                    showSelectView();
                }
            }
        };

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case ACTION_DOWN:
                        //记录初始位置
                        mTextViewDownPointF.x = event.getX();
                        mTextViewDownPointF.y = event.getY();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取消 左右游标和操作框
                closeSelectView();
            }
        });
        mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mEnable) {
                    final float x = mTextViewDownPointF.x - mTextView.getPaddingLeft();
                    final float y = mTextViewDownPointF.y - mTextView.getPaddingTop();

                    mIsScrolling = false;

                    showSelectView(x, y);
                }
                return true;
            }
        });
        mTextView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTextView.getLocationOnScreen(mTextViewCoord);
                //滑动会影响TextView坐标
                mTextViewCoord[0] = mTextViewCoord[0] + mScrollX;
                mTextViewCoord[1] = mTextViewCoord[1] + mScrollY;
            }
        });
        mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (mTextView.getParent() == null) {
                    //没有父布局，以TextView滑动位置为准
                    mScrollX = mTextView.getScrollX();
                    mScrollY = mTextView.getScrollY();
                } else {
                    final ViewGroup parent = (ViewGroup) mTextView.getParent();

                    if (parent.getScrollX() != 0 || parent.getScrollY() != 0) {
                        //如果父布局能滑动，则使用以父布局滑动位置为准
                        mScrollX = parent.getScrollX();
                        mScrollY = parent.getScrollY();
                    } else {
                        //如果父布局不能滑动，以TextView滑动位置为准
                        mScrollX = mTextView.getScrollX();
                        mScrollY = mTextView.getScrollY();
                    }
                }

                Log.e("测试", "scroll mIsScrolling: " + mIsScrolling);

                if (!mIsScrolling) {
                    mIsScrolling = true;
                    hideSelectView();
                }
            }
        });
        mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mIsScrolling) {
//                    mIsScrolling = false;
                    postShowSelectView(DEFAULT_SHOW_DURATION);
                }
                return true;
            }
        });

//        mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
//            @Override
//            public void onViewAttachedToWindow(View v) {
//
//            }
//
//            @Override
//            public void onViewDetachedFromWindow(View v) {
//                destroy();
//            }
//        });
    }

    /**
     * 延迟显示游标和操作框
     * @param duration 延迟时间
     */
    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    /**
     * 用于滑动结束后显示游标和操作框
     */
    private void showSelectView() {
        final Layout layout = mTextView.getLayout();
        final int startOffset = mSelectionInfo.startCursor.offset;
        final int startLine = mSelectionInfo.startCursor.line;
        final int endOffset = mSelectionInfo.endCursor.offset;
        final int endLine = mSelectionInfo.endCursor.line;
        final int endLineEndOffset = layout.getLineEnd(endLine);

        mSelectionInfo.startCursor.coord.x = layout.getPrimaryHorizontal(startOffset - 1);
        mSelectionInfo.startCursor.coord.y = layout.getLineBottom(startLine);

        //防止当前行中最后一个Offset出现问题
        if (endOffset < endLineEndOffset) {
            //按照索引(从0开始), 而不是offset(从1开始)
            mSelectionInfo.endCursor.coord.x = layout.getPrimaryHorizontal(endOffset);
        } else {
            mSelectionInfo.endCursor.coord.x = layout.getLineWidth(endLine);
        }
        mSelectionInfo.endCursor.coord.y = layout.getLineBottom(endLine);

        final int parentWidth = ((ViewGroup) mTextView.getParent()).getWidth() - mTextView.getPaddingLeft() - mTextView.getPaddingRight();
        final int parentHeight = ((ViewGroup) mTextView.getParent()).getHeight() - mTextView.getPaddingTop() - mTextView.getPaddingBottom();
        final int startX = (int) mSelectionInfo.startCursor.coord.x - mScrollX;
        final int startY = (int) mSelectionInfo.startCursor.coord.y - mScrollY;
        final int endX = (int) mSelectionInfo.endCursor.coord.x - mScrollX;
        final int endY = (int) mSelectionInfo.endCursor.coord.y - mScrollY;

        boolean showStartCursor = true;
        boolean showEndCursor = true;

        //判断是否超界
        if (startX < 0) {
            showStartCursor = false;
        } else if (startX > parentWidth) {
            showStartCursor = false;
        }
        if (startY < 0) {
            showStartCursor = false;
        } else if (startY > parentHeight) {
            showStartCursor = false;
        }
        if (endX < 0) {
            showEndCursor = false;
        } else if (endX > parentWidth) {
            showEndCursor = false;
        }
        if (endY < 0) {
            showEndCursor = false;
        } else if (endY > parentHeight) {
            showEndCursor = false;
        }

        if (showStartCursor && showEndCursor) {
            mStatus = ALL_SHOWED;
        } else if (showStartCursor) {
            mStatus = START_CURSOR_SHOWED;
        } else if (showEndCursor) {
            mStatus = END_CURSOR_SHOWED;
        } else {
            mStatus = ALL_HIDED;
        }

        if (mOperatePopupWindow != null) {
            mOperatePopupWindow.show();
        }
        if (mStartCursorHandle != null) {
            showCursorHandle(mStartCursorHandle);
        }
        if (mEndCursorHandle != null) {
            showCursorHandle(mEndCursorHandle);
        }
    }

    /**
     * 显示 游标和操作框
     * @param x  X轴
     * @param y  Y轴
     */
    private void showSelectView(final float x, final float y) {

        closeSelectView();

        mStatus = ALL_SHOWED;

        if (mStartCursorHandle == null) {
            mStartCursorHandle = new CursorHandle(mContext, START);
        }
        if (mEndCursorHandle == null) {
            mEndCursorHandle = new CursorHandle(mContext, END);
        }
        if (mOperatePopupWindow == null) {
            mOperatePopupWindow = new OperatePopupWindow();
        }

        final Layout layout = mTextView.getLayout();
        final int line = layout.getLineForVertical((int) y);
        final int offset = layout.getOffsetForHorizontal(line, x);

        if (offset > 0) {
            selectText(offset, offset);
            showCursorHandle(mStartCursorHandle);
            showCursorHandle(mEndCursorHandle);
            mOperatePopupWindow.show();
        }
    }

    /**
     * 用于滑动时隐藏游标和操作框
     */
    private void hideSelectView() {

        if (mStatus != ALL_CLOSED) {
            mStatus = ALL_HIDED;
        }

        if (mOperatePopupWindow != null) {
            mOperatePopupWindow.dismiss();
        }
        if (mStartCursorHandle != null) {
            mStartCursorHandle.hide();
        }
        if (mEndCursorHandle != null) {
            mEndCursorHandle.hide();
        }
    }

    /**
     * 关闭 游标和操作框
     */
    private void closeSelectView() {

        mIsScrolling = false;
        mStatus = ALL_CLOSED;

        if (mOperatePopupWindow != null) {
            mOperatePopupWindow.dismiss();
        }
        if (mStartCursorHandle != null) {
            mStartCursorHandle.dismiss();
        }
        if (mEndCursorHandle != null) {
            mEndCursorHandle.dismiss();
        }
        mSelectionInfoCache = mSelectionInfo.clone();
        mSelectionInfo.clear();
    }

    /**
     * 显示 游标
     * @param cursorHandle 游标
     */
    private void showCursorHandle(@NonNull final CursorHandle cursorHandle) {
        final Cursor cursor;
        int x = 0;
        int y = 0;

        switch (cursorHandle.mCursorType) {
            case START:
                cursor = mSelectionInfo.startCursor;

                x = (int) cursor.coord.x + getExtraX() - mScrollX;
                y = (int) cursor.coord.y + getExtraY() - mScrollY;

                if (mStatus == ALL_HIDED || mStatus == END_CURSOR_SHOWED) {
                    cursorHandle.hide();
                    return;
                }

                if (x <= 2 * (mCursorHandleSize + 2 * mCursorPadding)) {
                    cursorHandle.mHorizontalAdjust = true;
                } else {
                    cursorHandle.mHorizontalAdjust = false;
                    x -= (mCursorHandleSize + 2 * mCursorPadding);
                }
                break;
            case END:
                final int screenWidth = ScreenUtil.getScreenWidth(mContext);

                cursor = mSelectionInfo.endCursor;

                x = (int) cursor.coord.x + getExtraX() - mScrollX;
                y = (int) cursor.coord.y + getExtraY() - mScrollY;

                if (mStatus == ALL_HIDED || mStatus == START_CURSOR_SHOWED) {
                    cursorHandle.hide();
                    return;
                }

                if ((screenWidth - x) <= 2 * (mCursorHandleSize + 2 * mCursorPadding)) {
                    cursorHandle.mHorizontalAdjust = true;
                    x -= (mCursorHandleSize + 2 * mCursorPadding);
                } else {
                    cursorHandle.mHorizontalAdjust = false;
                }
                break;
            default:
                break;
        }

        cursorHandle.show(x, y);
    }

    /**
     * 选择文本
     */
    private void selectText(int startOffset, int endOffset) {
        if (startOffset > endOffset) {
            //交换位置
            int temp = startOffset;
            startOffset = endOffset;
            endOffset = temp;
        }

        setSelectionInfo(startOffset, endOffset);

        if (mSpannable == null && mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }

        //offset是从1开始，而index是从0开始
        final int startIndex = startOffset >= 1 ? startOffset - 1 : 0;
        final int endIndex = endOffset;

        if (mBackgroundColorSpan == null) {
            mBackgroundColorSpan = new BackgroundColorSpan(mSelectedColor);
        }

        mSelectionInfo.content = mSpannable.subSequence(startIndex, endIndex).toString();
        mSpannable.setSpan(mBackgroundColorSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (mOnSelectedListener != null) {
            mOnSelectedListener.onSelected(mSelectionInfo.content);
        }
    }

    /**
     * 获得 X轴 TextView相对坐标 转 绝对坐标 增量
     * @return X轴 增量
     */
    private int getExtraX() {
        return mTextViewCoord[0] + mTextView.getPaddingLeft();
    }

    /**
     * 获得 Y轴 TextView相对坐标 转 绝对坐标 增量
     * @return Y轴 增量
     */
    private int getExtraY() {
        return mTextViewCoord[1] + mTextView.getPaddingTop();
    }

    /**
     * 设置选择数据
     * @param startOffset 开始 Offset
     * @param endOffset 结束 Offset
     */
    private void setSelectionInfo(int startOffset, int endOffset) {
        final Layout layout = mTextView.getLayout();

        if (startOffset <= 1) {
            mSelectionInfo.startCursor.offset = 1;
            mSelectionInfo.startCursor.line = 0;
            mSelectionInfo.startCursor.coord.x = 0;
            mSelectionInfo.startCursor.coord.y = layout.getLineBottom(0);
        } else {
            mSelectionInfo.startCursor.offset = startOffset;
            mSelectionInfo.startCursor.line = layout.getLineForOffset(startOffset);
            //按照索引(从0开始), 而不是offset(从1开始)
            mSelectionInfo.startCursor.coord.x = layout.getPrimaryHorizontal(startOffset - 1);
            mSelectionInfo.startCursor.coord.y = layout.getLineBottom(mSelectionInfo.startCursor.line);
        }
        if (endOffset > mTextView.length()) {
            mSelectionInfo.endCursor.offset = mTextView.length();
            mSelectionInfo.endCursor.line = mTextView.getLineCount() - 1;
            //按照索引(从0开始), 而不是offset(从1开始)
            mSelectionInfo.endCursor.coord.x = layout.getPrimaryHorizontal(mTextView.length());
            mSelectionInfo.endCursor.coord.y = layout.getLineBottom(mSelectionInfo.endCursor.line);
        } else {
            mSelectionInfo.endCursor.offset = endOffset;
            mSelectionInfo.endCursor.line = layout.getLineForOffset(endOffset - 1);

            final int line = mSelectionInfo.endCursor.line;
            final int lineEndOffset = layout.getLineEnd(line);

            //防止当前行中最后一个Offset出现问题
            if (endOffset < lineEndOffset) {
                //按照索引(从0开始), 而不是offset(从1开始)
                mSelectionInfo.endCursor.coord.x = layout.getPrimaryHorizontal(endOffset);
            } else {
                mSelectionInfo.endCursor.coord.x = layout.getLineWidth(line);
            }

            mSelectionInfo.endCursor.coord.y = layout.getLineBottom(line);
        }
    }

    /**
     * 获取选择的字符
     * @return 选择的字符
     */
    public CharSequence getSelectedCharSequence() {
        int startIndex = mSelectionInfo.startCursor.offset - 1;
        int endIndex = mSelectionInfo.endCursor.offset - 1;

        if (startIndex < 0) {
            startIndex = 0;
        }
        if (endIndex < 0) {
            endIndex = 0;
        }

        if (mBackgroundColorSpan != null) {
            mSpannable.removeSpan(mBackgroundColorSpan);
        }

        return mSpannable.subSequence(startIndex, endIndex + 1);
    }

    /**
     * 关闭游标和操作框
     */
    public void close() {
        mOperatePopupWindow.dismiss();
        closeSelectView();
    }

    /**
     * 销毁
     */
    public void destroy() {
        closeSelectView();
        mSelectionInfo.clear();
        //移除引用 防止内存泄漏
        if (mOnScrollChangedListener != null) {
            mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        }
        if (mOnPreDrawListener != null) {
            mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        }
        if (mOnWindowFocusChangeListener != null) {
            mTextView.getViewTreeObserver().removeOnWindowFocusChangeListener(mOnWindowFocusChangeListener);
        }
        if (mOnScrollChangedListener != null) {
            mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        }
        if (mOnGlobalLayoutListener != null) {
            mTextView.getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
        mStartCursorHandle = null;
        mEndCursorHandle = null;
        mOperatePopupWindow = null;
        mOnSelectedListener = null;
        mContext = null;
        mTextView = null;
        mSpannable = null;
        mWindowView = null;
        //回收对象
//        Runtime.getRuntime().gc();
    }

    /**
     * 设置 文本选择监听器
     * @param onSelectedListener 文本选择监听器
     */
    public void setOnSelectedListener(@NonNull OnSelectedListener onSelectedListener) {
        this.mOnSelectedListener = onSelectedListener;
    }

    /**
     * 游标 View
     */
    private class CursorHandle extends View {
        private final PointF mCurrentPointF = new PointF();
        private PopupWindow mPopupWindow;
        private Paint mPaint;
        /** 当前游标类型  START 或 END */
        private int mCursorType;
        /** 最小滑动大小 px */
        private int mScaledTouchSlop;
        /** 游标位置是否水平校正  true 水平校正，false 不水平校正 (第一个或最后一个位置存在游标显示不全情况，需要调整 (比如容器全屏的情况下会出现)) */
        private boolean mHorizontalAdjust;
        /** 游标位置是否垂直校正  true: 垂直校正 false: 不垂直校正 */
        private boolean mVerticalAdjust;

        public CursorHandle(@NonNull Context context, @CursorType int cursorType) {
            super(context);
            this.mCursorType = cursorType;
            init();
        }

        /**
         * 初始化
         */
        private void init() {
            mScaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(mCursorHandleColor);

            mPopupWindow = new PopupWindow();
            mPopupWindow.setContentView(this);
            mPopupWindow.setWidth(WRAP_CONTENT);
            mPopupWindow.setHeight(WRAP_CONTENT);
            mPopupWindow.setClippingEnabled(false);

            invalidate();
        }

        /**
         * 更新游标位置和游标信息
         * @param dx X轴变化的距离  向左为负值，向右为正值
         * @param dy Y轴变化的距离  向上为负值，向下为正值
         */
        private void update(final float dx, final float dy) {
            final Layout layout = mTextView.getLayout();
            final Cursor currentCursor = (mCursorType == START ? mSelectionInfo.startCursor : mSelectionInfo.endCursor);
            final Cursor oppositeCursor = (mCursorType == END ? mSelectionInfo.startCursor : mSelectionInfo.endCursor);

            //是否更新游标位置
            boolean update = false;

            int newOffset = 1;
            float newOffsetWidth = 0F;
            float newX = 0F;

            int newLine = 0;
            float newLineHeight = 0F;
            float newY = 0F;

            final PointF newCurrentPointF = new PointF();

            //是否更新X轴位置
            boolean updateX = false;
            boolean updateY = false;

            newCurrentPointF.x = mCurrentPointF.x;
            newCurrentPointF.y = mCurrentPointF.y;

            if (dx < 0) {
                //向左选择
                final int lineStartOffset = layout.getLineStart(currentCursor.line) + 1;

                //只有大于左边第一个Offset X轴才可以向左选择
                if (currentCursor.offset > lineStartOffset) {
                    updateX = true;
                    newOffset = currentCursor.offset - 1;
                    newOffsetWidth = mTextView.getPaint().measureText(mTextView.getText().charAt(newOffset - 1) + "");
                    newX = currentCursor.coord.x - newOffsetWidth;
                    newCurrentPointF.x = mCurrentPointF.x - newOffsetWidth;
                }
            } else {
                //向右选择
                final int lineEndOffset = layout.getLineEnd(currentCursor.line);

                //只有小于右边最后一个Offset X轴才可以向右选择
                if (currentCursor.offset < lineEndOffset) {
                    updateX = true;
                    newOffset = currentCursor.offset + 1;
                    newOffsetWidth = mTextView.getPaint().measureText(mTextView.getText().charAt(newOffset - 1) + "");
                    newX = currentCursor.coord.x + newOffsetWidth;
                    newCurrentPointF.x = mCurrentPointF.x + newOffsetWidth;
                }
            }
            if (updateX && Math.abs(dx) > newOffsetWidth) {
                //更新X轴数据
                update = true;
                currentCursor.offset = newOffset;
                currentCursor.coord.x = newX;
                mCurrentPointF.x = newCurrentPointF.x;
            }

            if (dy < 0) {
                //向上选择
                //只有当 当前Line 不是第一行时，才可以向上滑动
                if (currentCursor.line > 0) {
                    updateY = true;
                    newLine = currentCursor.line - 1;
                    newLineHeight = layout.getLineBottom(currentCursor.line) - layout.getLineTop(currentCursor.line);
                    newY = currentCursor.coord.y - newLineHeight;
                    newCurrentPointF.y = mCurrentPointF.y - newLineHeight;
                }
            } else {
                //向下选择
                //只有当 当前Line 不是最后一行时，才能向下滑动
                if (currentCursor.line < layout.getLineCount() - 1) {
                    updateY = true;
                    newLine = currentCursor.line + 1;
                    newLineHeight = layout.getLineBottom(newLine) - layout.getLineTop(newLine);
                    newY = currentCursor.coord.y + newLineHeight;
                    newCurrentPointF.y = mCurrentPointF.y + newLineHeight;
                }
            }
            if (updateY && Math.abs(dy) > newLineHeight) {
                //更新Y轴数据
                update = true;
                currentCursor.line = newLine;
                currentCursor.coord.y = newY;
                mCurrentPointF.y = newCurrentPointF.y;

                newOffset = layout.getOffsetForHorizontal(newLine, currentCursor.coord.x);
                //此处是由于x坐标位于两个Offset边界处，会选择前一个Offset, 所以开始游标向上选择x轴会向前偏移，故Offset手动向后加 1
                if (mCursorType == START) {
                    ++newOffset;
                }

                if (newOffset >= 1 && newOffset <= mTextView.length()) {
                    newX = (mCursorType == START ? layout.getPrimaryHorizontal(newOffset - 1) : layout.getPrimaryHorizontal(newOffset));

                    currentCursor.offset = newOffset;
                    currentCursor.coord.x = newX;
                    mCurrentPointF.x = newCurrentPointF.x;
                }
            }

            if (update) {
                //是否交换游标
                boolean switchCursor = false;

                if (mCursorType == START) {
                    if (currentCursor.offset > oppositeCursor.offset) {
                        switchCursor = true;
                        ++(oppositeCursor.offset);
                    }
                } else if (mCursorType == END) {
                    if (currentCursor.offset < oppositeCursor.offset) {
                        switchCursor = true;
                        --(oppositeCursor.offset);
                    }
                }

                if (switchCursor) {
                    //相对立的游标
                    final CursorHandle oppositeCursorHandle = (mCursorType == START ? mEndCursorHandle : mStartCursorHandle);

                    //交换游标信息
                    final Cursor tempCursor = mSelectionInfo.startCursor;
                    mSelectionInfo.startCursor = mSelectionInfo.endCursor;
                    mSelectionInfo.endCursor = tempCursor;

                    //交换游标View
                    final CursorHandle tempCursorHandle = mStartCursorHandle;
                    mStartCursorHandle = mEndCursorHandle;
                    mEndCursorHandle = tempCursorHandle;

                    //更改游标类型
                    this.mCursorType = (this.mCursorType == START ? END : START);
                    oppositeCursorHandle.mCursorType = (oppositeCursorHandle.mCursorType == START ? END : START);

                    this.updateLocation();
                    //更新当前游标View数据
                    this.invalidate();

                    //更新相对立的游标View
                    oppositeCursorHandle.updateLocation();
                    oppositeCursorHandle.invalidate();
                } else {
                    this.updateLocation();
                    invalidate();
                }
                //选择文字
                selectText(mSelectionInfo.startCursor.offset, mSelectionInfo.endCursor.offset);
            }
        }

        /**
         * 更新游标位置
         */
        private void updateLocation() {
            final Layout layout = mTextView.getLayout();
            int line;
            int offset;
            int x = 0;
            int y = 0;

            if (layout != null) {
                if (mCursorType == START) {
                    offset = mSelectionInfo.startCursor.offset;
                    line = mSelectionInfo.startCursor.line;

                    //由于左右游标交换会出现位置错乱，故此处重新计算坐标
                    mSelectionInfo.startCursor.coord.x = layout.getPrimaryHorizontal(offset - 1);

                    x = (int) mSelectionInfo.startCursor.coord.x + getExtraX() - mScrollX;
                    y = (int) mSelectionInfo.startCursor.coord.y + getExtraY() - mScrollY;

                    final int lineStartOffset = layout.getLineStart(line) + 1;

                    //当前行是第一行，当左侧滑动空间不足以再向左侧滑动时，调整游标位置在左边界右侧
                    if (x <= 2 * (mCursorHandleSize + 2 * mCursorPadding)) {
                        mHorizontalAdjust = true;
                    } else {
                        mHorizontalAdjust = false;
                        x = x - mCursorHandleSize - 2 * mCursorPadding;
                    }
                } else if (mCursorType == END) {
                    final int screenWidth = ScreenUtil.getScreenWidth(mContext);

                    offset = mSelectionInfo.endCursor.offset;
                    line = mSelectionInfo.endCursor.line;

                    final int lineEndOffset = layout.getLineEnd(line);

                    if (offset < lineEndOffset) {
                        //由于左右游标交换会出现位置错乱，故此处重新计算坐标
                        mSelectionInfo.endCursor.coord.x = layout.getPrimaryHorizontal(offset);
                    }

                    x = (int) mSelectionInfo.endCursor.coord.x + getExtraX() - mScrollX;
                    y = (int) mSelectionInfo.endCursor.coord.y + getExtraY() - mScrollY;

                    //当前行是最后一行，当右侧滑动空间不足以再向右侧滑动时，调整游标位置在右边界左侧
                    if ((screenWidth - x) <= 2 * (mCursorHandleSize + 2 * mCursorPadding)) {
                        mHorizontalAdjust = true;
                        x = x - mCursorHandleSize - 2 * mCursorPadding;
                    } else {
                        mHorizontalAdjust = false;
                    }
                }

                mPopupWindow.update(x, y, WRAP_CONTENT, WRAP_CONTENT);
            }
        }

        /**
         * 显示游标
         * @param x X轴显示位置 (绝对坐标)
         * @param y Y轴显示位置 (相对坐标)
         */
        public void show(final int x, final int y) {
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x, y);
        }

        public void hide() {
            if (mPopupWindow != null && mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
        }

        /**
         * 隐藏 游标
         */
        public void dismiss() {
            mHorizontalAdjust = false;
            mVerticalAdjust = false;
            if (mPopupWindow != null && mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
            mSpannable.removeSpan(mBackgroundColorSpan);
            mBackgroundColorSpan = null;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(mCursorHandleSize + 2 * mCursorPadding, mCursorHandleSize + 2 * mCursorPadding);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float x = (float) (mCursorHandleSize / 2 + mCursorPadding);
            final float y = (float) (mCursorHandleSize / 2 + mCursorPadding);
            final float radius = (float) (mCursorHandleSize / 2);

            canvas.drawCircle(x, y, radius, mPaint);

            switch (mCursorType) {
                case START:
                    if (mHorizontalAdjust) {
                        //校正, 在开始边界右边
                        canvas.drawRect(mCursorPadding, mCursorPadding, mCursorPadding + radius, mCursorPadding + radius, mPaint);
                    } else {
                        //不校正, 在开始边界左边
                        canvas.drawRect(x, mCursorPadding, x + radius, radius + mCursorPadding, mPaint);
                    }
                    break;
                case END:
                    if (mHorizontalAdjust) {
                        //校正, 在结束边界左边
                        canvas.drawRect(x, mCursorPadding, x + radius, radius + mCursorPadding, mPaint);
                    } else {
                        //不校正, 在结束边界右边
                        canvas.drawRect(mCursorPadding, mCursorPadding, mCursorPadding + radius, mCursorPadding + radius, mPaint);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case ACTION_DOWN:
                    mCurrentPointF.x = event.getRawX();
                    mCurrentPointF.y = event.getRawY();
                    //隐藏 OperatePopupWindow
                    if (mOperatePopupWindow != null) {
                        mOperatePopupWindow.dismiss();
                    }
                    break;
                case ACTION_MOVE:
                    final float rawX = event.getRawX();
                    final float rawY = event.getRawY();
                    final float dx = rawX - mCurrentPointF.x;
                    final float dy = rawY - mCurrentPointF.y;

                    update(dx, dy);
                    break;
                case ACTION_UP:
                case ACTION_CANCEL:
                    //显示 OperatePopupWindow
                    if (mOperatePopupWindow == null) {
                        mOperatePopupWindow = new OperatePopupWindow();
                    }
                    mOperatePopupWindow.show();
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    /**
     * 操作框 PopupWindow
     */
    private class OperatePopupWindow {

        /** PopupWindow elevation阴影宽度 (px) */
        private final float ELEVATION_WIDTH = TextLayoutUtil.dp2px(mContext, 2);

        private PopupWindow mPopupWindow;

        public OperatePopupWindow() {
            mPopupWindow = new PopupWindow();
            mPopupWindow.setContentView(mWindowView);
            mPopupWindow.setWidth(mWindowViewWidth);
            mPopupWindow.setHeight(mWindowViewHeight);
            mPopupWindow.setClippingEnabled(false);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                mPopupWindow.setElevation(ELEVATION_WIDTH);
            }
        }

        public void show() {
            final Layout layout = mTextView.getLayout();
            int x;
            int y;

            x = (int) (layout.getPrimaryHorizontal(mSelectionInfo.startCursor.offset - 1) + mTextViewCoord[0] - mScrollX);
            y = (int) (layout.getLineTop(mSelectionInfo.startCursor.line) + mTextViewCoord[1] - mWindowViewHeight - 2 * ELEVATION_WIDTH - mScrollY);

            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }

            if (mStatus == END_CURSOR_SHOWED || mStatus == ALL_HIDED || mStatus == ALL_CLOSED) {
                return;
            }

            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x, y);
        }

        public void dismiss() {
            if (mPopupWindow != null && mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
        }

    }

    @IntDef({START, END})
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    private @interface CursorType {}

    public static class Builder {

        private TextView mTextView;
        private View mWindowView;
        private int mCursorHandleColor;
        private float mCursorHandleSizeInDp;
        private int mSelectedColor;

        public Builder(@NonNull TextView textView) {
            this.mTextView = textView;
            this.mCursorHandleColor = mTextView.getContext().getResources().getColor(R.color.def_bg_cursor);
            this.mCursorHandleSizeInDp = 20;
            this.mSelectedColor = mTextView.getContext().getResources().getColor(R.color.def_bg_selected);
        }

        public Builder setWindowView(@NonNull View view) {
            this.mWindowView = view;
            return this;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }

    }

}
