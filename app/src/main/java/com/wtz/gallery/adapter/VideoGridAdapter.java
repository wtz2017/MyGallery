package com.wtz.gallery.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.wtz.gallery.R;
import com.wtz.gallery.utils.FileUtil;

import java.util.List;


public class VideoGridAdapter extends BaseAdapter {
    private final static String TAG = VideoGridAdapter.class.getSimpleName();

    private Context mContext;
    private List<String> mDataList;
    private int mItemWidth;
    private AbsListView.LayoutParams mItemLayoutParams;
    private Handler mHandler;

    private View mLastView = null;

    public VideoGridAdapter(Context context, List<String> dataList, int itemWidth, Handler handler) {
        mContext = context;
        mDataList = dataList;
        mItemWidth = itemWidth;
        mHandler = handler;
    }

    public void updateData(List<String> dataList) {
        mDataList = dataList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return (mDataList == null) ? 0 : mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return (mDataList == null) ? null : mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_gridview, parent, false);
            if (mItemLayoutParams == null) {
                mItemLayoutParams = (AbsListView.LayoutParams) convertView.getLayoutParams();
                mItemLayoutParams.width = mItemWidth;
                mItemLayoutParams.height = mItemLayoutParams.width * 3 / 4;
            }
            convertView.setLayoutParams(mItemLayoutParams);
            holder.id = (String) getItem(position);
            holder.imageView = (ImageView) convertView.findViewById(R.id.iv_img);
            holder.cover = convertView.findViewById(R.id.v_cover);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.id = (String) getItem(position);
        final ViewHolder finalHolder = holder;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = (String) getItem(position);
                Log.d(TAG, "to getVideoThumbnail path = " + path);
                if (!path.equals(finalHolder.id)) {
                    Log.d(TAG, "before getVideoThumbnail path != finalHolder.id");
                    return;
                }
                final Bitmap bitmap = FileUtil.getVideoThumbnail((String) getItem(position));
                Log.d(TAG, "getVideoThumbnail bitmap==null? " + (bitmap == null));
                if (path.equals(finalHolder.id) && bitmap != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap scaled = FileUtil.scaleIntoSizeRange(bitmap, mItemLayoutParams.width,
                                    mItemLayoutParams.height);
                            finalHolder.imageView.setImageBitmap(scaled);
                        }
                    });
                } else {
                    Log.d(TAG, "after getVideoThumbnail path != finalHolder.id or bitmap is null");
                }
            }
        }).start();

        return convertView;
    }

    public void selectView(View view) {
        Log.d(TAG, "scaleDown: mLastView == view? " + (mLastView == view));
        if (mLastView == view) {
            return;
        }
        scaleDown(mLastView);// 缩小
        scaleUp(view);// 扩大
        mLastView = view;
    }

    private void scaleUp(View view) {
        Log.d(TAG, "scaleUp: " + view);
        if (view == null) return;

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.cover.setVisibility(View.VISIBLE);

        AnimatorSet animSet = new AnimatorSet();
        float[] values = new float[]{1.0f, 1.1f};
        animSet.playTogether(ObjectAnimator.ofFloat(view, "scaleX", values),
                ObjectAnimator.ofFloat(view, "scaleY", values));
        animSet.setDuration(10).start();
    }

    private void scaleDown(View view) {
        Log.d(TAG, "scaleDown: " + view);
        if (view == null) return;

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.cover.setVisibility(View.GONE);

        AnimatorSet animSet = new AnimatorSet();
        float[] values = new float[]{1.1f, 1.0f};
        animSet.playTogether(ObjectAnimator.ofFloat(view, "scaleX", values),
                ObjectAnimator.ofFloat(view, "scaleY", values));
        animSet.setDuration(10).start();
    }

    class ViewHolder {
        String id;
        ImageView imageView;
        View cover;
    }

}
