package com.wtz.gallery;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    private static final String SP_NAME = "user_settings";

    public static final String KEY_VIDEO_PLAY_MODE = "video_play_mode";

    public static SharedPreferences getSP(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

}
