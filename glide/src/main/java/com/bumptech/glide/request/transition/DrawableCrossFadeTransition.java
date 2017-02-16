package com.bumptech.glide.request.transition;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

/**
 * A cross fade {@link Transition} for {@link Drawable}s that uses an
 * {@link TransitionDrawable} to transition from an existing drawable
 * already visible on the target to a new drawable. If no existing drawable exists, this class can
 * instead fall back to a default animation that doesn't rely on {@link TransitionDrawable}.
 * <p>
 * 淡入淡出{@link Transition}的实现类，使用一个{@link TransitionDrawable}的{@link Drawable}
 * 将一个已经显示在{@link com.bumptech.glide.request.target.Target}上已存在的{@link Drawable}
 * 过渡转换成一个新的{@link Drawable}中（{@link TransitionDrawable}的渐变效果的实现）
 */
public class DrawableCrossFadeTransition implements Transition<Drawable> {
    private final Transition<Drawable> defaultAnimation;
    private final int duration;
    private final boolean isCrossFadeEnabled;

    /**
     * Constructor that takes a default animation and a duration in milliseconds that the cross fade
     * animation should last.
     *
     * @param defaultAnimation   The {@link Transition} to use if there is no previous
     *                           {@link Drawable} (either a placeholder or previous resource) to
     *                           transition from.
     * @param duration           The duration that the cross fade animation should run if there is something to
     *                           cross fade from when a new {@link Drawable} is put.
     * @param isCrossFadeEnabled If {@code true}, animates the previous resource's alpha to 0 while
     *                           animating the new resource's alpha to 100. Otherwise, only animates
     *                           the new resource's alpha to 100 while leaving the previous resource's
     *                           alpha at 100. See
     *                           {@link TransitionDrawable#setCrossFadeEnabled(boolean)}.
     */
    public DrawableCrossFadeTransition(Transition<Drawable> defaultAnimation, int duration,
                                       boolean isCrossFadeEnabled) {
        this.defaultAnimation = defaultAnimation;
        this.duration = duration;
        this.isCrossFadeEnabled = isCrossFadeEnabled;
    }

    /**
     * Animates from the previous drawable to the current drawable in one of two ways.
     * <p>
     * <ol> <li>Using the default animation provided in the constructor if the previous drawable is
     * null</li> <li>Using the cross fade animation with the duration provided in the constructor if
     * the previous drawable is non null</li> </ol>
     *
     * @param current {@inheritDoc}
     * @param adapter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean transition(Drawable current, ViewAdapter adapter) {
        Drawable previous = adapter.getCurrentDrawable();
        if (previous != null) {
            TransitionDrawable transitionDrawable =
                    new TransitionDrawable(new Drawable[]{previous, current});
            transitionDrawable.setCrossFadeEnabled(isCrossFadeEnabled);
            transitionDrawable.startTransition(duration);
            adapter.setDrawable(transitionDrawable);
            return true;
        } else {
            defaultAnimation.transition(current, adapter);
            return false;
        }
    }
}
