package com.wtz.gallery;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;

import com.wtz.gallery.adapter.ViewPagerAdapter;
import com.wtz.gallery.view.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements ViewPager.OnPageChangeListener, TabLayout.TabSelectListener, TabControlListener {
    private static final String TAG = "MainActivity";

    private TabLayout mTabLayout;
    private int mSelectedTab;

    private ViewPager mViewPager;
    private int mSelectedPage;

    private List<String> mTabTitleList;
    private List<Fragment> mFragmentList;

    private boolean isFirstShow = true;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabLayout = findViewById(R.id.tablayout);
        mViewPager = findViewById(R.id.view_pager);

        mTabTitleList = new ArrayList<>();
        mTabTitleList.add("图片");
        mTabTitleList.add("视频");
        mTabTitleList.add("音乐");

        mFragmentList = new ArrayList<>();
        mFragmentList.add(new ImageFragment());
        mFragmentList.add(new VideoFragment());
        mFragmentList.add(new MusicFragment());

        mTabLayout.setTabSelectListener(this)
                .setItemBackgroundResId(R.drawable.button_bg)
                .setItemSpacingResId(R.dimen.tab_item_spacing)
                .setTextSizeSp(R.dimen.tab_item_unfocused_text_size,
                        R.dimen.tab_item_unfocused_highlight_text_size,
                        R.dimen.tab_item_focused_text_size)
                .create(mTabTitleList);

        mViewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), mFragmentList, mTabTitleList));
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setOnPageChangeListener(this);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if (isFirstShow) {
            isFirstShow = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTabLayout.select(0);
                }
            }, 100);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + keyCode + "," + event.getAction());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp " + keyCode + "," + event.getAction());
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected " + position + ",mSelectedTab=" + mSelectedTab);
        mSelectedPage = position;
        if (mSelectedPage != mSelectedTab) {
            mTabLayout.select(mSelectedPage);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void selectTab(int index) {
        Log.d(TAG, "selectSelfTab " + index);
        mTabLayout.select(index);
    }

    @Override
    public int getTabCount() {
        return mTabTitleList.size();
    }

    @Override
    public void onTabSelected(int index) {
        Log.d(TAG, "onTabSelected " + index + ",mSelectedPage=" + mSelectedPage);
        mSelectedTab = index;
        if (mSelectedPage != mSelectedTab) {
            mViewPager.setCurrentItem(mSelectedTab);
        }
    }
}
