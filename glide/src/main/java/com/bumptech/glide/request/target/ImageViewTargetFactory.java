package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * A factory responsible for producing the correct type of
 * {@link Target} for a given {@link android.view.View} subclass.
 * <p>
 * 用来为给定的{@link android.view.View}的子类生产一个合适的{@link Target}类型
 */
public class ImageViewTargetFactory {

    @SuppressWarnings("unchecked")
    public <Z> Target<Z> buildTarget(ImageView view, Class<Z> clazz) {
        if (Bitmap.class.equals(clazz)) {
            return (Target<Z>) new BitmapImageViewTarget(view);
        } else if (Drawable.class.isAssignableFrom(clazz)) {
            return (Target<Z>) new DrawableImageViewTarget(view);
        } else {
            throw new IllegalArgumentException(
                    "Unhandled class: " + clazz + ", try .as*(Class).transcode(ResourceTranscoder)");
        }
    }
}
