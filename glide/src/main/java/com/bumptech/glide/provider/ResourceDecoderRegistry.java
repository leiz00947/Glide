package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.util.Synthetic;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains an ordered list of {@link ResourceDecoder}s capable of decoding arbitrary data types
 * into arbitrary resource types from highest priority decoders to lowest priority decoders.
 * <p>
 * 将一个任意数据类型的无序可解码的{@link ResourceDecoder}集合包含到按优先级从高到低解码的任意资源类型中去
 */
@SuppressWarnings("rawtypes")
public class ResourceDecoderRegistry {
    private final List<Entry<?, ?>> decoders = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public synchronized <T, R> List<ResourceDecoder<T, R>> getDecoders(Class<T> dataClass,
                                                                       Class<R> resourceClass) {
        List<ResourceDecoder<T, R>> result = new ArrayList<>();
        for (Entry<?, ?> entry : decoders) {
            if (entry.handles(dataClass, resourceClass)) {
                result.add((ResourceDecoder<T, R>) entry.decoder);
            }
        }
        // TODO: cache result list.

        return result;
    }

    /**
     * 返回{@link ResourceDecoderRegistry.Entry#resourceClass}的集合中分别和两个传参所实现的接口或
     * 所表示的类相同的列表
     */
    @SuppressWarnings("unchecked")
    public synchronized <T, R> List<Class<R>> getResourceClasses(Class<T> dataClass,
                                                                 Class<R> resourceClass) {
        List<Class<R>> result = new ArrayList<>();
        for (Entry<?, ?> entry : decoders) {
            if (entry.handles(dataClass, resourceClass)) {
                result.add((Class<R>) entry.resourceClass);
            }
        }
        return result;
    }

    public synchronized <T, R> void append(ResourceDecoder<T, R> decoder, Class<T> dataClass,
                                           Class<R> resourceClass) {
        decoders.add(new Entry<>(dataClass, resourceClass, decoder));
    }

    public synchronized <T, R> void prepend(ResourceDecoder<T, R> decoder, Class<T> dataClass,
                                            Class<R> resourceClass) {
        decoders.add(0, new Entry<>(dataClass, resourceClass, decoder));
    }

    private static class Entry<T, R> {
        private final Class<T> dataClass;
        @Synthetic
        final Class<R> resourceClass;
        @Synthetic
        final ResourceDecoder<T, R> decoder;

        public Entry(Class<T> dataClass, Class<R> resourceClass, ResourceDecoder<T, R> decoder) {
            this.dataClass = dataClass;
            this.resourceClass = resourceClass;
            this.decoder = decoder;
        }

        /**
         * 判定此Class对象所表示的类或接口与指定的Class参数所表示的类或接口是否相同，
         * 或是否是其超类或超接口
         */
        public boolean handles(Class<?> dataClass, Class<?> resourceClass) {
            return this.dataClass.isAssignableFrom(dataClass) && resourceClass
                    .isAssignableFrom(this.resourceClass);
        }
    }
}
