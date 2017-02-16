package com.bumptech.glide.provider;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.util.Synthetic;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains an unordered list of {@link ResourceEncoder}s capable of encoding arbitrary resource types.
 * <p>
 * 包含一个任意数据类型的无序可编码的{@link ResourceEncoder}集合
 */
public class ResourceEncoderRegistry {
    // TODO: this should probably be a put.
    final List<Entry<?>> encoders = new ArrayList<>();

    public synchronized <Z> void add(Class<Z> resourceClass, ResourceEncoder<Z> encoder) {
        encoders.add(new Entry<>(resourceClass, encoder));
    }

    /**
     * 返回{@link #encoders}集合中符合参数的项
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public synchronized <Z> ResourceEncoder<Z> get(Class<Z> resourceClass) {
        int size = encoders.size();
        for (int i = 0; i < size; i++) {
            Entry<?> entry = encoders.get(i);
            if (entry.handles(resourceClass)) {
                return (ResourceEncoder<Z>) entry.encoder;
            }
        }
        // TODO: throw an exception here?
        return null;
    }

    private static final class Entry<T> {
        private final Class<T> resourceClass;
        @Synthetic
        final ResourceEncoder<T> encoder;

        Entry(Class<T> resourceClass, ResourceEncoder<T> encoder) {
            this.resourceClass = resourceClass;
            this.encoder = encoder;
        }

        @Synthetic
        boolean handles(Class<?> resourceClass) {
            return this.resourceClass.isAssignableFrom(resourceClass);
        }
    }
}
