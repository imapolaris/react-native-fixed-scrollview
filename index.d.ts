import * as React from 'react';
import * as ReactNative from 'react-native';

declare module 'react-native-fixed-scrollview' {
    export interface FixedScrollViewProps {
        automaticallyAdjustContentInsets?: boolean;
        horizontal?: boolean;
        showsHorizontalScrollIndicator?: boolean;
        showsVerticalScrollIndicator?: boolean;
    }

    export default class ScrollView extends React.Component<FixedScrollViewProps, any> {}
}