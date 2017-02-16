package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

/**
 * An interface for writing data from a resource to some persistent data store (i.e. a local File cache).
 * <p>
 * 用来实现将资源写入持久化数据(比如本地文件缓存)
 *
 * @param <T> The type of the data contained by the resource.
 */
public interface ResourceEncoder<T> extends Encoder<Resource<T>> {
    // specializing the generic arguments
    EncodeStrategy getEncodeStrategy(Options options);
}
