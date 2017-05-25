package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;

import com.bumptech.glide.request.Request;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A base {@link Target} for loading {@link android.graphics.Bitmap}s into {@link View}s that
 * provides default implementations for most most methods and can determine the size of views using
 * a {@link ViewTreeObserver.OnDrawListener}.
 * <p>
 * 继承{@link BaseTarget}的抽象类，用来加载{@link android.graphics.Bitmap}到默认实现大多数方法并可以
 * 通过使用一个{@link ViewTreeObserver.OnDrawListener}来决定其大小的{@link View}
 * <p>
 * To detect {@link View} reuse in {@link android.widget.ListView} or any {@link
 * android.view.ViewGroup} that reuses views, this class uses the {@link View#setTag(Object)} method
 * to store some metadata so that if a view is reused, any previous loads or resources from previous
 * loads can be cancelled or reused.
 * <p>
 * Any calls to {@link View#setTag(Object)}} on a View given to this class will result in
 * excessive allocations and and/or {@link IllegalArgumentException}s. If you must call {@link
 * View#setTag(Object)} on a view, consider using {@link BaseTarget} or {@link SimpleTarget}
 * instead.
 * <p>
 * Subclasses must call super in {@link #onLoadCleared(Drawable)}
 *
 * @param <T> The specific subclass of view wrapped by this target.
 * @param <Z> The resource type this target will receive.
 */
public abstract class ViewTarget<T extends View, Z> extends BaseTarget<Z> {
    private static final String TAG = "ViewTarget";
    private static boolean isTagUsedAtLeastOnce = false;
    @Nullable
    private static Integer tagId = null;

    protected final T view;
    private final SizeDeterminer sizeDeterminer;

    public ViewTarget(T view) {
        this.view = Preconditions.checkNotNull(view);
        sizeDeterminer = new SizeDeterminer(view);
    }

    /**
     * Returns the wrapped {@link View}.
     */
    public T getView() {
        return view;
    }

    /**
     * Determines the size of the view by first checking {@link View#getWidth()} and
     * {@link View#getHeight()}. If one or both are zero, it then checks the view's
     * {@link LayoutParams}. If one or both of the params width and height are less than or equal to
     * zero, it then adds an {@link ViewTreeObserver.OnPreDrawListener} which waits until
     * the view has been measured before calling the callback with the view's drawn width and height.
     *
     * @param cb {@inheritDoc}
     */
    @Override
    public void getSize(SizeReadyCallback cb) {
        sizeDeterminer.getSize(cb);
    }

    @Override
    public void removeCallback(SizeReadyCallback cb) {
        sizeDeterminer.removeCallback(cb);
    }

    @Override
    public void onLoadCleared(Drawable placeholder) {
        super.onLoadCleared(placeholder);
        sizeDeterminer.clearCallbacksAndListener();
    }

    /**
     * Stores the request using {@link View#setTag(Object)}.
     *
     * @param request {@inheritDoc}
     */
    @Override
    public void setRequest(@Nullable Request request) {
        setTag(request);
    }

    /**
     * Returns any stored request using {@link View#getTag()}.
     * <p>
     * <p> For Glide to function correctly, Glide must be the only thing that calls {@link
     * View#setTag(Object)}. If the tag is cleared or put to another object type, Glide will not be
     * able to retrieve and cancel previous loads which will not only prevent Glide from reusing
     * resource, but will also result in incorrect images being loaded and lots of flashing of images
     * in lists. As a result, this will throw an {@link IllegalArgumentException} if {@link
     * View#getTag()}} returns a non null object that is not an {@link
     * com.bumptech.glide.request.Request}. </p>
     */
    @Override
    @Nullable
    public Request getRequest() {
        Object tag = getTag();
        Request request = null;
        if (tag != null) {
            if (tag instanceof Request) {
                request = (Request) tag;
            } else {
                throw new IllegalArgumentException(
                        "You must not call setTag() on a view Glide is targeting");
            }
        }
        return request;
    }

    @Override
    public String toString() {
        return "Target for: " + view;
    }

    private void setTag(@Nullable Object tag) {
        if (tagId == null) {
            isTagUsedAtLeastOnce = true;
            view.setTag(tag);
        } else {
            view.setTag(tagId, tag);
        }
    }

    @Nullable
    private Object getTag() {
        if (tagId == null) {
            return view.getTag();
        } else {
            return view.getTag(tagId);
        }
    }

    /**
     * Sets the android resource id to use in conjunction with {@link View#setTag(int, Object)}
     * to store temporary state allowing loads to be automatically cancelled and resources re-used
     * in scrolling lists.
     * <p>
     * <p>
     * If no tag id is set, Glide will use {@link View#setTag(Object)}.
     * </p>
     * <p>
     * <p>
     * Warning: prior to Android 4.0 tags were stored in a static map. Using this method prior
     * to Android 4.0 may cause memory leaks and isn't recommended. If you do use this method
     * on older versions, be sure to call {@link com.bumptech.glide.RequestManager#clear(View)} on
     * any view you start a load into to ensure that the static state is removed.
     * </p>
     *
     * @param tagId The android resource to use.
     */
    public static void setTagId(int tagId) {
        if (ViewTarget.tagId != null || isTagUsedAtLeastOnce) {
            throw new IllegalArgumentException("You cannot set the tag id more than once or change"
                    + " the tag id after the first request has been made");
        }
        ViewTarget.tagId = tagId;
    }

    private static class SizeDeterminer {
        // Some negative sizes (Target.SIZE_ORIGINAL) are valid, 0 is never valid.
        private static final int PENDING_SIZE = 0;
        private final View view;
        private final List<SizeReadyCallback> cbs = new ArrayList<>();

        @Nullable
        private SizeDeterminerLayoutListener layoutListener;

        SizeDeterminer(View view) {
            this.view = view;
        }

        /**
         * 通知控件宽高值已变化
         */
        private void notifyCbs(int width, int height) {
            for (SizeReadyCallback cb : cbs) {
                cb.onSizeReady(width, height);
            }
        }

        /**
         * 获取验证当前控件宽高值，然后回调宽高值变化接口
         */
        @Synthetic
        void checkCurrentDimens() {
            if (cbs.isEmpty()) {
                return;
            }

            int currentWidth = getTargetWidth();
            int currentHeight = getTargetHeight();
            if (!isViewStateAndSizeValid(currentWidth, currentHeight)) {
                return;
            }

            notifyCbs(currentWidth, currentHeight);
            clearCallbacksAndListener();
        }

        /**
         * 一般的，获取控件的宽高度值，若设置了任意的{@code android:padding}，也要减去，然后通知调用
         * {@link SizeReadyCallback#onSizeReady(int, int)}
         * <p>
         * 若获取控件宽高值失败，说明控件宽高值还未计算出来，需要借助{@link ViewTreeObserver}和
         * {@link ViewTreeObserver.OnPreDrawListener}来实现
         *
         * @see com.bumptech.glide.request.SingleRequest#onSizeReady(int, int)
         */
        void getSize(SizeReadyCallback cb) {
            int currentWidth = getTargetWidth();
            int currentHeight = getTargetHeight();
            if (isViewStateAndSizeValid(currentWidth, currentHeight)) {
                cb.onSizeReady(currentWidth, currentHeight);
                return;
            }

            // We want to notify callbacks in the order they were added and we only expect one or two
            // callbacks to be added a time, so a List is a reasonable choice.
            if (!cbs.contains(cb)) {
                cbs.add(cb);
            }
            if (layoutListener == null) {
                ViewTreeObserver observer = view.getViewTreeObserver();
                layoutListener = new SizeDeterminerLayoutListener(this);
                observer.addOnPreDrawListener(layoutListener);
            }
        }

        void removeCallback(SizeReadyCallback cb) {
            cbs.remove(cb);
        }

        void clearCallbacksAndListener() {
            // Keep a reference to the layout listener and remove it here
            // rather than having the observer remove itself because the observer
            // we add the listener to will be almost immediately merged into
            // another observer and will therefore never be alive. If we instead
            // keep a reference to the listener and remove it here, we get the
            // current view tree observer and should succeed.
            ViewTreeObserver observer = view.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnPreDrawListener(layoutListener);
            }
            layoutListener = null;
            cbs.clear();
        }

        private boolean isViewStateAndSizeValid(int width, int height) {
            return isViewStateValid() && isSizeValid(width) && isSizeValid(height);
        }

        private boolean isViewStateValid() {
            // We consider the view state as valid if the view has
            // non-null layout params and a non-zero layout width and height.
            if (view.getLayoutParams() != null
                    && view.getLayoutParams().width > 0
                    && view.getLayoutParams().height > 0) {
                return true;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return view.isLaidOut();
            }
            return !view.isLayoutRequested();
        }

        /**
         * 获取控件的高度数值
         */
        private int getTargetHeight() {
            int verticalPadding = view.getPaddingTop() + view.getPaddingBottom();
            LayoutParams layoutParams = view.getLayoutParams();
            int layoutParamSize = layoutParams != null ? layoutParams.height : PENDING_SIZE;
            return getTargetDimen(view.getHeight(), layoutParamSize, verticalPadding);
        }

        /**
         * 获取控件的宽度数值
         */
        private int getTargetWidth() {
            int horizontalPadding = view.getPaddingLeft() + view.getPaddingRight();
            LayoutParams layoutParams = view.getLayoutParams();
            int layoutParamSize = layoutParams != null ? layoutParams.width : PENDING_SIZE;
            return getTargetDimen(view.getWidth(), layoutParamSize, horizontalPadding);
        }

        /**
         * 若控件的宽高属性设置为自适应（{@link LayoutParams#WRAP_CONTENT}）,则返回当前设备屏幕的
         * 宽或高的像素值
         */
        private int getTargetDimen(int viewSize, int paramSize, int paddingSize) {
            int adjustedViewSize = viewSize - paddingSize;
            if (isSizeValid(adjustedViewSize)) {
                return adjustedViewSize;
            }

            if (paramSize == PENDING_SIZE) {
                return PENDING_SIZE;
            }

            if (paramSize == LayoutParams.WRAP_CONTENT) {
                return SIZE_ORIGINAL;
            } else if (paramSize > 0) {
                return paramSize - paddingSize;
            } else {
                return PENDING_SIZE;
            }
        }

        /**
         * 判断数值是否可取，即若传参为{@link LayoutParams#WRAP_CONTENT}或任何大于0的值，
         * 则返回{@code true}，否则返回{@code false}
         */
        private boolean isSizeValid(int size) {
            return size > 0 || size == SIZE_ORIGINAL;
        }

        /**
         * {@link ViewTreeObserver.OnPreDrawListener}即绘图监听接口
         */
        private static class SizeDeterminerLayoutListener implements ViewTreeObserver
                .OnPreDrawListener {
            private final WeakReference<SizeDeterminer> sizeDeterminerRef;

            SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
                sizeDeterminerRef = new WeakReference<>(sizeDeterminer);
            }

            @Override
            public boolean onPreDraw() {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "OnGlobalLayoutListener called listener=" + this);
                }
                SizeDeterminer sizeDeterminer = sizeDeterminerRef.get();
                if (sizeDeterminer != null) {
                    sizeDeterminer.checkCurrentDimens();
                }
                return true;
            }
        }
    }
}
