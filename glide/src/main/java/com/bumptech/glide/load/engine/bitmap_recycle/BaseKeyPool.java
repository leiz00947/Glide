package com.bumptech.glide.load.engine.bitmap_recycle;

import com.bumptech.glide.util.Util;

import java.util.Queue;

/**
 * 包含一个限定大小队列的基础操作类
 */
abstract class BaseKeyPool<T extends Poolable> {
    private static final int MAX_SIZE = 20;
    private final Queue<T> keyPool = Util.createQueue(MAX_SIZE);

    protected T get() {
        /**
         * 获取并移除此队列的头
         */
        T result = keyPool.poll();
        if (result == null) {
            result = create();
        }
        return result;
    }

    public void offer(T key) {
        /**
         * 将指定的元素插入此队列
         */
        if (keyPool.size() < MAX_SIZE) {
            keyPool.offer(key);
        }
    }

    protected abstract T create();
}
