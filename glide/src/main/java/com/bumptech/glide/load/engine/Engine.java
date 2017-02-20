package com.bumptech.glide.load.engine;

import android.os.Looper;
import android.os.MessageQueue;
import android.support.v4.util.Pools;
import android.util.Log;

import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import com.bumptech.glide.util.pool.FactoryPools;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for starting loads and managing active and cached resources.
 * <p>
 * 负责开始加载、动态管理及资源缓存
 */
public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener,
        EngineResource.ResourceListener {
    private static final String TAG = "Engine";
    private static final int JOB_POOL_SIZE = 150;
    private final Map<Key, EngineJob<?>> jobs;
    private final EngineKeyFactory keyFactory;
    private final MemoryCache cache;
    private final EngineJobFactory engineJobFactory;
    /**
     * 活动资源集合(即当前应用过程中使用过的{@link EngineResource}的集合)
     */
    private final Map<Key, WeakReference<EngineResource<?>>> activeResources;
    private final ResourceRecycler resourceRecycler;
    private final LazyDiskCacheProvider diskCacheProvider;
    private final DecodeJobFactory decodeJobFactory;

    /**
     * Lazily instantiate to avoid exceptions if Glide is initialized on a background thread.
     */
    private ReferenceQueue<EngineResource<?>> resourceReferenceQueue;

    /**
     * Allows a request to indicate it no longer is interested in a given load.
     * <p>
     * 可控制一个{@link EngineJob}取消{@link ResourceCallback}
     */
    public static class LoadStatus {
        private final EngineJob<?> engineJob;
        private final ResourceCallback cb;

        public LoadStatus(ResourceCallback cb, EngineJob<?> engineJob) {
            this.cb = cb;
            this.engineJob = engineJob;
        }

        public void cancel() {
            engineJob.removeCallback(cb);
        }
    }

    public Engine(MemoryCache memoryCache,
                  DiskCache.Factory diskCacheFactory,
                  GlideExecutor diskCacheExecutor,
                  GlideExecutor sourceExecutor,
                  GlideExecutor sourceUnlimitedExecutor) {
        this(memoryCache, diskCacheFactory, diskCacheExecutor, sourceExecutor, sourceUnlimitedExecutor,
                null, null, null, null, null, null);
    }

    /**
     * Visible for testing.
     * <p>
     * 这里之所以不用私有修饰符（{@code private}），是为了方便单元测试可测
     *
     * @param keyFactory {@link EngineKeyFactory}
     */
    Engine(MemoryCache cache,
           DiskCache.Factory diskCacheFactory,
           GlideExecutor diskCacheExecutor,
           GlideExecutor sourceExecutor,
           GlideExecutor sourceUnlimitedExecutor,
           Map<Key, EngineJob<?>> jobs,
           EngineKeyFactory keyFactory,
           Map<Key, WeakReference<EngineResource<?>>> activeResources,
           EngineJobFactory engineJobFactory,
           DecodeJobFactory decodeJobFactory,
           ResourceRecycler resourceRecycler) {
        this.cache = cache;
        this.diskCacheProvider = new LazyDiskCacheProvider(diskCacheFactory);

        if (activeResources == null) {
            activeResources = new HashMap<>();
        }
        this.activeResources = activeResources;

        if (keyFactory == null) {
            keyFactory = new EngineKeyFactory();
        }
        this.keyFactory = keyFactory;

        if (jobs == null) {
            jobs = new HashMap<>();
        }
        this.jobs = jobs;

        if (engineJobFactory == null) {
            engineJobFactory = new EngineJobFactory(diskCacheExecutor, sourceExecutor,
                    sourceUnlimitedExecutor, this);
        }
        this.engineJobFactory = engineJobFactory;

        if (decodeJobFactory == null) {
            decodeJobFactory = new DecodeJobFactory(diskCacheProvider);
        }
        this.decodeJobFactory = decodeJobFactory;

        if (resourceRecycler == null) {
            resourceRecycler = new ResourceRecycler();
        }
        this.resourceRecycler = resourceRecycler;

        cache.setResourceRemovedListener(this);
    }

    /**
     * Starts a load for the given arguments. Must be called on the main thread.
     * <p>
     * 用给定的参数开始加载.必须在主线程中调用
     * <p>
     * The flow for any request is as follows: <ul> <li>Check the memory cache and provide the
     * cached resource if present</li> <li>Check the current put of actively used resources and return
     * the active resource if present</li> <li>Check the current put of in progress loads and add the
     * cb to the in progress load if present</li> <li>Start a new load</li> </ul>
     * <p>
     * Active resources are those that have been provided to at least one request and have not yet
     * been released. Once all consumers of a resource have released that resource, the resource then
     * goes to cache. If the resource is ever returned to a new consumer from cache, it is re-added to
     * the active resources. If the resource is evicted from the cache, its resources are recycled and
     * re-used if possible and the resource is discarded. There is no strict requirement that
     * consumers release their resources so active resources are held weakly.
     * <p><ol>
     * <li>根据传参{@link Key}判断其所对应的{@link EngineResource}在{@link MemoryCache}
     * 中是否存在，若存在，则返回{@code null}，否则执行下一步</li>
     * <li>根据传参{@link Key}判断其所对应的{@link EngineResource}在{@link #activeResources}
     * 中是否存在，若存在，则返回{@code null}，否则执行下一步</li>
     * <li>根据传参{@link Key}判断在{@link #jobs}中是否存在对应的{@link EngineJob}，
     * 若存在，则返回对应的{@link LoadStatus}，否则执行下一步</li>
     * <li>根据{@link #engineJobFactory}和{@link #decodeJobFactory}创建对应的{@link EngineJob}
     * 和{@link DecodeJob}，并将该{@link EngineJob}对象添加到{@link #jobs}中去，
     * 并执行{@link EngineJob#start(DecodeJob)}，然后返回相应的{@link LoadStatus}</li></ol>
     *
     * @param width  The target width in pixels of the desired resource.
     * @param height The target height in pixels of the desired resource.
     * @param cb     The callback that will be called when the load completes.
     */
    public <R> LoadStatus load(
            GlideContext glideContext,
            Object model,
            Key signature,
            int width,
            int height,
            Class<?> resourceClass,
            Class<R> transcodeClass,
            Priority priority,
            DiskCacheStrategy diskCacheStrategy,
            Map<Class<?>, Transformation<?>> transformations,
            boolean isTransformationRequired,
            Options options,
            boolean isMemoryCacheable,
            boolean useUnlimitedSourceExecutorPool,
            boolean onlyRetrieveFromCache,
            ResourceCallback cb) {
        Util.assertMainThread();
        long startTime = LogTime.getLogTime();

        EngineKey key = keyFactory.buildKey(model, signature, width, height, transformations,
                resourceClass, transcodeClass, options);

        EngineResource<?> cached = loadFromCache(key, isMemoryCacheable);
        if (cached != null) {
            cb.onResourceReady(cached, DataSource.MEMORY_CACHE);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                logWithTimeAndKey("Loaded resource from cache", startTime, key);
            }
            return null;
        }

        EngineResource<?> active = loadFromActiveResources(key, isMemoryCacheable);
        if (active != null) {
            cb.onResourceReady(active, DataSource.MEMORY_CACHE);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                logWithTimeAndKey("Loaded resource from active resources", startTime, key);
            }
            return null;
        }

        EngineJob<?> current = jobs.get(key);
        if (current != null) {
            current.addCallback(cb);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                logWithTimeAndKey("Added to existing load", startTime, key);
            }
            return new LoadStatus(cb, current);
        }

        EngineJob<R> engineJob = engineJobFactory.build(key, isMemoryCacheable,
                useUnlimitedSourceExecutorPool);
        DecodeJob<R> decodeJob = decodeJobFactory.build(
                glideContext,
                model,
                key,
                signature,
                width,
                height,
                resourceClass,
                transcodeClass,
                priority,
                diskCacheStrategy,
                transformations,
                isTransformationRequired,
                onlyRetrieveFromCache,
                options,
                engineJob);
        jobs.put(key, engineJob);
        engineJob.addCallback(cb);
        engineJob.start(decodeJob);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Started new load", startTime, key);
        }
        return new LoadStatus(cb, engineJob);
    }

    private static void logWithTimeAndKey(String log, long startTime, Key key) {
        Log.v(TAG, log + " in " + LogTime.getElapsedMillis(startTime) + "ms, key: " + key);
    }

    /**
     * 从{@link #activeResources}中获取对应{@link Key}键的值，若获取到的
     * {@link EngineResource}已被回收，那么将该条映射记录从{@link #activeResources}中移除
     */
    private EngineResource<?> loadFromActiveResources(Key key, boolean isMemoryCacheable) {
        if (!isMemoryCacheable) {
            return null;
        }
        EngineResource<?> active = null;
        WeakReference<EngineResource<?>> activeRef = activeResources.get(key);
        if (activeRef != null) {
            active = activeRef.get();
            if (active != null) {
                active.acquire();
            } else {
                activeResources.remove(key);
            }
        }
        return active;
    }

    /**
     * 从内存缓存中获取对应{@link Key}键的{@link EngineResource}值，
     * 从{@link MemoryCache}中获取，并封装成{@link EngineResource}（如果有的话），
     * 然后将其添加到{@link #activeResources}
     */
    private EngineResource<?> loadFromCache(Key key, boolean isMemoryCacheable) {
        if (!isMemoryCacheable) {
            return null;
        }

        EngineResource<?> cached = getEngineResourceFromCache(key);
        if (cached != null) {
            cached.acquire();
            activeResources.put(key, new ResourceWeakReference(key, cached, getReferenceQueue()));
        }
        return cached;
    }

    /**
     * 根据给定的{@link Key},将在{@link #cache}（内存缓存）中对应{@code Key}键的{@link Resource}值
     * （如果有的话）封装成一个{@link EngineResource}对象作为返回值,并将该{@link Key}从内存缓存中移除掉
     */
    @SuppressWarnings("unchecked")
    private EngineResource<?> getEngineResourceFromCache(Key key) {
        Resource<?> cached = cache.remove(key);

        final EngineResource<?> result;
        if (cached == null) {
            result = null;
        } else if (cached instanceof EngineResource) {
            // Save an object allocation if we've cached an EngineResource (the typical case).
            result = (EngineResource<?>) cached;
        } else {
            result = new EngineResource<>(cached, true /*isMemoryCacheable*/);
        }
        return result;
    }

    /**
     * 资源释放
     */
    public void release(Resource<?> resource) {
        Util.assertMainThread();
        if (resource instanceof EngineResource) {
            ((EngineResource<?>) resource).release();
        } else {
            throw new IllegalArgumentException("Cannot release anything but an EngineResource");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEngineJobComplete(Key key, EngineResource<?> resource) {
        Util.assertMainThread();
        // A null resource indicates that the load failed, usually due to an exception.
        if (resource != null) {
            resource.setResourceListener(key, this);

            if (resource.isCacheable()) {
                activeResources.put(key, new ResourceWeakReference(key, resource, getReferenceQueue()));
            }
        }
        // TODO: should this check that the engine job is still current?
        jobs.remove(key);
    }

    @Override
    public void onEngineJobCancelled(EngineJob engineJob, Key key) {
        Util.assertMainThread();
        EngineJob<?> current = jobs.get(key);
        if (engineJob.equals(current)) {
            jobs.remove(key);
        }
    }

    /**
     * 将图片从{@link MemoryCache}中移除的回调接口
     */
    @Override
    public void onResourceRemoved(final Resource<?> resource) {
        Util.assertMainThread();
        resourceRecycler.recycle(resource);
    }

    /**
     * 资源释放（但非资源回收）的回调接口
     */
    @Override
    public void onResourceReleased(Key cacheKey, EngineResource resource) {
        Util.assertMainThread();
        activeResources.remove(cacheKey);
        if (resource.isCacheable()) {
            cache.put(cacheKey, resource);
        } else {
            resourceRecycler.recycle(resource);
        }
    }

    /**
     * 清空磁盘缓存
     */
    public void clearDiskCache() {
        diskCacheProvider.getDiskCache().clear();
    }

    /**
     * {@link #resourceReferenceQueue}的{@code getter}，并且若其为空，
     * 实例化一个对象，并添加线程空闲任务
     */
    private ReferenceQueue<EngineResource<?>> getReferenceQueue() {
        if (resourceReferenceQueue == null) {
            resourceReferenceQueue = new ReferenceQueue<>();
            MessageQueue queue = Looper.myQueue();
            queue.addIdleHandler(new RefQueueIdleHandler(activeResources, resourceReferenceQueue));
        }
        return resourceReferenceQueue;
    }

    /**
     * 实现提供生产{@link DiskCache}实例接口的类
     */
    private static class LazyDiskCacheProvider implements DecodeJob.DiskCacheProvider {
        private final DiskCache.Factory factory;
        private volatile DiskCache diskCache;

        /**
         * 传参是{@link com.bumptech.glide.GlideBuilder}默认创建的
         * {@link com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory}实例
         */
        public LazyDiskCacheProvider(DiskCache.Factory factory) {
            this.factory = factory;
        }

        @Override
        public DiskCache getDiskCache() {
            if (diskCache == null) {
                synchronized (this) {
                    if (diskCache == null) {
                        /**
                         * 获取{@link DiskLruCacheWrapper}实例
                         */
                        diskCache = factory.build();
                    }
                    if (diskCache == null) {
                        diskCache = new DiskCacheAdapter();
                    }
                }
            }
            return diskCache;
        }
    }

    /**
     * {@link EngineResource}的弱引用封装类，将传参{@link EngineResource}对象添加到
     * {@link ReferenceQueue}队列中（该对象中{@link #resourceReferenceQueue}队列）
     */
    private static class ResourceWeakReference extends WeakReference<EngineResource<?>> {
        @Synthetic
        final Key key;

        public ResourceWeakReference(Key key, EngineResource<?> r, ReferenceQueue<? super EngineResource<?>> q) {
            /**
             * 创建引用给定对象的新的弱引用，并向给定队列注册该引用
             */
            super(r, q);
            this.key = key;
        }
    }

    /**
     * Responsible for cleaning up the active resource map by remove weak references that have been cleared.
     * <p>
     * 在线程空闲的时候，进行指定的操作
     */
    private static class RefQueueIdleHandler implements MessageQueue.IdleHandler {
        private final Map<Key, WeakReference<EngineResource<?>>> activeResources;
        private final ReferenceQueue<EngineResource<?>> queue;

        public RefQueueIdleHandler(Map<Key, WeakReference<EngineResource<?>>> activeResources,
                                   ReferenceQueue<EngineResource<?>> queue) {
            this.activeResources = activeResources;
            this.queue = queue;
        }

        /**
         * @return 返回值boolean 意思是needKeep，
         * 若返回true，表示要保留，代表不移除这个idleHandler，可以反复执行，
         * 若返回false，表示执行完毕之后就移除这个idleHandler，也就是只执行一次
         */
        @Override
        public boolean queueIdle() {
            ResourceWeakReference ref = (ResourceWeakReference) queue.poll();
            if (ref != null) {
                activeResources.remove(ref.key);
            }

            return true;
        }
    }

    /**
     * 创建{@link DecodeJob}的工厂，用一个{@link Pools}来存放多个实例
     */
    // Visible for testing.
    static class DecodeJobFactory {
        @Synthetic
        final DecodeJob.DiskCacheProvider diskCacheProvider;
        @Synthetic
        final Pools.Pool<DecodeJob<?>> pool = FactoryPools.simple(JOB_POOL_SIZE,
                new FactoryPools.Factory<DecodeJob<?>>() {
                    @Override
                    public DecodeJob<?> create() {
                        return new DecodeJob<Object>(diskCacheProvider, pool);
                    }
                });
        private int creationOrder;

        DecodeJobFactory(DecodeJob.DiskCacheProvider diskCacheProvider) {
            this.diskCacheProvider = diskCacheProvider;
        }

        @SuppressWarnings("unchecked")
        <R> DecodeJob<R> build(GlideContext glideContext,
                               Object model,
                               EngineKey loadKey,
                               Key signature,
                               int width,
                               int height,
                               Class<?> resourceClass,
                               Class<R> transcodeClass,
                               Priority priority,
                               DiskCacheStrategy diskCacheStrategy,
                               Map<Class<?>, Transformation<?>> transformations,
                               boolean isTransformationRequired,
                               boolean onlyRetrieveFromCache,
                               Options options,
                               DecodeJob.Callback<R> callback) {
            DecodeJob<R> result = (DecodeJob<R>) pool.acquire();
            return result.init(
                    glideContext,
                    model,
                    loadKey,
                    signature,
                    width,
                    height,
                    resourceClass,
                    transcodeClass,
                    priority,
                    diskCacheStrategy,
                    transformations,
                    isTransformationRequired,
                    onlyRetrieveFromCache,
                    options,
                    callback,
                    creationOrder++);
        }
    }

    /**
     * 创建{@link EngineJob}的工厂，用一个{@link Pools}来存放多个实例
     */
    // Visible for testing.
    static class EngineJobFactory {
        @Synthetic
        final GlideExecutor diskCacheExecutor;
        @Synthetic
        final GlideExecutor sourceExecutor;
        @Synthetic
        final GlideExecutor sourceUnlimitedExecutor;
        @Synthetic
        final EngineJobListener listener;
        @Synthetic
        final Pools.Pool<EngineJob<?>> pool = FactoryPools.simple(JOB_POOL_SIZE,
                new FactoryPools.Factory<EngineJob<?>>() {
                    @Override
                    public EngineJob<?> create() {
                        return new EngineJob<Object>(diskCacheExecutor, sourceExecutor,
                                sourceUnlimitedExecutor, listener, pool);
                    }
                });

        EngineJobFactory(GlideExecutor diskCacheExecutor, GlideExecutor sourceExecutor,
                         GlideExecutor sourceUnlimitedExecutor, EngineJobListener listener) {
            this.diskCacheExecutor = diskCacheExecutor;
            this.sourceExecutor = sourceExecutor;
            this.sourceUnlimitedExecutor = sourceUnlimitedExecutor;
            this.listener = listener;
        }

        @SuppressWarnings("unchecked")
        <R> EngineJob<R> build(Key key, boolean isMemoryCacheable,
                               boolean useUnlimitedSourceGeneratorPool) {
            EngineJob<R> result = (EngineJob<R>) pool.acquire();
            return result.init(key, isMemoryCacheable, useUnlimitedSourceGeneratorPool);
        }
    }
}
