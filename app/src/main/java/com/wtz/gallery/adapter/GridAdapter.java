package com.wtz.gallery.adapter;

import android.content.Context;
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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Picasso.with(mContext)
                .load((String) getItem(position))
                // 解决 OOM 问题
                .resize(mItemLayoutParams.width, mItemLayoutParams.height)
//                .placeholder(R.drawable.image_default)
//                .error(R.drawable.image_default)
                .into(holder.imageView);

        return convertView;
    }

    class ViewHolder {
        ImageView imageView;
    }

}
