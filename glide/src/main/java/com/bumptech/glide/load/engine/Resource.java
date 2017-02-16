package com.bumptech.glide.load.engine;


/**
 * A resource interface that wraps a particular type so that it can be pooled and reused.
 * <p>
 * 封装一个可以被合并和重用的资源类型的接口
 *
 * @param <Z> The type of resource wrapped by this class.
 *            <p>封装成资源类型的类</p>
 */
public interface Resource<Z> {

    /**
     * Returns the {@link Class} of the wrapped resource.
     * <p>
     * 返回封装的资源类
     */
    Class<Z> getResourceClass();

    /**
     * Returns an instance of the wrapped resource.
     * <p>
     * 返回一个封装的资源类的实例
     * <p>
     * Note - This does not have to be the same instance of the wrapped resource class and in fact
     * it is often appropriate to return a new instance for each call. For example,
     * {@link android.graphics.drawable.Drawable Drawable}s should only be used by a single
     * {@link android.view.View View} at a time so each call to this method for Resources that wrap
     * {@link android.graphics.drawable.Drawable Drawable}s should always return a new
     * {@link android.graphics.drawable.Drawable Drawable}.
     * <p>
     * 不必每次都返回同一个实例，事实上通常调用该方法时也会返回一个新的实例，
     * 比如多个{@link android.graphics.drawable.Drawable}被单个{@link android.view.View}显示，
     * 而每次调用该方法时总是返回一个新的{@link android.graphics.drawable.Drawable}
     */
    Z get();

    /**
     * Returns the size in bytes of the wrapped resource to use to determine how much of the memory
     * cache this resource uses.
     * <p>
     * 返回封装的资源实例需要占用的内存的字节大小
     */
    int getSize();

    /**
     * Cleans up and recycles internal resources.
     * <p>
     * 清理回收内部资源
     * <p>
     * It is only safe to call this method if there are no current resource consumers and if this
     * method has not yet been called. Typically this occurs at one of two times:
     * <ul><li>During a resource load when the resource is transformed or transcoded before any consumer
     * have ever had access to this resource</li>
     * <li>After all consumers have released this resource and it has been evicted from the cache</li></ul>
     * <p>
     * For most users of this class, the only time this method should ever be called is during
     * transformations or transcoders, the framework will call this method when all consumers have
     * released this resource and it has been evicted from the cache. </p>
     */
    void recycle();
}
