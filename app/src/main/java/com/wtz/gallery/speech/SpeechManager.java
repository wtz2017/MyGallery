package com.wtz.gallery.speech;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeechManager {

    private final static String TAG = SpeechManager.class.getSimpleName();

    private Context mContext;
    private boolean isIniting;
    private boolean isInitSuccess;

    private HandlerThread hThread;
    private Handler tHandler;
    private static final int INIT = 1;
    private static final int RELEASE = 2;

    protected String appId = "16682981";
    protected String appKey = "Kiwh3oGKZ4paqHRkG5pvY8tW";
    protected String secretKey = "d4wPEmPAsx4PzB9wjGGxGKRS6f1yS9He";

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    protected TtsMode ttsMode = TtsMode.MIX;

    // 离线发音选择，VOICE_FEMALE即为离线女声发音。
    // assets目录下bd_etts_common_speech_m15_mand_eng_high_am-mix_v3.0.0_20170505.dat为离线男声模型；
    // assets目录下bd_etts_common_speech_f7_mand_eng_high_am-mix_v3.0.0_20170512.dat为离线女声模型
    protected String offlineVoice = OfflineResource.VOICE_MALE;

    // 主控制类，所有合成控制方法从这个类开始
    protected MySyntherizer mSynthesizer;

    private volatile static SpeechManager INSTANCE;

    public static SpeechManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SpeechManager.class) {
                if (INSTANCE == null)
                    INSTANCE = new SpeechManager();
            }
        }
        return INSTANCE;
    }

    private SpeechManager() {
    }

    /**
     * @param context
     * @param handler 用以消息回调，具体类型见 MessageListener
     */
    public void init(Context context, Handler handler) {
        if (isInitSuccess || isIniting) {
            return;
        }
        isIniting = true;

        mContext = context.getApplicationContext();
        mSynthesizer = new MySyntherizer(mContext);

        initThread();
        initTTs(handler);
    }

    private void initThread() {
        hThread = new HandlerThread("NonBlockSyntherizer-thread");
        hThread.start();
        tHandler = new Handler(hThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case INIT:
                        InitConfig config = (InitConfig) msg.obj;
                        boolean isSuccess = mSynthesizer.init(config);
                        Log.d(TAG, "init isSuccess: " + isSuccess);
                        isIniting = false;
                        if (isSuccess) {
                            isInitSuccess = true;
                        } else {
                            isInitSuccess = false;
                        }
                        break;
                    case RELEASE:
                        Log.d(TAG, "release...");
                        mSynthesizer.release();
                        if (Build.VERSION.SDK_INT < 18) {
                            getLooper().quit();
                        }
                        break;
                    default:
                        break;
                }

            }
        };
    }

    private void initTTs(Handler handler) {
        // 设置初始化参数
        // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类
        SpeechSynthesizerListener listener = new MessageListener(handler);
        Map<String, String> params = getParams();
        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        InitConfig initConfig = new InitConfig(appId, appKey, secretKey, ttsMode, params, listener);

        runInHandlerThread(INIT, initConfig);
    }

    private void runInHandlerThread(int action) {
        runInHandlerThread(action, null);
    }

    private void runInHandlerThread(int action, Object obj) {
        Message msg = Message.obtain();
        msg.what = action;
        msg.obj = obj;
        tHandler.sendMessage(msg);
    }

    public void destroy() {
        runInHandlerThread(RELEASE);
        if (Build.VERSION.SDK_INT >= 18) {
            hThread.quitSafely();
        }
        isInitSuccess = false;
        isIniting = false;
    }

    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return
     */
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        // 以下参数均为选填
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        params.put(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_PITCH, "5");

        params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        // 离线资源文件， 从assets目录中复制到临时目录，需要在initTTs方法前完成
        OfflineResource offlineResource = createOfflineResource(offlineVoice);
        // 声学模型文件路径 (离线引擎使用), 请确认下面两个文件存在
        params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, offlineResource.getTextFilename());
        params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, offlineResource.getModelFilename());
        return params;
    }

    private OfflineResource createOfflineResource(String voiceType) {
        OfflineResource offlineResource = null;
        try {
            offlineResource = new OfflineResource(mContext, voiceType);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "[error]:copy files from assets failed." + e.getMessage());
        }
        return offlineResource;
    }

    /**
     * speak 实际上是调用 synthesize后，获取音频流，然后播放。
     * 获取音频流的方式见SaveFileActivity及FileSaveListener
     * 需要合成的文本text的长度不能超过1024个GBK字节。
     */
    public void speak(String text) {
        if (!isInitSuccess) {
            return;
        }
        // 合成前可以修改参数：
        // Map<String, String> params = getParams();
        // mSynthesizer.setParams(params);
        int result = mSynthesizer.speak(text);
        checkResult(result, "speak");
    }


    /**
     * 合成但是不播放，
     * 音频流保存为文件的方法可以参见SaveFileActivity及FileSaveListener
     */
    public void synthesize(String text) {
        if (!isInitSuccess) {
            return;
        }
        int result = mSynthesizer.synthesize(text);
        checkResult(result, "synthesize");
    }

    /**
     * 批量播放
     */
    public void batchSpeak() {
        if (!isInitSuccess) {
            return;
        }
        List<Pair<String, String>> texts = new ArrayList<Pair<String, String>>();
        texts.add(new Pair<String, String>("开始批量播放，", "a0"));
        texts.add(new Pair<String, String>("123456，", "a1"));
        texts.add(new Pair<String, String>("欢迎使用百度语音，，，", "a2"));
        texts.add(new Pair<String, String>("重(chong2)量这个是多音字示例", "a3"));
        int result = mSynthesizer.batchSpeak(texts);
        checkResult(result, "batchSpeak");
    }


    /**
     * 切换离线发音。注意需要添加额外的判断：引擎在合成时该方法不能调用
     */
    public void loadModel(String mode) {
        if (!isInitSuccess) {
            return;
        }
        offlineVoice = mode;
        OfflineResource offlineResource = createOfflineResource(offlineVoice);
        Log.d(TAG, "切换离线语音：" + offlineResource.getModelFilename());
        int result = mSynthesizer.loadModel(offlineResource.getModelFilename(), offlineResource.getTextFilename());
        checkResult(result, "loadModel");
    }

    /**
     * 暂停播放。仅调用speak后生效
     */
    public void pause() {
        if (!isInitSuccess) {
            return;
        }
        int result = mSynthesizer.pause();
        checkResult(result, "pause");
    }

    /**
     * 继续播放。仅调用speak后生效，调用pause生效
     */
    public void resume() {
        if (!isInitSuccess) {
            return;
        }
        int result = mSynthesizer.resume();
        checkResult(result, "resume");
    }

    /*
     * 停止合成引擎。即停止播放，合成，清空内部合成队列。
     */
    public void stop() {
        if (!isInitSuccess) {
            return;
        }
        int result = mSynthesizer.stop();
        checkResult(result, "stop");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            Log.e(TAG, "error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

}
