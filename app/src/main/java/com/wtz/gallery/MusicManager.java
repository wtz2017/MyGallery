package com.wtz.gallery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

public class MusicManager {
    private final static String TAG = MusicManager.class.getSimpleName();

    private Context mContext;
    private boolean isInited;

    private MusicService mService;
    private boolean mBound = false;

    private String mCurrentPath;
    private boolean isLooping;

    private volatile static MusicManager INSTANCE;

    public static MusicManager getInstance() {
        if (INSTANCE == null) {
            synchronized (MusicManager.class) {
                if (INSTANCE == null)
                    INSTANCE = new MusicManager();
            }
        }
        return INSTANCE;
    }

    private MusicManager() {
    }

    public void init(Context context) {
        Log.d(TAG, "init isInited=" + isInited);
        if (isInited) {
            return;
        }

        mContext = context.getApplicationContext();
        Intent intent = new Intent(mContext, MusicService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        isInited = true;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setLooping(isLooping);
            openAudio();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void openAudioPath(String path) {
        Log.d(TAG, "openAudioPath: " + path);
        if (!isInited) throw new IllegalStateException("you need first invoke init()");

        stop();
        mCurrentPath = path;
        openAudio();
    }

    private void openAudio() {
        if (isServiceOK() && mCurrentPath != null) {
            mService.openMusic(mCurrentPath, mOnPreparedListener, mOnCompletionListener, mOnErrorListener);
        }
    }

    public void setLooping(boolean looping) {
        isLooping = looping;
        if (isServiceOK()) {
            mService.setLooping(looping);
        }
    }

    public void destroy() {
        Log.d(TAG, "destroy");
        stop();
        if (mBound) {
            mContext.unbindService(mConnection);
            mBound = false;
        }
        mService = null;
    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.d(TAG, "onPrepared");
            play();
        }
    };

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion");
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "onError: " + what + "," + extra);
            return false;
        }
    };

    public boolean isPlaying() {
        if (isServiceOK()) {
            return mService.isPlaying();
        }
        return false;
    }

    public int getDuration() {
        if (isServiceOK()) {
            return mService.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (isServiceOK()) {
            return mService.getCurrentPosition();
        }
        return 0;
    }

    public void play() {
        if (isServiceOK()) {
            mService.start();
        }
    }

    public void pause() {
        if (isServiceOK()) {
            mService.pause();
        }
    }

    public void seekTo(int msec) {
        if (isServiceOK()) {
            mService.seekTo(msec);
        }
    }

    public void stop() {
        if (isServiceOK()) {
            mService.stop();
        }
    }

    private boolean isServiceOK() {
        return mService != null && mBound;
    }

}
