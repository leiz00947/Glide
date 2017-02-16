package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

/**
 * Lazily allocates a {@link BitmapDrawable} from a given
 * {@link Bitmap} on the first call to {@link #get()}.
 * <p>
 * {@link Resource}的实现类，当第一次调用{@link #get()}时，为给定{@link Bitmap}分配一个
 * {@link BitmapDrawable}
 */
public class LazyBitmapDrawableResource implements Resource<BitmapDrawable>, Initializable {

    private final Bitmap bitmap;
    private final Resources resources;
    private final BitmapPool bitmapPool;

    public static LazyBitmapDrawableResource obtain(Context context, Bitmap bitmap) {
        return obtain(context.getResources(), Glide.get(context).getBitmapPool(), bitmap);
    }

    public static LazyBitmapDrawableResource obtain(Resources resources, BitmapPool bitmapPool,
                                                    Bitmap bitmap) {
        return new LazyBitmapDrawableResource(resources, bitmapPool, bitmap);
    }

    LazyBitmapDrawableResource(Resources resources, BitmapPool bitmapPool, Bitmap bitmap) {
        this.resources = Preconditions.checkNotNull(resources);
        this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
        this.bitmap = Preconditions.checkNotNull(bitmap);
    }

    @Override
    public Class<BitmapDrawable> getResourceClass() {
        return BitmapDrawable.class;
    }

    @Override
    public BitmapDrawable get() {
        return new BitmapDrawable(resources, bitmap);
    }

    @Override
    public int getSize() {
        return Util.getBitmapByteSize(bitmap);
    }

    @Override
    public void recycle() {
        bitmapPool.put(bitmap);
    }

    @Override
    public void initialize() {
        bitmap.prepareToDraw();
    }
}
