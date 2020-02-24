package com.wtz.gallery.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.LinearLayout;

import java.util.List;

import static com.wtz.gallery.view.TabItem.INVALID_COLOR;
import static com.wtz.gallery.view.TabItem.INVALID_RES_ID;
import static com.wtz.gallery.view.TabItem.dp2px;


public class TabLayout extends LinearLayout implements TabItem.TabItemFocusChangeListener {
    private static final String TAG = "TabLayout";

    private List<String> titles;

    private static final int DEFAULT_ITEM_SPACING_DP = 15;
    private int itemSpacingResId = INVALID_RES_ID;

    private int itemBackgroundId = INVALID_RES_ID;

    private int itemPaddingLeftResId = INVALID_RES_ID;
    private int itemPaddingTopResId = INVALID_RES_ID;
    private int itemPaddingRightResId = INVALID_RES_ID;
    private int itemPaddingBottomResId = INVALID_RES_ID;

    private int unfocusedTextColor = INVALID_COLOR;
    private int unfocusedHighlightTextColor = INVALID_COLOR;
    private int focusedTextColor = INVALID_COLOR;

    private int unfocusedTextSizeResId = INVALID_RES_ID;
    private int unfocusedHighlightTextSizeResId = INVALID_RES_ID;
    private int focusedTextSizeResId = INVALID_RES_ID;

    private int lastFocusedPosition;
    private int lastUnfocusedHighlightPosition;

    private TabSelectListener tabSelectListener;

    public interface TabSelectListener {
        void onTabSelected(int index);
    }

    public TabLayout(Context context) {
        this(context, null);
    }

    public TabLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    }

    public TabLayout setItemSpacingResId(@DimenRes int spacingResId) {
        itemSpacingResId = spacingResId;
        return this;
    }

    public TabLayout setItemBackgroundResId(@DrawableRes int resId) {
        itemBackgroundId = resId;
        return this;
    }

    public TabLayout setItemPaddingResId(@DimenRes int leftId, @DimenRes int topId,
                                         @DimenRes int rightId, @DimenRes int bottomId) {
        itemPaddingLeftResId = leftId;
        itemPaddingTopResId = topId;
        itemPaddingRightResId = rightId;
        itemPaddingBottomResId = bottomId;
        return this;
    }

    public TabLayout setTextColor(@ColorInt int unfocused, @ColorInt int unfocusedHighlight,
                                  @ColorInt int focused) {
        unfocusedTextColor = unfocused;
        unfocusedHighlightTextColor = unfocusedHighlight;
        focusedTextColor = focused;
        return this;
    }

    public TabLayout setTextSizeSp(@DimenRes int unfocusedSpId, @DimenRes int unfocusedHighlightSpId,
                                   @DimenRes int focusedSpId) {
        unfocusedTextSizeResId = unfocusedSpId;
        unfocusedHighlightTextSizeResId = unfocusedHighlightSpId;
        focusedTextSizeResId = focusedSpId;
        return this;
    }

    public TabLayout setTabSelectListener(TabSelectListener tabSelectListener) {
        this.tabSelectListener = tabSelectListener;
        return this;
    }

    public void create(List<String> titles) {
        removeAllViews();

        int spacing = dp2px(getContext(), DEFAULT_ITEM_SPACING_DP);
        if (itemSpacingResId != INVALID_RES_ID) {
            spacing = getResources().getDimensionPixelSize(itemSpacingResId);
        }

        this.titles = titles;
        int listSize = titles.size();
        for (int i = 0; i < listSize; i++) {
            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = spacing;
            TabItem tabItem = new TabItem(getContext())
                    .setItemFocusChangeListener(this)
                    .setItemCount(listSize)
                    .setIndex(i)
                    .setItemBackgroundResId(itemBackgroundId)
                    .setItemPaddingResId(itemPaddingLeftResId, itemPaddingTopResId,
                            itemPaddingRightResId, itemPaddingBottomResId)
                    .setTitle(titles.get(i))
                    .setTitleColor(unfocusedTextColor, unfocusedHighlightTextColor, focusedTextColor)
                    .setTitleSizeSp(unfocusedTextSizeResId, unfocusedHighlightTextSizeResId, focusedTextSizeResId)
                    .create();
            addView(tabItem, layoutParams);
        }
    }

    public void select(int index) {
        if (index < 0 || index >= titles.size()) {
            return;
        }
        TabItem tabItem = (TabItem) getChildAt(index);
        tabItem.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        Log.d(TAG, "onFocusChange gainFocus=" + gainFocus + ",lastFocusedPosition=" + lastFocusedPosition);
        if (gainFocus) {
            // 从外界切到 TabLayout 时，选择上次获取焦点的子 item
            getChildAt(lastFocusedPosition).requestFocus();
        }
    }

    @Override
    public void onTabItemFocusChanged(TabItem tabItem, TabItem.STATUS status) {
        Log.d(TAG, "onTabItemFocusChanged tabItem index=" + tabItem.getIndex() + ",status=" + status);
        switch (status) {
            case FOCUSED:
                lastFocusedPosition = tabItem.getIndex();
                if (lastFocusedPosition != lastUnfocusedHighlightPosition) {
                    TabItem highlightTabItem = (TabItem) getChildAt(lastUnfocusedHighlightPosition);
                    highlightTabItem.setShouldHoldHighlight(false);
                    highlightTabItem.performUnfocused();
                }
                if (tabSelectListener != null) {
                    tabSelectListener.onTabSelected(lastFocusedPosition);
                }
                break;
            case UNFOCUSED_HIGHLIGHT:
                lastUnfocusedHighlightPosition = tabItem.getIndex();
                break;
            case UNFOCUSED:
                break;
        }
    }

}
