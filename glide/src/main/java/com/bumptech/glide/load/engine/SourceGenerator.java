package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.bumptech.glide.util.LogTime;

import java.util.Collections;

/**
 * Generates {@link DataFetcher DataFetchers} from original source data
 * using registered {@link ModelLoader ModelLoaders} and the model
 * provided for the load.
 * <p>
 * {@link DataFetcherGenerator}接口的实现类
 * <p>
 * Depending on the disk cache strategy, source data may first be written to disk and then
 * loaded from the cache file rather than returned directly.
 * <p>
 * 获取到远程端的图片原始数据后，为什么在{@link #onDataReady(Object)}要跳转到
 * {@link DecodeJob#reschedule()}进行切换线程操作呢？
 */
class SourceGenerator implements DataFetcherGenerator,
        DataFetcher.DataCallback<Object>,
        DataFetcherGenerator.FetcherReadyCallback {
    private static final String TAG = "SourceGenerator";

    private final DecodeHelper<?> helper;
    private final FetcherReadyCallback cb;

    private int loadDataListIndex;
    private DataCacheGenerator sourceCacheGenerator;
    /**
     * 存放远程端图片的解码数据
     */
    private Object dataToCache;
    private volatile LoadData<?> loadData;
    private DataCacheKey originalKey;

    public SourceGenerator(DecodeHelper<?> helper, FetcherReadyCallback cb) {
        this.helper = helper;
        this.cb = cb;
    }

    /**
     * 调用{@link DataFetcher#loadData(Priority, DataFetcher.DataCallback)}加载远程端图片
     */
    @Override
    public boolean startNext() {
        if (dataToCache != null) {
            Object data = dataToCache;
            dataToCache = null;
            cacheData(data);
        }

        if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
            return true;
        }
        sourceCacheGenerator = null;

        loadData = null;
        boolean started = false;
        while (!started && hasNextModelLoader()) {
            loadData = helper.getLoadData().get(loadDataListIndex++);
            if (loadData != null
                    && (helper.getDiskCacheStrategy().isDataCacheable(loadData.fetcher.getDataSource())
                    || helper.hasLoadPath(loadData.fetcher.getDataClass()))) {
                started = true;
                loadData.fetcher.loadData(helper.getPriority(), this);
            }
        }
        return started;
    }

    private boolean hasNextModelLoader() {
        return loadDataListIndex < helper.getLoadData().size();
    }

    /**
     * 将获取到的原始数据写入到缓存文件
     */
    private void cacheData(Object dataToCache) {
        long startTime = LogTime.getLogTime();
        try {
            Encoder<Object> encoder = helper.getSourceEncoder(dataToCache);
            DataCacheWriter<Object> writer =
                    new DataCacheWriter<>(encoder, dataToCache, helper.getOptions());
            originalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
            helper.getDiskCache().put(originalKey, writer);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Finished encoding source to cache"
                        + ", key: " + originalKey
                        + ", data: " + dataToCache
                        + ", encoder: " + encoder
                        + ", duration: " + LogTime.getElapsedMillis(startTime));
            }
        } finally {
            loadData.fetcher.cleanup();
        }

        sourceCacheGenerator =
                new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), helper, this);
    }

    @Override
    public void cancel() {
        LoadData<?> local = loadData;
        if (local != null) {
            local.fetcher.cancel();
        }
    }

    @Override
    public void onDataReady(Object data) {
        DiskCacheStrategy diskCacheStrategy = helper.getDiskCacheStrategy();
        if (data != null && diskCacheStrategy.isDataCacheable(loadData.fetcher.getDataSource())) {
            dataToCache = data;
            /**
             * We might be being called back on someone else's thread. Before doing anything,
             * we should reschedule to get back onto Glide's thread.
             */
            cb.reschedule();
        } else {
            cb.onDataFetcherReady(loadData.sourceKey, data, loadData.fetcher,
                    loadData.fetcher.getDataSource(), originalKey);
        }
    }

    @Override
    public void onLoadFailed(Exception e) {
        cb.onDataFetcherFailed(originalKey, e, loadData.fetcher, loadData.fetcher.getDataSource());
    }

    @Override
    public void reschedule() {
        // We don't expect this to happen, although if we ever need it to we can delegate to our
        // callback.
        throw new UnsupportedOperationException();
    }

    // Called from source cache generator.
    @Override
    public void onDataFetcherReady(Key sourceKey, Object data, DataFetcher<?> fetcher,
                                   DataSource dataSource, Key attemptedKey) {
        // This data fetcher will be loading from a File and provide the wrong data source, so override
        // with the data source of the original fetcher
        cb.onDataFetcherReady(sourceKey, data, fetcher, loadData.fetcher.getDataSource(), sourceKey);
    }

    @Override
    public void onDataFetcherFailed(Key sourceKey, Exception e, DataFetcher<?> fetcher,
                                    DataSource dataSource) {
        cb.onDataFetcherFailed(sourceKey, e, fetcher, loadData.fetcher.getDataSource());
    }
}
