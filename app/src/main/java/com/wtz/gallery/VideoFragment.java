package com.wtz.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wtz.gallery.adapter.VideoGridAdapter;
import com.wtz.gallery.data.Item;
import com.wtz.gallery.utils.FileChooser;
import com.wtz.gallery.utils.ScreenUtils;
import com.wtz.gallery.utils.UsbHelper;
import com.wtz.gallery.view.ScaleGridView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class VideoFragment extends BaseFragment implements View.OnClickListener, View.OnKeyListener {
    private static final String TAG = "VideoFragment";

    private TextView mCurrentPathView;
    private Button mSelectFileButton;
    private Button mSDCardDefaultButton;
    private Button mUsbDefaultButton;
    private LinearLayout mNoContentLayout;
    private Button mNoContentBackButton;

    private static final int GRIDVIEW_COLUMNS = 4;
    private static final int GRIDVIEW_VERTICAL_SPACE_DIP = 12;
    private static final int GRIDVIEW_HORIZONTAL_SPACE_DIP = 12;
    private ScaleGridView mGridView;
    private VideoGridAdapter mGridAdapter;
    private ArrayList<Item> mTotalList = new ArrayList<>();
    private ArrayList<Item> mVideoList = new ArrayList<>();

    // 进入过的每层文件夹中的位置，供返回时使用
    private ArrayList<Integer> mEnterDirIndexList = new ArrayList<>();

    private UsbHelper mUsbHelper;
    private static final String DEFAULT_USB_VIDEO_DIR_NAME = "my_videos";

    private SharedPreferences mSp;
    private static final String SP_NAME = "config";
    private static final String SP_KEY_LAST_VIDEO_PATH = "sp_key_last_video_path";
    private String mLastVideoPath;
    private String mRootPath;

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
        mRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        mCurrentPathView = view.findViewById(R.id.tv_path);
        mCurrentPathView.setText(mLastVideoPath);
        mNoContentLayout = view.findViewById(R.id.ll_no_content);
        mNoContentLayout.setVisibility(View.GONE);
        mNoContentLayout.setOnKeyListener(this);
        initButtons(view);
        initGridView(view);
        return view;
    }

    private void initButtons(View root) {
        mSelectFileButton = root.findViewById(R.id.btn_select_file);
        mSelectFileButton.setOnClickListener(this);
        mSelectFileButton.setOnKeyListener(this);

        mSDCardDefaultButton = root.findViewById(R.id.btn_sdcard_default_video);
        mSDCardDefaultButton.setOnClickListener(this);
        mSDCardDefaultButton.setOnKeyListener(this);

        mUsbDefaultButton = root.findViewById(R.id.btn_usb_default_video);
        mUsbDefaultButton.setOnClickListener(this);
        mUsbDefaultButton.setOnKeyListener(this);

        mNoContentBackButton = root.findViewById(R.id.btn_no_content_back);
        mNoContentBackButton.setOnClickListener(this);
        mNoContentBackButton.setOnKeyListener(this);
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
        mGridAdapter = new VideoGridAdapter(getActivity(), mTotalList,
                columnWidth - horizontalSapce * 2, mHandler);
        mGridView.setAdapter(mGridAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = (Item) mGridAdapter.getItem(position);
                Log.d(TAG, "mGridView onItemClick position=" + position + ", path=" + item.path);
                if (item.type == Item.TYPE_VIDEO) {
                    play(item);
                } else if (item.type == Item.TYPE_DIR) {
                    mEnterDirIndexList.add(position);
                    mLastVideoPath = item.path;
                    mCurrentPathView.setText(mLastVideoPath);
                    parseAndUpdateGridview(false);
                }
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
                    delayShowSelectEffect();
                } else {
                    mGridAdapter.selectView(null);
                }
            }
        });
        mGridView.setOnKeyListener(this);
        parseAndUpdateGridview(false);
    }

    private void parseVideoDir(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            Log.e(TAG, "Video path is null");
            return;
        }

        File dir = new File(videoPath);
        if (!dir.exists() || !dir.isDirectory()) {
            Log.e(TAG, "Video dir not exist");
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            Log.e(TAG, "target File[] files is null");
            return;
        }

        int index;
        String suffix;
        String path;
        mTotalList.clear();
        mVideoList.clear();
        for (File file : files) {
            path = file.getAbsolutePath();
            if (file.isDirectory()) {
                mTotalList.add(new Item(Item.TYPE_DIR, file.getName(), path));
            } else {
                index = path.lastIndexOf(".");
                if (index > 0 && index < path.length() - 1) {
                    suffix = path.substring(index);
                    if (VIDEO_SUFFIX.containsKey(suffix.toLowerCase())) {
                        Item audio = new Item(Item.TYPE_VIDEO, stripFileName(file), path);
                        mTotalList.add(audio);
                        mVideoList.add(audio);
                    }
                }
            }
        }
        if (mTotalList.size() == 0) {
            return;
        }
        if (mTotalList.size() > 0) {
            Collections.sort(mTotalList);
        }
        if (mVideoList.size() > 0) {
            Collections.sort(mVideoList);
        }
    }

    private String fileUri(String filePath) {
        return "file://" + filePath;
    }

    private String stripFileName(File file) {
        String orign = file.getName();
        int end = orign.lastIndexOf(".");
        if (end == -1 || end == 0) {
            end = orign.length();
        }

        return orign.substring(0, end);
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
            case KeyEvent.KEYCODE_BACK:
                if (mRootPath == null || mRootPath.equals(mLastVideoPath)) {
                    return false;
                }
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    back();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // 过滤ACTION_DOWN 是为了只处理从谁开始落下按键的情况
                    if (v.getId() == R.id.btn_select_file ||
                            v.getId() == R.id.btn_usb_default_video || v.getId() == R.id.btn_sdcard_default_video) {
                        selectSelfTab();
                        mGridView.scrollTo(0, 0);
                        return true;
                    }
                    if (v.getId() == R.id.gridView_video) {
                        int gridIndex = mGridView.getSelectedItemPosition();
                        Log.d(TAG, "onKey gridIndex=" + gridIndex);
                        if (gridIndex >= 0 && gridIndex <= 3) {
                            mSelectFileButton.requestFocus();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (v.getId() == R.id.btn_select_file ||
                            v.getId() == R.id.btn_usb_default_video || v.getId() == R.id.btn_sdcard_default_video) {
                        mGridView.setSelection(0);
                        mGridView.requestFocus();
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (v.getId() == R.id.btn_usb_default_video) {
                        selectRightTab();
                        return true;
                    }
                    if (v.getId() == R.id.gridView_video) {
                        int gridIndex = mGridView.getSelectedItemPosition();
                        Log.d(TAG, "onKey KEYCODE_DPAD_RIGHT gridIndex=" + gridIndex
                                + ", mGridView.getNumColumn=" + mGridView.getNumColumns());
                        if ((gridIndex + 1) % mGridView.getNumColumns() == 0
                                || gridIndex == mGridView.getCount() - 1) {
                            selectRightTab();//TODO
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (v.getId() == R.id.btn_select_file) {
                        selectLeftTab();
                        return true;
                    }
                    if (v.getId() == R.id.gridView_video) {
                        int gridIndex = mGridView.getSelectedItemPosition();
                        Log.d(TAG, "onKey KEYCODE_DPAD_RIGHT gridIndex=" + gridIndex
                                + ", mGridView.getNumColumn=" + mGridView.getNumColumns());
                        if (gridIndex % mGridView.getNumColumns() == 0) {
                            selectLeftTab();//TODO
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
            case R.id.btn_no_content_back:
                Log.d(TAG, "onClick btn_no_content_back");
                back();
                break;
        }
    }

    private void back() {
        if (mRootPath == null || mRootPath.equals(mLastVideoPath)) {
            return;
        }
        int end = mLastVideoPath.lastIndexOf(File.separator);
        if (end == -1 || end == 0) {
            mLastVideoPath = mRootPath;
        } else {
            mLastVideoPath = mLastVideoPath.substring(0, end);
        }
        mCurrentPathView.setText(mLastVideoPath);
        parseAndUpdateGridview(true);
    }

    private void play(Item item) {
        if (mVideoList.isEmpty()) {
            Toast.makeText(getActivity(), "请选择视频", Toast.LENGTH_SHORT).show();
            return;
        }
        int index = mVideoList.indexOf(item);
        Intent i = new Intent(getActivity(), VideoPlayer.class);
        i.putExtra(VideoPlayer.KEY_VIDEO_LIST, mVideoList);
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
                mCurrentPathView.setText(mLastVideoPath);
                parseAndUpdateGridview(false);
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
        mCurrentPathView.setText(mLastVideoPath);
        parseAndUpdateGridview(false);
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
        mCurrentPathView.setText(mLastVideoPath);
        parseAndUpdateGridview(false);
    }

    private void parseAndUpdateGridview(final boolean back) {
        Log.d(TAG, "parseAndUpdateGridview path: " + mLastVideoPath);
        saveVideoPath(mLastVideoPath);
        new Thread(new Runnable() {
            @Override
            public void run() {
                parseVideoDir(mLastVideoPath);
                if (back) {
                    mHandler.post(mBackDirRunnable);
                } else {
                    mHandler.post(mEnterDirRunnable);
                }
            }
        }).start();
    }

    private Runnable mEnterDirRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mEnterDirRunnable mTotalList.size()=" + mTotalList.size());
            updateGridView(false);
        }
    };

    private Runnable mBackDirRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mBackDirRunnable mTotalList.size()=" + mTotalList.size());
            updateGridView(true);
        }
    };

    private void updateGridView(boolean back) {
        mGridAdapter.updateData(mTotalList);
        if (mTotalList.size() == 0) {
            mNoContentLayout.setVisibility(View.VISIBLE);
            mNoContentLayout.requestFocus();
        } else {
            int selectIndex = 0;// 进入子文件夹默认位置为第一个
            if (back && mEnterDirIndexList.size() > 0) {
                selectIndex = mEnterDirIndexList.get(mEnterDirIndexList.size() - 1);
                mEnterDirIndexList.remove(mEnterDirIndexList.size() - 1);
            }
            // GridView.setSelection 要早于 GridView.requestFocus，
            // 否则与默认select 0 冲突出现焦点跳跃
            mGridView.setSelection(selectIndex);
            mGridView.requestFocus();
            mNoContentLayout.setVisibility(View.GONE);
            // 这里是针对切换文件夹时，子文件夹内容数量与父文件夹内容数量相等时，
            // 且选中位置都为 0 时，GridView 不会回调 onItemSelected 的情况的补充。
            delayShowSelectEffect();
        }
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
                mGridAdapter.selectView(mGridView.getSelectedView());
            }
        }
    };

    private void saveVideoPath(String path) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(SP_KEY_LAST_VIDEO_PATH, path);
        editor.apply();
    }

    @Override
    protected int getSelfTabIndex() {
        return 1;
    }

}
