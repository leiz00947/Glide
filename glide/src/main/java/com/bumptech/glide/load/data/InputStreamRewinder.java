package com.bumptech.glide.load.data;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation for {@link InputStream}s that rewinds streams by wrapping them in a buffered stream.
 * <p>
 * 通过封装到一个缓存流中来实现回倒流
 */
public final class InputStreamRewinder implements DataRewinder<InputStream> {
    // 5mb.
    private static final int MARK_LIMIT = 5 * 1024 * 1024;

    private final RecyclableBufferedInputStream bufferedStream;

    InputStreamRewinder(InputStream is, ArrayPool byteArrayPool) {
        bufferedStream = new RecyclableBufferedInputStream(is, byteArrayPool);
        bufferedStream.mark(MARK_LIMIT);
    }

    @Override
    public InputStream rewindAndGet() throws IOException {
        bufferedStream.reset();
        return bufferedStream;
    }

    @Override
    public void cleanup() {
        bufferedStream.release();
    }

    /**
     * Factory for producing {@link InputStreamRewinder}s from {@link
     * InputStream}s.
     */
    public static final class Factory implements DataRewinder.Factory<InputStream> {
        private final ArrayPool byteArrayPool;

        public Factory(ArrayPool byteArrayPool) {
            this.byteArrayPool = byteArrayPool;
        }

        @Override
        public DataRewinder<InputStream> build(InputStream data) {
            return new InputStreamRewinder(data, byteArrayPool);
        }

        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }
    }
}
