package com.bumptech.glide.load;

/**
 * Indicates the origin of some retrieved data.
 * <p>
 * 被恢复数据的来源的枚举类(如是本地、远程或缓存等)
 */
public enum DataSource {
    /**
     * Indicates data was probably retrieved locally from the device, although it may have been
     * obtained through a content provider that may have obtained the data from a remote source.
     * <p>
     * 表示本地媒体文件类型
     */
    LOCAL,
    /**
     * Indicates data was retrieved from a remote source other than the device.
     * <p>
     * 表示需要连网获取的远程端的图片资源类型
     */
    REMOTE,
    /**
     * Indicates data was retrieved unmodified from the on device cache.
     * <p>
     * 表示原始未被修改过的ROM缓存文件类型（即存放的从远程端获取到的原始图片数据）
     */
    DATA_DISK_CACHE,
    /**
     * Indicates data was retrieved from modified content in the on device cache.
     * <p>
     * 表示按照特定规格修改过后的ROM缓存文件类型
     */
    RESOURCE_DISK_CACHE,
    /**
     * Indicates data was retrieved from the in memory cache.
     * <p>
     * 表示存在于RAM中的缓存类型
     */
    MEMORY_CACHE,
}
