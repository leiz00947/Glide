package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * 将指定的元素插入{@link java.util.Queue}（如果立即可行且不会违反容量限制）的接口
 */
interface Poolable {
    void offer();
}
