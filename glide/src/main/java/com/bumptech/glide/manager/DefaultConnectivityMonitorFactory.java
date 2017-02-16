package com.bumptech.glide.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

/**
 * A factory class that produces a functional {@link ConnectivityMonitor}
 * if the application has the {@code android.permission.ACCESS_NETWORK_STATE} permission and a no-op
 * non functional {@link ConnectivityMonitor} if the app does not have
 * the required permission.
 * <p>
 * 如果应用获得了{@code android.permission.ACCESS_NETWORK_STATE}权限，那么就返回一个
 * {@link DefaultConnectivityMonitor}实例，否则返回一个{@link NullConnectivityMonitor}实例
 */
public class DefaultConnectivityMonitorFactory implements ConnectivityMonitorFactory {
    @NonNull
    public ConnectivityMonitor build(
            @NonNull Context context,
            @NonNull ConnectivityMonitor.ConnectivityListener listener) {
        final int res = context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE");
        final boolean hasPermission = res == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            return new DefaultConnectivityMonitor(context, listener);
        } else {
            return new NullConnectivityMonitor();
        }
    }
}
