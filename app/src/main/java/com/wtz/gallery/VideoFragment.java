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

import com.wtz.gallery.adapter.VideoGridAdapter;
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


public class VideoFragment extends BaseFragment implements View.OnClickListener, View.OnKeyListener {
    private static final String TAG = "VideoFragment";

    private Button mStartPlayButton;
    private Button mSelectFileButton;
    private Button mSDCardDefaultButton;
    private Button mUsbDefaultButton;

    private static final int GRIDVIEW_COLUMNS = 4;
    private static final int GRIDVIEW_VERTICAL_SPACE_DIP = 12;
    private static final int GRIDVIEW_HORIZONTAL_SPACE_DIP = 12;
    private ScaleGridView mGridView;
    private VideoGridAdapter mGridAdapter;
    private ArrayList<String> mVideoList = new ArrayList<>();

    private UsbHelper mUsbHelper;
    private static final String DEFAULT_USB_VIDEO_DIR_NAME = "my_videos";

    private SharedPreferences mSp;
    private static final String SP_NAME = "config";
    private static final String SP_KEY_LAST_VIDEO_PATH = "sp_key_last_video_path";
    private String mLastVideoPath;

    private static final Map<String, String> VIDEO_SUFFIX = new HashMap<>();

    static {
        VIDEO_SUFFIX.put(".mp4", ".mp4");
        VIDEO_SUFFIX.put(".avi", ".avi");
        VIDEO_SUFFIX.put(".rmvb", ".rmvb");
        VIDEO_SUFFIX.put(".wma", ".wma");
        VIDEO_SUFFIX.put(".mkv", ".mkv");
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean isFirstSelect = true;

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

        View view = inflater.inflate(R.layout.fragment_video, container, false);
        EventBus.getDefault().register(this);

        mUsbHelper = new UsbHelper();
        mUsbHelper.initUsb(getActivity());

        mSp = getActivity().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mLastVideoPath = mSp.getString(SP_KEY_LAST_VIDEO_PATH, "");
        new Thread(new Runnable() {
            @Override
            public void run() {
                parseVideoDir(mLastVideoPath);
            }
        }).start();

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

        mSDCardDefaultButton = root.findViewById(R.id.btn_sdcard_default_video);
        mSDCardDefaultButton.setOnClickListener(this);
        mSDCardDefaultButton.setOnKeyListener(this);

        mUsbDefaultButton = root.findViewById(R.id.btn_usb_default_video);
        mUsbDefaultButton.setOnClickListener(this);
        mUsbDefaultButton.setOnKeyListener(this);
    }

    private void initGridView(View root) {
        mGridView = root.findViewById(R.id.gridView_video);
        mGridView.setFocusable(false);
        mGridView.setFocusableInTouchMode(false);

        mGridView.setNumColumns(GRIDVIEW_COLUMNS);

        int[] wh = ScreenUtils.getScreenPixels(getActivity());
        int columnWidth = wh[0] / GRIDVIEW_COLUMNS;
        mGridView.setColumnWidth(columnWidth);

        int verticalSapce = ScreenUtils.dip2px(getActivity(), GRIDVIEW_VERTICAL_SPACE_DIP);
        mGridView.setVerticalSpacing(verticalSapce);

        int horizontalSapce = ScreenUtils.dip2px(getActivity(), GRIDVIEW_HORIZONTAL_SPACE_DIP);
        mGridAdapter = new VideoGridAdapter(getActivity(), mVideoList,
                columnWidth - horizontalSapce * 2, mHandler);
        mGridView.setAdapter(mGridAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mGridView onItemClick position=" + position);
                playVideo(position);
            }
        });
        mGridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mGridView onItemSelected position=" + position + ", isFirstSelect=" + isFirstSelect);
                if (isFirstSelect) {
                    isFirstSelect = false;
                    return;
                }
                mGridView.smoothScrollToPositionFromTop(position, 200);
                // 调用此自定义view里的方法，解决Item放大后，id靠前的Item放大后会被后面的遮盖问题
                mGridAdapter.selectView(view);
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
//                    mGridView.setSelection(0);
//                    mGridView.selectView(mGridView.getChildAt(mGridView.getSelectedItemPosition()));
                    mGridView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mGridView.hasFocus()) {
                                mGridAdapter.selectView(mGridView.getChildAt(mGridView.getSelectedItemPosition()));
//                                mGridView.selectView(mGridView.getSelectedView());
                            }
                        }
                    }, 200);
                } else {
                    mGridAdapter.selectView(null);
                }
            }
        });
        mGridView.setOnKeyListener(this);
    }

    private void parseVideoDir(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            Log.d(TAG, "Video path is null");
            return;
        }
        File dir = new File(videoPath);
        if (!dir.exists() || !dir.isDirectory()) {
            Log.d(TAG, "Video dir not exist");
            return;
        }

        File[] files = dir.listFiles();
        int index;
        String suffix;
        String path;
        for (File file : files) {
            if (file.isDirectory()) {
                parseVideoDir(file.getAbsolutePath());
            } else {
                path = file.getAbsolutePath();
                index = path.lastIndexOf(".");
                if (index > 0 && index < path.length() - 1) {
                    suffix = path.substring(index);
                    if (VIDEO_SUFFIX.containsKey(suffix.toLowerCase())) {
//                        String fileUri = fileUri(path);
//                        mVideoList.add(fileUri);
                        // video 加载不可以添加 "file://"
                        mVideoList.add(path);
                    }
                }
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            mGridView.setFocusable(true);
            mGridView.setFocusableInTouchMode(true);
        }
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
                            v.getId() == R.id.btn_usb_default_video || v.getId() == R.id.btn_sdcard_default_video) {
                        selectTab();
                        mGridView.scrollTo(0, 0);
                        return true;
                    }
                    if (v.getId() == R.id.gridView_video) {
                        int gridIndex = mGridView.getSelectedItemPosition();
                        Log.d(TAG, "onKey gridIndex=" + gridIndex);
                        if (gridIndex >= 0 && gridIndex <= 3) {
                            mStartPlayButton.requestFocus();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (v.getId() == R.id.btn_start_play || v.getId() == R.id.btn_select_file ||
                            v.getId() == R.id.btn_usb_default_video || v.getId() == R.id.btn_sdcard_default_video) {
                        mGridView.setSelection(0);
                        mGridView.requestFocus();
                        return true;
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
                playVideo(0);
                break;
            case R.id.btn_select_file:
                Log.d(TAG, "onClick btn_select_file");
                mSelectRequestCode = FileChooser.chooseVideo(getActivity());
                break;
            case R.id.btn_sdcard_default_video:
                Log.d(TAG, "onClick btn_sdcard_default_video");
                loadVideoFromDefaultSDCard();
                break;
            case R.id.btn_usb_default_video:
                Log.d(TAG, "onClick btn_usb_default_video");
                loadVideoFromDefaultUsb();
                break;
        }
    }

    private void playVideo(int index) {
        if (mVideoList.isEmpty()) {
            Toast.makeText(getActivity(), "请选择视频", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(getActivity(), VideoPlayer.class);
        i.putStringArrayListExtra(VideoPlayer.KEY_VIDEO_LIST, mVideoList);
        i.putExtra(VideoPlayer.KEY_VIDEO_INDEX, index);
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
                mVideoList.clear();
                mLastVideoPath = file.getParent();
                updateGridview();
            }
        }
    }

    private void loadVideoFromDefaultSDCard() {
        File dir = new File("/sdcard/", DEFAULT_USB_VIDEO_DIR_NAME);
        if (!dir.exists() || !dir.isDirectory()) {
            Toast.makeText(getActivity(), "SD卡视频目录my_videos不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        mVideoList.clear();
        mLastVideoPath = dir.getAbsolutePath();
        updateGridview();
    }

    private void loadVideoFromDefaultUsb() {
        String usb = mUsbHelper.getUsbPath();
        if (TextUtils.isEmpty(usb)) {
            Toast.makeText(getActivity(), "未找到U盘", Toast.LENGTH_SHORT).show();
            return;
        }
        File usbDir = new File(usb, DEFAULT_USB_VIDEO_DIR_NAME);
        if (!usbDir.exists() || !usbDir.isDirectory()) {
            Toast.makeText(getActivity(), "U盘视频目录my_videos不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        mVideoList.clear();
        mLastVideoPath = usbDir.getAbsolutePath();
        updateGridview();
    }

    private void updateGridview() {
        Log.d(TAG, "updateGridview path: " + mLastVideoPath);
        saveVideoPath(mLastVideoPath);
        parseVideoDir(mLastVideoPath);
        mGridAdapter.updateData(mVideoList);
    }

    private void saveVideoPath(String path) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(SP_KEY_LAST_VIDEO_PATH, path);
        editor.apply();
    }

    @Override
    protected void selectTab() {
        selectTabIndex(1);
    }
}
