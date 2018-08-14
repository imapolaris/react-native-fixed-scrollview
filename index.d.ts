import * as React from 'react';
import * as ReactNative from 'react-native';

declare module 'react-native-fixed-scrollview' {
    export interface FixedScrollViewProps {
        automaticallyAdjustContentInsets?: boolean;
        horizontal?: boolean;
        showsHorizontalScrollIndicator?: boolean;
        showsVerticalScrollIndicator?: boolean;
        style?: any;

        onScroll?(event: Object): void;
    }

    export default class FixedScrollView extends React.Component<FixedScrollViewProps, any> {}
}