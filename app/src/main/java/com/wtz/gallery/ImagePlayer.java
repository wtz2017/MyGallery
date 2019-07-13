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
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.wtz.gallery.speech.SpeechManager;
import com.wtz.gallery.speech.SpeechMessageListener;

import java.io.File;
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

    private static final String PATH_KEY_NO_VOICE = "no_voice";

    private List<String> mImageList = new ArrayList<>();
    private Map<String, String> mAudioMap;
    private int mIndex;
    private int mSize;
    private int mCurrentPage = -1;
    private String mCurrentName;
    private int mCurrentSpeakCount;
    private static final int MAX_SPEAK_COUNT = 2;

    private ImageView mImageView;

    private static final int DELAY_CHANGE_IMAGE_INTERVAL = 12000;
    private static final int MSG_CHANGE_IMAGE = 100;
    private static final int MSG_SPEECH = 102;
    private static final int MSG_PLAY_AUDIO = 103;
    private Handler mTotalControlHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHANGE_IMAGE:
                    nextIndex();
                    showImage();
                    break;
                case MSG_SPEECH:
                    SpeechManager.getInstance().speak(mCurrentName, mCurrentName);
                    mCurrentSpeakCount++;
                    break;
                case MSG_PLAY_AUDIO:
                    MusicManager.getInstance().openAudioPath(mAudioMap.get(mImageList.get(mCurrentPage)));
                    break;
            }
        }
    };

    private Handler mSpeechHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SpeechMessageListener.MSG_SYNTHESIZE_START:
                    Log.d(TAG, "mSpeechHandler MSG_SYNTHESIZE_START: " + msg);
                    break;
                case SpeechMessageListener.MSG_SYNTHESIZE_DATA_ARRIVED:
                    break;
                case SpeechMessageListener.MSG_SYNTHESIZE_FINISH:
                    Log.d(TAG, "mSpeechHandler MSG_SYNTHESIZE_FINISH: " + msg);
                    break;
                case SpeechMessageListener.MSG_SPEECH_START:
                    Log.d(TAG, "mSpeechHandler MSG_SPEECH_START: " + msg);
                    break;
                case SpeechMessageListener.MSG_SPEECH_PROGRESS_CHANGED:
                    break;
                case SpeechMessageListener.MSG_SPEECH_FINISH:
                    Log.d(TAG, "mSpeechHandler MSG_SPEECH_FINISH: " + msg
                            + ", mCurrentSpeakCount=" + mCurrentSpeakCount
                            + ", mCurrentName=" + mCurrentName);
                    if (msg != null && msg.obj != null) {
                        Bundle bundle = (Bundle) msg.obj;
                        String utterance_id = bundle.getString(SpeechMessageListener.BUNDLE_KEY_UTTERANCE_ID);
                        if (utterance_id != null && utterance_id.equals(mCurrentName)) {
                            if (mCurrentSpeakCount < MAX_SPEAK_COUNT) {
                                speechDelay(300);
                            } else {
                                playAudioDelay(300);
                            }
                        }
                    }
                    break;
                case SpeechMessageListener.MSG_ERROR:
                    Log.d(TAG, "mSpeechHandler MSG_ERROR: " + msg);
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

        SpeechManager.getInstance().init(this, mSpeechHandler);
        MusicManager.getInstance().init(this);
        MusicManager.getInstance().setRepeatCount(2);
        MusicManager.getInstance().setRepeatInterval(600);

        setScreen();

        setContentView(R.layout.activity_image_player);

        mImageView = findViewById(R.id.iv_image_show);

        showImage();
    }

    private void setScreen() {
        // 解锁屏幕，允许在锁屏上显示
        // 对于小米手机，还需要用户在设置里找到本应用权限允许在锁屏上显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 用来控制屏幕常亮
        final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_DIM_WAKE_LOCK |
                        PowerManager.ON_AFTER_RELEASE,
                this.getClass().getCanonicalName());
        mWakeLock.acquire();// 保持屏幕常亮
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + keyCode + "," + event.getAction());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp " + keyCode + "," + event.getAction());
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            delayAutoCycle();
        }
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

    private void delayAutoCycle() {
        mTotalControlHandler.removeMessages(MSG_CHANGE_IMAGE);
        mTotalControlHandler.sendEmptyMessageDelayed(MSG_CHANGE_IMAGE, DELAY_CHANGE_IMAGE_INTERVAL);
    }

    private void lastIndex() {
        mTotalControlHandler.removeMessages(MSG_CHANGE_IMAGE);
        mIndex--;
        if (mIndex < 0) {
            mIndex = mSize - 1;
        }
    }

    private void nextIndex() {
        mTotalControlHandler.removeMessages(MSG_CHANGE_IMAGE);
        mIndex++;
        if (mIndex > mSize - 1) {
            mIndex = 0;
        }
    }

    private void showImage() {
        String imagePath = mImageList.get(mIndex);
        Log.d(TAG, "showImage " + mIndex + ": " + imagePath);

        Picasso.get().load(imagePath)
                .fit()
                .centerInside()// 需要先调用fit或resize设置目标大小，否则直接调用会报错：Center inside requires calling resize with positive width and height.
                .placeholder(mImageView.getDrawable())
//                .noFade()
                .into(mImageView);
        mTotalControlHandler.sendEmptyMessageDelayed(MSG_CHANGE_IMAGE, DELAY_CHANGE_IMAGE_INTERVAL);

        selectImage(mIndex);
    }

    private void selectImage(int position) {
        if (position != mCurrentPage) {
            stopSpeech();
            stopAudio();

            mCurrentPage = position;
            mCurrentName = getImageName(position);
            mCurrentSpeakCount = 0;

            boolean canSpeech = !mImageList.get(mCurrentPage).contains(PATH_KEY_NO_VOICE);
            if (canSpeech) {
                speechDelay(1000);
            } else {
                playAudioDelay(1000);
            }
        }
    }

    private String getImageName(int position) {
        String path = mImageList.get(position);
        int slashIndex = path.lastIndexOf(File.separator);
        int suffixIndex = path.lastIndexOf(".");
        String name = path.substring(slashIndex + 1, suffixIndex);
        if ("知更鸟".equals(name)) {
            name = "知更(geng1)鸟";
        }
        return name;
    }

    private void speechDelay(long delayMillis) {
        stopSpeech();
        mTotalControlHandler.sendEmptyMessageDelayed(MSG_SPEECH, delayMillis);
    }

    private void stopSpeech() {
        mTotalControlHandler.removeMessages(MSG_SPEECH);
        SpeechManager.getInstance().stop();
    }

    private void playAudioDelay(long delayMillis) {
        stopAudio();
        mTotalControlHandler.sendEmptyMessageDelayed(MSG_PLAY_AUDIO, delayMillis);
    }

    private void stopAudio() {
        mTotalControlHandler.removeMessages(MSG_PLAY_AUDIO);
        MusicManager.getInstance().stop();
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
        SpeechManager.getInstance().destroy();
        MusicManager.getInstance().destroy();
        if (mWakeLock != null) {
            mWakeLock.release();// 取消屏幕常亮
        }
        mSpeechHandler.removeCallbacksAndMessages(null);
        mTotalControlHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

}
