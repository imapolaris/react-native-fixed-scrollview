import * as React from 'react';
import * as ReactNative from 'react-native';

declare module 'react-native-fixed-scrollview' {
    export interface FixedScrollViewProps {
        automaticallyAdjustContentInsets?: boolean;
        horizontal?: boolean;
        showsHorizontalScrollIndicator?: boolean;
        showsVerticalScrollIndicator?: boolean;
    }

    class ScrollView extends React.Component<FixedScrollViewProps, any> {}

    export default {FixedScrollView: ScrollView};
}