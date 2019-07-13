package com.wtz.gallery.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.wtz.gallery.R;

import java.util.List;


public class GridAdapter extends BaseAdapter {
    private final static String TAG = GridAdapter.class.getSimpleName();

    private Context mContext;
    private List<String> mDataList;
    private int mItemWidth;
    private AbsListView.LayoutParams mItemLayoutParams;

    private View mLastView = null;

    public GridAdapter(Context context, List<String> dataList, int itemWidth) {
        mContext = context;
        mDataList = dataList;
        mItemWidth = itemWidth;
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
    public View getView(int position, View convertView, ViewGroup parent) {
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
            holder.imageView = (ImageView) convertView.findViewById(R.id.iv_img);
            holder.cover = convertView.findViewById(R.id.v_cover);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Picasso.get()
                .load((String) getItem(position))
                // 解决 OOM 问题
                .resize(mItemLayoutParams.width, mItemLayoutParams.height)
                .centerCrop()// 需要先调用fit或resize设置目标大小，否则会报错：Center crop requires calling resize with positive width and height
//                .placeholder(R.drawable.image_default)
//                .error(R.drawable.image_default)
                .noFade()
                .into(holder.imageView);

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
        ImageView imageView;
        View cover;
    }

}
