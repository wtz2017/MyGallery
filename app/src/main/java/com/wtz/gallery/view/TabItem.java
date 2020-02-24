package com.wtz.gallery.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TabItem extends RelativeLayout {
    private static final String TAG = "TabItem";

    private int itemCount;
    private int index;

    private String title;
    private TextView titleView;
    private boolean shouldHoldHighlight;

    public static final int INVALID_RES_ID = -1;
    public static final int INVALID_COLOR = -1;

    private int backgroundId = INVALID_RES_ID;

    private int itemPaddingLeftPx = dp2px(getContext(), 20);
    private int itemPaddingTopPx = dp2px(getContext(), 0);
    private int itemPaddingRightPx = dp2px(getContext(), 20);
    private int itemPaddingBottomPx = dp2px(getContext(), 0);

    private int unfocusedTextColor = Color.parseColor("#66ffffff");
    private int unfocusedHighlightTextColor = Color.parseColor("#E07800");
    private int focusedTextColor = Color.parseColor("#ffffff");

    private int textSizeUnit = TypedValue.COMPLEX_UNIT_SP;
    private float unfocusedTextSize = 20;
    private float unfocusedHighlightTextSize = 22;
    private float focusedTextSize = 22;

    enum STATUS {
        FOCUSED/*有焦点*/,
        UNFOCUSED/*无焦点*/,
        UNFOCUSED_HIGHLIGHT/*无焦点但保持高亮状态*/
    }

    interface TabItemFocusChangeListener {
        void onTabItemFocusChanged(TabItem tabItem, STATUS status);
    }

    private STATUS status = STATUS.UNFOCUSED;
    private TabItemFocusChangeListener tabItemFocusChangeListener;

    public TabItem(Context context) {
        this(context, null);
    }

    public TabItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TabItem setItemBackgroundResId(@DrawableRes int resId) {
        backgroundId = resId;
        return this;
    }

    public TabItem setItemPaddingResId(@DimenRes int leftId, @DimenRes int topId,
                                       @DimenRes int rightId, @DimenRes int bottomId) {
        if (leftId != INVALID_RES_ID) {
            itemPaddingLeftPx = getResources().getDimensionPixelSize(leftId);
        }
        if (topId != INVALID_RES_ID) {
            itemPaddingTopPx = getResources().getDimensionPixelSize(topId);
        }
        if (rightId != INVALID_RES_ID) {
            itemPaddingRightPx = getResources().getDimensionPixelSize(rightId);
        }
        if (bottomId != INVALID_RES_ID) {
            itemPaddingBottomPx = getResources().getDimensionPixelSize(bottomId);
        }
        return this;
    }

    public TabItem setItemCount(int itemCount) {
        this.itemCount = itemCount;
        return this;
    }

    public TabItem setIndex(int index) {
        this.index = index;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public TabItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public TabItem setTitleColor(@ColorInt int unfocused, @ColorInt int unfocusedHighlight,
                                 @ColorInt int focused) {
        if (unfocused != INVALID_COLOR) {
            unfocusedTextColor = unfocused;
        }
        if (unfocusedHighlight != INVALID_COLOR) {
            unfocusedHighlightTextColor = unfocusedHighlight;
        }
        if (focused != INVALID_COLOR) {
            focusedTextColor = focused;
        }
        return this;
    }

    public TabItem setTitleSizeSp(@DimenRes int unfocusedSpId, @DimenRes int unfocusedHighlightSpId,
                                  @DimenRes int focusedSpId) {
        textSizeUnit = TypedValue.COMPLEX_UNIT_SP;
        if (unfocusedSpId != INVALID_RES_ID) {
            unfocusedTextSize = getResources().getDimension(unfocusedSpId);
        }
        if (unfocusedHighlightSpId != INVALID_RES_ID) {
            unfocusedHighlightTextSize = getResources().getDimension(unfocusedHighlightSpId);
        }
        if (focusedSpId != INVALID_RES_ID) {
            focusedTextSize = getResources().getDimension(focusedSpId);
        }
        return this;
    }

    public TabItem setItemFocusChangeListener(TabItemFocusChangeListener tabItemFocusChangeListener) {
        this.tabItemFocusChangeListener = tabItemFocusChangeListener;
        return this;
    }

    public TabItem create() {
        setFocusable(true);
        setFocusableInTouchMode(true);

        if (backgroundId != INVALID_RES_ID) {
            setBackgroundResource(backgroundId);
        }

        setPadding(itemPaddingLeftPx, itemPaddingTopPx, itemPaddingRightPx, itemPaddingBottomPx);

        titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextColor(unfocusedTextColor);
        titleView.setTextSize(textSizeUnit, unfocusedTextSize);
        titleView.setGravity(Gravity.CENTER);

        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(titleView, layoutParams);
        return this;
    }

    public static int dp2px(Context context, float dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown index=" + index + ",keyCode=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                setShouldHoldHighlight(true);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (index == 0) {
                    return true;
                } else {
                    setShouldHoldHighlight(false);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (index == itemCount - 1) {
                    return true;
                } else {
                    setShouldHoldHighlight(false);
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setShouldHoldHighlight(boolean shouldHoldHighlight) {
        this.shouldHoldHighlight = shouldHoldHighlight;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        Log.w(TAG, "onFocusChange index=" + index + ",gainFocus=" + gainFocus);
        if (gainFocus) {
            performFocused();
        } else {
            if (shouldHoldHighlight) {
                performUnfocusedHighlight();
            } else {
                performUnfocused();
            }
        }
    }

    public void performFocused() {
        if (status == STATUS.FOCUSED) {
            return;
        }
        Log.w(TAG, "TabItem performFocused index= " + index);
        status = STATUS.FOCUSED;
        titleView.setTextSize(textSizeUnit, focusedTextSize);
        titleView.setTextColor(focusedTextColor);
        tabItemFocusChangeListener.onTabItemFocusChanged(this, STATUS.FOCUSED);
    }

    public void performUnfocusedHighlight() {
        if (status == STATUS.UNFOCUSED_HIGHLIGHT) {
            return;
        }
        Log.w(TAG, "TabItem performUnfocusedHighlight index= " + index);
        status = STATUS.UNFOCUSED_HIGHLIGHT;
        titleView.setTextSize(textSizeUnit, unfocusedHighlightTextSize);
        titleView.setTextColor(unfocusedHighlightTextColor);
        tabItemFocusChangeListener.onTabItemFocusChanged(this, STATUS.UNFOCUSED_HIGHLIGHT);
    }

    public void performUnfocused() {
        if (status == STATUS.UNFOCUSED) {
            return;
        }
        Log.w(TAG, "TabItem performUnfocused index= " + index);
        status = STATUS.UNFOCUSED;
        titleView.setTextSize(textSizeUnit, unfocusedTextSize);
        titleView.setTextColor(unfocusedTextColor);
        tabItemFocusChangeListener.onTabItemFocusChanged(this, STATUS.UNFOCUSED);
    }

}