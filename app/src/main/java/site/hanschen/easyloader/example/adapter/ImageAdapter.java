package site.hanschen.easyloader.example.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import site.hanschen.easyloader.EasyLoader;
import site.hanschen.easyloader.example.R;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {

    private Drawable[] randomDrawable = new Drawable[]{new ColorDrawable(0xA0E84796),
                                                       new ColorDrawable(0xA024BEBF),
                                                       new ColorDrawable(0xA0ADB8F4),
                                                       new ColorDrawable(0xA0BC3818),
                                                       new ColorDrawable(0xA0526573),
                                                       new ColorDrawable(0xA0DFECC4),
                                                       new ColorDrawable(0xA0047870),
                                                       new ColorDrawable(0xA0732763),
                                                       new ColorDrawable(0xA06050AD),
                                                       new ColorDrawable(0xA0347074),
                                                       new ColorDrawable(0xA0DCBFC9),
                                                       new ColorDrawable(0xA036301C),
                                                       new ColorDrawable(0xA0C69BA6),};
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

        int height = 800;
        ViewGroup.LayoutParams layoutParams = holder.picture.getLayoutParams();
        if (position == 0) {
            layoutParams.height = height / 2;
        } else {
            layoutParams.height = height;
        }
        EasyLoader.with(context).load(data.get(position))
//                  .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                  .diskPolicy(DiskPolicy.NO_CACHE, DiskPolicy.NO_STORE)
                  .placeholder(randomDrawable[position % randomDrawable.length]).into(holder.picture);
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
