package com.bumptech.glide.load.engine;

import android.os.Looper;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.Preconditions;

/**
 * A wrapper resource that allows reference counting a wrapped {@link Resource} interface.
 * <p>
 * 引用计数器封装的一个{@link Resource}接口，每当该{@link EngineResource}对象引用使用时，
 * {@link #acquired}（原始值为0）加1，当{@link #acquired}大于0的时候，表示该对象正在被引用使用中，
 * 每当该对象引用使用完毕时调用{@link #release()}，{@link #acquired}减1，当所有引用使用该
 * 对象的地方都使用完毕后，那么{@link #acquired}就变成了原始值0，表示没有地方再
 * 引用使用该对象了，就可以{@link #recycle()}了，同时自动回调
 * {@link ResourceListener#onResourceReleased(Key, EngineResource)}接口
 *
 * @param <Z> The type of data returned by the wrapped {@link Resource}.
 */
class EngineResource<Z> implements Resource<Z> {
    private final boolean isCacheable;
    private ResourceListener listener;
    private Key key;
    private int acquired;
    private boolean isRecycled;
    private final Resource<Z> resource;

    /**
     * 资源释放接口（但非资源回收）
     */
    interface ResourceListener {
        void onResourceReleased(Key key, EngineResource<?> resource);
    }

    EngineResource(Resource<Z> toWrap, boolean isCacheable) {
        resource = Preconditions.checkNotNull(toWrap);
        this.isCacheable = isCacheable;
    }

    void setResourceListener(Key key, ResourceListener listener) {
        this.key = key;
        this.listener = listener;
    }

    boolean isCacheable() {
        return isCacheable;
    }

    @Override
    public Class<Z> getResourceClass() {
        return resource.getResourceClass();
    }

    @Override
    public Z get() {
        return resource.get();
    }

    @Override
    public int getSize() {
        return resource.getSize();
    }

    @Override
    public void recycle() {
        if (acquired > 0) {
            throw new IllegalStateException("Cannot recycle a resource while it is still acquired");
        }
        if (isRecycled) {
            throw new IllegalStateException("Cannot recycle a resource that has already been recycled");
        }
        isRecycled = true;
        resource.recycle();
    }

    /**
     * Increments the number of consumers using the wrapped resource. Must be called on the main thread.
     * <p>
     * 增加使用资源的数量({@code acquired}++),该方法必须在主线程中调用
     * <p>
     * This must be called with a number corresponding to the number of new consumers each time
     * new consumers begin using the wrapped resource. It is always safer to call acquire more often
     * than necessary. Generally external users should never call this method, the framework will take
     * care of this for you.
     * <p>
     * 将该资源设置为占用状态
     */
    void acquire() {
        if (isRecycled) {
            throw new IllegalStateException("Cannot acquire a recycled resource");
        }
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new IllegalThreadStateException("Must call acquire on the main thread");
        }
        ++acquired;
    }

    /**
     * Decrements the number of consumers using the wrapped resource. Must be called on the main thread.
     * <p>
     * This must only be called when a consumer that called the {@link #acquire()} method is now
     * done with the resource. Generally external users should never callthis method, the framework
     * will take care of this for you.
     */
    void release() {
        if (acquired <= 0) {
            throw new IllegalStateException("Cannot release a recycled or not yet acquired resource");
        }
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new IllegalThreadStateException("Must call release on the main thread");
        }
        if (--acquired == 0) {
            listener.onResourceReleased(key, this);
        }
    }

    @Override
    public String toString() {
        return "EngineResource{"
                + "isCacheable=" + isCacheable
                + ", listener=" + listener
                + ", key=" + key
                + ", acquired=" + acquired
                + ", isRecycled=" + isRecycled
                + ", resource=" + resource
                + '}';
    }
}
