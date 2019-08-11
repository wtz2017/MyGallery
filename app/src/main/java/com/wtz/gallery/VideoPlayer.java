package com.wtz.gallery;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wtz.gallery.view.SurfaceVideoView;

import java.util.ArrayList;
import java.util.List;


public class VideoPlayer extends Activity {
    private static final String TAG = VideoPlayer.class.getSimpleName();

    public static final String KEY_VIDEO_LIST = "key_video_list";
    public static final String KEY_VIDEO_INDEX = "key_video_index";

    private List<String> mVideoList = new ArrayList<>();
    private int mSize;
    private int mIndex;

    private SurfaceVideoView videoView;
    private SeekBar seekBar;
    private ImageButton playPause;

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

        playPause = findViewById(R.id.ib_play_pause);
        final TextView currentTime = findViewById(R.id.tv_current_time);
        final TextView totalTime = findViewById(R.id.tv_total_time);
        seekBar = findViewById(R.id.seek_bar);
        videoView = findViewById(R.id.video_view);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                int duration = videoView.getDuration();
                totalTime.setText(getTimeFormat(duration));
                seekBar.setMax(duration);
                playPause.setImageResource(R.drawable.pause);
                startTimeUpdate();
                videoView.start();
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playPause.setImageResource(R.drawable.play);
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playPause.setImageResource(R.drawable.play);
                } else {
                    videoView.start();
                    playPause.setImageResource(R.drawable.pause);
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

        playPause.requestFocus();

        showVideo();
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
                showVideo();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                nextIndex();
                showVideo();
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                playPause.requestFocus();
                break;
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

    private void showVideo() {
        String videoPath = mVideoList.get(mIndex);
        Log.d(TAG, "showVideo " + mIndex + ": " + videoPath);
        videoView.openVideo(videoPath);
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
