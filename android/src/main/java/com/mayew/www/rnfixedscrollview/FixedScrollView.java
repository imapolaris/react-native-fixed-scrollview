package com.mayew.www.rnfixedscrollview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;
import android.widget.ScrollView;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.MeasureSpecAssertions;
import com.facebook.react.uimanager.ReactClippingViewGroup;
import com.facebook.react.uimanager.ReactClippingViewGroupHelper;
import com.facebook.react.uimanager.events.NativeGestureUtil;
import com.facebook.react.views.scroll.FpsListener;
import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.views.scroll.ReactScrollViewHelper;
import com.facebook.react.views.scroll.VelocityHelper;
import com.facebook.react.views.view.ReactViewBackgroundManager;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

/**
 * Created by alex on 08/08/2018.
 */
public class FixedScrollView extends ScrollView implements ReactClippingViewGroup, ViewGroup.OnHierarchyChangeListener, View.OnLayoutChangeListener {
    private static Field sScrollerField;
    private static boolean sTriedToGetScrollerField = false;

    private final OnScrollDispatchHelper mOnScrollDispatchHelper = new OnScrollDispatchHelper();
    private final OverScroller mScroller;
    private final VelocityHelper mVelocityHelper = new VelocityHelper();

    private @Nullable Rect mClippingRect;
    private boolean mDoneFlinging;
    private boolean mDragging;
    private boolean mFlinging;
    private boolean mRemoveClippedSubviews;
    private boolean mScrollEnabled = true;
    private boolean mSendMomentumEvents;
    private @Nullable FpsListener mFpsListener = null;
    private @Nullable String mScrollPerfTag;
    private @Nullable Drawable mEndBackground;
    private int mEndFillColor = Color.TRANSPARENT;
    private View mContentView;
    private ReactViewBackgroundManager mReactBackgroundManager;

    private boolean isIntercept = false;
    private boolean isbottom = false;
    private boolean istop = true;
    private float x1, x2, y1, y2;
    private ScrollView scrollView = null;
    private static final int FLING_MIN_DISTANCE = 5;

    public FixedScrollView(ReactContext context) {
        this(context, null);
    }

    public FixedScrollView(ReactContext context, @Nullable FpsListener fpsListener) {
        super(context);
        mFpsListener = fpsListener;
        mReactBackgroundManager = new ReactViewBackgroundManager(this);

        if (!sTriedToGetScrollerField) {
            sTriedToGetScrollerField = true;
            try {
                sScrollerField = ScrollView.class.getDeclaredField("mScroller");
                sScrollerField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.w(
                        ReactConstants.TAG,
                        "Failed to get mScroller field for ScrollView! " +
                                "This app will exhibit the bounce-back scrolling bug :(");
            }
        }

        if (sScrollerField != null) {
            try {
                Object scroller = sScrollerField.get(this);
                if (scroller instanceof OverScroller) {
                    mScroller = (OverScroller) scroller;
                } else {
                    Log.w(
                            ReactConstants.TAG,
                            "Failed to cast mScroller field in ScrollView (probably due to OEM changes to AOSP)! " +
                                    "This app will exhibit the bounce-back scrolling bug :(");
                    mScroller = null;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get mScroller from ScrollView!", e);
            }
        } else {
            mScroller = null;
        }

        setOnHierarchyChangeListener(this);
        setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mScrollEnabled) {
            return false;
        }

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候
            x1 = ev.getX();
            y1 = ev.getY();
            scrollView = findScrollView(this);
            isIntercept = false;
        }

        if ((action == MotionEvent.ACTION_MOVE) || (action == MotionEvent.ACTION_UP)) {
            if (scrollView != null) {
                //当手指移动或者抬起的时候计算其值
                x2 = ev.getX();
                y2 = ev.getY();
                //是否到底部
                isbottom = isAtBottom();
                //是否到顶部
                istop = isAtTop();
                //向上滑动
                if (y1 - y2 > FLING_MIN_DISTANCE ) {
                    if (!isbottom) {
                        isIntercept = true;
                    } else {
                        isIntercept = false;
                    }
                    return isIntercept;
                } //向下滑动
                else if (y2 - y1 > FLING_MIN_DISTANCE ) {
                    int st = scrollView.getScrollY();
                    if (!isbottom && !istop) {
                        isIntercept = true;
                    } else {
                        if (st == 0) {
                            isIntercept = true;
                        } else {
                            if (istop) {
                                isIntercept = false;
                            } else {
                                isIntercept = true;
                            }
                        }
                    }
                    return isIntercept;
                }
            }
        }
        //不加的话 ReactScrollView滑动不了
        if (super.onInterceptTouchEvent(ev)) {
            NativeGestureUtil.notifyNativeGestureStarted(this, ev);
            ReactScrollViewHelper.emitScrollBeginDragEvent(this);
            mDragging = true;
            enableFpsListener();
            return true;
        }
        return false;
    }

    /**
     * 从当前页面中查找第一个ScrollView控件
     * @param group
     * @return
     */
    private ScrollView findScrollView(ViewGroup group) {
        if (group != null) {
            for (int i = 0, j = group.getChildCount(); i < j; i++) {
                View child = group.getChildAt(i);
                if (child instanceof ScrollView) {
                    return (ScrollView) child;
                } else if (child instanceof ViewGroup) {
                    ScrollView result = findScrollView((ViewGroup) child);
                    if (result != null)
                        return result;
                }
            }
        }
        return null;
    }

    public boolean isAtBottom() {
        return getScrollY() == getChildAt(getChildCount() - 1).getBottom() + getPaddingBottom() - getHeight();
    }

    public boolean isAtTop() {
        return getScrollY() == 0;
    }

    public void setSendMomentumEvents(boolean sendMomentumEvents) {
        mSendMomentumEvents = sendMomentumEvents;
    }

    public void setScrollPerfTag(String scrollPerfTag) {
        mScrollPerfTag = scrollPerfTag;
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        mScrollEnabled = scrollEnabled;
    }

    public void flashScrollIndicators() {
        awakenScrollBars();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        MeasureSpecAssertions.assertExplicitMeasureSpec(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Call with the present values in order to re-layout if necessary
        scrollTo(getScrollX(), getScrollY());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mRemoveClippedSubviews) {
            updateClippingRect();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mRemoveClippedSubviews) {
            updateClippingRect();
        }
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);

        if (mOnScrollDispatchHelper.onScrollChanged(x, y)) {
            if (mRemoveClippedSubviews) {
                updateClippingRect();
            }

            if (mFlinging) {
                mDoneFlinging = false;
            }

            ReactScrollViewHelper.emitScrollEvent(
                    this,
                    mOnScrollDispatchHelper.getXFlingVelocity(),
                    mOnScrollDispatchHelper.getYFlingVelocity());
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mScrollEnabled) {
            return false;
        }

        mVelocityHelper.calculateVelocity(ev);
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_UP && mDragging) {
            ReactScrollViewHelper.emitScrollEndDragEvent(
                    this,
                    mVelocityHelper.getXVelocity(),
                    mVelocityHelper.getYVelocity());
            mDragging = false;
            disableFpsListener();
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void setRemoveClippedSubviews(boolean removeClippedSubviews) {
        if (removeClippedSubviews && mClippingRect == null) {
            mClippingRect = new Rect();
        }
        mRemoveClippedSubviews = removeClippedSubviews;
        updateClippingRect();
    }

    @Override
    public boolean getRemoveClippedSubviews() {
        return mRemoveClippedSubviews;
    }

    @Override
    public void updateClippingRect() {
        if (!mRemoveClippedSubviews) {
            return;
        }

        Assertions.assertNotNull(mClippingRect);

        ReactClippingViewGroupHelper.calculateClippingRect(this, mClippingRect);
        View contentView = getChildAt(0);
        if (contentView instanceof ReactClippingViewGroup) {
            ((ReactClippingViewGroup) contentView).updateClippingRect();
        }
    }

    @Override
    public void getClippingRect(Rect outClippingRect) {
        outClippingRect.set(Assertions.assertNotNull(mClippingRect));
    }

    @Override
    public void fling(int velocityY) {
        if (mScroller != null) {
            // FB SCROLLVIEW CHANGE

            // We provide our own version of fling that uses a different call to the standard OverScroller
            // which takes into account the possibility of adding new content while the ScrollView is
            // animating. Because we give essentially no max Y for the fling, the fling will continue as long
            // as there is content. See #onOverScrolled() to see the second part of this change which properly
            // aborts the scroller animation when we get to the bottom of the ScrollView content.

            int scrollWindowHeight = getHeight() - getPaddingBottom() - getPaddingTop();

            mScroller.fling(
                    getScrollX(),
                    getScrollY(),
                    0,
                    velocityY,
                    0,
                    0,
                    0,
                    Integer.MAX_VALUE,
                    0,
                    scrollWindowHeight / 2);

            postInvalidateOnAnimation();

            // END FB SCROLLVIEW CHANGE
        } else {
            super.fling(velocityY);
        }

        if (mSendMomentumEvents || isScrollPerfLoggingEnabled()) {
            mFlinging = true;
            enableFpsListener();
            ReactScrollViewHelper.emitScrollMomentumBeginEvent(this);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (mDoneFlinging) {
                        mFlinging = false;
                        disableFpsListener();
                        ReactScrollViewHelper.emitScrollMomentumEndEvent(FixedScrollView.this);
                    } else {
                        mDoneFlinging = true;
                        FixedScrollView.this.postOnAnimationDelayed(this, ReactScrollViewHelper.MOMENTUM_DELAY);
                    }
                }
            };
            postOnAnimationDelayed(r, ReactScrollViewHelper.MOMENTUM_DELAY);
        }
    }

    private void enableFpsListener() {
        if (isScrollPerfLoggingEnabled()) {
            Assertions.assertNotNull(mFpsListener);
            Assertions.assertNotNull(mScrollPerfTag);
            mFpsListener.enable(mScrollPerfTag);
        }
    }

    private void disableFpsListener() {
        if (isScrollPerfLoggingEnabled()) {
            Assertions.assertNotNull(mFpsListener);
            Assertions.assertNotNull(mScrollPerfTag);
            mFpsListener.disable(mScrollPerfTag);
        }
    }

    private boolean isScrollPerfLoggingEnabled() {
        return mFpsListener != null && mScrollPerfTag != null && !mScrollPerfTag.isEmpty();
    }

    private int getMaxScrollY() {
        int contentHeight = mContentView.getHeight();
        int viewportHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        return Math.max(0, contentHeight - viewportHeight);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mEndFillColor != Color.TRANSPARENT) {
            final View content = getChildAt(0);
            if (mEndBackground != null && content != null && content.getBottom() < getHeight()) {
                mEndBackground.setBounds(0, content.getBottom(), getWidth(), getHeight());
                mEndBackground.draw(canvas);
            }
        }
        super.draw(canvas);
    }

    public void setEndFillColor(int color) {
        if (color != mEndFillColor) {
            mEndFillColor = color;
            mEndBackground = new ColorDrawable(mEndFillColor);
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (mScroller != null) {
            // FB SCROLLVIEW CHANGE

            // This is part two of the reimplementation of fling to fix the bounce-back bug. See #fling() for
            // more information.

            if (!mScroller.isFinished() && mScroller.getCurrY() != mScroller.getFinalY()) {
                int scrollRange = getMaxScrollY();
                if (scrollY >= scrollRange) {
                    mScroller.abortAnimation();
                    scrollY = scrollRange;
                }
            }

            // END FB SCROLLVIEW CHANGE
        }

        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        mContentView = child;
        mContentView.addOnLayoutChangeListener(this);
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        mContentView.removeOnLayoutChangeListener(this);
        mContentView = null;
    }

    /**
     * Called when a mContentView's layout has changed. Fixes the scroll position if it's too large
     * after the content resizes. Without this, the user would see a blank ScrollView when the scroll
     * position is larger than the ScrollView's max scroll position after the content shrinks.
     */
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (mContentView == null) {
            return;
        }

        int currentScrollY = getScrollY();
        int maxScrollY = getMaxScrollY();
        if (currentScrollY > maxScrollY) {
            scrollTo(getScrollX(), maxScrollY);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        mReactBackgroundManager.setBackgroundColor(color);
    }

    public void setBorderWidth(int position, float width) {
        mReactBackgroundManager.setBorderWidth(position, width);
    }

    public void setBorderColor(int position, float color, float alpha) {
        mReactBackgroundManager.setBorderColor(position, color, alpha);
    }

    public void setBorderRadius(float borderRadius) {
        mReactBackgroundManager.setBorderRadius(borderRadius);
    }

    public void setBorderRadius(float borderRadius, int position) {
        mReactBackgroundManager.setBorderRadius(borderRadius, position);
    }

    public void setBorderStyle(@Nullable String style) {
        mReactBackgroundManager.setBorderStyle(style);
    }
}
