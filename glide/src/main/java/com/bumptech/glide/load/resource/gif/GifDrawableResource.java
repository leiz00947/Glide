package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.resource.drawable.DrawableResource;

/**
 * A resource wrapping an {@link GifDrawable}.
 * <p>
 * 封装一个{@link GifDrawable}的{@link com.bumptech.glide.load.engine.Resource}
 */
public class GifDrawableResource extends DrawableResource<GifDrawable> implements Initializable {
    public GifDrawableResource(GifDrawable drawable) {
        super(drawable);
    }

    @Override
    public Class<GifDrawable> getResourceClass() {
        return GifDrawable.class;
    }

    @Override
    public int getSize() {
        return drawable.getSize();
    }

    @Override
    public void recycle() {
        drawable.stop();
        drawable.recycle();
    }

    @Override
    public void initialize() {
        drawable.getFirstFrame().prepareToDraw();
    }
}
