package com.bumptech.glide.load.data;

import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fetches an {@link InputStream} for an asset path.
 * <p>
 * 获取指定的资源目录的输入流
 */
public class StreamAssetPathFetcher extends AssetPathFetcher<InputStream> {
    public StreamAssetPathFetcher(AssetManager assetManager, String assetPath) {
        super(assetManager, assetPath);
    }

    @Override
    protected InputStream loadResource(AssetManager assetManager, String path) throws IOException {
        return assetManager.open(path);
    }

    @Override
    protected void close(InputStream data) throws IOException {
        data.close();
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }
}
