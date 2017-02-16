package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.security.MessageDigest;

/**
 * Returns the image with its original size if its dimensions match or are smaller
 * than the target's, couple with {@link android.widget.ImageView.ScaleType#CENTER_INSIDE}
 * in order to center it in Target. If not, then it is scaled so that one of the dimensions of
 * the image will be equal to the given dimension and the other will be less than the given
 * dimension (maintaining the image's aspect ratio).
 * <p>
 * 若图片尺寸小于目标尺寸，那么就采用{@link android.widget.ImageView.ScaleType#CENTER_INSIDE}样式，
 * 否则就会缩放到目标尺寸大小（保持图片纵横比）
 */
public class CenterInside extends BitmapTransformation {
    private static final String ID = "com.bumptech.glide.load.resource.bitmap.CenterInside";
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    public CenterInside(Context context) {
        super(context);
    }

    public CenterInside(BitmapPool bitmapPool) {
        super(bitmapPool);
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth,
                               int outHeight) {
        return TransformationUtils.centerInside(pool, toTransform, outWidth, outHeight);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CenterInside;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}


