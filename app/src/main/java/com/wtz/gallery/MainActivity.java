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
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import com.wtz.gallery.adapter.GridAdapter;
import com.wtz.gallery.utils.FileChooser;
import com.wtz.gallery.utils.ScreenUtils;
import com.wtz.gallery.utils.UsbHelper;
import com.wtz.gallery.view.ScaleGridView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener, View.OnKeyListener {
    private static final String TAG = "MainActivity";

    private Button mStartPlayButton;
    private Button mSelectFileButton;
    private Button mUsbDefaultButton;

    private static final int GRIDVIEW_COLUMNS = 4;
    private static final int GRIDVIEW_VERTICAL_SPACE_DIP = 4;
    private static final int GRIDVIEW_HORIZONTAL_SPACE_DIP = 4;
    private ScaleGridView mGridView;
    private GridAdapter mGridAdapter;
    private ArrayList<String> mImageList = new ArrayList<>();

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

    private boolean isFirstShow = true;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        mUsbHelper = new UsbHelper();
        mUsbHelper.initUsb(this);

        mSp = this.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mLastImagePath = mSp.getString(SP_KEY_LAST_IMAGE_PATH, "");
        parseImageDir(mLastImagePath);

        initButtons();
        initGridView();
    }

    private void initButtons() {
        mStartPlayButton = findViewById(R.id.btn_start_play);
        mStartPlayButton.setOnClickListener(this);
        mStartPlayButton.setOnKeyListener(this);

        mSelectFileButton = findViewById(R.id.btn_select_file);
        mSelectFileButton.setOnClickListener(this);
        mSelectFileButton.setOnKeyListener(this);

        mUsbDefaultButton = findViewById(R.id.btn_usb_default_img);
        mUsbDefaultButton.setOnClickListener(this);
        mUsbDefaultButton.setOnKeyListener(this);
    }

    private void initGridView() {
        mGridView = findViewById(R.id.gridView);
        mGridView.setNumColumns(GRIDVIEW_COLUMNS);

        int[] wh = ScreenUtils.getScreenPixels(this);
        int columnWidth = wh[0] / GRIDVIEW_COLUMNS;
        mGridView.setColumnWidth(columnWidth);

        int verticalSapce = ScreenUtils.dip2px(this, GRIDVIEW_VERTICAL_SPACE_DIP);
        mGridView.setVerticalSpacing(verticalSapce);

        int horizontalSapce = ScreenUtils.dip2px(this, GRIDVIEW_HORIZONTAL_SPACE_DIP);
        mGridAdapter = new GridAdapter(this, mImageList, columnWidth - horizontalSapce * 2);
        mGridView.setAdapter(mGridAdapter);
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
                Log.d(TAG, "onItemSelected position=" + position);
                // 调用此自定义view里的方法，解决Item放大后，id靠前的Item放大后会被后面的遮盖问题
                mGridView.onItemSelected(parent, view, position, id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected");
            }
        });
        mGridView.setOnKeyListener(this);
    }

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
                        mImageList.add(fileUri(path));
                    }
                }
            }
        }
    }

    private String fileUri(String filePath) {
        return "file://" + filePath;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        if (isFirstShow) {
            isFirstShow = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mStartPlayButton.requestFocus();
                }
            }, 100);
        }
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
        mUsbHelper.unregisterUsbReceiver(this);
        EventBus.getDefault().unregister(this);
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
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "onKey " + keyCode + "," + event.getAction());
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
                mSelectRequestCode = FileChooser.chooseImage(this);
                break;
            case R.id.btn_usb_default_img:
                Log.d(TAG, "onClick btn_usb_default_img");
                loadImageFromDefaultUsb();
                break;
        }
    }

    private void playImage(int index) {
        if (mImageList.isEmpty()) {
            Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, ImagePlayer.class);
        i.putStringArrayListExtra(ImagePlayer.KEY_IMAGE_LIST, mImageList);
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

    private void loadImageFromDefaultUsb() {
        String usb = mUsbHelper.getUsbPath();
        if (TextUtils.isEmpty(usb)) {
            Toast.makeText(this, "未找到U盘", Toast.LENGTH_SHORT).show();
            return;
        }
        File usbImgDir = new File(usb, DEFAULT_USB_IMG_DIR_NAME);
        if (!usbImgDir.exists() || !usbImgDir.isDirectory()) {
            Toast.makeText(this, "U盘图片目录my_images不存在", Toast.LENGTH_SHORT).show();
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
        mGridAdapter.updateData(mImageList);
    }

    private void saveImagePath(String path) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(SP_KEY_LAST_IMAGE_PATH, path);
        editor.apply();
    }

}