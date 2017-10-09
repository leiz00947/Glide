package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bytes.BytesResource;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * An {@link ResourceTranscoder} that converts {@link Bitmap}s into byte arrays using
 * {@link Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)}.
 * <p>
 * {@link ResourceTranscoder}的实现类，使用
 * {@link Bitmap#compress(Bitmap.CompressFormat, int, OutputStream)}来将{@link Bitmap}转换成字节数组
 */
public class BitmapBytesTranscoder implements ResourceTranscoder<Bitmap, byte[]> {
    private final Bitmap.CompressFormat compressFormat;
    private final int quality;

    public BitmapBytesTranscoder() {
        this(Bitmap.CompressFormat.JPEG, 100);
    }

    public BitmapBytesTranscoder(Bitmap.CompressFormat compressFormat, int quality) {
        this.compressFormat = compressFormat;
        this.quality = quality;
    }

    @Override
    public Resource<byte[]> transcode(Resource<Bitmap> toTranscode, Options options) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        toTranscode.get().compress(compressFormat, quality, os);
        toTranscode.recycle();
        return new BytesResource(os.toByteArray());
    }
}
