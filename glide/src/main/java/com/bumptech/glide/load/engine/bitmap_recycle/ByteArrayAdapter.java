package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * Adapter for handling primitive byte arrays.
 * <p>
 * {@link ArrayAdapterInterface}接口的字节数组类型的实现类
 */
@SuppressWarnings("PMD.UseVarargs")
public final class ByteArrayAdapter implements ArrayAdapterInterface<byte[]> {
    private static final String TAG = "ByteArrayPool";

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public int getArrayLength(byte[] array) {
        return array.length;
    }

    @Override
    public byte[] newArray(int length) {
        return new byte[length];
    }

    @Override
    public int getElementSizeInBytes() {
        return 1;
    }
}
