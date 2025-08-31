package com.topdon.module.user.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.topdon.lib.core.utils.ScreenUtil;

public class DragCustomerView extends androidx.appcompat.widget.AppCompatImageView {
    float mDownX;
    float mDownY;
    private int mWidth;
    private int mHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    private Context mContext;
    private boolean isDrag = false;

    public DragCustomerView(Context context) {
        super(context);
        this.mContext = context;
    }

    public DragCustomerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public DragCustomerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(!isInEditMode()){
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();
            mScreenWidth = ScreenUtil.getScreenWidth(getContext());
            mScreenHeight = ScreenUtil.getScreenHeight(getContext()) - BarUtils.getStatusBarHeight() - BarUtils.getNavBarHeight() - SizeUtils.dp2px(62f);
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    isDrag = false;
                    mDownX = event.getX();
                    mDownY = event.getY();
                    setPressed(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float mXDistance = event.getX() - mDownX;
                    float mYDistance = event.getY() - mDownY;
                    int left, right, top, bottom;
                    if (Math.abs(mXDistance) > 10 || Math.abs(mYDistance) > 10 && !isDrag) {
                        isDrag = true;
                        left = (int) (getLeft() + mXDistance);
                        right = left + mWidth;
                        top = (int) (getTop() + mYDistance);
                        bottom = top + mHeight;
                        if (left < 0) {
                            left = 0;
                            right = left + mWidth;
                        } else if (right > mScreenWidth) {
                            right = mScreenWidth;
                            left = right - mWidth;
                        }
                        if (top < 0) {
                            top = 0;
                            bottom = top + mHeight;
                        } else if (bottom > mScreenHeight) {
                            bottom = mScreenHeight;
                            top = bottom - mHeight;
                        }
                        this.layout(left, top, right, bottom);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(isDrag){
                        setPressed(false);
                    }
                    break;
            }
        return super.onTouchEvent(event);
        }
}
