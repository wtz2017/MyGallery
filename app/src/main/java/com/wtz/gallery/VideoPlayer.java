package com.wtz.gallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wtz.gallery.view.SurfaceVideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class VideoPlayer extends Activity implements View.OnKeyListener {
    private static final String TAG = VideoPlayer.class.getSimpleName();

    public static final String KEY_VIDEO_LIST = "key_video_list";
    public static final String KEY_VIDEO_INDEX = "key_video_index";

    private List<String> mVideoList = new ArrayList<>();
    private int mSize;
    private int mIndex;

    private SurfaceVideoView videoView;
    private View playPanel;
    private SeekBar seekBar;
    private ImageView playPause;

    private static final int AUTO_CLOSE_PLAY_PANEL_TIME = 5 * 1000;// milliseconds
    private static final int SEEK_STEP_LENGTH = 5 * 1000;// milliseconds
    private int mDuration;

    private static final int PLAY_MODE_SINGLE_LOOP = 0;
    private static final int PLAY_MODE_SEQUENTIAL_PLAY = 1;
    private static final int PLAY_MODE_RANDOM_PLAY = 2;
    private static final String[] PLAY_MODE_NAMES = {"单曲循环", "顺序播放", "随机播放"};
    private int mPlayModeIndex = PLAY_MODE_SEQUENTIAL_PLAY;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        List<String> list = intent.getStringArrayListExtra(KEY_VIDEO_LIST);
        if (list == null || list.isEmpty()) {
            finish();
            return;
        }

        mVideoList.addAll(list);
        mSize = mVideoList.size();
        mIndex = intent.getIntExtra(KEY_VIDEO_INDEX, 0);
        if (mIndex < 0 || mIndex >= mSize) {
            mIndex = 0;
        }

        setContentView(R.layout.activity_video_player);

        playPanel = findViewById(R.id.ll_play_panel);
        playPanel.setOnKeyListener(this);

        playPause = findViewById(R.id.iv_play_pause);
        playPause.setOnKeyListener(this);

        final TextView currentTime = findViewById(R.id.tv_current_time);
        final TextView totalTime = findViewById(R.id.tv_total_time);

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnKeyListener(this);

        videoView = findViewById(R.id.video_view);
        videoView.setOnKeyListener(this);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                showPlayPanel();
                mDuration = videoView.getDuration();
                totalTime.setText(getTimeFormat(mDuration));
                seekBar.setMax(mDuration);
                playPause.setImageResource(R.drawable.pause);
                videoView.start();
                startTimeUpdate();
                startTimeClosePlayPanel();
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "videoView...onCompletion");
                playPause.setImageResource(R.drawable.play);
                switch (mPlayModeIndex) {
                    case PLAY_MODE_SINGLE_LOOP:
                        showVideo();
                        break;
                    case PLAY_MODE_SEQUENTIAL_PLAY:
                        nextIndex();
                        showVideo();
                        break;
                    case PLAY_MODE_RANDOM_PLAY:
                        randomIndex();
                        showVideo();
                        break;
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoView.seekTo(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentTime.setText(getTimeFormat(progress));
            }
        });

        showVideo();
        mPlayModeIndex = getPlayModeFromSP(this);
    }

    private void setPlayPause() {
        if (videoView.isPlaying()) {
            Log.d(TAG, "setPlayPause...is Playing");
            videoView.pause();
            playPause.setImageResource(R.drawable.play);
        } else {
            Log.d(TAG, "setPlayPause...not Playing");
            videoView.start();
            playPause.setImageResource(R.drawable.pause);
        }
    }

    private String getTimeFormat(int time) {
        String timeFormat = "";
        int flag = time / 60000;
        if (flag < 10) {
            timeFormat = "0" + time / 60000;
        } else {
            timeFormat = "" + time / 60000;
        }
        flag = time % 60000 / 1000;
        if (flag < 10) {
            timeFormat += ":0" + flag;
        } else {
            timeFormat += ":" + flag;
        }
        return timeFormat;
    }

    private void startTimeUpdate() {
        mHandler.removeCallbacks(mUpdateTimeRunnable);
        mHandler.post(mUpdateTimeRunnable);
    }

    private void stopTimeUpdate() {
        mHandler.removeCallbacks(mUpdateTimeRunnable);
    }

    private Runnable mUpdateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (seekBar != null) {
                seekBar.setProgress(videoView.getCurrentPosition());
            }
            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, 1000);
        }
    };

    private void startTimeClosePlayPanel() {
        Log.d(TAG, "startTimeClosePlayPanel...");
        mHandler.removeCallbacks(mAutoClosePlayPanelRunnable);
        mHandler.postDelayed(mAutoClosePlayPanelRunnable, AUTO_CLOSE_PLAY_PANEL_TIME);
    }

    private void stopTimeClosePlayPanel() {
        Log.d(TAG, "stopTimeClosePlayPanel...");
        mHandler.removeCallbacks(mAutoClosePlayPanelRunnable);
    }

    private Runnable mAutoClosePlayPanelRunnable = new Runnable() {
        @Override
        public void run() {
            hidePlayPanel();
            mHandler.removeCallbacks(this);
        }
    };

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "onKey " + keyCode + "," + event.getAction() + "; view is " + v);
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + keyCode + "," + event.getAction());
        stopTimeClosePlayPanel();
        showPlayPanel();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp " + keyCode + "," + event.getAction());
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                lastIndex();
                showVideo();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                nextIndex();
                showVideo();
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                seekBack();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                seekForward();
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                setPlayPause();
                break;
            case KeyEvent.KEYCODE_MENU:
                showOptionDialog(VideoPlayer.this);
                break;
        }

        if (videoView.isPlaying()) {
            // don't close if paused
            startTimeClosePlayPanel();
        }

        return super.onKeyUp(keyCode, event);
    }

    private void lastIndex() {
        mIndex--;
        if (mIndex < 0) {
            mIndex = mSize - 1;
        }
    }

    private void nextIndex() {
        mIndex++;
        if (mIndex > mSize - 1) {
            mIndex = 0;
        }
    }

    private void randomIndex() {
        mIndex = new Random().nextInt(mSize);
    }

    private void showVideo() {
        String videoPath = mVideoList.get(mIndex);
        Log.d(TAG, "showVideo " + mIndex + ": " + videoPath);
        videoView.openVideo(videoPath);
    }

    private void showPlayPanel() {
        playPanel.setVisibility(View.VISIBLE);
    }

    private void hidePlayPanel() {
        playPanel.setVisibility(View.GONE);
    }

    private void seekForward() {
        int target = videoView.getCurrentPosition() + SEEK_STEP_LENGTH;
        if (target > mDuration) {
            target = mDuration;
        }
        videoView.seekTo(target);
    }

    private void seekBack() {
        int target = videoView.getCurrentPosition() - SEEK_STEP_LENGTH;
        if (target < 0) {
            target = 0;
        }
        videoView.seekTo(target);
    }

    private void showOptionDialog(final Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("播放模式")
                .setSingleChoiceItems(PLAY_MODE_NAMES, mPlayModeIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "OptionDialog onClick which=" + which + ":" + PLAY_MODE_NAMES[which]);
                        mPlayModeIndex = which;
                        savePlayModeToSP(context);
                    }
                })
                .setCancelable(true)
                .create();
        dialog.show();
    }

    private int getPlayModeFromSP(Context context) {
        SharedPreferences sp = Preferences.getSP(context);
        return sp.getInt(Preferences.KEY_VIDEO_PLAY_MODE, PLAY_MODE_SEQUENTIAL_PLAY);
    }

    private void savePlayModeToSP(Context context) {
        SharedPreferences sp = Preferences.getSP(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(Preferences.KEY_VIDEO_PLAY_MODE, mPlayModeIndex);
        editor.apply();
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
        stopTimeUpdate();
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

}
