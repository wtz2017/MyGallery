package com.wtz.gallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

public abstract class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";

    private TabControlListener mTabControlListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    protected abstract void selectTab();

    protected void selectTabIndex(int index) {
        if (mTabControlListener == null) {
            mTabControlListener = (TabControlListener) getActivity();
        }
        mTabControlListener.selectTab(index);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

}
