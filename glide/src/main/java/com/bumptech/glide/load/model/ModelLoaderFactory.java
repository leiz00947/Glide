package com.bumptech.glide.load.model;

import android.content.Context;

import com.bumptech.glide.Registry;

/**
 * An interface for creating a {@link ModelLoader} for a given model type. Will be retained
 * statically so should not retain {@link Context} or any other objects that cannot be retained for
 * the life of the application. ModelLoaders will not be retained statically so it is safe for any
 * ModelLoader built by this factory to retain a reference to a {@link Context}.
 * <p>
 * 一个用于创建给定模型{@link ModelLoader}的接口类，由于要被静态保留，所以不应该持有{@link Context}
 * 或任何不应该在整个应用周期内保留的对象，{@link ModelLoader}不会被静态保留，所以对于任何通过该类来
 * 创建保留持有{@link Context}引用的{@link ModelLoader}是安全的
 *
 * @param <T> The type of the model the {@link ModelLoader}s built by this factory can handle
 *            <p>用来被该工厂类创建{@link ModelLoader}的模型</p>
 * @param <Y> The type of data the {@link ModelLoader}s built by this factory can load.
 */
public interface ModelLoaderFactory<T, Y> {

    /**
     * Build a concrete ModelLoader for this model type.
     * <p>
     * 根据给定的模型来创建具体的{@link ModelLoader}
     *
     * @param multiFactory A map of classes to factories that can be used to construct additional
     *                     {@link ModelLoader}s that this factory's {@link ModelLoader} may depend on
     * @return A new {@link ModelLoader}
     */
    ModelLoader<T, Y> build(MultiModelLoaderFactory multiFactory);

    /**
     * A lifecycle method that will be called when this factory is about to replaced.
     * <p>
     * 一个生命周期方法，当该工厂类将要被替换时会被调用，即当重写
     * {@link com.bumptech.glide.module.GlideModule}，并在其实现的
     * {@link com.bumptech.glide.module.GlideModule#registerComponents(Context, Registry)}方法中
     * 调用了{@link Registry#replace(Class, Class, ModelLoaderFactory)}的时候回调
     * <p>
     * 事实上，通过所有实现该接口的类的源码看出，所有实现该接口方法中都未实现任何动作
     */
    void teardown();
}
