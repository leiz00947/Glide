package com.bumptech.glide.load;

import java.io.File;

/**
 * An interface for writing data to some persistent data store (i.e. a local File cache).
 * <p>
 * 写入持久化数据的接口(如写入一个本地文件缓存)
 *
 * @param <T> The type of the data that will be written.
 *            <p>要被写入的数据类型</p>
 */
public interface Encoder<T> {

    /**
     * Writes the given data to the given output stream and returns True if the write completed
     * successfully and should be committed.
     * <p>
     * 将给定的数据写入到输出流中,写入成功会返回为{@code true}
     *
     * @param data    The data to write.
     * @param file    The File to write the data to.
     * @param options The put of options to apply when encoding.
     */
    boolean encode(T data, File file, Options options);
}
