/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule FixedScrollView
 * @flow
 */
'use strict';
/* $FlowFixMe(>=0.54.0 site=react_native_oss) This comment suppresses an error
 * found when Flow v0.54 was deployed. To see the error delete this comment and
 * run Flow. */
import React from 'react';
import {
    View, ScrollView, Platform, StyleSheet, requireNativeComponent
} from 'react-native';
const invariant = require('fbjs/lib/invariant');
const flattenStyle = StyleSheet.flatten;

/**
 * Component that wraps platform ScrollView while providing
 * integration with touch locking "responder" system.
 *
 * Keep in mind that ScrollViews must have a bounded height in order to work,
 * since they contain unbounded-height children into a bounded container (via
 * a scroll interaction). In order to bound the height of a ScrollView, either
 * set the height of the view directly (discouraged) or make sure all parent
 * views have bounded height. Forgetting to transfer `{flex: 1}` down the
 * view stack can lead to errors here, which the element inspector makes
 * easy to debug.
 *
 * Doesn't yet support other contained responders from blocking this scroll
 * view from becoming the responder.
 *
 *
 * `<ScrollView>` vs [`<FlatList>`](/react-native/docs/flatlist.html) - which one to use?
 *
 * `ScrollView` simply renders all its react child components at once. That
 * makes it very easy to understand and use.
 *
 * On the other hand, this has a performance downside. Imagine you have a very
 * long list of items you want to display, maybe several screens worth of
 * content. Creating JS components and native views for everything all at once,
 * much of which may not even be shown, will contribute to slow rendering and
 * increased memory usage.
 *
 * This is where `FlatList` comes into play. `FlatList` renders items lazily,
 * just when they are about to appear, and removes items that scroll way off
 * screen to save memory and processing time.
 *
 * `FlatList` is also handy if you want to render separators between your items,
 * multiple columns, infinite scroll loading, or any number of other features it
 * supports out of the box.
 */
// $FlowFixMe(>=0.41.0)
class FixedScrollView extends ScrollView<any, any> {
    render() {
        let ScrollViewClass;
        let ScrollContentContainerViewClass;
        if (Platform.OS === 'ios') {
            ScrollViewClass = RCTScrollView;
            ScrollContentContainerViewClass = RCTScrollContentView;
            warning(
                !this.props.snapToInterval || !this.props.pagingEnabled,
                'snapToInterval is currently ignored when pagingEnabled is true.'
            );
        } else if (Platform.OS === 'android') {
            if (this.props.horizontal) {
                ScrollViewClass = AndroidHorizontalScrollView;
                ScrollContentContainerViewClass = AndroidHorizontalScrollContentView;
            } else {
                ScrollViewClass = AndroidScrollView;
                ScrollContentContainerViewClass = View;
            }
        }

        invariant(
            ScrollViewClass !== undefined,
            'ScrollViewClass must not be undefined'
        );

        invariant(
            ScrollContentContainerViewClass !== undefined,
            'ScrollContentContainerViewClass must not be undefined'
        );

        const contentContainerStyle = [
            this.props.horizontal && styles.contentContainerHorizontal,
            this.props.contentContainerStyle,
        ];
        let style, childLayoutProps;
        if (__DEV__ && this.props.style) {
            style = flattenStyle(this.props.style);
            childLayoutProps = ['alignItems', 'justifyContent']
                .filter((prop) => style && style[prop] !== undefined);
            invariant(
                childLayoutProps.length === 0,
                'ScrollView child layout (' + JSON.stringify(childLayoutProps) +
                ') must be applied through the contentContainerStyle prop.'
            );
        }

        let contentSizeChangeProps = {};
        if (this.props.onContentSizeChange) {
            contentSizeChangeProps = {
                onLayout: this._handleContentOnLayout,
            };
        }

        const {stickyHeaderIndices} = this.props;
        const hasStickyHeaders = stickyHeaderIndices && stickyHeaderIndices.length > 0;
        const childArray = hasStickyHeaders && React.Children.toArray(this.props.children);
        const children = hasStickyHeaders ?
            childArray.map((child, index) => {
                const indexOfIndex = child ? stickyHeaderIndices.indexOf(index) : -1;
                if (indexOfIndex > -1) {
                    const key = child.key;
                    const nextIndex = stickyHeaderIndices[indexOfIndex + 1];
                    return (
                        <ScrollViewStickyHeader
                            key={key}
                            ref={(ref) => this._setStickyHeaderRef(key, ref)}
                            nextHeaderLayoutY={
                                this._headerLayoutYs.get(this._getKeyForIndex(nextIndex, childArray))
                            }
                            onLayout={(event) => this._onStickyHeaderLayout(index, event, key)}
                            scrollAnimatedValue={this._scrollAnimatedValue}>
                            {child}
                        </ScrollViewStickyHeader>
                    );
                } else {
                    return child;
                }
            }) :
            this.props.children;
        const contentContainer =
            <ScrollContentContainerViewClass
                {...contentSizeChangeProps}
                ref={this._setInnerViewRef}
                style={contentContainerStyle}
                removeClippedSubviews={
                    // Subview clipping causes issues with sticky headers on Android and
                    // would be hard to fix properly in a performant way.
                    Platform.OS === 'android' && hasStickyHeaders ?
                        false :
                        this.props.removeClippedSubviews
                }
                collapsable={false}>
                {children}
            </ScrollContentContainerViewClass>;

        const alwaysBounceHorizontal =
            this.props.alwaysBounceHorizontal !== undefined ?
                this.props.alwaysBounceHorizontal :
                this.props.horizontal;

        const alwaysBounceVertical =
            this.props.alwaysBounceVertical !== undefined ?
                this.props.alwaysBounceVertical :
                !this.props.horizontal;

        const DEPRECATED_sendUpdatedChildFrames =
            !!this.props.DEPRECATED_sendUpdatedChildFrames;

        const baseStyle = this.props.horizontal ? styles.baseHorizontal : styles.baseVertical;
        const props = {
            ...this.props,
            alwaysBounceHorizontal,
            alwaysBounceVertical,
            style: ([baseStyle, this.props.style]: ?Array<any>),
            // Override the onContentSizeChange from props, since this event can
            // bubble up from TextInputs
            onContentSizeChange: null,
            onMomentumScrollBegin: this.scrollResponderHandleMomentumScrollBegin,
            onMomentumScrollEnd: this.scrollResponderHandleMomentumScrollEnd,
            onResponderGrant: this.scrollResponderHandleResponderGrant,
            onResponderReject: this.scrollResponderHandleResponderReject,
            onResponderRelease: this.scrollResponderHandleResponderRelease,
            onResponderTerminate: this.scrollResponderHandleTerminate,
            onResponderTerminationRequest: this.scrollResponderHandleTerminationRequest,
            onScroll: this._handleScroll,
            onScrollBeginDrag: this.scrollResponderHandleScrollBeginDrag,
            onScrollEndDrag: this.scrollResponderHandleScrollEndDrag,
            onScrollShouldSetResponder: this.scrollResponderHandleScrollShouldSetResponder,
            onStartShouldSetResponder: this.scrollResponderHandleStartShouldSetResponder,
            onStartShouldSetResponderCapture: this.scrollResponderHandleStartShouldSetResponderCapture,
            onTouchEnd: this.scrollResponderHandleTouchEnd,
            onTouchMove: this.scrollResponderHandleTouchMove,
            onTouchStart: this.scrollResponderHandleTouchStart,
            onTouchCancel: this.scrollResponderHandleTouchCancel,
            scrollEventThrottle: hasStickyHeaders ? 1 : this.props.scrollEventThrottle,
            sendMomentumEvents: (this.props.onMomentumScrollBegin || this.props.onMomentumScrollEnd) ?
                true : false,
            DEPRECATED_sendUpdatedChildFrames,
        };

        const { decelerationRate } = this.props;
        if (decelerationRate) {
            props.decelerationRate = processDecelerationRate(decelerationRate);
        }

        const refreshControl = this.props.refreshControl;

        if (refreshControl) {
            if (Platform.OS === 'ios') {
                // On iOS the RefreshControl is a child of the ScrollView.
                // tvOS lacks native support for RefreshControl, so don't include it in that case
                return (
                    <ScrollViewClass {...props} ref={this._setScrollViewRef}>
                        {Platform.isTVOS ? null : refreshControl}
                        {contentContainer}
                    </ScrollViewClass>
                );
            } else if (Platform.OS === 'android') {
                // On Android wrap the ScrollView with a AndroidSwipeRefreshLayout.
                // Since the ScrollView is wrapped add the style props to the
                // AndroidSwipeRefreshLayout and use flex: 1 for the ScrollView.
                // Note: we should only apply props.style on the wrapper
                // however, the ScrollView still needs the baseStyle to be scrollable

                return React.cloneElement(
                    refreshControl,
                    {style: props.style},
                    <ScrollViewClass {...props} style={baseStyle} ref={this._setScrollViewRef}>
                        {contentContainer}
                    </ScrollViewClass>
                );
            }
        }
        return (
            <ScrollViewClass {...props} ref={this._setScrollViewRef}>
                {contentContainer}
            </ScrollViewClass>
        );
    }
};

const styles = StyleSheet.create({
    baseVertical: {
        flexGrow: 1,
        flexShrink: 1,
        flexDirection: 'column',
        overflow: 'scroll',
    },
    baseHorizontal: {
        flexGrow: 1,
        flexShrink: 1,
        flexDirection: 'row',
        overflow: 'scroll',
    },
    contentContainerHorizontal: {
        flexDirection: 'row',
    },
});

let nativeOnlyProps,
    AndroidScrollView,
    AndroidHorizontalScrollContentView,
    AndroidHorizontalScrollView,
    RCTScrollView,
    RCTScrollContentView;
if (Platform.OS === 'android') {
    nativeOnlyProps = {
        nativeOnly: {
            sendMomentumEvents: true,
        }
    };
    AndroidScrollView = requireNativeComponent(
        'FixedScrollView',
        (FixedScrollView: React.ComponentType<any>),
        nativeOnlyProps
    );

} else if (Platform.OS === 'ios') {
    nativeOnlyProps = {
        nativeOnly: {
            onMomentumScrollBegin: true,
            onMomentumScrollEnd : true,
            onScrollBeginDrag: true,
            onScrollEndDrag: true,
        }
    };
    RCTScrollView = requireNativeComponent(
        'FixedScrollView',
        (FixedScrollView: React.ComponentType<any>),
        nativeOnlyProps,
    );
    RCTScrollContentView = requireNativeComponent('FixedScrollView', View);
}

module.exports = FixedScrollView;
