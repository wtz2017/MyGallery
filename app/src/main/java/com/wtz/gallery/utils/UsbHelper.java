package com.wtz.gallery.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class UsbHelper {
    private static final String TAG = UsbHelper.class.getSimpleName();

    private BroadcastReceiver mUsbReceiver;
    private String mUsbPath;
    private static final String[] USB_PATH_KEYS = new String[]{
            "/sda", "/sdb", "/sdc"
    };

    public void initUsb(Context context) {
        queryUsbPath(context);
        registerUsbReceiver(context);// 在主动获取USB列表之后注册，用以更新
    }

    private void queryUsbPath(Context context) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            String[] paths = (String[]) sm.getClass()
                    .getMethod("getVolumePaths", null).invoke(sm, null);
            int size = paths.length;
            String tempPath;
            for (int i = 0; i < size; i++) {
                tempPath = paths[i];
                Log.d(TAG, "Query Usb Path:" + tempPath);
                if (tempPath != null) {
                    for (int j = 0; j < USB_PATH_KEYS.length; j++) {
                        if (tempPath.contains(USB_PATH_KEYS[j])) {
                            mUsbPath = tempPath;
                            break;
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Final UsbPath:" + mUsbPath);
    }

    private void registerUsbReceiver(Context context) {
        if (mUsbReceiver == null) {
            mUsbReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(TAG, "onReceive: " + action + ", Data:" + intent.getDataString());

                    String path = intent.getDataString();
                    String head = "file://";
                    int headLength = head.length();
                    if (!TextUtils.isEmpty(path) && path.startsWith(head) && path.length() > headLength) {
                        path = path.substring(headLength);
                    }
                    if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                        if (mUsbPath == null || !mUsbPath.equals(path)) {
                            mUsbPath = path;
                            Log.d(TAG, "update usb path:" + mUsbPath);
                        }
                    } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                        if (mUsbPath != null && mUsbPath.equals(path)) {
                            mUsbPath = null;
                            Log.d(TAG, "update usb path:" + mUsbPath);
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addDataScheme("file");
            filter.addAction(Intent.ACTION_MEDIA_CHECKING);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_SHARED);
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
            filter.addAction(Intent.ACTION_MEDIA_REMOVED);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            context.registerReceiver(mUsbReceiver, filter);
        }
    }

    public void unregisterUsbReceiver(Context context) {
        if (mUsbReceiver != null) {
            context.unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
    }

    public String getUsbPath() {
        return mUsbPath;
    }

}
