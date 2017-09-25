package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for pre-filling {@link Bitmap Bitmaps} in a
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}.
 * <p>
 * 预填充Bitmap到BitmapPool中去
 */
public final class BitmapPreFiller {

    private final MemoryCache memoryCache;
    private final BitmapPool bitmapPool;
    private final DecodeFormat defaultFormat;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BitmapPreFillRunner current;

    public BitmapPreFiller(MemoryCache memoryCache, BitmapPool bitmapPool,
                           DecodeFormat defaultFormat) {
        this.memoryCache = memoryCache;
        this.bitmapPool = bitmapPool;
        this.defaultFormat = defaultFormat;
    }

    /**
     * 若{@link PreFillType#getConfig()}为空，{@link #defaultFormat}为{@link DecodeFormat#PREFER_ARGB_8888}，
     * 则{@link Bitmap#setConfig(Bitmap.Config)}设置为{@link android.graphics.Bitmap.Config#ARGB_8888}，
     * 否则为{@link android.graphics.Bitmap.Config#RGB_565}
     */
    public void preFill(PreFillType.Builder... bitmapAttributeBuilders) {
        if (current != null) {
            current.cancel();
        }

        PreFillType[] bitmapAttributes = new PreFillType[bitmapAttributeBuilders.length];
        /**
         * 在这个循环里面主要是遍历每个{@link PreFillType}，然后对
         * {@link PreFillType.Builder#getConfig()}为空的对象进行重新整理
         */
        for (int i = 0; i < bitmapAttributeBuilders.length; i++) {
            PreFillType.Builder builder = bitmapAttributeBuilders[i];
            if (builder.getConfig() == null) {
                builder.setConfig(defaultFormat == DecodeFormat.PREFER_ARGB_8888
                        || defaultFormat == DecodeFormat.PREFER_ARGB_8888_DISALLOW_HARDWARE
                        ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            }
            bitmapAttributes[i] = builder.build();
        }

        PreFillQueue allocationOrder = generateAllocationOrder(bitmapAttributes);
        current = new BitmapPreFillRunner(bitmapPool, memoryCache, allocationOrder);
        handler.post(current);
    }

    // Visible for testing.
    PreFillQueue generateAllocationOrder(PreFillType... preFillSizes) {
        final int maxSize = memoryCache.getMaxSize()
                - memoryCache.getCurrentSize()
                + bitmapPool.getMaxSize();

        int totalWeight = 0;
        for (PreFillType size : preFillSizes) {
            totalWeight += size.getWeight();
        }

        /**
         * 基于闲置的内存以{@link PreFillType#weight}为单位，计算每份所分配的字节数
         */
        final float bytesPerWeight = maxSize / (float) totalWeight;

        Map<PreFillType, Integer> attributeToCount = new HashMap<>();
        for (PreFillType size : preFillSizes) {
            /**
             * 根据该{@link PreFillType}对象的{@link PreFillType#getWeight()}来获取可以分配到的内存数
             */
            int bytesForSize = Math.round(bytesPerWeight * size.getWeight());
            /**
             * 根据该{@link PreFillType}对象的宽高和存储图片的色彩模式来计算实际所需的内存字节数
             */
            int bytesPerBitmap = getSizeInBytes(size);
            /**
             * 表示基于允许分配的内存数，该图片可以申请的个数（能确保这个数不为0？）
             */
            int bitmapsForSize = bytesForSize / bytesPerBitmap;
            attributeToCount.put(size, bitmapsForSize);
        }

        return new PreFillQueue(attributeToCount);
    }

    /**
     * 返回给定宽高值的图片所占用的字节数
     */
    private static int getSizeInBytes(PreFillType size) {
        return Util.getBitmapByteSize(size.getWidth(), size.getHeight(), size.getConfig());
    }
}

