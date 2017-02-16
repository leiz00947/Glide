package com.bumptech.glide.load;

import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

import java.security.MessageDigest;
import java.util.Map;

/**
 * A set of {@link Option Options} to apply to in memory and disk cache keys.
 * <p>
 * 适用于内存和磁盘缓存键的一些选项
 * <p>
 * 常用设置的选项有:
 * <ul><li>{@link com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder#DISABLE_ANIMATION}</li>
 * <li>{@link com.bumptech.glide.load.resource.gif.StreamGifDecoder#DISABLE_ANIMATION}</li>
 * <li>{@link com.bumptech.glide.load.resource.bitmap.Downsampler#DOWNSAMPLE_STRATEGY}</li>
 * <li>{@link com.bumptech.glide.load.resource.bitmap.Downsampler#DECODE_FORMAT}</li>
 * <li>{@link com.bumptech.glide.load.resource.bitmap.BitmapEncoder#COMPRESSION_FORMAT}</li>
 * <li>{@link com.bumptech.glide.load.resource.bitmap.BitmapEncoder#COMPRESSION_QUALITY}</li>
 * <li>{@link com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder#TARGET_FRAME}</li></ul>
 */
public final class Options implements Key {
    private final ArrayMap<Option<?>, Object> values = new ArrayMap<>();

    public void putAll(Options other) {
        values.putAll((SimpleArrayMap<Option<?>, Object>) other.values);
    }

    public <T> Options set(Option<T> option, T value) {
        values.put(option, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Option<T> option) {
        return values.containsKey(option) ? (T) values.get(option) : option.getDefaultValue();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Options) {
            Options other = (Options) o;
            return values.equals(other.values);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        for (Map.Entry<Option<?>, Object> entry : values.entrySet()) {
            updateDiskCacheKey(entry.getKey(), entry.getValue(), messageDigest);
        }
    }

    @Override
    public String toString() {
        return "Options{"
                + "values=" + values
                + '}';
    }

    @SuppressWarnings("unchecked")
    private static <T> void updateDiskCacheKey(Option<T> option, Object value, MessageDigest md) {
        option.update((T) value, md);
    }
}
