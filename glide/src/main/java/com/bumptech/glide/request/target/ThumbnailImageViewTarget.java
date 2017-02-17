package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.RequestBuilder;

/**
 * Avoids extra calls to {@link android.view.View#requestLayout} when loading more than once
 * image into an {@link ImageView} with fixed dimensions.
 * <p>
 * 避免当多次加载图片到一个固定尺寸的{@link ImageView}时额外调用{@link View#requestLayout()}
 * <p>
 * Typically it makes sense
 * to use this class when loading multiple images with the {@link
 * com.bumptech.glide.RequestBuilder#thumbnail(com.bumptech.glide.RequestBuilder)} API into views in
 * a scrolling list like ListView, GridView, or RecyclerView.
 * <p>
 * 尤其有意义的是当用{@link com.bumptech.glide.RequestBuilder#thumbnail(RequestBuilder)}
 * 加载许多图片到一个正在滑动的列表（比如ListView，GridView或RecyclerView）
 * <p>
 * {@link FixedSizeDrawable} may cause skewing or other undesirable behavior depending on
 * your images, views, and scaling. If this occurs, consider {@link DrawableImageViewTarget} or
 * {@link BitmapImageViewTarget} as alternatives.
 *
 * @param <T> The type of resource that will be displayed in the ImageView.
 */
public abstract class ThumbnailImageViewTarget<T> extends ImageViewTarget<T> {

    public ThumbnailImageViewTarget(ImageView view) {
        super(view);
    }

    /**
     * @param resource {@link Drawable} or {@link android.graphics.drawable.BitmapDrawable}
     */
    @Override
    protected void setResource(@Nullable T resource) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        Drawable result = getDrawable(resource);
        if (layoutParams != null && layoutParams.width > 0 && layoutParams.height > 0) {
            result = new FixedSizeDrawable(result, layoutParams.width, layoutParams.height);
        }

        view.setImageDrawable(result);
    }

    protected abstract Drawable getDrawable(T resource);
}
