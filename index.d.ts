import * as React from 'react';
import * as ReactNative from 'react-native';
import {NativeScrollEvent, NativeSyntheticEvent, RefreshControlProps} from "react-native";

declare module 'react-native-fixed-scrollview' {
    export interface FixedScrollViewProps {
        horizontal?: boolean;
        showsHorizontalScrollIndicator?: boolean;
        showsVerticalScrollIndicator?: boolean;
        style?: any;
        maxScrollHeight?:number;
        autoScroll?:boolean;
        autoScrollAnimated?:boolean;
        autoScrollThresholdUp?:number;
        autoScrollThresholdDown?:number;
        overScrollMode?: "auto" | "always" | "never";
        scrollEnabled?:boolean;
        scrollEventThrottle?:number;
        scrollsToTop?:boolean;
        refreshControl?: React.ReactElement<RefreshControlProps>;

        onScroll?(event: NativeSyntheticEvent<NativeScrollEvent>): void;
        onScrollBeginDrag?(event: NativeSyntheticEvent<NativeScrollEvent>): void;
        onScrollEndDrag?(event: NativeSyntheticEvent<NativeScrollEvent>): void;
        onMomentumScrollEnd?(event: NativeSyntheticEvent<NativeScrollEvent>): void;
        onMomentumScrollBegin?(event: NativeSyntheticEvent<NativeScrollEvent>): void;
    }

    export default class FixedScrollView extends React.Component<FixedScrollViewProps, any> {}
}