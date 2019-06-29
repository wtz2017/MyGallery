package com.wtz.gallery.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * @author zhanghuagang 2017.7.6
 * 解决Item放大后，id靠前的Item放大后会被后面的遮盖问题
 */
public class ScaleGridView extends GridView {
    private static final String TAG = "ScaleGridView";

    public ScaleGridView(Context context) {
        this(context, null);
    }

    public ScaleGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setChildrenDrawingOrderEnabled(true);
        setSmoothScrollbarEnabled(true);
    }

    /**
     * 改变GridView对子view的绘制顺序，将选中的item项绘制显示在顶层
     */
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        // childCount 表示要绘制的可见的子view数量，i表示其中第几个条目
        if (this.getSelectedItemPosition() != -1) {
            if (i + this.getFirstVisiblePosition() == this.getSelectedItemPosition()) {
                // 这是选中的item，需要在最后一个刷新，所以设置最后一个位置
                return childCount - 1;
            }
            if (i == childCount - 1) {
                // 这是最后一个位置的条目，需要与选中条目交换位置
                return this.getSelectedItemPosition() - this.getFirstVisiblePosition();
            }
        }
        return i;
    }

}
