package com.bumptech.glide.manager;

/**
 * A {@link Lifecycle} implementation for tracking and notifying
 * listeners of {@link android.app.Application} lifecycle events.
 * <p>
 * 跟踪并通知{@link android.app.Application}的生命周期事件的监听器
 * <p>
 * Since there are essentially no {@link android.app.Application} lifecycle events, this class
 * simply defaults to notifying new listeners that they are started.
 */
class ApplicationLifecycle implements Lifecycle {
    @Override
    public void addListener(LifecycleListener listener) {
        listener.onStart();
    }

    @Override
    public void removeListener(LifecycleListener listener) {
        // Do nothing.
    }
}
