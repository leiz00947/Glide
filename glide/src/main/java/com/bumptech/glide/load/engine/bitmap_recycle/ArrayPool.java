package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * Interface for an array pool that pools arrays of different types.
 * <p>
 * 不同类型数组池的接口
 */
public interface ArrayPool {
    /**
     * A standard size to use to increase hit rates when the required size isn't defined.
     * Currently 64KB.
     * <p>
     * 当请求未定义图片大小时，默认标准大小为64KB
     */
    int STANDARD_BUFFER_SIZE_BYTES = 64 * 1024;

    /**
     * Optionally adds the given array of the given type to the pool.
     * <p>
     * 随意添加给定类型的数组到这个数组池中
     * <p>
     * Arrays may be ignored, for example if the array is larger than the maximum size of the pool.
     * <p>
     * 传入的数组可能被忽略，比如传入的数组超过数组池的最大容量
     */
    <T> void put(T array, Class<T> arrayClass);

    /**
     * Returns a non-null array of the given type with a length >= to the given size.
     * <p>
     * 返回一个给定类型的非空数组，返回的数组大小不小于给定的大小
     * <p>
     * If an array of the given size isn't in the pool, a new one will be allocated.
     * <p>
     * This class makes no guarantees about the contents of the returned array.
     */
    <T> T get(int size, Class<T> arrayClass);

    /**
     * Clears all arrays from the pool.
     */
    void clearMemory();

    /**
     * Trims the size to the appropriate level.
     *
     * @param level A trim specified in {@link android.content.ComponentCallbacks2}.
     */
    void trimMemory(int level);

}
