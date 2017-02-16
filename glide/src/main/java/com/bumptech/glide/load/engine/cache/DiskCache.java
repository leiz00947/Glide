package com.bumptech.glide.load.engine.cache;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.Key;

import java.io.File;

/**
 * An interface for writing to and reading from a disk cache.
 * <p>
 * 磁盘缓存接口
 */
public interface DiskCache {
    /**
     * An interface for lazily creating a disk cache.
     * <p>
     * 创建磁盘缓存的工厂
     */
    interface Factory {
        /**
         * 250 MB of cache.
         * <p>
         * 默认磁盘缓存大小为250MB，{@link InternalCacheDiskCacheFactory}
         */
        int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;
        String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";

        /**
         * Returns a new disk cache, or {@code null} if no disk cache could be created.
         * <p>
         * 返回一个新的{@link DiskCache}实例或者{@code null}(如磁盘缓存不能被创建)
         */
        @Nullable
        DiskCache build();
    }

    /**
     * An interface to actually write data to a key in the disk cache.
     * <p>
     * 写入数据到磁盘缓存中
     */
    interface Writer {
        /**
         * Writes data to the file and returns true if the write was successful and should be committed,
         * and false if the write should be aborted.
         * <p>
         * 写入数据到文件中去,当写入成功返回{@code true}
         *
         * @param file The File the Writer should write to.
         */
        boolean write(File file);
    }

    /**
     * Get the cache for the value at the given key.
     * <p>
     * Note - This is potentially dangerous, someone may write a new value to the file at any
     * point in time and we won't know about it.
     *
     * @param key The key in the cache.
     * @return An InputStream representing the data at key at the time get is called.
     */
    @Nullable
    File get(Key key);

    /**
     * Write to a key in the cache. {@link Writer} is used so that the cache implementation can
     * perform actions after the write finishes, like commit (via atomic file rename).
     *
     * @param key    The key to write to.
     * @param writer An interface that will write data given an OutputStream for the key.
     */
    void put(Key key, Writer writer);

    /**
     * Remove the key and value from the cache.
     *
     * @param key The key to remove.
     */
    void delete(Key key);

    /**
     * Clear the cache.
     */
    void clear();
}
