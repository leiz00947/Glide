package com.bumptech.glide.request.transition;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * An interface that allows a transition to be applied to {@link View}s in {@link
 * com.bumptech.glide.request.target.Target}s in across resource types. Targets that wrap views will
 * be able to provide all of the necessary arguments and start the transition. Those that do not
 * will be unable to provide the necessary arguments and will therefore be forced to ignore the
 * transition. This interface is a compromise that allows view specific transition in Glide's
 * complex world of arbitrary resource types and arbitrary target types.
 * <p>
 * 一个允许用{@link com.bumptech.glide.request.target.Target}将过渡转变应用到{@link View}的接口类
 * （{@link android.graphics.drawable.TransitionDrawable}实现渐变效果）
 *
 * @param <R> The type of the resource whose entrance will be transitioned.
 *            <p>要被过渡转变的资源入口类型</p>
 */
public interface Transition<R> {

    /**
     * An interface wrapping a view that exposes the necessary methods to run the various types of
     * android animations as transitions: ({@link ViewTransition}, {@link ViewPropertyTransition} and
     * animated {@link Drawable}s).
     * <p>
     * 一个封装了可执行各种各样Android动画的必要公共方法的一个View的接口
     */
    interface ViewAdapter {
        /**
         * Returns the wrapped {@link View}.
         */
        View getView();

        /**
         * Returns the current drawable being displayed in the view, or null if no such drawable exists
         * (or one cannot be retrieved).
         */
        @Nullable
        Drawable getCurrentDrawable();

        /**
         * Sets the current drawable (usually an animated drawable) to display in the wrapped view.
         *
         * @param drawable The drawable to display in the wrapped view.
         */
        void setDrawable(Drawable drawable);
    }

    /**
     * Animates from the previous {@link Drawable} that is currently being
     * displayed in the given view, if not null, to the new resource that should be displayed in the
     * view.
     *
     * @param current The new resource that will be displayed in the view.
     * @param adapter The {@link ViewAdapter} wrapping a view that can at least return an
     *                {@link View} from {@link ViewAdapter#getView()}.
     * @return True if in the process of running the transition, the new resource was put on the view,
     * false if the caller needs to manually put the current resource on the view.
     */
    boolean transition(R current, ViewAdapter adapter);
}
