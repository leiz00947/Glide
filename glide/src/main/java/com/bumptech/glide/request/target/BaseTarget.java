package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;

/**
 * A base {@link Target} for loading {@link com.bumptech.glide.load.engine.Resource}s that provides
 * basic or empty implementations for most methods.
 * <p>
 * 用来给加载{@link com.bumptech.glide.load.engine.Resource}提供大量方法的抽象类
 * <p>
 * For maximum efficiency, clear this target when you have finished using or displaying the
 * {@link com.bumptech.glide.load.engine.Resource} loaded into it using
 * {@link com.bumptech.glide.RequestManager#clear(Target)}.
 * <p>
 * 为了获得最大效率，当结束使用或使用{@link com.bumptech.glide.RequestManager#clear(Target)}
 * 展示{@link com.bumptech.glide.load.engine.Resource}时清理掉该{@link Target}
 * <p>
 * For loading {@link com.bumptech.glide.load.engine.Resource}s into {@link android.view.View}s,
 * {@link com.bumptech.glide.request.target.ViewTarget} or
 * {@link com.bumptech.glide.request.target.ImageViewTarget} are preferable.
 * <p>
 * 将{@link com.bumptech.glide.load.engine.Resource}加载到{@link android.view.View}，
 * {@link ViewTarget}或{@link ImageViewTarget}是更可取的
 * <p>
 * 一个抽象类，由于接口类过多，在这里实现了大多数方法，这样一来，当子类继承该抽象类时，也就只必须要实现
 * {@link #getSize(SizeReadyCallback)}和{@link #onResourceReady(Object, Transition)}这两个方法就
 * 可以了
 *
 * @param <Z> The type of resource that will be received by this target.
 */
public abstract class BaseTarget<Z> implements Target<Z> {

    private Request request;

    @Override
    public void setRequest(@Nullable Request request) {
        this.request = request;
    }

    @Nullable
    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        // Do nothing.
    }

    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
        // Do nothing.
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        // Do nothing.
    }

    @Override
    public void onStart() {
        // Do nothing.
    }

    @Override
    public void onStop() {
        // Do nothing.
    }

    @Override
    public void onDestroy() {
        // Do nothing.
    }
}
