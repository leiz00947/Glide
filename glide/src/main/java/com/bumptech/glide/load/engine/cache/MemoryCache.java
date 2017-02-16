package com.bumptech.glide.load.engine.cache;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;

/**
 * An interface for adding and removing resources from an in memory cache.
 * <p>
 * 从内存缓存中添加和移除资源的接口
 */
public interface MemoryCache {
    /**
     * An interface that will be called whenever a bitmap is removed from the cache.
     * <p>
     * 每当一个Bitmap从缓存中移除就会调用该接口
     */
    interface ResourceRemovedListener {
        void onResourceRemoved(Resource<?> removed);
    }

    /**
     * Returns the sum of the sizes of all the contents of the cache in bytes.
     * <p>
     * 返回已占用的缓存内存
     */
    int getCurrentSize();

    /**
     * Returns the current maximum size in bytes of the cache.
     * <p>
     * 返回缓存分配到的最大内存
     */
    int getMaxSize();

    /**
     * Adjust the maximum size of the cache by multiplying the original size of the cache by the given
     * multiplier.
     * <p>
     * If the size multiplier causes the size of the cache to be decreased, items will be evicted
     * until the cache is smaller than the new size.
     *
     * @param multiplier A size multiplier >= 0.
     */
    void setSizeMultiplier(float multiplier);

    /**
     * Removes the value for the given key and returns it if present or null otherwise.
     *
     * @param key The key.
     */
    @Nullable
    Resource<?> remove(Key key);

    /**
     * Add bitmap to the cache with the given key.
     *
     * @param key      The key to retrieve the bitmap.
     * @param resource The {@link com.bumptech.glide.load.engine.EngineResource} to store.
     * @return The old value of key (null if key is not in map).
     */
    @Nullable
    Resource<?> put(Key key, Resource<?> resource);

    /**
     * Set the listener to be called when a bitmap is removed from the cache.
     *
     * @param listener The listener.
     */
    void setResourceRemovedListener(ResourceRemovedListener listener);

    /**
     * Evict all items from the memory cache.
     * <p>
     * 从内存缓存中去除所有缓存项
     */
    void clearMemory();

    /**
     * Trim the memory cache to the appropriate level. Typically called on the callback onTrimMemory.
     *
     * @param level This integer represents a trim level as specified in {@link
     *              android.content.ComponentCallbacks2}.
     */
    void trimMemory(int level);
}
