package com.bumptech.glide.load.engine.executor;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.util.Synthetic;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A prioritized {@link ThreadPoolExecutor} for running jobs in Glide.
 * <p>
 * 针对Glide进行优化的{@link ThreadPoolExecutor}
 * <p>
 * <ul>疑问：
 * <li>{@link #executeSynchronously}在{@link #maybeWait(Future)}中起什么作用</li>
 * <li>DiskCache线程池和Source线程池的具体工作</li>
 * <li>{@code preventNetworkOperations}传参在创建DiskCache线程池时为true，而在创建
 * Source线程池为false是什么情况</li>
 * <li>{@link #calculateBestThreadCount()}中计算出来的cpuCount和availableProcessors的区别</li></ul>
 */
public final class GlideExecutor extends ThreadPoolExecutor {

    /**
     * The default thread name prefix for executors used to load/decode/transform data not found in
     * cache.
     */
    public static final String DEFAULT_SOURCE_EXECUTOR_NAME = "source";
    /**
     * The default thread name prefix for executors used to load/decode/transform data found in
     * Glide's cache.
     */
    public static final String DEFAULT_DISK_CACHE_EXECUTOR_NAME = "disk-cache";
    /**
     * The default thread count for executors used to load/decode/transform data found in Glide's
     * cache.
     */
    public static final int DEFAULT_DISK_CACHE_EXECUTOR_THREADS = 1;

    private static final String TAG = "GlideExecutor";
    /**
     * CPU名称的正则表达式
     */
    private static final String CPU_NAME_REGEX = "cpu[0-9]+";
    private static final String CPU_LOCATION = "/sys/devices/system/cpu/";
    // Don't use more than four threads when automatically determining thread count..
    private static final int MAXIMUM_AUTOMATIC_THREAD_COUNT = 4;
    // May be accessed on other threads, but this is an optimization only so it's ok if we set its
    // value more than once.
    private static volatile int bestThreadCount;
    private final boolean executeSynchronously;

    /**
     * The default thread name prefix for executors from unlimited thread pool used to
     * load/decode/transform data not found in cache.
     */
    private static final String SOURCE_UNLIMITED_EXECUTOR_NAME = "source-unlimited";
    /**
     * The default keep alive time for threads in our cached thread pools in milliseconds.
     */
    private static final long KEEP_ALIVE_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    private static final String ANIMATION_EXECUTOR_NAME = "animation";

    /**
     * Returns a new fixed thread pool with the default thread count returned from
     * {@link #calculateBestThreadCount()}, the {@link #DEFAULT_DISK_CACHE_EXECUTOR_NAME} thread name
     * prefix, and the
     * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
     * uncaught throwable strategy.
     * <p>
     * <p>Disk cache executors do not allow network operations on their threads.
     * <p>
     * 创建一个固定线程数为1，任务队列无限制的线程池；捕获到异常时，记录异常日志
     * <p>
     * 创建的是读取{@code ROM}缓存的线程池
     */
    public static GlideExecutor newDiskCacheExecutor() {
        return newDiskCacheExecutor(DEFAULT_DISK_CACHE_EXECUTOR_THREADS,
                DEFAULT_DISK_CACHE_EXECUTOR_NAME, UncaughtThrowableStrategy.DEFAULT);
    }

    /**
     * Returns a new fixed thread pool with the default thread count returned from
     * {@link #calculateBestThreadCount()}, the {@link #DEFAULT_DISK_CACHE_EXECUTOR_NAME} thread name
     * prefix, and a custom
     * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}
     * uncaught throwable strategy.
     * <p>
     * <p>Disk cache executors do not allow network operations on their threads.
     *
     * @param uncaughtThrowableStrategy The {@link
     *                                  com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
     *                                  handle uncaught exceptions.
     */
    public static GlideExecutor newDiskCacheExecutor(
            UncaughtThrowableStrategy uncaughtThrowableStrategy) {
        return newDiskCacheExecutor(DEFAULT_DISK_CACHE_EXECUTOR_THREADS,
                DEFAULT_DISK_CACHE_EXECUTOR_NAME, uncaughtThrowableStrategy);
    }

    /**
     * Returns a new fixed thread pool with the given thread count, thread name prefix,
     * and {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}.
     * <p>
     * <p>Disk cache executors do not allow network operations on their threads.
     * <p>
     * 创建一个固定数量核心线程，没有临时线程，但任务队列无大小限制的线程池；支持预防网络操作
     *
     * @param threadCount               The number of threads.
     * @param name                      The prefix for each thread name.
     * @param uncaughtThrowableStrategy The {@link
     *                                  com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
     *                                  handle uncaught exceptions.
     */
    public static GlideExecutor newDiskCacheExecutor(int threadCount, String name,
                                                     UncaughtThrowableStrategy uncaughtThrowableStrategy) {
        return new GlideExecutor(threadCount, name, uncaughtThrowableStrategy,
                true /*preventNetworkOperations*/, false /*executeSynchronously*/);
    }

    /**
     * Returns a new fixed thread pool with the default thread count returned from
     * {@link #calculateBestThreadCount()}, the {@link #DEFAULT_SOURCE_EXECUTOR_NAME} thread name
     * prefix, and the
     * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
     * uncaught throwable strategy.
     * <p>
     * <p>Source executors allow network operations on their threads.
     * <p>
     * 根据计算CPU核数，设置一个核心线程数不大于{@link #MAXIMUM_AUTOMATIC_THREAD_COUNT}，
     * 不创建临时线程，任务队列无大小限制的线程池；对网络操作不进行干涉
     */
    public static GlideExecutor newSourceExecutor() {
        return newSourceExecutor(calculateBestThreadCount(), DEFAULT_SOURCE_EXECUTOR_NAME,
                UncaughtThrowableStrategy.DEFAULT);
    }

    /**
     * Returns a new fixed thread pool with the default thread count returned from
     * {@link #calculateBestThreadCount()}, the {@link #DEFAULT_SOURCE_EXECUTOR_NAME} thread name
     * prefix, and a custom
     * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}
     * uncaught throwable strategy.
     * <p>
     * <p>Source executors allow network operations on their threads.
     *
     * @param uncaughtThrowableStrategy The {@link
     *                                  com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
     *                                  handle uncaught exceptions.
     */
    public static GlideExecutor newSourceExecutor(
            UncaughtThrowableStrategy uncaughtThrowableStrategy) {
        return newDiskCacheExecutor(DEFAULT_DISK_CACHE_EXECUTOR_THREADS,
                DEFAULT_DISK_CACHE_EXECUTOR_NAME, uncaughtThrowableStrategy);
    }

    /**
     * Returns a new fixed thread pool with the given thread count, thread name prefix,
     * and {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}.
     * <p>
     * <p>Source executors allow network operations on their threads.
     * <p>
     * 创建一个固定数量核心线程，没有临时线程，但任务队列无大小限制的线程池；不支持预防网络操作
     *
     * @param threadCount               The number of threads.
     * @param name                      The prefix for each thread name.
     * @param uncaughtThrowableStrategy The {@link
     *                                  com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
     *                                  handle uncaught exceptions.
     */
    public static GlideExecutor newSourceExecutor(int threadCount, String name,
                                                  UncaughtThrowableStrategy uncaughtThrowableStrategy) {
        return new GlideExecutor(threadCount, name, uncaughtThrowableStrategy,
                false /*preventNetworkOperations*/, false /*executeSynchronously*/);
    }

    /**
     * Returns a new unlimited thread pool with zero core thread count to make sure no threads are
     * created by default, {@link #KEEP_ALIVE_TIME_MS} keep alive
     * time, the {@link #SOURCE_UNLIMITED_EXECUTOR_NAME} thread name prefix, the
     * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
     * uncaught throwable strategy, and the {@link SynchronousQueue} since using default unbounded
     * blocking queue, for example, {@link PriorityBlockingQueue} effectively won't create more than
     * {@code corePoolSize} threads.
     * See <a href=
     * "http://developer.android.com/reference/java/util/concurrent/ThreadPoolExecutor.html">
     * ThreadPoolExecutor documentation</a>.
     * <p>
     * <p>Source executors allow network operations on their threads.
     * <p>
     * 创建一个无核心线程，但可以无限制创建临时线程的线程池，任务队列无大小限制
     */
    public static GlideExecutor newUnlimitedSourceExecutor() {
        return new GlideExecutor(0 /* corePoolSize */,
                Integer.MAX_VALUE /* maximumPoolSize */,
                KEEP_ALIVE_TIME_MS,
                SOURCE_UNLIMITED_EXECUTOR_NAME,
                UncaughtThrowableStrategy.DEFAULT,
                false /*preventNetworkOperations*/,
                false /*executeSynchronously*/,
                new SynchronousQueue<Runnable>());
    }

    public static GlideExecutor newAnimationExecutor() {
        int bestThreadCount = calculateBestThreadCount();
        // We don't want to add a ton of threads running animations in parallel with our source and
        // disk cache executors. Doing so adds unnecessary CPU load and can also dramatically increase
        // our maximum memory usage. Typically one thread is sufficient here, but for higher end devices
        // with more cores, two threads can provide better performance if lots of GIFs are showing at
        // once.
        int maximumPoolSize = bestThreadCount >= 4 ? 2 : 1;
        return new GlideExecutor(
        /*corePoolSize=*/ 0,
                maximumPoolSize,
                KEEP_ALIVE_TIME_MS,
                ANIMATION_EXECUTOR_NAME,
                UncaughtThrowableStrategy.DEFAULT,
        /*preventNetworkOperations=*/ true,
        /*executeSynchronously=*/ false,
                new PriorityBlockingQueue<Runnable>());
    }

    // Visible for testing.
    GlideExecutor(int poolSize, String name,
                  UncaughtThrowableStrategy uncaughtThrowableStrategy, boolean preventNetworkOperations,
                  boolean executeSynchronously) {
        this(
                poolSize /* corePoolSize */,
                poolSize /* maximumPoolSize */,
                0 /* keepAliveTimeInMs */,
                name,
                uncaughtThrowableStrategy,
                preventNetworkOperations,
                executeSynchronously);
    }

    GlideExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTimeInMs, String name,
                  UncaughtThrowableStrategy uncaughtThrowableStrategy, boolean preventNetworkOperations,
                  boolean executeSynchronously) {
        this(
                corePoolSize,
                maximumPoolSize,
                keepAliveTimeInMs,
                name,
                uncaughtThrowableStrategy,
                preventNetworkOperations,
                executeSynchronously,
                new PriorityBlockingQueue<Runnable>());
    }

    GlideExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTimeInMs, String name,
                  UncaughtThrowableStrategy uncaughtThrowableStrategy, boolean preventNetworkOperations,
                  boolean executeSynchronously, BlockingQueue<Runnable> queue) {
        super(
                corePoolSize,
                maximumPoolSize,
                keepAliveTimeInMs,
                TimeUnit.MILLISECONDS,
                queue,
                new DefaultThreadFactory(name, uncaughtThrowableStrategy, preventNetworkOperations));
        this.executeSynchronously = executeSynchronously;
    }

    @Override
    public void execute(Runnable command) {
        if (executeSynchronously) {
            command.run();
        } else {
            super.execute(command);
        }
    }

    @NonNull
    @Override
    public Future<?> submit(Runnable task) {
        return maybeWait(super.submit(task));
    }

    private <T> Future<T> maybeWait(Future<T> future) {
        if (executeSynchronously) {
            boolean interrupted = false;
            try {
                while (!future.isDone()) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return future;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return maybeWait(super.submit(task, result));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return maybeWait(super.submit(task));
    }

    /**
     * Determines the number of cores available on the device.
     * <p>
     * <p>{@link Runtime#availableProcessors()} returns the number of awake cores, which may not
     * be the number of available cores depending on the device's current state. See
     * http://goo.gl/8H670N.
     * <p>
     * 根据CPU个数配置一个不大于{@link #MAXIMUM_AUTOMATIC_THREAD_COUNT}的线程数
     */
    public static int calculateBestThreadCount() {
        if (bestThreadCount == 0) {
            // We override the current ThreadPolicy to allow disk reads.
            // This shouldn't actually do disk-IO and accesses a device file.
            // See: https://github.com/bumptech/glide/issues/1170
            ThreadPolicy originalPolicy = StrictMode.allowThreadDiskReads();
            File[] cpus = null;
            try {
                File cpuInfo = new File(CPU_LOCATION);
                final Pattern cpuNamePattern = Pattern.compile(CPU_NAME_REGEX);
                cpus = cpuInfo.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return cpuNamePattern.matcher(s).matches();
                    }
                });
            } catch (Throwable t) {
                if (Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, "Failed to calculate accurate cpu count", t);
                }
            } finally {
                StrictMode.setThreadPolicy(originalPolicy);
            }

            int cpuCount = cpus != null ? cpus.length : 0;
            int availableProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
            /**
             * {@link cpuCount}和{@link availableProcessors}有什么区别呢？
             */
            bestThreadCount =
                    Math.min(MAXIMUM_AUTOMATIC_THREAD_COUNT, Math.max(availableProcessors, cpuCount));
        }
        return bestThreadCount;
    }

    /**
     * A strategy for handling unexpected and uncaught {@link Throwable}s thrown by futures run on the
     * pool.
     * <p>
     * 异常处理策略
     */
    public interface UncaughtThrowableStrategy {
        /**
         * Silently catches and ignores the uncaught {@link Throwable}s.
         */
        UncaughtThrowableStrategy IGNORE = new UncaughtThrowableStrategy() {
            @Override
            public void handle(Throwable t) {
                //ignore
            }
        };
        /**
         * Logs the uncaught {@link Throwable}s using {@link #TAG} and {@link Log}.
         */
        UncaughtThrowableStrategy LOG = new UncaughtThrowableStrategy() {
            @Override
            public void handle(Throwable t) {
                if (t != null && Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, "Request threw uncaught throwable", t);
                }
            }
        };
        /**
         * Rethrows the uncaught {@link Throwable}s to crash the app.
         */
        UncaughtThrowableStrategy THROW = new UncaughtThrowableStrategy() {
            @Override
            public void handle(Throwable t) {
                if (t != null) {
                    throw new RuntimeException("Request threw uncaught throwable", t);
                }
            }
        };

        /**
         * The default strategy, currently {@link #LOG}.
         */
        UncaughtThrowableStrategy DEFAULT = LOG;

        void handle(Throwable t);
    }

    /**
     * A {@link java.util.concurrent.ThreadFactory} that builds threads slightly above priority {@link
     * android.os.Process#THREAD_PRIORITY_BACKGROUND}.
     */
    private static final class DefaultThreadFactory implements ThreadFactory {
        private final String name;
        @Synthetic
        final UncaughtThrowableStrategy uncaughtThrowableStrategy;
        @Synthetic
        final boolean preventNetworkOperations;
        private int threadNum;

        /**
         * @param name                      线程名
         * @param uncaughtThrowableStrategy 捕获到异常时，对异常的处理策略
         * @param preventNetworkOperations  是否预防网络操作
         */
        DefaultThreadFactory(String name, UncaughtThrowableStrategy uncaughtThrowableStrategy,
                             boolean preventNetworkOperations) {
            this.name = name;
            this.uncaughtThrowableStrategy = uncaughtThrowableStrategy;
            this.preventNetworkOperations = preventNetworkOperations;
        }

        @Override
        public synchronized Thread newThread(@NonNull Runnable runnable) {
            final Thread result = new Thread(runnable, "glide-" + name + "-thread-" + threadNum) {
                @Override
                public void run() {
                    /**
                     * 设置线程的优先级
                     */
                    android.os.Process.setThreadPriority(
                            android.os.Process.THREAD_PRIORITY_BACKGROUND
                                    + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
                    if (preventNetworkOperations) {
                        /**
                         * 线程加入严苛模式（{@link StrictMode}），设置检测网络操作，
                         * 并设置违规应用崩溃处罚
                         */
                        StrictMode.setThreadPolicy(
                                new ThreadPolicy.Builder()
                                        .detectNetwork()
                                        .penaltyDeath()
                                        .build());
                    }
                    try {
                        super.run();
                    } catch (Throwable t) {
                        uncaughtThrowableStrategy.handle(t);
                    }
                }
            };
            threadNum++;
            return result;
        }
    }
}