package com.bumptech.glide.request.target;

/**
 * A callback that must be called when the target has determined its size. For fixed size targets it
 * can be called synchronously.
 * <p>
 * 当{@link Target}确定了大小那么就会调用该接口方法
 */
public interface SizeReadyCallback {
    /**
     * A callback called on the main thread.
     *
     * @param width  The width in pixels of the target, or {@link Target#SIZE_ORIGINAL} to indicate
     *               that we want the resource at its original width.
     * @param height The height in pixels of the target, or {@link Target#SIZE_ORIGINAL} to indicate
     *               that we want the resource at its original height.
     */
    void onSizeReady(int width, int height);
}
