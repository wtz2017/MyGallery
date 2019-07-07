package com.wtz.gallery.speech;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizerListener;

/**
 * SpeechSynthesizerListener 简单地实现，仅仅记录日志
 * Created by fujiayi on 2017/5/19.
 */

public class SpeechMessageListener implements SpeechSynthesizerListener {

    private static final String TAG = "SpeechMessageListener";

    private Handler handler;

    public static final int MSG_SYNTHESIZE_START = 1;
    public static final int MSG_SYNTHESIZE_DATA_ARRIVED = 2;
    public static final int MSG_SYNTHESIZE_FINISH = 3;
    public static final int MSG_SPEECH_START = 4;
    public static final int MSG_SPEECH_PROGRESS_CHANGED = 5;
    public static final int MSG_SPEECH_FINISH = 6;
    public static final int MSG_ERROR = 7;

    public static final String BUNDLE_KEY_UTTERANCE_ID = "utterance_id";
    public static final String BUNDLE_KEY_PROGRESS = "progress";
    public static final String BUNDLE_KEY_BYTES = "bytes";
    public static final String BUNDLE_KEY_ERROR_CODE = "error_code";
    public static final String BUNDLE_KEY_ERROR_DESC = "error_desc";

    public SpeechMessageListener(Handler handler) {
        this.handler = handler;
    }

    /**
     * 播放开始，每句播放开始都会回调
     *
     * @param utteranceId
     */
    @Override
    public void onSynthesizeStart(String utteranceId) {
        Log.d(TAG, "onSynthesizeStart: " + utteranceId);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        sendMessage(MSG_SYNTHESIZE_START, bundle);
    }

    /**
     * 语音流 16K采样率 16bits编码 单声道 。
     *
     * @param utteranceId
     * @param bytes       二进制语音 ，注意可能有空data的情况，可以忽略
     * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法和合成到第几个字对应。
     */
    @Override
    public void onSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress) {
//        Log.d(TAG, "onSynthesizeDataArrived: " + utteranceId + ", progress = " + progress + ", bytes=" + bytes);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        bundle.putInt(BUNDLE_KEY_PROGRESS, progress);
        bundle.putByteArray(BUNDLE_KEY_BYTES, bytes);
        sendMessage(MSG_SYNTHESIZE_DATA_ARRIVED, bundle);
    }

    /**
     * 合成正常结束，每句合成正常结束都会回调，如果过程中出错，则回调onError，不再回调此接口
     *
     * @param utteranceId
     */
    @Override
    public void onSynthesizeFinish(String utteranceId) {
        Log.d(TAG, "onSynthesizeFinish: " + utteranceId);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        sendMessage(MSG_SYNTHESIZE_FINISH, bundle);
    }

    @Override
    public void onSpeechStart(String utteranceId) {
        Log.d(TAG, "onSpeechStart: " + utteranceId);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        sendMessage(MSG_SPEECH_START, bundle);
    }

    /**
     * 播放进度回调接口，分多次回调
     *
     * @param utteranceId
     * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法保证和合成到第几个字对应。
     */
    @Override
    public void onSpeechProgressChanged(String utteranceId, int progress) {
//        Log.d(TAG, "onSpeechProgressChanged: " + utteranceId + ", progress = " + progress);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        bundle.putInt(BUNDLE_KEY_PROGRESS, progress);
        sendMessage(MSG_SPEECH_PROGRESS_CHANGED, bundle);
    }

    /**
     * 播放正常结束，每句播放正常结束都会回调，如果过程中出错，则回调onError,不再回调此接口
     *
     * @param utteranceId
     */
    @Override
    public void onSpeechFinish(String utteranceId) {
        Log.d(TAG, "onSpeechFinish: " + utteranceId);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        sendMessage(MSG_SPEECH_FINISH, bundle);

    }

    /**
     * 当合成或者播放过程中出错时回调此接口
     *
     * @param utteranceId
     * @param speechError 包含错误码和错误信息
     */
    @Override
    public void onError(String utteranceId, SpeechError speechError) {
        Log.d(TAG, "onError utteranceId: " + utteranceId + ", error: " + speechError.code
                + ", " + speechError.description);
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_UTTERANCE_ID, utteranceId);
        bundle.putInt(BUNDLE_KEY_ERROR_CODE, speechError.code);
        bundle.putString(BUNDLE_KEY_ERROR_DESC, speechError.description);
        sendMessage(MSG_ERROR, bundle);
    }

    private void sendMessage(int type, Bundle bundle) {
        Message msg = Message.obtain();
        msg.what = type;
        msg.obj = bundle;
        handler.sendMessage(msg);
    }

}
