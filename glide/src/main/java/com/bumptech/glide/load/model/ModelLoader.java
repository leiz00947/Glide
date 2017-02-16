package com.bumptech.glide.load.model;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * A factory interface for translating an arbitrarily complex data model into a concrete data type
 * that can be used by an {@link DataFetcher} to obtain the data for a resource represented by the
 * model.
 * <p>
 * 一个用来转换一个任意合成的数据模型到一个可用来被{@link DataFetcher}获取所表示的资源模型的具体数据类型
 * <p>
 * This interface has two objectives:
 * 1. To translate a specific model into a data type that can be decoded into a resource.
 * 2. To allow a model to be combined with the dimensions of the view to fetch a resource of a
 * specific size.
 * <p>
 * 该接口有两个目的：
 * <ul><li>将一个特定模型转换成可用来被解码成资源的数据类型</li>
 * <li>允许一个模型附带取得的资源{@link android.view.View}的尺寸</li></ul>
 * <p>
 * This not only avoids having to duplicate dimensions in xml and in your code in order to determine
 * the size of a view on devices with different densities, but also allows you to use layout weights
 * or otherwise programmatically put the dimensions of the view without forcing you to fetch a
 * generic resource size.
 * <p>
 * 这样不仅仅避免了为了在不同尺寸的设备上显示而复制尺寸到XML文本和代码中，而且允许你使用布局的权重或
 * 编程方式来设置{@link android.view.View}的尺寸而不用迫使你要设置一个一般资源尺寸的大小
 * <p>
 * The smaller the resource you fetch, the less bandwidth and battery life you use, and the lower
 * your memory footprint per resource.
 * <p>
 * 这样一来获取的资源更小，更节省带宽和使用电量，并且降低了每个资源的内存占用
 *
 * @param <Model> The type of the model.
 * @param <Data>  The type of the data that can be used by a
 *                {@link com.bumptech.glide.load.ResourceDecoder} to decode a resource.
 */
public interface ModelLoader<Model, Data> {

    /**
     * Contains a set of {@link com.bumptech.glide.load.Key Keys} identifying the source of the load,
     * alternate cache keys pointing to equivalent data, and a
     * {@link DataFetcher} that can be used to fetch data not found in
     * cache.
     *
     * @param <Data> The type of data that well be loaded.
     */
    class LoadData<Data> {
        public final Key sourceKey;
        public final List<Key> alternateKeys;
        public final DataFetcher<Data> fetcher;

        public LoadData(Key sourceKey, DataFetcher<Data> fetcher) {
            this(sourceKey, Collections.<Key>emptyList(), fetcher);
        }

        public LoadData(Key sourceKey, List<Key> alternateKeys, DataFetcher<Data> fetcher) {
            this.sourceKey = Preconditions.checkNotNull(sourceKey);
            this.alternateKeys = Preconditions.checkNotNull(alternateKeys);
            this.fetcher = Preconditions.checkNotNull(fetcher);
        }
    }

    /**
     * Returns a {@link LoadData} containing a
     * {@link DataFetcher} required to decode the resource
     * represented by this model, as well as a set of {@link com.bumptech.glide.load.Key Keys} that
     * identify the data loaded by the {@link DataFetcher} as well as an
     * optional list of alternate keys from which equivalent data can be loaded. The
     * {@link DataFetcher} will not be used if the resource is already cached.
     * <p>
     * <p> Note - If no valid data fetcher can be returned (for example if a model has a null URL),
     * then it is acceptable to return a null data fetcher from this method. </p>
     *
     * @param model  The model representing the resource.
     * @param width  The width in pixels of the view or target the resource will be loaded into, or
     *               {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate that
     *               the resource should be loaded at its original width.
     * @param height The height in pixels of the view or target the resource will be loaded into, or
     *               {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate that
     *               the resource should be loaded at its original height.
     */
    @Nullable
    LoadData<Data> buildLoadData(Model model, int width, int height, Options options);

    /**
     * Returns true if the given model is a of a recognized type that this loader can probably load.
     * <p>
     * 判断传参是否是公认的正确格式（比如传参是一个Uri对象，判断该Uri是否是Asset格式的Uri，
     * {@link AssetUriLoader}）
     * <p>
     * For example, you may want multiple Uri -> InputStream loaders. One might handle media
     * store Uris, another might handle asset Uris, and a third might handle file Uris etc.
     * <p>
     * This method is generally expected to do no I/O and complete quickly, so best effort
     * results are acceptable. {@link ModelLoader ModelLoaders} that return true from this method may
     * return {@code null} from {@link #buildLoadData(Object, int, int, Options)}
     */
    boolean handles(Model model);
}
