package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * Interface for handling operations on a primitive array type.
 * <p>
 * 处理在原始数组类型上操作的接口
 *
 * @param <T> Array type (e.g. byte[], int[])
 *            <p>数组类型（比如字节数组，整型数组等）</p>
 */
public interface ArrayAdapterInterface<T> {
    /**
     * TAG for logging.
     * <p>
     * 获取日志的标签
     */
    String getTag();

    /**
     * Return the length of the given array.
     * <p>
     * 返回给定数组的长度
     */
    int getArrayLength(T array);

    /**
     * Allocate and return an array of the specified size.
     * <p>
     * 分配返回一个指定长度的数组
     */
    T newArray(int length);

    /**
     * Return the size of an element in the array in bytes (e.g. for int return 4).
     * <p>
     * 返回数组中某个元素所占用的字节数（比如一个int类型的数占用4个字节）
     */
    int getElementSizeInBytes();
}
