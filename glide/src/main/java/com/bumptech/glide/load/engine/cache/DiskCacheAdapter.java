package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;

import java.io.File;

/**
 * A simple class that returns null for all gets and ignores all writes.
 * <p>
 * {@link DiskCache}的实现类，一个所有返回文件为空并忽略所有写入操作的简单类
 */
public class DiskCacheAdapter implements DiskCache {
    @Override
    public File get(Key key) {
        // no op, default for overriders
        return null;
    }

    @Override
    public void put(Key key, Writer writer) {
        // no op, default for overriders
    }

    @Override
    public void delete(Key key) {
        // no op, default for overriders
    }

    @Override
    public void clear() {
        // no op, default for overriders
    }
}
