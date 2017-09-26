package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.SampleSizeRounding;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Downsamples, decodes, and rotates images according to their exif orientation.
 * <p>
 * 降低采样解码，并且根据<em>EXIF</em>的方向信息进行旋转
 */
public final class Downsampler {
    static final String TAG = "Downsampler";
    /**
     * Indicates the {@link com.bumptech.glide.load.DecodeFormat} that will be used in conjunction
     * with the image format to determine the {@link Config} to provide to
     * {@link BitmapFactory.Options#inPreferredConfig} when decoding the image.
     */
    public static final Option<DecodeFormat> DECODE_FORMAT = Option.memory(
            "com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeFormat", DecodeFormat.DEFAULT);
    /**
     * Indicates the {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option that
     * will be used to calculate the sample size to use to downsample an image given the original
     * and target dimensions of the image.
     */
    public static final Option<DownsampleStrategy> DOWNSAMPLE_STRATEGY =
            Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.DownsampleStrategy",
                    DownsampleStrategy.AT_LEAST);
    /**
     * Ensure that the size of the bitmap is fixed to the requested width and height of the
     * resource from the caller.  The final resource dimensions may differ from the requested
     * width and height, and thus setting this to true may result in the bitmap size differing
     * from the resource dimensions.
     * <p>
     * This can be used as a performance optimization for KitKat and above by fixing the size of the
     * bitmap for a collection of requested resources so that the bitmap pool will not need to
     * allocate new bitmaps for images of different sizes.
     */
    public static final Option<Boolean> FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS =
            Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.FixBitmapSize", false);

    /**
     * Indicates that it's safe or unsafe to decode {@link Bitmap}s with
     * {@link Bitmap.Config#HARDWARE}.
     * <p>
     * <p>Callers should almost never set this value to {@code true} manually. Glide will already do
     * so when Glide believes it's safe to do (when no transformations are applied). Instead, callers
     * can set this value to {@code false} to prevent Glide from decoding hardware bitmaps if Glide
     * is unable to detect that hardware bitmaps are unsafe. For example, you should set this to
     * {@code false} if you plan to draw it to a software {@link android.graphics.Canvas} or if you
     * plan to inspect the {@link Bitmap}s pixels with {@link Bitmap#getPixel(int, int)} or
     * {@link Bitmap#getPixels(int[], int, int, int, int, int, int)}.
     * <p>
     * <p>Callers can disable hardware {@link Bitmap}s for all loads using
     * {@link com.bumptech.glide.GlideBuilder#setDefaultRequestOptions(RequestOptions)}.
     * <p>
     * <p>This option is ignored unless we're on Android O+.
     */
    public static final Option<Boolean> ALLOW_HARDWARE_CONFIG =
            Option.memory(
                    "com.bumtpech.glide.load.resource.bitmap.Downsampler.AllowHardwareDecode", null);

    private static final String WBMP_MIME_TYPE = "image/vnd.wap.wbmp";
    private static final String ICO_MIME_TYPE = "image/x-ico";
    private static final Set<String> NO_DOWNSAMPLE_PRE_N_MIME_TYPES =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    WBMP_MIME_TYPE,
                                    ICO_MIME_TYPE
                            )
                    )
            );
    private static final DecodeCallbacks EMPTY_CALLBACKS = new DecodeCallbacks() {
        @Override
        public void onObtainBounds() {
            // Do nothing.
        }

        @Override
        public void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException {
            // Do nothing.
        }
    };
    private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL_PRE_KITKAT =
            Collections.unmodifiableSet(
                    EnumSet.of(
                            ImageHeaderParser.ImageType.JPEG,
                            ImageHeaderParser.ImageType.PNG_A,
                            ImageHeaderParser.ImageType.PNG
                    )
            );
    private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);
    // 5MB. This is the max image header size we can handle, we preallocate a much smaller buffer
    // but will resize up to this amount if necessary.
    private static final int MARK_POSITION = 5 * 1024 * 1024;

    private final BitmapPool bitmapPool;
    private final DisplayMetrics displayMetrics;
    private final ArrayPool byteArrayPool;
    private final List<ImageHeaderParser> parsers;
    private final HardwareConfigState hardwareConfigState = HardwareConfigState.getInstance();

    public Downsampler(List<ImageHeaderParser> parsers, DisplayMetrics displayMetrics,
                       BitmapPool bitmapPool, ArrayPool byteArrayPool) {
        this.parsers = parsers;
        this.displayMetrics = Preconditions.checkNotNull(displayMetrics);
        this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
        this.byteArrayPool = Preconditions.checkNotNull(byteArrayPool);
    }

    public boolean handles(InputStream is) {
        // We expect Downsampler to handle any available type Android supports.
        return true;
    }

    public boolean handles(ByteBuffer byteBuffer) {
        // We expect downsampler to handle any available type Android supports.
        return true;
    }

    /**
     * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
     * data present in the stream and that is downsampled according to the given dimensions and any
     * provided  {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option.
     *
     * @see #decode(InputStream, int, int, Options, DecodeCallbacks)
     */
    public Resource<Bitmap> decode(InputStream is, int outWidth, int outHeight,
                                   Options options) throws IOException {
        return decode(is, outWidth, outHeight, options, EMPTY_CALLBACKS);
    }

    /**
     * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
     * data present in the stream and that is downsampled according to the given dimensions and any
     * provided  {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option.
     * <p>
     * <p> If a Bitmap is present in the
     * {@link BitmapPool} whose dimensions exactly match
     * those of the image for the given InputStream is available, the operation is much less expensive
     * in terms of memory. </p>
     * <p>
     * <p> The provided {@link InputStream} must return <code>true</code> from
     * {@link InputStream#markSupported()} and is expected to support a reasonably large
     * mark limit to accommodate reading large image headers (~5MB). </p>
     *
     * @param is              An {@link InputStream} to the data for the image.
     * @param requestedWidth  The width the final image should be close to.
     * @param requestedHeight The height the final image should be close to.
     * @param options         A set of options that may contain one or more supported options that influence
     *                        how a Bitmap will be decoded from the given stream.
     * @param callbacks       A set of callbacks allowing callers to optionally respond to various
     *                        significant events during the decode process.
     * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is
     * not null.
     */
    @SuppressWarnings("resource")
    public Resource<Bitmap> decode(InputStream is, int requestedWidth, int requestedHeight,
                                   Options options, DecodeCallbacks callbacks) throws IOException {
        Preconditions.checkArgument(is.markSupported(), "You must provide an InputStream that supports"
                + " mark()");

        byte[] bytesForOptions = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
        BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();
        bitmapFactoryOptions.inTempStorage = bytesForOptions;

        DecodeFormat decodeFormat = options.get(DECODE_FORMAT);
        DownsampleStrategy downsampleStrategy = options.get(DOWNSAMPLE_STRATEGY);
        boolean fixBitmapToRequestedDimensions = options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS);
        boolean isHardwareConfigAllowed =
                options.get(ALLOW_HARDWARE_CONFIG) != null && options.get(ALLOW_HARDWARE_CONFIG);
        if (decodeFormat == DecodeFormat.PREFER_ARGB_8888_DISALLOW_HARDWARE) {
            isHardwareConfigAllowed = false;
        }

        try {
            Bitmap result = decodeFromWrappedStreams(is, bitmapFactoryOptions,
                    downsampleStrategy, decodeFormat, isHardwareConfigAllowed, requestedWidth,
                    requestedHeight, fixBitmapToRequestedDimensions, callbacks);
            return BitmapResource.obtain(result, bitmapPool);
        } finally {
            releaseOptions(bitmapFactoryOptions);
            byteArrayPool.put(bytesForOptions, byte[].class);
        }
    }

    /**
     * 返回缩放并旋转后的图片
     */
    private Bitmap decodeFromWrappedStreams(InputStream is,
                                            BitmapFactory.Options options, DownsampleStrategy downsampleStrategy,
                                            DecodeFormat decodeFormat, boolean isHardwareConfigAllowed, int requestedWidth,
                                            int requestedHeight, boolean fixBitmapToRequestedDimensions,
                                            DecodeCallbacks callbacks) throws IOException {

        int[] sourceDimensions = getDimensions(is, options, callbacks, bitmapPool);
        int sourceWidth = sourceDimensions[0];
        int sourceHeight = sourceDimensions[1];
        String sourceMimeType = options.outMimeType;

        int orientation = ImageHeaderParserUtils.getOrientation(parsers, is, byteArrayPool);
        /**
         * 获取图片旋转度数
         */
        int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
        boolean isExifOrientationRequired = TransformationUtils.isExifOrientationRequired(orientation);

        int targetWidth = requestedWidth == Target.SIZE_ORIGINAL ? sourceWidth : requestedWidth;
        int targetHeight = requestedHeight == Target.SIZE_ORIGINAL ? sourceHeight : requestedHeight;

        calculateScaling(downsampleStrategy, degreesToRotate, sourceWidth, sourceHeight, targetWidth,
                targetHeight, options);
        calculateConfig(
                is,
                decodeFormat,
                isHardwareConfigAllowed,
                isExifOrientationRequired,
                options,
                targetWidth,
                targetHeight);

        boolean isKitKatOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
        if ((options.inSampleSize == 1 || isKitKatOrGreater)
                && shouldUsePool(is)) {
            int expectedWidth;
            int expectedHeight;
            if (fixBitmapToRequestedDimensions && isKitKatOrGreater) {
                expectedWidth = targetWidth;
                expectedHeight = targetHeight;
            } else {
                float densityMultiplier = isScaling(options)
                        ? (float) options.inTargetDensity / options.inDensity : 1f;
                int sampleSize = options.inSampleSize;
                int downsampledWidth = (int) Math.ceil(sourceWidth / (float) sampleSize);
                int downsampledHeight = (int) Math.ceil(sourceHeight / (float) sampleSize);
                /**
                 * 根据不同密度的设备来计算图片的实际宽高值（单位：px）
                 */
                expectedWidth = Math.round(downsampledWidth * densityMultiplier);
                expectedHeight = Math.round(downsampledHeight * densityMultiplier);

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Calculated target [" + expectedWidth + "x" + expectedHeight + "] for source"
                            + " [" + sourceWidth + "x" + sourceHeight + "]"
                            + ", sampleSize: " + sampleSize
                            + ", targetDensity: " + options.inTargetDensity
                            + ", density: " + options.inDensity
                            + ", density multiplier: " + densityMultiplier);
                }
            }
            // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
            // will be -1 here.
            if (expectedWidth > 0 && expectedHeight > 0) {
                setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
            }
        }
        Bitmap downsampled = decodeStream(is, options, callbacks, bitmapPool);
        callbacks.onDecodeComplete(bitmapPool, downsampled);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logDecode(sourceWidth, sourceHeight, sourceMimeType, options, downsampled,
                    requestedWidth, requestedHeight);
        }

        Bitmap rotated = null;
        if (downsampled != null) {
            // If we scaled, the Bitmap density will be our inTargetDensity. Here we correct it back to
            // the expected density dpi.
            downsampled.setDensity(displayMetrics.densityDpi);

            rotated = TransformationUtils.rotateImageExif(bitmapPool, downsampled, orientation);
            if (!downsampled.equals(rotated)) {
                bitmapPool.put(downsampled);
            }
        }

        return rotated;
    }

    /**
     * 设计图片缩放的相关属性，即{@link BitmapFactory.Options#inSampleSize}、
     * {@link BitmapFactory.Options#inScaled}、{@link BitmapFactory.Options#inDensity}、
     * {@link BitmapFactory.Options#inTargetDensity}
     */
    // Visible for testing.
    static void calculateScaling(DownsampleStrategy downsampleStrategy,
                                 int degreesToRotate,
                                 int sourceWidth, int sourceHeight, int targetWidth, int targetHeight,
                                 BitmapFactory.Options options) {
        // We can't downsample source content if we can't determine its dimensions.
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }

        /**
         * 解码后的图片相较于原始图片的缩放系数
         */
        final float exactScaleFactor;
        if (degreesToRotate == 90 || degreesToRotate == 270) {
            // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image
            // width is decreased to near our target's height and the image height is decreased to near
            // our target width.
            //noinspection SuspiciousNameCombination
            exactScaleFactor = downsampleStrategy.getScaleFactor(sourceHeight, sourceWidth,
                    targetWidth, targetHeight);
        } else {
            exactScaleFactor = downsampleStrategy.getScaleFactor(sourceWidth, sourceHeight,
                    targetWidth, targetHeight);
        }

        if (exactScaleFactor <= 0f) {
            throw new IllegalArgumentException("Cannot scale with factor: " + exactScaleFactor
                    + " from: " + downsampleStrategy);
        }
        SampleSizeRounding rounding = downsampleStrategy.getSampleSizeRounding(sourceWidth,
                sourceHeight, targetWidth, targetHeight);
        if (rounding == null) {
            throw new IllegalArgumentException("Cannot round with null rounding");
        }

        int outWidth = (int) (exactScaleFactor * sourceWidth + 0.5f);
        int outHeight = (int) (exactScaleFactor * sourceHeight + 0.5f);

        int widthScaleFactor = sourceWidth / outWidth;
        int heightScaleFactor = sourceHeight / outHeight;

        int scaleFactor = rounding == SampleSizeRounding.MEMORY
                ? Math.max(widthScaleFactor, heightScaleFactor)
                : Math.min(widthScaleFactor, heightScaleFactor);

        /**
         * 用来设置{@link BitmapFactory.Options#inSampleSize}值
         */
        int powerOfTwoSampleSize;
        // BitmapFactory does not support downsampling wbmp files on platforms <= M. See b/27305903.
        if (Build.VERSION.SDK_INT <= 23
                && NO_DOWNSAMPLE_PRE_N_MIME_TYPES.contains(options.outMimeType)) {
            powerOfTwoSampleSize = 1;
        } else {
            /**
             * {@link Integer#highestOneBit(int)}的作用：
             * <p>
             * <ol><li>如果传参为0，则返回0</li>
             * <li>如果传参为负数，则返回-2147483648：[1000,0000,0000,0000,0000,0000,0000,0000]
             * （二进制表示的数）</li>
             * <li>如果传参为正数，返回的则是跟它最靠近的比它小的2的N次方</li></ol>
             */
            powerOfTwoSampleSize = Math.max(1, Integer.highestOneBit(scaleFactor));
            if (rounding == SampleSizeRounding.MEMORY
                    && powerOfTwoSampleSize < (1.f / exactScaleFactor)) {
                /**
                 * <<：左移运算符，num << 1,相当于num乘以2
                 * <p>
                 * >>：右移运算符，num >> 1,相当于num除以2
                 */
                powerOfTwoSampleSize = powerOfTwoSampleSize << 1;
            }
        }

        float adjustedScaleFactor = powerOfTwoSampleSize * exactScaleFactor;

        options.inSampleSize = powerOfTwoSampleSize;
        // Density scaling is only supported if inBitmap is null prior to KitKat. Avoid setting
        // densities here so we calculate the final Bitmap size correctly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            options.inTargetDensity = (int) (1000 * adjustedScaleFactor + 0.5f);
            options.inDensity = 1000;
        }
        if (isScaling(options)) {
            options.inScaled = true;
        } else {
            options.inDensity = options.inTargetDensity = 0;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Calculate scaling"
                    + ", source: [" + sourceWidth + "x" + sourceHeight + "]"
                    + ", target: [" + targetWidth + "x" + targetHeight + "]"
                    + ", exact scale factor: " + exactScaleFactor
                    + ", power of 2 sample size: " + powerOfTwoSampleSize
                    + ", adjusted scale factor: " + adjustedScaleFactor
                    + ", target density: " + options.inTargetDensity
                    + ", density: " + options.inDensity);
        }
    }

    private boolean shouldUsePool(InputStream is) throws IOException {
        // On KitKat+, any bitmap (of a given config) can be used to decode any other bitmap
        // (with the same config).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return true;
        }

        try {
            ImageHeaderParser.ImageType type = ImageHeaderParserUtils.getType(parsers, is, byteArrayPool);
            // We cannot reuse bitmaps when decoding images that are not PNG or JPG prior to KitKat.
            // See: https://groups.google.com/forum/#!msg/android-developers/Mp0MFVFi1Fo/e8ZQ9FGdWdEJ
            return TYPES_THAT_USE_POOL_PRE_KITKAT.contains(type);
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Cannot determine the image type from header", e);
            }
        }
        return false;
    }

    private void calculateConfig(
            InputStream is,
            DecodeFormat format,
            boolean isHardwareConfigAllowed,
            boolean isExifOrientationRequired,
            BitmapFactory.Options optionsWithScaling,
            int targetWidth,
            int targetHeight)
            throws IOException {

        if (hardwareConfigState.setHardwareConfigIfAllowed(
                targetWidth,
                targetHeight,
                optionsWithScaling,
                format,
                isHardwareConfigAllowed,
                isExifOrientationRequired)) {
            return;
        }

        // Changing configs can cause skewing on 4.1, see issue #128.
        if (format == DecodeFormat.PREFER_ARGB_8888
                || format == DecodeFormat.PREFER_ARGB_8888_DISALLOW_HARDWARE
                || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            optionsWithScaling.inPreferredConfig = Config.ARGB_8888;
            return;
        }

        boolean hasAlpha = false;
        try {
            hasAlpha = ImageHeaderParserUtils.getType(parsers, is, byteArrayPool).hasAlpha();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Cannot determine whether the image has alpha or not from header"
                        + ", format " + format, e);
            }
        }

        optionsWithScaling.inPreferredConfig =
                hasAlpha ? Config.ARGB_8888 : Config.RGB_565;
        if (optionsWithScaling.inPreferredConfig == Config.RGB_565
                || optionsWithScaling.inPreferredConfig == Config.ARGB_4444
                || optionsWithScaling.inPreferredConfig == Config.ALPHA_8) {
            optionsWithScaling.inDither = true;
        }
    }

    /**
     * A method for getting the dimensions of an image from the given InputStream.
     * <p>
     * 获取图片的宽度和高度值组成的数组
     *
     * @param is      The InputStream representing the image.
     * @param options The options to pass to {@link BitmapFactory#decodeStream(InputStream,
     *                android.graphics.Rect, BitmapFactory.Options)}.
     * @return an array containing the dimensions of the image in the form {width, height}.
     */
    private static int[] getDimensions(InputStream is, BitmapFactory.Options options,
                                       DecodeCallbacks decodeCallbacks, BitmapPool bitmapPool) throws IOException {
        options.inJustDecodeBounds = true;
        decodeStream(is, options, decodeCallbacks, bitmapPool);
        options.inJustDecodeBounds = false;
        return new int[]{options.outWidth, options.outHeight};
    }

    private static Bitmap decodeStream(InputStream is, BitmapFactory.Options options,
                                       DecodeCallbacks callbacks, BitmapPool bitmapPool) throws IOException {
        if (options.inJustDecodeBounds) {
            is.mark(MARK_POSITION);
        } else {
            // Once we've read the image header, we no longer need to allow the buffer to expand in
            // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
            // is no larger than our current buffer size here. We need to do so immediately before
            // decoding the full image to avoid having our mark limit overridden by other calls to
            // markand reset. See issue #225.
            callbacks.onObtainBounds();
        }
        // BitmapFactory.Options out* variables are reset by most calls to decodeStream, successful or
        // otherwise, so capture here in case we log below.
        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;
        String outMimeType = options.outMimeType;
        final Bitmap result;
        TransformationUtils.getBitmapDrawableLock().lock();
        try {
            result = BitmapFactory.decodeStream(is, null, options);
        } catch (IllegalArgumentException e) {
            IOException bitmapAssertionException =
                    newIoExceptionForInBitmapAssertion(e, sourceWidth, sourceHeight, outMimeType, options);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to decode with inBitmap, trying again without Bitmap re-use",
                        bitmapAssertionException);
            }
            if (options.inBitmap != null) {
                try {
                    is.reset();
                    bitmapPool.put(options.inBitmap);
                    options.inBitmap = null;
                    return decodeStream(is, options, callbacks, bitmapPool);
                } catch (IOException resetException) {
                    throw bitmapAssertionException;
                }
            }
            throw bitmapAssertionException;
        } finally {
            TransformationUtils.getBitmapDrawableLock().unlock();
        }

        if (options.inJustDecodeBounds) {
            is.reset();
        }
        return result;
    }

    private static boolean isScaling(BitmapFactory.Options options) {
        return options.inTargetDensity > 0 && options.inDensity > 0
                && options.inTargetDensity != options.inDensity;
    }

    private static void logDecode(int sourceWidth, int sourceHeight, String outMimeType,
                                  BitmapFactory.Options options, Bitmap result, int requestedWidth, int requestedHeight) {
        Log.v(TAG, "Decoded " + getBitmapString(result)
                + " from [" + sourceWidth + "x" + sourceHeight + "] " + outMimeType
                + " with inBitmap " + getInBitmapString(options)
                + " for [" + requestedWidth + "x" + requestedHeight + "]"
                + ", sample size: " + options.inSampleSize
                + ", density: " + options.inDensity
                + ", target density: " + options.inTargetDensity
                + ", thread: " + Thread.currentThread().getName());
    }

    private static String getInBitmapString(BitmapFactory.Options options) {
        return getBitmapString(options.inBitmap);
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getBitmapString(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        String sizeString = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                ? " (" + bitmap.getAllocationByteCount() + ")" : "";
        return "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig()
                + sizeString;
    }

    // BitmapFactory throws an IllegalArgumentException if any error occurs attempting to decode a
    // file when inBitmap is non-null, including those caused by partial or corrupt data. We still log
    // the error because the IllegalArgumentException is supposed to catch errors reusing Bitmaps, so
    // want some useful log output. In most cases this can be safely treated as a normal IOException.
    private static IOException newIoExceptionForInBitmapAssertion(IllegalArgumentException e,
                                                                  int outWidth, int outHeight, String outMimeType, BitmapFactory.Options options) {
        return new IOException("Exception decoding bitmap"
                + ", outWidth: " + outWidth
                + ", outHeight: " + outHeight
                + ", outMimeType: " + outMimeType
                + ", inBitmap: " + getInBitmapString(options), e);
    }

    /**
     * 设置{@link BitmapFactory.Options#inBitmap}属性
     */
    // Avoid short circuiting SDK checks.
    @SuppressWarnings("PMD.CollapsibleIfStatements")
    @TargetApi(Build.VERSION_CODES.O)
    private static void setInBitmap(BitmapFactory.Options options, BitmapPool bitmapPool, int width,
                                    int height) {
        // Avoid short circuiting, it appears to break on some devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (options.inPreferredConfig == Config.HARDWARE) {
                return;
            }
        }
        // BitmapFactory will clear out the Bitmap before writing to it, so getDirty is safe.
        options.inBitmap = bitmapPool.getDirty(width, height, options.inPreferredConfig);
    }

    /**
     * 获取{@link #OPTIONS_QUEUE}队列头的{@link BitmapFactory.Options}，若为空，则重新创建一个，
     * 并将其中属性重置
     */
    private static synchronized BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options decodeBitmapOptions;
        synchronized (OPTIONS_QUEUE) {
            /**
             * 获取并移除此队列的头
             */
            decodeBitmapOptions = OPTIONS_QUEUE.poll();
        }
        if (decodeBitmapOptions == null) {
            decodeBitmapOptions = new BitmapFactory.Options();
            resetOptions(decodeBitmapOptions);
        }

        return decodeBitmapOptions;
    }

    private static void releaseOptions(BitmapFactory.Options decodeBitmapOptions) {
        resetOptions(decodeBitmapOptions);
        synchronized (OPTIONS_QUEUE) {
            OPTIONS_QUEUE.offer(decodeBitmapOptions);
        }
    }

    /**
     * 重置{@link BitmapFactory.Options}选项
     */
    private static void resetOptions(BitmapFactory.Options decodeBitmapOptions) {
        /**
         * 设置临时存储的字节数组
         */
        decodeBitmapOptions.inTempStorage = null;
        /**
         * 这个值和抖动解码有关，默认值为false，表示不采用抖动解码
         */
        decodeBitmapOptions.inDither = false;
        /**
         * 设置这个Bitmap是否可以被缩放，默认值是true，表示可以被缩放
         */
        decodeBitmapOptions.inScaled = false;
        /**
         * 按比例缩小为原图的{@code 1/inSampleSize}
         */
        decodeBitmapOptions.inSampleSize = 1;
        /**
         * 这个值是设置色彩模式，默认值是ARGB_8888，在这个模式下，一个像素点占用4bytes空间，
         * 一般对透明度不做要求的话，一般采用RGB_565模式，这个模式下一个像素点占用2bytes
         */
        decodeBitmapOptions.inPreferredConfig = null;
        /**
         * 如果将这个值置为true，那么在解码的时候将不会返回bitmap，只会返回这个bitmap的尺寸；
         * 这个属性的目的是，如果你只想知道一个bitmap的尺寸，但又不想将其加载到内存时
         */
        decodeBitmapOptions.inJustDecodeBounds = false;
        /**
         * 表示这个bitmap的像素密度（对应的是DisplayMetrics中的densityDpi，不是density），
         * 默认值为160，即在Density=160的设备上1dp=1px
         */
        decodeBitmapOptions.inDensity = 0;
        /**
         * 表示要被画出来时的目标像素密度（对应的是DisplayMetrics中的densityDpi，不是density），
         * 默认值为当前设备的{@link DisplayMetrics#densityDpi}
         */
        decodeBitmapOptions.inTargetDensity = 0;
        /**
         * 表示这个Bitmap的宽度值
         */
        decodeBitmapOptions.outWidth = 0;
        /**
         * 表示这个Bitmap的高度值
         */
        decodeBitmapOptions.outHeight = 0;
        /**
         * 表示图片类型
         */
        decodeBitmapOptions.outMimeType = null;

        /**
         * 使用{@link BitmapFactory.Options#inBitmap}前，每创建一个{@link Bitmap}需要独占一块内存，
         * 使用{@link BitmapFactory.Options#inBitmap}后，多个{@link Bitmap}会复用同一块内存。
         * <p>
         * 所以使用{@link BitmapFactory.Options#inBitmap}能够大大提高内存的利用效率，
         * 但是它也有几个限制条件：
         * <ol><li>在SDK 11 -> 18之间，重用的{@link Bitmap}大小必须是一致的，
         * 例如给{@link BitmapFactory.Options#inBitmap}赋值的图片大小为100-100，
         * 那么新申请的{@link Bitmap}必须也为100-100才能够被重用。从SDK 19开始，
         * 新申请的{@link Bitmap}大小必须小于或者等于已经赋值过的{@link Bitmap}大小</li>
         * <li>新申请的{@link Bitmap}与旧的{@link Bitmap}必须有相同的解码格式，例如大家都是
         * {@link Config#ARGB_8888}的，如果前面的{@link Bitmap}是
         * {@link Config#ARGB_8888}，那么就不能支持{@link Config#ARGB_4444}
         * 与{@link Config#RGB_565}格式的{@link Bitmap}了，
         * 不过可以通过创建一个包含多种典型可重用{@link Bitmap}的对象池，
         * 这样后续的{@link Bitmap}创建都能够找到合适的“模板”去进行重用</li>
         * <li>{@link BitmapFactory.Options#inMutable}要设置为{@code true}</li></ol>
         */
        decodeBitmapOptions.inBitmap = null;
        /**
         * 配置Bitmap是否可以更改，比如：在Bitmap上隔几个像素加一条线段
         */
        decodeBitmapOptions.inMutable = true;
    }

    /**
     * Callbacks for key points during decodes.
     */
    public interface DecodeCallbacks {
        void onObtainBounds();

        void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException;
    }
}
