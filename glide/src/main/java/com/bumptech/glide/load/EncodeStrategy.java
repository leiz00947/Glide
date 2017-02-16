package com.bumptech.glide.load;

/**
 * Details how an {@link com.bumptech.glide.load.ResourceEncoder} will encode a resource to cache.
 * <p>
 * 编码类型的枚举类
 */
public enum EncodeStrategy {
    /**
     * Writes the original unmodified data for the resource to disk, not include downsampling or transformations.
     * <p>
     * 写入原始数据到磁盘缓存文件中去
     */
    SOURCE,

    /**
     * Writes the decoded, downsampled and transformed data for the resource to disk.
     * <p>
     * 写入降低采样和转换过后的数据到磁盘文件中去
     */
    TRANSFORMED,

    /**
     * Will write no data.
     * <p>
     * 不写入数据
     */
    NONE,
}
