package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * A {@link Target} that can display an {@link
 * Bitmap} in an {@link ImageView}.
 * <p>
 * 可用来在一个{@link ImageView}中展示一个{@link Bitmap}
 */
public class BitmapImageViewTarget extends ImageViewTarget<Bitmap> {
    public BitmapImageViewTarget(ImageView view) {
        super(view);
    }

    public BitmapImageViewTarget(ImageView view, boolean waitForLayout) {
        super(view, waitForLayout);
    }

    /**
     * Sets the {@link Bitmap} on the view using {@link
     * ImageView#setImageBitmap(Bitmap)}.
     *
     * @param resource The bitmap to display.
     */
    @Override
    protected void setResource(Bitmap resource) {
        view.setImageBitmap(resource);
    }
}
