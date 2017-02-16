package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

/**
 * An {@link BitmapPool BitmapPool} implementation
 * that rejects all {@link Bitmap Bitmap}s added to it and always returns {@code
 * null} from get.
 * <p>
 * {@link BitmapPool}接口的实现类，拒绝添加所有{@link Bitmap}并且
 * {@link #get(int, int, Bitmap.Config)}总是为{@code null}
 */
public class BitmapPoolAdapter implements BitmapPool {
    @Override
    public int getMaxSize() {
        return 0;
    }

    @Override
    public void setSizeMultiplier(float sizeMultiplier) {
        // Do nothing.
    }

    @Override
    public void put(Bitmap bitmap) {
        bitmap.recycle();
    }

    @NonNull
    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        return Bitmap.createBitmap(width, height, config);
    }

    @NonNull
    @Override
    public Bitmap getDirty(int width, int height, Bitmap.Config config) {
        return get(width, height, config);
    }

    @Override
    public void clearMemory() {
        // Do nothing.
    }

    @Override
    public void trimMemory(int level) {
        // Do nothing.
    }
}
