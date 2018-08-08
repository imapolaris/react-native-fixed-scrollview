package com.mayew.www.rnfixedscrollview;

import android.graphics.Color;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ReactClippingViewGroupHelper;
import com.facebook.react.uimanager.Spacing;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.annotations.ReactPropGroup;
import com.facebook.react.views.scroll.FpsListener;
import com.facebook.react.views.scroll.ReactScrollViewCommandHelper;
import com.facebook.react.views.scroll.ReactScrollViewHelper;
import com.facebook.react.views.scroll.ScrollEventType;
import com.facebook.yoga.YogaConstants;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by alex on 08/08/2018.
 */
@ReactModule(name = FixedScrollViewModule.REACT_CLASS)
public class FixedScrollViewModule
        extends ViewGroupManager<FixedScrollView>
        implements ReactScrollViewCommandHelper.ScrollCommandHandler<FixedScrollView> {
    protected static final String REACT_CLASS = "FixedScrollView";

    private static final int[] SPACING_TYPES = {
            Spacing.ALL, Spacing.LEFT, Spacing.RIGHT, Spacing.TOP, Spacing.BOTTOM,
    };

    private @Nullable
    FpsListener mFpsListener = null;

    public FixedScrollViewModule() {
        this(null);
    }

    public FixedScrollViewModule(@Nullable FpsListener fpsListener) {
        mFpsListener = fpsListener;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public FixedScrollView createViewInstance(ThemedReactContext context) {
        return new FixedScrollView(context, mFpsListener);
    }

    @ReactProp(name = "scrollEnabled", defaultBoolean = true)
    public void setScrollEnabled(FixedScrollView view, boolean value) {
        view.setScrollEnabled(value);
    }

    @ReactProp(name = "showsVerticalScrollIndicator")
    public void setShowsVerticalScrollIndicator(FixedScrollView view, boolean value) {
        view.setVerticalScrollBarEnabled(value);
    }

    @ReactProp(name = ReactClippingViewGroupHelper.PROP_REMOVE_CLIPPED_SUBVIEWS)
    public void setRemoveClippedSubviews(FixedScrollView view, boolean removeClippedSubviews) {
        view.setRemoveClippedSubviews(removeClippedSubviews);
    }

    /**
     * Computing momentum events is potentially expensive since we post a runnable on the UI thread
     * to see when it is done.  We only do that if {@param sendMomentumEvents} is set to true.  This
     * is handled automatically in js by checking if there is a listener on the momentum events.
     *
     * @param view
     * @param sendMomentumEvents
     */
    @ReactProp(name = "sendMomentumEvents")
    public void setSendMomentumEvents(FixedScrollView view, boolean sendMomentumEvents) {
        view.setSendMomentumEvents(sendMomentumEvents);
    }

    /**
     * Tag used for logging scroll performance on this scroll view. Will force momentum events to be
     * turned on (see setSendMomentumEvents).
     *
     * @param view
     * @param scrollPerfTag
     */
    @ReactProp(name = "scrollPerfTag")
    public void setScrollPerfTag(FixedScrollView view, String scrollPerfTag) {
        view.setScrollPerfTag(scrollPerfTag);
    }

    /**
     * When set, fills the rest of the scrollview with a color to avoid setting a background and
     * creating unnecessary overdraw.
     * @param view
     * @param color
     */
    @ReactProp(name = "endFillColor", defaultInt = Color.TRANSPARENT, customType = "Color")
    public void setBottomFillColor(FixedScrollView view, int color) {
        view.setEndFillColor(color);
    }

    /**
     * Controls overScroll behaviour
     */
    @ReactProp(name = "overScrollMode")
    public void setOverScrollMode(FixedScrollView view, String value) {
        view.setOverScrollMode(ReactScrollViewHelper.parseOverScrollMode(value));
    }

    @Override
    public @Nullable
    Map<String, Integer> getCommandsMap() {
        return ReactScrollViewCommandHelper.getCommandsMap();
    }

    @Override
    public void receiveCommand(
            FixedScrollView scrollView,
            int commandId,
            @Nullable ReadableArray args) {
        ReactScrollViewCommandHelper.receiveCommand(this, scrollView, commandId, args);
    }

    @Override
    public void flashScrollIndicators(FixedScrollView scrollView) {
        scrollView.flashScrollIndicators();
    }

    @Override
    public void scrollTo(
            FixedScrollView scrollView, ReactScrollViewCommandHelper.ScrollToCommandData data) {
        if (data.mAnimated) {
            scrollView.smoothScrollTo(data.mDestX, data.mDestY);
        } else {
            scrollView.scrollTo(data.mDestX, data.mDestY);
        }
    }
    @ReactPropGroup(names = {
            ViewProps.BORDER_RADIUS,
            ViewProps.BORDER_TOP_LEFT_RADIUS,
            ViewProps.BORDER_TOP_RIGHT_RADIUS,
            ViewProps.BORDER_BOTTOM_RIGHT_RADIUS,
            ViewProps.BORDER_BOTTOM_LEFT_RADIUS
    }, defaultFloat = YogaConstants.UNDEFINED)
    public void setBorderRadius(FixedScrollView view, int index, float borderRadius) {
        if (!YogaConstants.isUndefined(borderRadius)) {
            borderRadius = PixelUtil.toPixelFromDIP(borderRadius);
        }

        if (index == 0) {
            view.setBorderRadius(borderRadius);
        } else {
            view.setBorderRadius(borderRadius, index - 1);
        }
    }

    @ReactProp(name = "borderStyle")
    public void setBorderStyle(FixedScrollView view, @Nullable String borderStyle) {
        view.setBorderStyle(borderStyle);
    }

    @ReactPropGroup(names = {
            ViewProps.BORDER_WIDTH,
            ViewProps.BORDER_LEFT_WIDTH,
            ViewProps.BORDER_RIGHT_WIDTH,
            ViewProps.BORDER_TOP_WIDTH,
            ViewProps.BORDER_BOTTOM_WIDTH,
    }, defaultFloat = YogaConstants.UNDEFINED)
    public void setBorderWidth(FixedScrollView view, int index, float width) {
        if (!YogaConstants.isUndefined(width)) {
            width = PixelUtil.toPixelFromDIP(width);
        }
        view.setBorderWidth(SPACING_TYPES[index], width);
    }

    @ReactPropGroup(names = {
            "borderColor", "borderLeftColor", "borderRightColor", "borderTopColor", "borderBottomColor"
    }, customType = "Color")
    public void setBorderColor(FixedScrollView view, int index, Integer color) {
        float rgbComponent =
                color == null ? YogaConstants.UNDEFINED : (float) ((int)color & 0x00FFFFFF);
        float alphaComponent = color == null ? YogaConstants.UNDEFINED : (float) ((int)color >>> 24);
        view.setBorderColor(SPACING_TYPES[index], rgbComponent, alphaComponent);
    }

    @Override
    public void scrollToEnd(
            FixedScrollView scrollView,
            ReactScrollViewCommandHelper.ScrollToEndCommandData data) {
        // ScrollView always has one child - the scrollable area
        int bottom =
                scrollView.getChildAt(0).getHeight() + scrollView.getPaddingBottom();
        if (data.mAnimated) {
            scrollView.smoothScrollTo(scrollView.getScrollX(), bottom);
        } else {
            scrollView.scrollTo(scrollView.getScrollX(), bottom);
        }
    }

    @Override
    public @Nullable Map getExportedCustomDirectEventTypeConstants() {
        return createExportedCustomDirectEventTypeConstants();
    }

    public static Map createExportedCustomDirectEventTypeConstants() {
        return MapBuilder.builder()
                .put(ScrollEventType.SCROLL.getJSEventName(), MapBuilder.of("registrationName", "onScroll"))
                .put(ScrollEventType.BEGIN_DRAG.getJSEventName(), MapBuilder.of("registrationName", "onScrollBeginDrag"))
                .put(ScrollEventType.END_DRAG.getJSEventName(), MapBuilder.of("registrationName", "onScrollEndDrag"))
                .put(ScrollEventType.MOMENTUM_BEGIN.getJSEventName(), MapBuilder.of("registrationName", "onMomentumScrollBegin"))
                .put(ScrollEventType.MOMENTUM_END.getJSEventName(), MapBuilder.of("registrationName", "onMomentumScrollEnd"))
                .build();
    }
}
