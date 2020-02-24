package com.wtz.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import com.wtz.gallery.adapter.ImageGridAdapter;
import com.wtz.gallery.utils.FileChooser;
import com.wtz.gallery.utils.ScreenUtils;
import com.wtz.gallery.utils.UsbHelper;
import com.wtz.gallery.view.ScaleGridView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageFragment extends BaseFragment implements View.OnClickListener, View.OnKeyListener {
    private static final String TAG = "ImageFragment";

    private Button mStartPlayButton;
    private Button mSelectFileButton;
    private Button mSDCardDefaultButton;
    private Button mUsbDefaultButton;

    private static final int GRIDVIEW_COLUMNS = 4;
    private static final int GRIDVIEW_VERTICAL_SPACE_DIP = 12;
    private static final int GRIDVIEW_HORIZONTAL_SPACE_DIP = 12;
    private ScaleGridView mGridView;
    private ImageGridAdapter mImageGridAdapter;
    private ArrayList<String> mImageList = new ArrayList<>();
    private Map<String, String> mAudioMap = new HashMap<>();

    private UsbHelper mUsbHelper;
    private static final String DEFAULT_USB_IMG_DIR_NAME = "my_images";

    private SharedPreferences mSp;
    private static final String SP_NAME = "config";
    private static final String SP_KEY_LAST_IMAGE_PATH = "sp_key_last_image_path";
    private String mLastImagePath;

    private static final Map<String, String> IMAGE_SUFFIX = new HashMap<>();

    static {
        IMAGE_SUFFIX.put(".png", ".png");
        IMAGE_SUFFIX.put(".jpg", ".jpg");
        IMAGE_SUFFIX.put(".jpeg", ".jpeg");
        IMAGE_SUFFIX.put(".bmp", ".bmp");
        IMAGE_SUFFIX.put(".gif", ".gif");
    }

    private static final List<String> AUDIO_SUFFIX = new ArrayList<>();

    static {
        AUDIO_SUFFIX.add(".mp3");
        AUDIO_SUFFIX.add(".wma");
        AUDIO_SUFFIX.add(".wav");
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_image, container, false);

        EventBus.getDefault().register(this);

        mUsbHelper = new UsbHelper();
        mUsbHelper.initUsb(getActivity());

        mSp = getActivity().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mLastImagePath = mSp.getString(SP_KEY_LAST_IMAGE_PATH, "");
        parseImageDir(mLastImagePath);

        initButtons(view);
        initGridView(view);

        return view;
    }

    private void initButtons(View root) {
        mStartPlayButton = root.findViewById(R.id.btn_start_play);
        mStartPlayButton.setOnClickListener(this);
        mStartPlayButton.setOnKeyListener(this);

        mSelectFileButton = root.findViewById(R.id.btn_select_file);
        mSelectFileButton.setOnClickListener(this);
        mSelectFileButton.setOnKeyListener(this);

        mSDCardDefaultButton = root.findViewById(R.id.btn_sdcard_default_img);
        mSDCardDefaultButton.setOnClickListener(this);
        mSDCardDefaultButton.setOnKeyListener(this);

        mUsbDefaultButton = root.findViewById(R.id.btn_usb_default_img);
        mUsbDefaultButton.setOnClickListener(this);
        mUsbDefaultButton.setOnKeyListener(this);
    }

    private void initGridView(View root) {
        mGridView = root.findViewById(R.id.gridView_image);
        mGridView.setNumColumns(GRIDVIEW_COLUMNS);

        int[] wh = ScreenUtils.getScreenPixels(getActivity());
        int columnWidth = wh[0] / GRIDVIEW_COLUMNS;
        mGridView.setColumnWidth(columnWidth);

        int verticalSapce = ScreenUtils.dip2px(getActivity(), GRIDVIEW_VERTICAL_SPACE_DIP);
        mGridView.setVerticalSpacing(verticalSapce);

        int horizontalSapce = ScreenUtils.dip2px(getActivity(), GRIDVIEW_HORIZONTAL_SPACE_DIP);
        mImageGridAdapter = new ImageGridAdapter(getActivity(), mImageList, columnWidth - horizontalSapce * 2);
        mGridView.setAdapter(mImageGridAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mGridView onItemClick position=" + position);
                playImage(position);
            }
        });
        mGridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mGridView onItemSelected position=" + position);
                mGridView.smoothScrollToPositionFromTop(position, 200);
                // 调用此自定义view里的方法，解决Item放大后，id靠前的Item放大后会被后面的遮盖问题
                mImageGridAdapter.selectView(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "mGridView onNothingSelected");
            }
        });
        mGridView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "mGridView onFocusChange hasFocus=" + hasFocus);
                if (hasFocus) {
                    delayShowSelectEffect();
                } else {
                    mImageGridAdapter.selectView(null);
                }
            }
        });
        mGridView.setOnKeyListener(this);
    }

    private void delayShowSelectEffect() {
        mHandler.postDelayed(mDelayShowSelectEffectRunnable, 100);
    }

    private Runnable mDelayShowSelectEffectRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mDelayShowSelectEffectRunnable GridView selectedItem="
                    + mGridView.getSelectedItemPosition()
                    + ", mGridView.hasFocus=" + mGridView.hasFocus());
            if (mGridView.hasFocus()) {
                mImageGridAdapter.selectView(mGridView.getSelectedView());
            }
        }
    };

    private void parseImageDir(String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            Log.d(TAG, "Image path is null");
            return;
        }
        File dir = new File(imagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            Log.d(TAG, "Image dir not exist");
            return;
        }

        File[] files = dir.listFiles();
        int index;
        String suffix;
        String path;
        for (File file : files) {
            if (file.isDirectory()) {
                parseImageDir(file.getAbsolutePath());
            } else {
                path = file.getAbsolutePath();
                index = path.lastIndexOf(".");
                if (index > 0 && index < path.length() - 1) {
                    suffix = path.substring(index);
                    if (IMAGE_SUFFIX.containsKey(suffix.toLowerCase())) {
                        String fileUri = fileUri(path);
                        mImageList.add(fileUri);
                        findAudioFile(fileUri, path.substring(0, index));
                    }
                }
            }
        }
    }

    /**
     * 解析同名音频文件
     */
    private void findAudioFile(String key, String nameNoSuffix) {
        File file;
        for (String audioSuffix : AUDIO_SUFFIX) {
            if ((file = new File(nameNoSuffix + audioSuffix)).exists()
                    && file.isFile()) {
                mAudioMap.put(key, file.getAbsolutePath());
            }
        }
    }

    private String fileUri(String filePath) {
        return "file://" + filePath;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        mUsbHelper.unregisterUsbReceiver(getActivity());
        EventBus.getDefault().unregister(this);
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "onKey " + keyCode + "," + event.getAction() + ",v.getId=" + v.getId());
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // 过滤ACTION_DOWN 是为了只处理从谁开始落下按键的情况
                    if (v.getId() == R.id.btn_start_play || v.getId() == R.id.btn_select_file ||
                            v.getId() == R.id.btn_usb_default_img || v.getId() == R.id.btn_sdcard_default_img) {
                        selectTab();
                        mGridView.scrollTo(0, 0);
                        return true;
                    }
                    if (v.getId() == R.id.gridView_image ) {
                        int gridIndex = mGridView.getSelectedItemPosition();
                        Log.d(TAG, "onKey gridIndex=" + gridIndex);
                        if (gridIndex >= 0 && gridIndex <= 3) {
                            mStartPlayButton.requestFocus();
                            return true;
                        }
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_play:
                Log.d(TAG, "onClick btn_start_play");
                playImage(0);
                break;
            case R.id.btn_select_file:
                Log.d(TAG, "onClick btn_select_file");
                mSelectRequestCode = FileChooser.chooseImage(getActivity());
                break;
            case R.id.btn_sdcard_default_img:
                Log.d(TAG, "onClick btn_sdcard_default_img");
                loadImageFromDefaultSDCard();
                break;
            case R.id.btn_usb_default_img:
                Log.d(TAG, "onClick btn_usb_default_img");
                loadImageFromDefaultUsb();
                break;
        }
    }

    private void playImage(int index) {
        if (mImageList.isEmpty()) {
            Toast.makeText(getActivity(), "请选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(getActivity(), ImagePlayer.class);
        i.putStringArrayListExtra(ImagePlayer.KEY_IMAGE_LIST, mImageList);
        i.putExtra(ImagePlayer.KEY_AUDIO_MAP, (Serializable) mAudioMap);
        i.putExtra(ImagePlayer.KEY_IMAGE_INDEX, index);
        startActivity(i);
    }

    private int mSelectRequestCode;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChooseResult(FileChooser.ChooseResult chooseResult) {
        Log.d(TAG, "onChooseResult requestCode=" + chooseResult.getRequestCode()
                + "; fileUri=" + chooseResult.getFilePath());
        if (chooseResult.getRequestCode() == mSelectRequestCode) {
            String filePath = chooseResult.getFilePath();
            File file;
            if (!TextUtils.isEmpty(filePath) && (file = new File(filePath)).isFile()) {
                mImageList.clear();
                mLastImagePath = file.getParent();
                updateImage();
            }
        }
    }

    private void loadImageFromDefaultSDCard() {
        File dir = new File("/sdcard/", DEFAULT_USB_IMG_DIR_NAME);
        if (!dir.exists() || !dir.isDirectory()) {
            Toast.makeText(getActivity(), "SD卡图片目录my_images不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        mImageList.clear();
        mLastImagePath = dir.getAbsolutePath();
        updateImage();
    }

    private void loadImageFromDefaultUsb() {
        String usb = mUsbHelper.getUsbPath();
        if (TextUtils.isEmpty(usb)) {
            Toast.makeText(getActivity(), "未找到U盘", Toast.LENGTH_SHORT).show();
            return;
        }
        File usbImgDir = new File(usb, DEFAULT_USB_IMG_DIR_NAME);
        if (!usbImgDir.exists() || !usbImgDir.isDirectory()) {
            Toast.makeText(getActivity(), "U盘图片目录my_images不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        mImageList.clear();
        mLastImagePath = usbImgDir.getAbsolutePath();
        updateImage();
    }

    private void updateImage() {
        Log.d(TAG, "updateImage path: " + mLastImagePath);
        saveImagePath(mLastImagePath);
        parseImageDir(mLastImagePath);
        mImageGridAdapter.updateData(mImageList);
    }

    private void saveImagePath(String path) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(SP_KEY_LAST_IMAGE_PATH, path);
        editor.apply();
    }

    @Override
    protected void selectTab() {
        selectTabIndex(0);
    }

}
