package com.bumptech.glide.request;

/**
 * An interface for coordinating multiple requests with the same {@link
 * com.bumptech.glide.request.target.Target}.
 * <p>
 * 用同一个{@link com.bumptech.glide.request.target.Target}整合多个{@link Request}
 */
public interface RequestCoordinator {

    /**
     * Returns true if the {@link Request} can display a loaded bitmap.
     * <p>
     * 返回请求加载的{@link android.graphics.Bitmap}是否可以显示出来
     *
     * @param request The {@link Request} requesting permission to display a bitmap.
     */
    boolean canSetImage(Request request);

    /**
     * Returns true if the {@link Request} can display a placeholder.
     * <p>
     * 用来表示请求图片正在加载时是否显示一张特定的图片
     *
     * @param request The {@link Request} requesting permission to display a placeholder.
     */
    boolean canNotifyStatusChanged(Request request);

    /**
     * Returns true if any coordinated {@link Request} has successfully completed.
     *
     * @see Request#isComplete()
     */
    boolean isAnyResourceSet();

    /**
     * Must be called when a {@link Request} coordinated by this object completes successfully.
     */
    void onRequestSuccess(Request request);

    /**
     * Must be called when a {@link Request} coordinated by this object fails.
     */
    void onRequestFailed(Request request);
}
