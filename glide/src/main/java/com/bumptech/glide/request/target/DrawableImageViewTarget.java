package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

/**
 * A target for display {@link Drawable} objects in {@link ImageView}s.
 * <p>
 * 展示一个{@link Drawable}对象到{@link ImageView}
 */
public class DrawableImageViewTarget extends ImageViewTarget<Drawable> {

    public DrawableImageViewTarget(ImageView view) {
        super(view);
    }

    @Override
    protected void setResource(@Nullable Drawable resource) {
        view.setImageDrawable(resource);
    }
}
