package com.wtz.gallery.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wtz.gallery.R;
import com.wtz.gallery.data.Item;
import com.wtz.gallery.utils.FileUtil;

import java.util.ArrayList;
import java.util.List;


public class VideoGridAdapter extends BaseAdapter {
    private final static String TAG = VideoGridAdapter.class.getSimpleName();

    private Context mContext;
    private List<Item> mDataList = new ArrayList<>();
    private int mItemWidth;
    private AbsListView.LayoutParams mItemLayoutParams;
    private Handler mHandler;

    private View mLastView = null;

    public VideoGridAdapter(Context context, List<Item> dataList, int itemWidth, Handler handler) {
        mContext = context;
        mDataList.addAll(dataList);
        mItemWidth = itemWidth;
        mHandler = handler;
    }

    public void updateData(List<Item> dataList) {
        mDataList.clear();
        mDataList.addAll(dataList);
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.video_item_gridview, parent, false);
            if (mItemLayoutParams == null) {
                mItemLayoutParams = (AbsListView.LayoutParams) convertView.getLayoutParams();
                mItemLayoutParams.width = mItemWidth;
                mItemLayoutParams.height = mItemLayoutParams.width * 3 / 4;
            }
            convertView.setLayoutParams(mItemLayoutParams);
            holder.imageView = (ImageView) convertView.findViewById(R.id.iv_img);
            holder.name = convertView.findViewById(R.id.tv_name);
            holder.cover = convertView.findViewById(R.id.v_cover);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Item item = (Item) getItem(position);
        holder.id = item.path;
        holder.name.setText(item.name);
        if (item.type == Item.TYPE_DIR) {
            holder.imageView.setImageResource(R.drawable.icon_folder);
        } else if (item.type == Item.TYPE_VIDEO) {
            holder.imageView.setImageResource(R.drawable.icon_video);
            final ViewHolder finalHolder = holder;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "to getVideoThumbnail path = " + item.path);
                    if (!item.path.equals(finalHolder.id)) {
                        Log.d(TAG, "before getVideoThumbnail path != finalHolder.id");
                        return;
                    }
                    final Bitmap bitmap = FileUtil.getVideoThumbnail(item.path);
                    Log.d(TAG, "getVideoThumbnail bitmap==null? " + (bitmap == null));
                    if (item.path.equals(finalHolder.id) && bitmap != null) {
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
        }

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
        float[] values = new float[]{1.0f, 1.08f};
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
        float[] values = new float[]{1.08f, 1.0f};
        animSet.playTogether(ObjectAnimator.ofFloat(view, "scaleX", values),
                ObjectAnimator.ofFloat(view, "scaleY", values));
        animSet.setDuration(10).start();
    }

    class ViewHolder {
        String id;
        ImageView imageView;
        TextView name;
        View cover;
    }

}
