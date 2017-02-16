package com.bumptech.glide.load.model;

import android.util.Log;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.util.ByteBufferUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Writes {@link ByteBuffer ByteBuffers} to {@link File Files}.
 * <p>
 * {@link Encoder}的实现类，将{@link ByteBuffer}写入到文件
 */
public class ByteBufferEncoder implements Encoder<ByteBuffer> {
    private static final String TAG = "ByteBufferEncoder";

    /**
     * 将数据写入到文件
     */
    @Override
    public boolean encode(ByteBuffer data, File file, Options options) {
        boolean success = false;
        try {
            ByteBufferUtil.toFile(data, file);
            success = true;
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to write data", e);
            }
        }
        return success;
    }
}
