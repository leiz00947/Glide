package com.bumptech.glide.manager;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * A factory class that produces a functional {@link ConnectivityMonitor}.
 * <p>
 * 生产{@link ConnectivityMonitor}对象的工厂
 */
public interface ConnectivityMonitorFactory {
    @NonNull
    ConnectivityMonitor build(@NonNull Context context, @NonNull ConnectivityMonitor.ConnectivityListener listener);
}
