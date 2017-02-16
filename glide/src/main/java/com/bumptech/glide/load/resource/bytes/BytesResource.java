package com.bumptech.glide.load.resource.bytes;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Preconditions;

/**
 * An {@link Resource} wrapping a byte array.
 * <p>
 * 封装字节数组的{@link Resource}
 */
public class BytesResource implements Resource<byte[]> {
    private final byte[] bytes;

    public BytesResource(byte[] bytes) {
        this.bytes = Preconditions.checkNotNull(bytes);
    }

    @Override
    public Class<byte[]> getResourceClass() {
        return byte[].class;
    }

    @Override
    public byte[] get() {
        return bytes;
    }

    @Override
    public int getSize() {
        return bytes.length;
    }

    @Override
    public void recycle() {
        // Do nothing.
    }
}
