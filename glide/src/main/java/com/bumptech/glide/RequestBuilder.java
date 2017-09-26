package com.bumptech.glide;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.SingleRequest;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.target.PreloadTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ApplicationVersionSignature;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.net.URL;
import java.util.UUID;

import static com.bumptech.glide.request.RequestOptions.signatureOf;

/**
 * A generic class that can handle setting options and staring loads for generic resource types.
 * <p>
 * 一个可以设置选项和加载通用资源类型的类
 *
 * @param <TranscodeType> The type of resource that will be delivered to the {@link Target}.
 *                        <p>要被递送到{@link Target}的资源类型</p>
 */
public class RequestBuilder<TranscodeType> implements Cloneable {
    // Used in generated subclasses
    protected static final RequestOptions DOWNLOAD_ONLY_OPTIONS =
            new RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW)
                    .skipMemoryCache(true);

    private final GlideContext context;
    private final RequestManager requestManager;
    /**
     * 用来确定加载图片到一个{@link ImageView}时容器的类型，该变量即表示存放加载资源的容器的类
     * （如{@link android.graphics.drawable.Drawable}、{@link android.graphics.Bitmap}、
     * {@link File}等）
     *
     * @see {@link com.bumptech.glide.request.target.ImageViewTargetFactory#buildTarget(ImageView, Class)}
     */
    private final Class<TranscodeType> transcodeClass;
    private final RequestOptions defaultRequestOptions;
    private final Glide glide;

    @NonNull
    protected RequestOptions requestOptions;

    @NonNull
    @SuppressWarnings("unchecked")
    private TransitionOptions<?, ? super TranscodeType> transitionOptions;

    @Nullable
    private Object model;
    // model may occasionally be null, so to enforce that load() was called, put a boolean rather
    // than relying on model not to be null.
    @Nullable
    private RequestListener<TranscodeType> requestListener;
    @Nullable
    private RequestBuilder<TranscodeType> thumbnailBuilder;
    @Nullable
    private Float thumbSizeMultiplier;
    private boolean isDefaultTransitionOptionsSet = true;
    private boolean isModelSet;
    private boolean isThumbnailBuilt;

    protected RequestBuilder(Glide glide, RequestManager requestManager,
                             Class<TranscodeType> transcodeClass) {
        this.glide = glide;
        this.requestManager = requestManager;
        this.context = glide.getGlideContext();
        this.transcodeClass = transcodeClass;
        this.defaultRequestOptions = requestManager.getDefaultRequestOptions();
        this.transitionOptions = requestManager.getDefaultTransitionOptions(transcodeClass);
        this.requestOptions = defaultRequestOptions;
    }

    protected RequestBuilder(Class<TranscodeType> transcodeClass, RequestBuilder<?> other) {
        this(other.glide, other.requestManager, transcodeClass);
        model = other.model;
        isModelSet = other.isModelSet;
        requestOptions = other.requestOptions;
    }

    /**
     * Applies the given options to the request, options set or unset in the given options will
     * replace those previously set in options in this class.
     * <p>
     * 申请一个新的Glide配置对象并赋值给{@link #requestOptions}
     *
     * @return This request builder.
     * @see RequestOptions#apply(RequestOptions)
     */
    public RequestBuilder<TranscodeType> apply(@NonNull RequestOptions requestOptions) {
        Preconditions.checkNotNull(requestOptions);
        this.requestOptions = getMutableOptions().apply(requestOptions);
        return this;
    }

    protected RequestOptions getMutableOptions() {
        return defaultRequestOptions == this.requestOptions
                ? this.requestOptions.clone() : this.requestOptions;
    }

    /**
     * Sets the {@link TransitionOptions} to use to transition from the placeholder or thumbnail when
     * this load completes.
     * <p>
     * 设置当加载图片成功时控制从{@link RequestOptions#placeholderDrawable}或缩略图过渡到请求规格
     * 图片的{@link TransitionOptions}实例
     * <p>
     * The given {@link TransitionOptions} will replace any {@link TransitionOptions} set
     * previously.
     *
     * @return This request builder.
     */
    public RequestBuilder<TranscodeType> transition(
            @NonNull TransitionOptions<?, ? super TranscodeType> transitionOptions) {
        this.transitionOptions = Preconditions.checkNotNull(transitionOptions);
        isDefaultTransitionOptionsSet = false;
        return this;
    }

    /**
     * Sets a RequestBuilder listener to monitor the resource load. It's best to create a single
     * instance of an exception handler per type of request (usually activity/fragment) rather than
     * pass one in per request to avoid some redundant object allocation.
     * <p>
     * 设置一个在图片加载过程中，在显示界面做相应处理的监听器；最好是给每种类型的请求设置一个监听器，
     * 而不是给每个请求都设置一个监听器，从而导致资源浪费
     *
     * @param requestListener The request listener to use.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<TranscodeType> listener(
            @Nullable RequestListener<TranscodeType> requestListener) {
        this.requestListener = requestListener;
        return this;
    }

    /**
     * Loads and displays the resource retrieved by the given thumbnail request if it finishes before
     * this request. Best used for loading thumbnail resources that are smaller and will be loaded
     * more quickly than the full size resource. There are no guarantees about the order in which the
     * requests will actually finish. However, if the thumb request completes after the full request,
     * the thumb resource will never replace the full resource.
     *
     * @param thumbnailRequest The request to use to load the thumbnail.
     * @return This request builder.
     * @see #thumbnail(float)
     * <p>
     * <p> Recursive calls to thumbnail are supported. </p>
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<TranscodeType> thumbnail(
            @Nullable RequestBuilder<TranscodeType> thumbnailRequest) {
        this.thumbnailBuilder = thumbnailRequest;

        return this;
    }

    /**
     * Loads a resource in an identical manner to this request except with the dimensions of the
     * target multiplied by the given size multiplier. If the thumbnail load completes before the full
     * size load, the thumbnail will be shown. If the thumbnail load completes after the full size
     * load, the thumbnail will not be shown.
     * <p>
     * <p> Note - The thumbnail resource will be smaller than the size requested so the target (or
     * {@link ImageView}) must be able to scale the thumbnail appropriately. See
     * {@link ImageView.ScaleType}. </p>
     * <p>
     * <p> Almost all options will be copied from the original load, including the {@link
     * com.bumptech.glide.load.model.ModelLoader}, {@link com.bumptech.glide.load.ResourceDecoder},
     * and {@link com.bumptech.glide.load.Transformation}s. However,
     * {@link RequestOptions#placeholder(int)} and
     * {@link RequestOptions#error(int)}, and
     * {@link #listener(RequestListener)} will only be used on the full size load and will not be
     * copied for the thumbnail load. </p>
     * <p>
     * <p> Recursive calls to thumbnail are supported. </p>
     *
     * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading
     *                       the thumbnail.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<TranscodeType> thumbnail(float sizeMultiplier) {
        if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.thumbSizeMultiplier = sizeMultiplier;

        return this;
    }

    /**
     * Sets the specific model to load data for.
     * <p>
     * <p> This method must be called at least once before
     * {@link #into(Target)} is called. </p>
     *
     * @param model The model to load data for, or null.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<TranscodeType> load(@Nullable Object model) {
        return loadGeneric(model);
    }

    private RequestBuilder<TranscodeType> loadGeneric(@Nullable Object model) {
        this.model = model;
        isModelSet = true;
        return this;
    }

    /**
     * Returns a request builder to load the given {@link String}. signature.
     * <p>
     * <p> Note - this method caches data using only the given String as the cache key. If the data is
     * a Uri outside of your control, or you otherwise expect the data represented by the given String
     * to change without the String identifier changing, Consider using
     * {@link RequestOptions#signature(com.bumptech.glide.load.Key)} to
     * mixin a signature you create that identifies the data currently at the given String that will
     * invalidate the cache if that data changes. Alternatively, using
     * {@link DiskCacheStrategy#NONE} and/or
     * {@link RequestOptions#skipMemoryCache(boolean)} may be
     * appropriate.
     * </p>
     *
     * @param string A file path, or a uri or url handled by
     *               {@link com.bumptech.glide.load.model.UriLoader}.
     * @see #load(Object)
     */
    public RequestBuilder<TranscodeType> load(@Nullable String string) {
        return loadGeneric(string);
    }

    /**
     * Returns a request builder to load the given {@link Uri}.
     * <p>
     * <p> Note - this method caches data at Uris using only the Uri itself as the cache key. The data
     * represented by Uris from some content providers may change without the Uri changing, which
     * means using this method can lead to displaying stale data. Consider using
     * {@link RequestOptions#signature(com.bumptech.glide.load.Key)} to
     * mixin a signature you create based on the data at the given Uri that will invalidate the cache
     * if that data changes. Alternatively, using
     * {@link DiskCacheStrategy#NONE} and/or
     * {@link RequestOptions#skipMemoryCache(boolean)} may be
     * appropriate. </p>
     *
     * @param uri The Uri representing the image. Must be of a type handled by
     *            {@link com.bumptech.glide.load.model.UriLoader}.
     * @see #load(Object)
     */
    public RequestBuilder<TranscodeType> load(@Nullable Uri uri) {
        return loadGeneric(uri);
    }

    /**
     * Returns a request builder to load the given {@link File}.
     * <p>
     * <p> Note - this method caches data for Files using only the file path itself as the cache key.
     * The data in the File can change so using this method can lead to displaying stale data. If you
     * expect the data in the File to change, Consider using
     * {@link RequestOptions#signature(com.bumptech.glide.load.Key)}
     * to mixin a signature you create that identifies the data currently in the File that will
     * invalidate the cache if that data changes. Alternatively, using
     * {@link DiskCacheStrategy#NONE} and/or
     * {@link RequestOptions#skipMemoryCache(boolean)} may be
     * appropriate.
     * </p>
     *
     * @param file The File containing the image
     * @see #load(Object)
     */
    public RequestBuilder<TranscodeType> load(@Nullable File file) {
        return loadGeneric(file);
    }

    /**
     * Returns a request builder to load the given resource id. Returns a request builder that uses
     * the {@link com.bumptech.glide.load.model.ModelLoaderFactory} currently registered or
     * {@link Integer} to load the image represented by the given {@link Integer} resource id.
     * Defaults to {@link com.bumptech.glide.load.model.ResourceLoader} to load resource id models.
     * <p>
     * <p> By default this method adds a version code based signature to the cache key used to cache
     * this resource in Glide. This signature is sufficient to guarantee that end users will see the
     * most up to date versions of your Drawables, but during development if you do not increment your
     * version code before each install and you replace a Drawable with different data without
     * changing the Drawable name, you may see inconsistent cached data. To get around this, consider
     * using {@link DiskCacheStrategy#NONE} via
     * {@link RequestOptions#diskCacheStrategy(DiskCacheStrategy)}
     * during development, and re-enabling the default
     * {@link DiskCacheStrategy#RESOURCE} for release builds. </p>
     *
     * @see #load(Integer)
     * @see ApplicationVersionSignature
     */
    public RequestBuilder<TranscodeType> load(@Nullable Integer resourceId) {
        return loadGeneric(resourceId).apply(signatureOf(ApplicationVersionSignature.obtain(context)));
    }

    /**
     * Returns a request builder to load the given {@link URL}.
     *
     * @param url The URL representing the image.
     * @see #load(Object)
     * @deprecated The {@link URL} class has <a href="http://goo.gl/c4hHNu">a number of
     * performance problems</a> and should generally be avoided when possible. Prefer
     * {@link #load(Uri)} or {@link #load(String)}.
     */
    @Deprecated
    public RequestBuilder<TranscodeType> load(@Nullable URL url) {
        return loadGeneric(url);
    }

    /**
     * Returns a request to load the given byte array.
     * <p>
     * <p> Note - by default loads for bytes are not cached in either the memory or the disk cache.
     * </p>
     *
     * @param model the data to load.
     * @see #load(Object)
     */
    public RequestBuilder<TranscodeType> load(@Nullable byte[] model) {
        return loadGeneric(model).apply(signatureOf(new ObjectKey(UUID.randomUUID().toString()))
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true /*skipMemoryCache*/));
    }

    /**
     * Returns a copy of this request builder with all of the options put so far on this builder.
     * <p>
     * <p> This method returns a "deep" copy in that all non-immutable arguments are copied such that
     * changes to one builder will not affect the other builder. However, in addition to immutable
     * arguments, the current model is not copied copied so changes to the model will affect both
     * builders. </p>
     */
    @SuppressWarnings("unchecked")
    @Override
    public RequestBuilder<TranscodeType> clone() {
        try {
            RequestBuilder<TranscodeType> result = (RequestBuilder<TranscodeType>) super.clone();
            result.requestOptions = result.requestOptions.clone();
            result.transitionOptions = result.transitionOptions.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the target the resource will be loaded into.
     * <p>
     * 设置需要加载资源的容器，若设置的容器已经持有一个请求了，那么还得将之前这个请求清除掉，
     * 然后执行当前的请求
     *
     * @param target The target to load the resource into.
     * @return The given target.
     * @see RequestManager#clear(Target)
     */
    public <Y extends Target<TranscodeType>> Y into(@NonNull Y target) {
        Util.assertMainThread();
        Preconditions.checkNotNull(target);
        if (!isModelSet) {
            throw new IllegalArgumentException("You must call #load() before calling #into()");
        }

        requestOptions.lock();
        Request request = buildRequest(target);

        Request previous = target.getRequest();
        // When request was failed or cancelled, be sure to use the updated model as it can contains
        // unexposed data that could help the request to succeed on restart.
        // See https://github.com/bumptech/glide/issues/2270
        if (request.isEquivalentTo(previous)
                && (Preconditions.checkNotNull(previous).isComplete()
                || Preconditions.checkNotNull(previous).isRunning())) {
            request.recycle();
            // If the request is completed, beginning again will ensure the result is re-delivered,
            // triggering RequestListeners and Targets. If the request is already
            // running, we can let it continue running without interruption.
            if (!Preconditions.checkNotNull(previous).isRunning()) {
                previous.begin();
            }
            return target;
        }

        requestManager.clear(target);
        target.setRequest(request);
        requestManager.track(target, request);
        return target;
    }

    /**
     * Sets the {@link ImageView} the resource will be loaded into, cancels any existing loads into
     * the view, and frees any resources Glide may have previously loaded into the view so they may be
     * reused.
     *
     * @param view The view to cancel previous loads for and load the new resource into.
     * @return The
     * {@link Target} used to wrap the given {@link ImageView}.
     * @see RequestManager#clear(Target)
     * @see com.bumptech.glide.request.target.ImageViewTargetFactory#buildTarget(ImageView, Class)
     */
    public Target<TranscodeType> into(ImageView view) {
        Util.assertMainThread();
        Preconditions.checkNotNull(view);

        if (!requestOptions.isTransformationSet()
                && requestOptions.isTransformationAllowed()
                && view.getScaleType() != null) {
            if (requestOptions.isLocked()) {
                requestOptions = requestOptions.clone();
            }
            switch (view.getScaleType()) {
                case CENTER_CROP:
                    requestOptions.optionalCenterCrop();
                    break;
                case CENTER_INSIDE:
                    requestOptions.optionalCenterInside();
                    break;
                case FIT_CENTER:
                case FIT_START:
                case FIT_END:
                    requestOptions.optionalFitCenter();
                    break;
                case FIT_XY:
                    requestOptions.optionalCenterInside();
                    break;
                case CENTER:
                case MATRIX:
                default:
                    // Do nothing.
            }
        }

        return into(context.buildImageViewTarget(view, transcodeClass));
    }

    /**
     * Returns a future that can be used to do a blocking get on a background thread.
     *
     * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
     *               overridden by
     *               {@link RequestOptions#override(int, int)} if
     *               previously called.
     * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
     *               overridden by
     *               {@link RequestOptions#override(int, int)}} if
     *               previously called).
     * @see RequestManager#clear(Target)
     * @deprecated Use {@link #submit(int, int)} instead.
     */
    @Deprecated
    public FutureTarget<TranscodeType> into(int width, int height) {
        return submit(width, height);
    }

    /**
     * Returns a future that can be used to do a blocking get on a background thread.
     * <p>
     * <p>This method defaults to {@link Target#SIZE_ORIGINAL} for the width and the height. However,
     * since the width and height will be overridden by values passed to {@link
     * RequestOptions#override(int, int)}, this method can be used whenever {@link RequestOptions}
     * with override values are applied, or whenever you want to retrieve the image in its original
     * size.
     *
     * @see #submit(int, int)
     * @see #into(Target)
     */
    public FutureTarget<TranscodeType> submit() {
        return submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
    }

    /**
     * Returns a future that can be used to do a blocking get on a background thread.
     *
     * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
     *               overridden by
     *               {@link RequestOptions#override(int, int)} if
     *               previously called.
     * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
     *               overridden by
     *               {@link RequestOptions#override(int, int)}} if
     *               previously called).
     */
    public FutureTarget<TranscodeType> submit(int width, int height) {
        final RequestFutureTarget<TranscodeType> target =
                new RequestFutureTarget<>(context.getMainHandler(), width, height);

        if (Util.isOnBackgroundThread()) {
            context.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (!target.isCancelled()) {
                        into(target);
                    }
                }
            });
        } else {
            into(target);
        }

        return target;
    }

    /**
     * Preloads the resource into the cache using the given width and height.
     * <p>
     * Pre-loading is useful for making sure that resources you are going to to want in the near
     * future are available quickly.
     *
     * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
     *               overridden by
     *               {@link RequestOptions#override(int, int)} if
     *               previously called.
     * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
     *               overridden by
     *               {@link RequestOptions#override(int, int)}} if
     *               previously called).
     * @return A {@link Target} that can be used to cancel the load via
     * {@link RequestManager#clear(Target)}.
     * @see ListPreloader
     */
    public Target<TranscodeType> preload(int width, int height) {
        final PreloadTarget<TranscodeType> target = PreloadTarget.obtain(requestManager, width, height);
        return into(target);
    }

    /**
     * Preloads the resource into the cache using {@link Target#SIZE_ORIGINAL} as the target width and
     * height. Equivalent to calling {@link #preload(int, int)} with {@link Target#SIZE_ORIGINAL} as
     * the width and height.
     *
     * @return A {@link Target} that can be used to cancel the load via
     * {@link RequestManager#clear(Target)}
     * @see #preload(int, int)
     */
    public Target<TranscodeType> preload() {
        return preload(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
    }

    /**
     * Loads the original unmodified data into the cache and calls the given Target with the cache
     * File.
     *
     * @param target The Target that will receive the cache File when the load completes
     * @param <Y>    The type of Target.
     * @return The given Target.
     * @deprecated Use {@link RequestManager#downloadOnly()} and {@link #into(Target)}.
     */
    @Deprecated
    public <Y extends Target<File>> Y downloadOnly(Y target) {
        return getDownloadOnlyRequest().into(target);
    }

    /**
     * Loads the original unmodified data into the cache and returns a
     * {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the
     * data.
     *
     * @param width  The width in pixels to use to fetch the data.
     * @param height The height in pixels to use to fetch the data.
     * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File
     * containing the data.
     * @deprecated Use {@link RequestManager#downloadOnly()} and {@link #into(int, int)}.
     */
    @Deprecated
    public FutureTarget<File> downloadOnly(int width, int height) {
        return getDownloadOnlyRequest().submit(width, height);
    }

    protected RequestBuilder<File> getDownloadOnlyRequest() {
        return new RequestBuilder<>(File.class, this).apply(DOWNLOAD_ONLY_OPTIONS);
    }

    /**
     * 请求缩略图的优先级高一级
     */
    private Priority getThumbnailPriority(Priority current) {
        switch (current) {
            case LOW:
                return Priority.NORMAL;
            case NORMAL:
                return Priority.HIGH;
            case HIGH:
            case IMMEDIATE:
                return Priority.IMMEDIATE;
            default:
                throw new IllegalArgumentException("unknown priority: " + requestOptions.getPriority());
        }
    }

    private Request buildRequest(Target<TranscodeType> target) {
        return buildRequestRecursive(target, null, transitionOptions, requestOptions.getPriority(),
                requestOptions.getOverrideWidth(), requestOptions.getOverrideHeight());
    }

    private Request buildRequestRecursive(Target<TranscodeType> target,
                                          @Nullable ThumbnailRequestCoordinator parentCoordinator,
                                          TransitionOptions<?, ? super TranscodeType> transitionOptions,
                                          Priority priority, int overrideWidth, int overrideHeight) {
        if (thumbnailBuilder != null) {
            // Recursive case: contains a potentially recursive thumbnail request builder.
            if (isThumbnailBuilt) {
                throw new IllegalStateException("You cannot use a request as both the main request and a "
                        + "thumbnail, consider using clone() on the request(s) passed to thumbnail()");
            }

            TransitionOptions<?, ? super TranscodeType> thumbTransitionOptions =
                    thumbnailBuilder.transitionOptions;

            // Apply our transition by default to thumbnail requests but avoid overriding custom options
            // that may have been applied on the thumbnail request explicitly.
            if (thumbnailBuilder.isDefaultTransitionOptionsSet) {
                thumbTransitionOptions = transitionOptions;
            }

            Priority thumbPriority = thumbnailBuilder.requestOptions.isPrioritySet()
                    ? thumbnailBuilder.requestOptions.getPriority() : getThumbnailPriority(priority);

            int thumbOverrideWidth = thumbnailBuilder.requestOptions.getOverrideWidth();
            int thumbOverrideHeight = thumbnailBuilder.requestOptions.getOverrideHeight();
            if (Util.isValidDimensions(overrideWidth, overrideHeight)
                    && !thumbnailBuilder.requestOptions.isValidOverride()) {
                thumbOverrideWidth = requestOptions.getOverrideWidth();
                thumbOverrideHeight = requestOptions.getOverrideHeight();
            }

            ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            Request fullRequest = obtainRequest(target, requestOptions, coordinator,
                    transitionOptions, priority, overrideWidth, overrideHeight);
            isThumbnailBuilt = true;
            // Recursively generate thumbnail requests.
            Request thumbRequest = thumbnailBuilder.buildRequestRecursive(target, coordinator,
                    thumbTransitionOptions, thumbPriority, thumbOverrideWidth, thumbOverrideHeight);
            isThumbnailBuilt = false;
            coordinator.setRequests(fullRequest, thumbRequest);
            return coordinator;
        } else if (thumbSizeMultiplier != null) {
            // Base case: thumbnail multiplier generates a thumbnail request, but cannot recurse.
            ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            Request fullRequest = obtainRequest(target, requestOptions, coordinator, transitionOptions,
                    priority, overrideWidth, overrideHeight);
            RequestOptions thumbnailOptions = requestOptions.clone()
                    .sizeMultiplier(thumbSizeMultiplier);

            Request thumbnailRequest = obtainRequest(target, thumbnailOptions, coordinator,
                    transitionOptions, getThumbnailPriority(priority), overrideWidth, overrideHeight);

            coordinator.setRequests(fullRequest, thumbnailRequest);
            return coordinator;
        } else {
            // Base case: no thumbnail.
            return obtainRequest(target, requestOptions, parentCoordinator, transitionOptions, priority,
                    overrideWidth, overrideHeight);
        }
    }

    private Request obtainRequest(Target<TranscodeType> target,
                                  RequestOptions requestOptions, RequestCoordinator requestCoordinator,
                                  TransitionOptions<?, ? super TranscodeType> transitionOptions, Priority priority,
                                  int overrideWidth, int overrideHeight) {
        requestOptions.lock();

        return SingleRequest.obtain(
                context,
                model,
                transcodeClass,
                requestOptions,
                overrideWidth,
                overrideHeight,
                priority,
                target,
                requestListener,
                requestCoordinator,
                context.getEngine(),
                transitionOptions.getTransitionFactory());
    }
}
