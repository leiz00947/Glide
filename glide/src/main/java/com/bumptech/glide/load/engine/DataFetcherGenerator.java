package com.bumptech.glide.load.engine;

import android.support.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;

/**
 * Generates a series of {@link DataFetcher DataFetchers} using
 * registered {@link com.bumptech.glide.load.model.ModelLoader ModelLoaders} and a model.
 * <p>
 * 使用注册的{@link com.bumptech.glide.load.model.ModelLoader}和一个模型来生成一系列的
 * {@link DataFetcher}的接口类
 */
interface DataFetcherGenerator {
    /**
     * Called when the generator has finished loading data from a {@link DataFetcher}.
     * <p>
     * 当从{@link DataFetcher}加载数据并生成结束时被调用
     */
    interface FetcherReadyCallback {

        /**
         * Requests that we call startNext() again on a Glide owned thread.
         */
        void reschedule();

        /**
         * Notifies the callback that the load is complete.
         *
         * @param sourceKey    The id of the loaded data.
         * @param data         The loaded data, or null if the load failed.
         * @param fetcher      The data fetcher we attempted to load from.
         * @param dataSource   The data source we were loading from.
         * @param attemptedKey The key we were loading data from (may be an alternate).
         */
        void onDataFetcherReady(Key sourceKey, @Nullable Object data, DataFetcher<?> fetcher,
                                DataSource dataSource, Key attemptedKey);

        /**
         * Notifies the callback when the load fails.
         *
         * @param attemptedKey The key we were using to load (may be an alternate).
         * @param e            The exception that caused the load to fail.
         * @param fetcher      The fetcher we were loading from.
         * @param dataSource   The data source we were loading from.
         */
        void onDataFetcherFailed(Key attemptedKey, Exception e, DataFetcher<?> fetcher,
                                 DataSource dataSource);
    }

    /**
     * Attempts to a single new {@link DataFetcher} and returns true if
     * a {@link DataFetcher} was started, and false otherwise.
     * <p>
     * 当一个新的{@link DataFetcher}已经开始则返回{@code true}（也可简单理解为若执行到了相应的
     * {@link DataFetcher#loadData(Priority, DataFetcher.DataCallback)}方法，那么就返回{@code true}）
     */
    boolean startNext();

    /**
     * Attempts to cancel the currently running fetcher.
     * <p>
     * This will be called on the main thread and should complete quickly.
     */
    void cancel();
}
