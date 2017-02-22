package com.bumptech.glide.load.model;

import android.net.Uri;

import com.bumptech.glide.load.Options;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles http/https Uris by delegating to the {@link ModelLoader} for {@link GlideUrl GlideUrls}.
 * <p>
 * 用来处理http/https Uri的{@link ModelLoader}
 * <p>
 * 就功能上来说，该类和{@link com.bumptech.glide.load.model.stream.HttpUriLoader}类似，
 * 只是前者加载完成后获取到的是数据流{@link InputStream}，而该类是泛型
 *
 * @param <Data> The type of data this Loader will obtain for a {@link Uri}.
 */
public class UrlUriLoader<Data> implements ModelLoader<Uri, Data> {
    private static final Set<String> SCHEMES = Collections.unmodifiableSet(
            new HashSet<>(
                    Arrays.asList(
                            "http",
                            "https"
                    )
            )
    );
    private final ModelLoader<GlideUrl, Data> urlLoader;

    public UrlUriLoader(ModelLoader<GlideUrl, Data> urlLoader) {
        this.urlLoader = urlLoader;
    }

    @Override
    public LoadData<Data> buildLoadData(Uri uri, int width, int height, Options options) {
        GlideUrl glideUrl = new GlideUrl(uri.toString());
        return urlLoader.buildLoadData(glideUrl, width, height, options);
    }

    @Override
    public boolean handles(Uri uri) {
        return SCHEMES.contains(uri.getScheme());
    }

    /**
     * Loads {@link InputStream InputStreams} from {@link Uri Uris} with http
     * or https schemes.
     * <p>
     * 就功能上来说，和{@link com.bumptech.glide.load.model.stream.HttpUriLoader.Factory}等价
     */
    public static class StreamFactory implements ModelLoaderFactory<Uri, InputStream> {

        @Override
        public ModelLoader<Uri, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new UrlUriLoader<>(multiFactory.build(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
