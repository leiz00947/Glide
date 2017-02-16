package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.File;

/**
 * Writes original source data or downsampled/transformed resource data to cache using the
 * provided {@link com.bumptech.glide.load.Encoder} or
 * {@link com.bumptech.glide.load.ResourceEncoder} and the given data or
 * {@link Resource}.
 * <p>
 * 将原始资源数据或经过降低采样或经过转换后的资源数据写入到缓存中去
 *
 * @param <DataType> The type of data that will be encoded (InputStream, ByteBuffer,
 *                   Resource<Bitmap> etc).
 */
class DataCacheWriter<DataType> implements DiskCache.Writer {
    private final Encoder<DataType> encoder;
    private final DataType data;
    private final Options options;

    DataCacheWriter(Encoder<DataType> encoder, DataType data, Options options) {
        this.encoder = encoder;
        this.data = data;
        this.options = options;
    }

    @Override
    public boolean write(File file) {
        return encoder.encode(data, file, options);
    }
}
