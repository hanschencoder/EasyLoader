package com.hanschen.easyloader.example.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hanschen.easyloader.EasyLoader;
import com.hanschen.easyloader.example.R;

import java.util.List;

/**
 * Created by chenhang on 2016/10/26.
 */

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {

    private Context      context;
    private List<String> data;

    public ImageAdapter(Context context, List<String> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ImageHolder(LayoutInflater.from(context).inflate(R.layout.item_image_list, parent, false));
    }

    @Override
    public void onBindViewHolder(ImageHolder holder, int position) {
        EasyLoader.with(context).load(data.get(position))
//                  .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                  .diskPolicy(DiskPolicy.NO_CACHE, DiskPolicy.NO_STORE)
                  .placeholder(R.mipmap.ic_launcher).into(holder.picture);
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    class ImageHolder extends RecyclerView.ViewHolder {

        ImageView picture;

        ImageHolder(View view) {
            super(view);
            picture = (ImageView) view.findViewById(R.id.item_picture);
        }
    }
}
