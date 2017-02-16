package com.bumptech.glide.load.resource.bitmap;

import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.util.Util;

/**
 * A {@link com.bumptech.glide.load.engine.Resource} that wraps an {@link BitmapDrawable}
 * <p>
 * 一个封装{@link BitmapDrawable}的{@link com.bumptech.glide.load.engine.Resource}实现类
 * <p>
 * This class ensures that every call to {@link #get()}} always returns a new
 * {@link BitmapDrawable} to avoid rendering issues if used in multiple
 * views and is also responsible for returning the underlying {@link android.graphics.Bitmap} to the
 * given {@link BitmapPool} when the resource is recycled.
 * <p>
 * 确保每次调用{@link #get()}总是返回一个新的{@link BitmapDrawable}以免在多个控件中出现显示问题
 */
public class BitmapDrawableResource extends DrawableResource<BitmapDrawable> implements Initializable {
    private final BitmapPool bitmapPool;

    public BitmapDrawableResource(BitmapDrawable drawable, BitmapPool bitmapPool) {
        super(drawable);
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Class<BitmapDrawable> getResourceClass() {
        return BitmapDrawable.class;
    }

    @Override
    public int getSize() {
        return Util.getBitmapByteSize(drawable.getBitmap());
    }

    @Override
    public void recycle() {
        bitmapPool.put(drawable.getBitmap());
    }

    @Override
    public void initialize() {
        drawable.getBitmap().prepareToDraw();
    }
}
