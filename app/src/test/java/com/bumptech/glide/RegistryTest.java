package com.bumptech.glide;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.samples.BuildConfig;
import com.bumptech.glide.samples.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;

/**
 * Created by Zsago on 2017/2/20.
 *
 * @since 1.0.0
 */
@RunWith(value = RobolectricTestRunner.class)
@Config(sdk = 21, constants = BuildConfig.class)
public class RegistryTest {
    private MainActivity mActivity;

    @Before
    public void start() {
        mActivity = Robolectric.setupActivity(MainActivity.class);
    }

    @Test
    public void testSystem() {
        Glide.with(mActivity)
                .load("http://www.clker.com/cliparts/u/Z/2/b/a/6/android-toy-h.svg")
                .into(new ImageView(mActivity));
    }

    @Test
    public void testDiskCachePath() {
        InternalCacheDiskCacheFactory internalCache = new InternalCacheDiskCacheFactory(mActivity);
        internalCache.build();
        ExternalCacheDiskCacheFactory externalCache = new ExternalCacheDiskCacheFactory(mActivity);
        externalCache.build();
        System.out.println("internal path:"+mActivity.getCacheDir().getPath());
//        System.out.println("external path:"+mActivity.getExternalCacheDir().getPath());
    }

    @Test
    public void testGetModelLoaders() {
        List<ModelLoader<String, ?>> list = Glide.get(mActivity).getRegistry()
                .getModelLoaders(Mockito.anyString());
        System.out.println(list);
    }

    /**
     * <ul>结合README.md文件，进行如下流程：
     * <li>先在{@link com.bumptech.glide.load.model.MultiModelLoaderFactory#entries}
     * 中查找其中第一二个传参为{@code String.class}和{@code Object.class}的对象，
     * 可知符合条件的有两个InputStream和一个ParcelFileDescriptor，在List中存放不重复，
     * 所以也就InputStream和ParcelFileDescriptor</li>
     * <li>在{@link com.bumptech.glide.provider.ResourceDecoderRegistry#decoders}中分别匹配第一个
     * 参数为InputStream和ParcelFileDescriptor的项，并返回每项第二个参数的集合，可知，InputStream
     * 匹配的项的返回集合中有：GifDrawable、Bitmap、BitmapDrawable，而ParcelFileDescriptor的则有：
     * Bitmap和BitmapDrawable</li>
     * <li>其中GifDrawable和BitmapDrawable的基类都是{@code Drawable.class}所以确认添加到返回集合中去，
     * 而在{@link com.bumptech.glide.load.resource.transcode.TranscoderRegistry#transcoders}
     * 中有一个项的第一二个参数的基类恰分别是Bitmap和Drawable，所以Bitmap也确认添加到返回的集合中去</li></ul>
     */
    @Test
    public void testGetRegisteredResourceClasses() {
        List<Class<?>> list = Glide.get(mActivity).getRegistry()
                .getRegisteredResourceClasses(String.class, Object.class, Drawable.class);
        System.out.println(list);
    }

    @Test
    public void testGetModelLoadersOfFile() {
        File file = new File("file");
        List<ModelLoader<File, ?>> list = Glide.get(mActivity).getRegistry()
                .getModelLoaders(file);
        System.out.println(list);
    }
}
