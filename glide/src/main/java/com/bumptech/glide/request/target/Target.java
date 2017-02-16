package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;

/**
 * An interface that Glide can load a resource into and notify of relevant lifecycle events during a load.
 * <p>
 * 用来存放加载资源的容器（如{@link android.widget.ImageView}）
 * <p>
 * The lifecycle events in this class are as follows:
 * <ul> <li>onLoadStarted</li>
 * <li>onResourceReady</li>
 * <li>onLoadCleared</li>
 * <li>onLoadFailed</li> </ul>
 * <p>
 * 加载周期方法如下：<ul>
 * <li>onLoadStarted</li>
 * <li>onResourceReady</li>
 * <li>onLoadCleared</li>
 * <li>onLoadFailed</li> </ul>
 * <p>
 * The typical lifecycle is onLoadStarted -> onResourceReady or onLoadFailed -> onLoadCleared.
 * However, there are no guarantees. onLoadStarted may not be called if the resource is in memory or
 * if the load will fail because of a null model object. onLoadCleared similarly may never be called
 * if the target is never cleared. See the docs for the individual methods for details.
 * <p>
 * 常见加载周期流程是：onLoadStarted -> onResourceReady 或 onLoadFailed -> onLoadCleared.
 * 但并不保证完全是这样的，onLoadStarted可能不被调用（如资源已经在内存中或加载的资源本身就不存在）.
 * 同样地onLoadCleared也可能不会被调用
 *
 * @param <R> The type of resource the target can display.
 *            <p>可用来显示的资源类型——target</p>
 */
public interface Target<R> extends LifecycleListener {
    /**
     * Indicates that we want the resource in its original unmodified width and/or height.
     * <p>
     * 该值表示想要加载的资源的原始未定义的宽高值（即表示该资源不需要进行
     * {@link com.bumptech.glide.load.Transformation}）
     */
    int SIZE_ORIGINAL = Integer.MIN_VALUE;

    /**
     * A lifecycle callback that is called when a load is started.
     * <p>
     * 当开始加载资源时回调该方法（加载周期方法）
     * <p>
     * Note - This may not be called for every load, it is possible for example for loads to fail
     * before the load starts (when the model object is null).
     * <p>
     * 该方法不一定每次加载都被调用，例如，当加载的资源本身就为空
     * <p>
     * Note - This method may be called multiple times before any other lifecycle method is
     * called. Loads can be paused and restarted due to lifecycle or connectivity events and each
     * restart may cause a call here.
     * <p>
     * 该方法也可能在其它加载周期方法被调用之前被多次调用，例如加载被暂停然后被重新加载
     * <p>
     * You must ensure that any current Drawable received in {@link #onResourceReady(Object,
     * Transition)} is no longer displayed before redrawing the container (usually a View) or
     * changing its visibility.
     * <p>
     * 确保当前加载的资源在传递到{@link #onResourceReady(Object, Transition)}时，
     * 在显示该资源的容器（通常为一个View）大小被调整或转换显隐状态前不再被显示
     *
     * @param placeholder The placeholder drawable to optionally show, or null.
     */
    void onLoadStarted(@Nullable Drawable placeholder);

    /**
     * A lifecycle callback that is called when a load fails.
     * <p>
     * 当加载资源失败时回调该方法（加载周期方法）
     * <p>
     * Note - This may be called before {@link #onLoadStarted(Drawable)} if the model object is null.
     * <p>
     * 该方法可能在{@link #onLoadStarted(Drawable)}之前被调用
     * <p>
     * You must ensure that any current Drawable received in {@link #onResourceReady(Object,
     * Transition)} is no longer displayed before redrawing the container (usually a View) or
     * changing its visibility.
     *
     * @param errorDrawable The error drawable to optionally show, or null.
     */
    void onLoadFailed(@Nullable Drawable errorDrawable);

    /**
     * The method that will be called when the resource load has finished.
     * <p>
     * 当加载资源结束时该方法被调用（加载周期方法）
     *
     * @param resource the loaded resource.
     */
    void onResourceReady(R resource, Transition<? super R> transition);

    /**
     * A lifecycle callback that is called when a load is cancelled and its resources are freed.
     * <p>
     * 当取消加载资源时回调该方法（加载周期方法）
     * <p>
     * You must ensure that any current Drawable received in {@link #onResourceReady(Object,
     * Transition)} is no longer displayed before redrawing the container (usually a View) or
     * changing its visibility.
     *
     * @param placeholder The placeholder drawable to optionally show, or null.
     */
    void onLoadCleared(@Nullable Drawable placeholder);

    /**
     * A method to retrieve the size of this target.
     *
     * @param cb The callback that must be called when the size of the target has been determined
     */
    void getSize(SizeReadyCallback cb);

    /**
     * Sets the current request for this target to retain, should not be called outside of Glide.
     * <p>
     * 设置当前容器所持有的请求
     */
    void setRequest(@Nullable Request request);

    /**
     * Retrieves the current request for this target, should not be called outside of Glide.
     * <p>
     * 检索当前容器所持有的请求
     */
    @Nullable
    Request getRequest();
}
