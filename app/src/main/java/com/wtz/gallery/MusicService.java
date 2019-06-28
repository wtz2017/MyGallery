package com.wtz.gallery;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = MusicService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_PREPARING = 2;
    private static final int STATE_PREPARED = 3;
    private static final int STATE_PLAYING = 4;
    private static final int STATE_PAUSED = 5;
    private static final int STATE_STOPED = 6;
    private static final int STATE_PLAYBACK_COMPLETED = 7;
    private static final int STATE_RELEASED = 8;

    private int mCurrentState = STATE_ERROR;

    private MediaPlayer mMediaPlayer = null;
    private boolean isLooping;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;

    public MusicService() {
    }

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        Notification notification = new Notification();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public void openMusic(String path, MediaPlayer.OnPreparedListener onPreparedListener,
                          MediaPlayer.OnCompletionListener onCompletionListener,
                          MediaPlayer.OnErrorListener onErrorListener) {
        Log.d(TAG, "openMusic...path = " + path);
        mOnPreparedListener = onPreparedListener;
        mOnCompletionListener = onCompletionListener;
        mOnErrorListener = onErrorListener;

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mCurrentState = STATE_RELEASED;
        }
        try {
            mMediaPlayer = new MediaPlayer();
            mCurrentState = STATE_IDLE;

            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setLooping(isLooping);

            /*
             * Must call this method before prepare() or prepareAsync() in order
			 * for the target stream type to become effective thereafter.
			 */
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            if (path.startsWith("content://")) {
                mMediaPlayer.setDataSource(MusicService.this, Uri.parse(path));
            } else {
                mMediaPlayer.setDataSource(path);
            }

            mCurrentState = STATE_INITIALIZED;

			/*
             * After setting the datasource and the display surface, you need to
			 * either call prepare() or prepareAsync(). For streams, you should
			 * call prepareAsync(), which returns immediately, rather than
			 * blocking until enough data has been buffered.
			 */
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;

        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + path, ex);
            this.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + path, ex);
            this.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        mCurrentState = STATE_PREPARED;
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(null);
        }
    }

    public void start() {
        Log.d(TAG, "start...");
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PAUSED
                    || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.start();
                mCurrentState = STATE_PLAYING;
                Log.d(TAG, "started");
            }
        }
    }

    public void pause() {
        Log.d(TAG, "pause...");
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PLAYING) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                Log.d(TAG, "paused");
            }
        }
    }

    public void seekTo(int msec) {
        Log.d(TAG, "seekTo...msec = " + msec);
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.seekTo(msec);
                Log.d(TAG, "seeked");
            }
        }
    }

    public void setLooping(boolean looping) {
        isLooping = looping;
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(looping);
        }
    }

    public void stop() {
        Log.d(TAG, "stop...");
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.stop();
                mCurrentState = STATE_STOPED;
                Log.d(TAG, "stoped");
            }
        }
    }

    public void release() {
        Log.d(TAG, "release...");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mCurrentState = STATE_RELEASED;
            mMediaPlayer = null;
            Log.d(TAG, "released");
        }
    }

    public int getDuration() {
        int duration = 0;
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                duration = mMediaPlayer.getDuration();
            }
        }
        return duration;
    }

    public int getCurrentPosition() {
        int currentPosition = 0;
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                currentPosition = mMediaPlayer.getCurrentPosition();
            }
        }
        return currentPosition;
    }

    public boolean isPlaying() {
        boolean isPlaying = false;
        try {
            isPlaying = mMediaPlayer.isPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isPlaying;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(null);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mCurrentState = STATE_ERROR;
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                /*
                 * Creating a new MediaPlayer and settings its wakemode does not
                 * require the media service, so it's OK to do this now, while
                 * the service is still being restarted
                 */
                mMediaPlayer = new MediaPlayer();// TODO: 2017/11/4
                mMediaPlayer.setWakeMode(MusicService.this, PowerManager.PARTIAL_WAKE_LOCK);
                Log.e(TAG, "Error: MEDIA_ERROR_SERVER_DIED what=" + what + ", extra="
                        + extra + "; release MediaPlayer and reset MediaPlayer");
                return true;
            default:
                Log.e(TAG, "Error: Song format is not correct! default what=" + what
                        + ",extra=" + extra + "; release MediaPlayer and reset MediaPlayer");
                break;
        }
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(null, what, extra);
        }
        return false;
    }
}
