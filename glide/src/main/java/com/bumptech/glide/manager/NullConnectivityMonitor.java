package com.bumptech.glide.manager;

/**
 * A no-op {@link ConnectivityMonitor}.
 * <p>
 * 一个无操作的{@link ConnectivityMonitor}的实现类
 */
class NullConnectivityMonitor implements ConnectivityMonitor {

    @Override
    public void onStart() {
        // Do nothing.
    }

    @Override
    public void onStop() {
        // Do nothing.
    }

    @Override
    public void onDestroy() {
        // Do nothing.
    }
}
