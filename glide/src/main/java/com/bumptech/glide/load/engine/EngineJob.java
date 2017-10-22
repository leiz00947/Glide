package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.Pools;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import com.bumptech.glide.util.pool.FactoryPools.Poolable;
import com.bumptech.glide.util.pool.StateVerifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that manages a load by adding and removing callbacks for for the load and notifying
 * callbacks when the load completes.
 * <p>
 * 任务启动器对象
 * <p>
 * 当加载完成时对于这个加载和其需要通知的回调通过添加和移除回调来进行管理
 */
class EngineJob<R> implements DecodeJob.Callback<R>,
        Poolable {
    private static final EngineResourceFactory DEFAULT_FACTORY = new EngineResourceFactory();
    private static final Handler MAIN_THREAD_HANDLER =
            new Handler(Looper.getMainLooper(), new MainThreadCallback());

    private static final int MSG_COMPLETE = 1;
    private static final int MSG_EXCEPTION = 2;
    // Used when we realize we're cancelled on a background thread in reschedule and can recycle
    // immediately rather than waiting for a result or an error.
    private static final int MSG_CANCELLED = 3;

    /**
     * 事实上就是存放{@link com.bumptech.glide.request.SingleRequest}的对象集合
     */
    private final List<ResourceCallback> cbs = new ArrayList<>(2);
    /**
     * 默认情况下为{@link StateVerifier.DefaultStateVerifier}对象
     */
    private final StateVerifier stateVerifier = StateVerifier.newInstance();
    private final Pools.Pool<EngineJob<?>> pool;
    private final EngineResourceFactory engineResourceFactory;
    /**
     * 默认{@link Engine}对象
     */
    private final EngineJobListener listener;
    /**
     * @see com.bumptech.glide.GlideBuilder#diskCacheExecutor
     */
    private final GlideExecutor diskCacheExecutor;
    private final GlideExecutor sourceExecutor;
    private final GlideExecutor sourceUnlimitedExecutor;
    private final GlideExecutor animationExecutor;

    private Key key;
    /**
     * 若为{@code true}，则表示可以在{@link com.bumptech.glide.load.engine.cache.MemoryCache}中缓存
     */
    private boolean isCacheable;
    private boolean useUnlimitedSourceGeneratorPool;
    private boolean useAnimationPool;
    private Resource<?> resource;
    private DataSource dataSource;
    /**
     * 标识{@link #handleResultOnMainThread()}方法是否被调用过，若是，则置为{@code true}
     */
    private boolean hasResource;
    private GlideException exception;
    /**
     * 标识{@link #handleExceptionOnMainThread()}方法是否被调用过，若是，则置为{@code true}
     */
    private boolean hasLoadFailed;
    // A put of callbacks that are removed while we're notifying other callbacks of a change in
    // status.
    private List<ResourceCallback> ignoredCallbacks;
    private EngineResource<?> engineResource;
    private DecodeJob<R> decodeJob;

    // Checked primarily on the main thread, but also on other threads in reschedule.
    /**
     * 用来标识该启动器是否可以正常工作，若为{@code true}，表示不能正常工作
     */
    private volatile boolean isCancelled;

    EngineJob(
            GlideExecutor diskCacheExecutor,
            GlideExecutor sourceExecutor,
            GlideExecutor sourceUnlimitedExecutor,
            GlideExecutor animationExecutor,
            EngineJobListener listener,
            Pools.Pool<EngineJob<?>> pool) {
        this(
                diskCacheExecutor,
                sourceExecutor,
                sourceUnlimitedExecutor,
                animationExecutor,
                listener,
                pool,
                DEFAULT_FACTORY);
    }

    // Visible for testing.
    EngineJob(
            GlideExecutor diskCacheExecutor,
            GlideExecutor sourceExecutor,
            GlideExecutor sourceUnlimitedExecutor,
            GlideExecutor animationExecutor,
            EngineJobListener listener,
            Pools.Pool<EngineJob<?>> pool,
            EngineResourceFactory engineResourceFactory) {
        this.diskCacheExecutor = diskCacheExecutor;
        this.sourceExecutor = sourceExecutor;
        this.sourceUnlimitedExecutor = sourceUnlimitedExecutor;
        this.animationExecutor = animationExecutor;
        this.listener = listener;
        this.pool = pool;
        this.engineResourceFactory = engineResourceFactory;
    }

    // Visible for testing.
    EngineJob<R> init(
            Key key,
            boolean isCacheable,
            boolean useUnlimitedSourceGeneratorPool,
            boolean useAnimationPool) {
        this.key = key;
        this.isCacheable = isCacheable;
        this.useUnlimitedSourceGeneratorPool = useUnlimitedSourceGeneratorPool;
        this.useAnimationPool = useAnimationPool;
        return this;
    }

    /**
     * 开始执行{@link DecodeJob#run()}
     */
    public void start(DecodeJob<R> decodeJob) {
        this.decodeJob = decodeJob;
        GlideExecutor executor = decodeJob.willDecodeFromCache()
                ? diskCacheExecutor
                : getActiveSourceExecutor();
        executor.execute(decodeJob);
    }

    public void addCallback(ResourceCallback cb) {
        Util.assertMainThread();
        stateVerifier.throwIfRecycled();
        if (hasResource) {
            cb.onResourceReady(engineResource, dataSource);
        } else if (hasLoadFailed) {
            cb.onLoadFailed(exception);
        } else {
            cbs.add(cb);
        }
    }

    public void removeCallback(ResourceCallback cb) {
        Util.assertMainThread();
        stateVerifier.throwIfRecycled();
        if (hasResource || hasLoadFailed) {
            addIgnoredCallback(cb);
        } else {
            cbs.remove(cb);
            if (cbs.isEmpty()) {
                cancel();
            }
        }
    }

    private GlideExecutor getActiveSourceExecutor() {
        return useUnlimitedSourceGeneratorPool
                ? sourceUnlimitedExecutor : (useAnimationPool ? animationExecutor : sourceExecutor);
    }

    // We cannot remove callbacks while notifying our list of callbacks directly because doing so
    // would cause a ConcurrentModificationException. However, we need to obey the cancellation
    // request such that if notifying a callback early in the callbacks list cancels a callback later
    // in the request list, the cancellation for the later request is still obeyed. Using a put of
    // ignored callbacks allows us to avoid the exception while still meeting the requirement.
    private void addIgnoredCallback(ResourceCallback cb) {
        if (ignoredCallbacks == null) {
            ignoredCallbacks = new ArrayList<>(2);
        }
        if (!ignoredCallbacks.contains(cb)) {
            ignoredCallbacks.add(cb);
        }
    }

    private boolean isInIgnoredCallbacks(ResourceCallback cb) {
        return ignoredCallbacks != null && ignoredCallbacks.contains(cb);
    }

    /**
     * 取消启动任务
     */
    // Exposed for testing.
    void cancel() {
        if (hasLoadFailed || hasResource || isCancelled) {
            return;
        }

        isCancelled = true;
        decodeJob.cancel();
        // TODO: Consider trying to remove jobs that have never been run before from executor queues.
        // Removing jobs that have run before can break things. See #1996.
        listener.onEngineJobCancelled(this, key);
    }

    // Exposed for testing.
    boolean isCancelled() {
        return isCancelled;
    }

    /**
     * 一般的，开始执行启动任务，即调用{@link Engine#onEngineJobComplete(Key, EngineResource)}，
     * 然后调用资源加载成功的回调接口，即
     * {@link com.bumptech.glide.request.SingleRequest#onResourceReady(Resource, DataSource)}
     */
    @Synthetic
    void handleResultOnMainThread() {
        stateVerifier.throwIfRecycled();
        if (isCancelled) {
            resource.recycle();
            release(false /*isRemovedFromQueue*/);
            return;
        } else if (cbs.isEmpty()) {
            throw new IllegalStateException("Received a resource without any callbacks to notify");
        } else if (hasResource) {
            throw new IllegalStateException("Already have resource");
        }
        engineResource = engineResourceFactory.build(resource, isCacheable);
        hasResource = true;

        // Hold on to resource for duration of request so we don't recycle it in the middle of
        // notifying if it synchronously released by one of the callbacks.
        engineResource.acquire();
        listener.onEngineJobComplete(key, engineResource);

        for (ResourceCallback cb : cbs) {
            if (!isInIgnoredCallbacks(cb)) {
                engineResource.acquire();
                cb.onResourceReady(engineResource, dataSource);
            }
        }
        // Our request is complete, so we can release the resource.
        engineResource.release();

        release(false /*isRemovedFromQueue*/);
    }

    /**
     * 取消任务启动任务，即调用{@link Engine#onEngineJobCancelled(EngineJob, Key)}
     */
    @Synthetic
    void handleCancelledOnMainThread() {
        stateVerifier.throwIfRecycled();
        if (!isCancelled) {
            throw new IllegalStateException("Not cancelled");
        }
        listener.onEngineJobCancelled(this, key);
        release(false /*isRemovedFromQueue*/);
    }

    /**
     * 还原/置空变量，便于内存回收
     */
    private void release(boolean isRemovedFromQueue) {
        Util.assertMainThread();
        cbs.clear();
        key = null;
        engineResource = null;
        resource = null;
        if (ignoredCallbacks != null) {
            ignoredCallbacks.clear();
        }
        hasLoadFailed = false;
        isCancelled = false;
        hasResource = false;
        decodeJob.release(isRemovedFromQueue);
        decodeJob = null;
        exception = null;
        dataSource = null;
        pool.release(this);
    }

    @Override
    public void onResourceReady(Resource<R> resource, DataSource dataSource) {
        this.resource = resource;
        this.dataSource = dataSource;
        MAIN_THREAD_HANDLER.obtainMessage(MSG_COMPLETE, this).sendToTarget();
    }

    @Override
    public void onLoadFailed(GlideException e) {
        this.exception = e;
        MAIN_THREAD_HANDLER.obtainMessage(MSG_EXCEPTION, this).sendToTarget();
    }

    @Override
    public void reschedule(DecodeJob<?> job) {
        // Even if the job is cancelled here, it still needs to be scheduled so that it can clean itself
        // up.
        getActiveSourceExecutor().execute(job);
    }

    /**
     * 一般的，开始启动任务，即调用{@link Engine#onEngineJobComplete(Key, EngineResource)}，
     * 然后调用资源加载失败的回调接口，即
     * {@link com.bumptech.glide.request.SingleRequest#onLoadFailed(GlideException)}
     */
    @Synthetic
    void handleExceptionOnMainThread() {
        stateVerifier.throwIfRecycled();
        if (isCancelled) {
            release(false /*isRemovedFromQueue*/);
            return;
        } else if (cbs.isEmpty()) {
            throw new IllegalStateException("Received an exception without any callbacks to notify");
        } else if (hasLoadFailed) {
            throw new IllegalStateException("Already failed once");
        }
        hasLoadFailed = true;

        listener.onEngineJobComplete(key, null);

        for (ResourceCallback cb : cbs) {
            if (!isInIgnoredCallbacks(cb)) {
                cb.onLoadFailed(exception);
            }
        }

        release(false /*isRemovedFromQueue*/);
    }

    @Override
    public StateVerifier getVerifier() {
        return stateVerifier;
    }

    /**
     * 生产{@link EngineResource}实例的工厂
     */
    // Visible for testing.
    static class EngineResourceFactory {
        public <R> EngineResource<R> build(Resource<R> resource, boolean isMemoryCacheable) {
            return new EngineResource<>(resource, isMemoryCacheable);
        }
    }

    private static class MainThreadCallback implements Handler.Callback {

        @Synthetic
        MainThreadCallback() {
        }

        @Override
        public boolean handleMessage(Message message) {
            EngineJob<?> job = (EngineJob<?>) message.obj;
            switch (message.what) {
                case MSG_COMPLETE:
                    job.handleResultOnMainThread();
                    break;
                case MSG_EXCEPTION:
                    job.handleExceptionOnMainThread();
                    break;
                case MSG_CANCELLED:
                    job.handleCancelledOnMainThread();
                    break;
                default:
                    throw new IllegalStateException("Unrecognized message: " + message.what);
            }
            return true;
        }
    }
}