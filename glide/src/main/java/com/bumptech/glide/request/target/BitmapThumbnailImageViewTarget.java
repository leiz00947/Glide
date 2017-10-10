package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Efficiently displays multiple Bitmaps loaded serially into a single {@link android.view.View}.
 * <p>
 * 高效地将许多的Bitmap连续加载显示到一个单独的{@link android.view.View}中去
 */
public class BitmapThumbnailImageViewTarget extends ThumbnailImageViewTarget<Bitmap> {
    public BitmapThumbnailImageViewTarget(ImageView view) {
        super(view);
    }

    public BitmapThumbnailImageViewTarget(ImageView view, boolean waitForLayout) {
        super(view, waitForLayout);
    }

    @Override
    protected Drawable getDrawable(Bitmap resource) {
        return new BitmapDrawable(view.getResources(), resource);
    }
}
