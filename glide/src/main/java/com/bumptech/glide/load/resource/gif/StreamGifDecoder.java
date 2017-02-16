package com.bumptech.glide.load.resource.gif;

import android.util.Log;

import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * A relatively inefficient decoder for {@link GifDrawable}
 * that converts {@link InputStream}s to {@link ByteBuffer}s and then passes
 * the buffer to a wrapped decoder.
 * <p>
 * 对{@link GifDrawable}相对低效的解码，将{@link InputStream}转换成{@link ByteBuffer}，
 * 然后将其传递给一个{@link ResourceDecoder}
 */
public class StreamGifDecoder implements ResourceDecoder<InputStream, GifDrawable> {
    private static final String TAG = "StreamGifDecoder";
    /**
     * If set to {@code true}, disables this decoder
     * ({@link #handles(InputStream, Options)} will return {@code false}). Defaults to
     * {@code false}.
     */
    public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
            "com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder.DisableAnimation", false);

    private final List<ImageHeaderParser> parsers;
    private final ResourceDecoder<ByteBuffer, GifDrawable> byteBufferDecoder;
    private final ArrayPool byteArrayPool;

    public StreamGifDecoder(List<ImageHeaderParser> parsers, ResourceDecoder<ByteBuffer,
            GifDrawable> byteBufferDecoder, ArrayPool byteArrayPool) {
        this.parsers = parsers;
        this.byteBufferDecoder = byteBufferDecoder;
        this.byteArrayPool = byteArrayPool;
    }

    @Override
    public boolean handles(InputStream source, Options options) throws IOException {
        return !options.get(DISABLE_ANIMATION)
                && ImageHeaderParserUtils.getType(parsers, source, byteArrayPool) == ImageHeaderParser.ImageType.GIF;
    }

    @Override
    public Resource<GifDrawable> decode(InputStream source, int width, int height,
                                        Options options) throws IOException {
        byte[] data = inputStreamToBytes(source);
        if (data == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        return byteBufferDecoder.decode(byteBuffer, width, height, options);
    }

    /**
     * 读取{@link InputStream}中的数据，并将其转化成字节数组
     */
    private static byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16384;// 16KB
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
        try {
            int nRead;
            byte[] data = new byte[bufferSize];
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Error reading data from stream", e);
            }
            return null;
        }
        return buffer.toByteArray();
    }
}
