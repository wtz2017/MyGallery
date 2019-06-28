package com.wtz.gallery;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;
import com.wtz.gallery.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ImagePlayer extends Activity {
    private static final String TAG = ImagePlayer.class.getSimpleName();

    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager.KeyguardLock mKeyguardLock;

    public static final String KEY_IMAGE_LIST = "key_image_list";
    public static final String KEY_AUDIO_MAP = "key_audio_map";
    public static final String KEY_IMAGE_INDEX = "key_image_index";

    private List<String> mImageList = new ArrayList<>();
    private Map<String, String> mAudioMap;
    private int mIndex;
    private int mSize;

    private SliderLayout mSliderLayout;
    private DefaultSliderView mSliderView;
    private static SliderLayout.Transformer[] TFS = new SliderLayout.Transformer[]{
            SliderLayout.Transformer.Accordion,
            SliderLayout.Transformer.Fade,
            SliderLayout.Transformer.Stack,
            SliderLayout.Transformer.ZoomIn,
            SliderLayout.Transformer.Tablet
    };
    private static final boolean useOnlyOneSliderView = false;

    private static final int DELAY_INTERVAL = 10000;
    private static final int MSG_CHANGE_IMAGE = 100;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHANGE_IMAGE:
                    nextIndex();
                    showImage();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        List<String> list = intent.getStringArrayListExtra(KEY_IMAGE_LIST);
        if (list == null || list.isEmpty()) {
            finish();
            return;
        }

        mAudioMap = (Map<String, String>) intent.getSerializableExtra(KEY_AUDIO_MAP);
        mImageList.addAll(list);
        mSize = mImageList.size();
        mIndex = intent.getIntExtra(KEY_IMAGE_INDEX, 0);
        if (mIndex < 0 || mIndex >= mSize) {
            mIndex = 0;
        }

        // 解锁屏幕，允许在锁屏上显示
        // 对于小米手机，还需要用户在设置里找到本应用权限允许在锁屏上显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 用来控制屏幕常亮
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_DIM_WAKE_LOCK |
                        PowerManager.ON_AFTER_RELEASE,
                this.getClass().getCanonicalName());
        mWakeLock.acquire();// 保持屏幕常亮

        setContentView(R.layout.activity_image_player);

        mSliderLayout = (SliderLayout) findViewById(R.id.slider);
        mSliderLayout.setDuration(DELAY_INTERVAL);
        mSliderLayout.setIndicatorVisibility(PagerIndicator.IndicatorVisibility.Invisible);
        mSliderLayout.addOnPageChangeListener(new ViewPagerEx.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                Log.d(TAG, "onPageScrolled " + position + ":" + positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected " + position);
                MusicManager.getInstance().openAudioPath(mAudioMap.get(mImageList.get(position)));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//                Log.d(TAG, "onPageScrollStateChanged " + state);
            }
        });

        if (useOnlyOneSliderView) {
            // 正常是有多少图片，就new多少个SliderView加入到SliderLayout
            // 由于我这里有太多图片，为了内存就只new一个SliderView，通过更改其url来切换图片
            mSliderView = new DefaultSliderView(this);
            mSliderView.setScaleType(BaseSliderView.ScaleType.CenterInside);
            mSliderLayout.addSlider(mSliderView);
        } else {
            int[] wh = ScreenUtils.getScreenPixels(this);
            for (String url : mImageList) {
                BaseSliderView sliderView = new DefaultSliderView(this);
                sliderView.setScaleType(BaseSliderView.ScaleType.CenterInside);
                sliderView.image(url);
                mSliderLayout.addSlider(sliderView);
            }
        }

        showImage();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        mSliderLayout.startAutoCycle();
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mSliderLayout.stopAutoCycle();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mWakeLock != null) {
            mWakeLock.release();// 取消屏幕常亮
        }
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                lastIndex();
                showImage();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                nextIndex();
                showImage();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void lastIndex() {
        mHandler.removeMessages(MSG_CHANGE_IMAGE);
        mIndex--;
        if (mIndex < 0) {
            mIndex = mSize - 1;
        }
    }

    private int getLastIndex(int index) {
        index--;
        if (index < 0) {
            index = mSize - 1;
        }
        return index;
    }

    private void nextIndex() {
        mHandler.removeMessages(MSG_CHANGE_IMAGE);
        mIndex++;
        if (mIndex > mSize - 1) {
            mIndex = 0;
        }
    }

    private void showImage() {
        String imagePath = mImageList.get(mIndex);
        Log.d(TAG, "showImage " + mIndex + ": " + imagePath);
        mSliderLayout.setPresetTransformer(TFS[mIndex % TFS.length]);
        if (useOnlyOneSliderView) {
            mSliderView.image(imagePath);
        } else {
//            mSliderLayout.setCurrentPosition(mIndex);// 为何会播放指定Index的下一张？
            mSliderLayout.setCurrentPosition(getLastIndex(mIndex));
        }
        mHandler.sendEmptyMessageDelayed(MSG_CHANGE_IMAGE, DELAY_INTERVAL);
    }

}
