package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Efficiently displays multiple Drawables loaded serially into a single {@link android.view.View}.
 * <p>
 * 高效地将许多的Drawable连续加载显示到一个单独的{@link android.view.View}中去
 */
public class DrawableThumbnailImageViewTarget extends ThumbnailImageViewTarget<Drawable> {
    public DrawableThumbnailImageViewTarget(ImageView view) {
        super(view);
    }

    public DrawableThumbnailImageViewTarget(ImageView view, boolean waitForLayout) {
        super(view, waitForLayout);
    }

    @Override
    protected Drawable getDrawable(Drawable resource) {
        return resource;
    }
}
