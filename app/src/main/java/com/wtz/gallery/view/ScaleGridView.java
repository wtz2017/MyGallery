package com.wtz.gallery.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * @author zhanghuagang 2017.7.6
 * 解决Item放大后，id靠前的Item放大后会被后面的遮盖问题
 */
public class ScaleGridView extends GridView {
    private View mLastView = null;

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

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (view != null)
            zoomInView(view);// 扩大
        if (view != mLastView && mLastView != null) {
            zoomOutView(mLastView);// 缩小
        }
        mLastView = view;
    }

    private void zoomInView(View v) {
        AnimatorSet animSet = new AnimatorSet();
        float[] values = new float[]{1.0f, 1.1f};
        animSet.playTogether(ObjectAnimator.ofFloat(v, "scaleX", values),
                ObjectAnimator.ofFloat(v, "scaleY", values));
        animSet.setDuration(50).start();
    }

    private void zoomOutView(View v) {
        AnimatorSet animSet = new AnimatorSet();
        float[] values = new float[]{1.18f, 1.0f};
        animSet.playTogether(ObjectAnimator.ofFloat(v, "scaleX", values),
                ObjectAnimator.ofFloat(v, "scaleY", values));
        animSet.setDuration(50).start();
    }

}
