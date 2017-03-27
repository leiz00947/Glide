package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

/**
 * A wrapper drawable to square the wrapped drawable so that it expands to fill a square with
 * exactly the given side length. The goal of this drawable is to ensure that square thumbnail
 * drawables always match the size of the view they will be displayed in to avoid a costly
 * requestLayout call. This class should not be used with views or drawables that are not square.
 * <p>
 * 将封装的Drawable形成方形以便用给定的精确边长来扩充一个方形，该类不应该在不是方形的View或Drawable中使用
 */
public class FixedSizeDrawable extends Drawable {
    private final Matrix matrix;
    private final RectF wrappedRect;
    private final RectF bounds;
    private Drawable wrapped;
    private State state;
    private boolean mutated;

    public FixedSizeDrawable(Drawable wrapped, int width, int height) {
        this(new State(wrapped.getConstantState(), width, height), wrapped);
    }

    FixedSizeDrawable(State state, Drawable wrapped) {
        this.state = Preconditions.checkNotNull(state);
        this.wrapped = Preconditions.checkNotNull(wrapped);

        // We will do our own scaling.
        wrapped.setBounds(0, 0, wrapped.getIntrinsicWidth(), wrapped.getIntrinsicHeight());

        matrix = new Matrix();
        /**
         * {@link Drawable#getIntrinsicWidth()}和{@link Drawable#getIntrinsicHeight()}分别获取
         * 其所在对应设备上显示的实际宽高度值
         */
        wrappedRect = new RectF(0, 0, wrapped.getIntrinsicWidth(), wrapped.getIntrinsicHeight());
        bounds = new RectF();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        bounds.set(left, top, right, bottom);
        updateMatrix();
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        this.bounds.set(bounds);
        updateMatrix();
    }

    private void updateMatrix() {
        matrix.setRectToRect(wrappedRect, this.bounds, Matrix.ScaleToFit.CENTER);
    }

    @Override
    public void setChangingConfigurations(int configs) {
        wrapped.setChangingConfigurations(configs);
    }

    @Override
    public int getChangingConfigurations() {
        return wrapped.getChangingConfigurations();
    }

    @Override
    public void setDither(boolean dither) {
        wrapped.setDither(dither);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        wrapped.setFilterBitmap(filter);
    }

    @Override
    public Callback getCallback() {
        return wrapped.getCallback();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public int getAlpha() {
        return wrapped.getAlpha();
    }

    @Override
    public void setColorFilter(int color, PorterDuff.Mode mode) {
        wrapped.setColorFilter(color, mode);
    }

    @Override
    public void clearColorFilter() {
        wrapped.clearColorFilter();
    }

    @Override
    public Drawable getCurrent() {
        return wrapped.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return wrapped.setVisible(visible, restart);
    }

    @Override
    public int getIntrinsicWidth() {
        return state.width;
    }

    @Override
    public int getIntrinsicHeight() {
        return state.height;
    }

    @Override
    public int getMinimumWidth() {
        return wrapped.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return wrapped.getMinimumHeight();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return wrapped.getPadding(padding);
    }

    @Override
    public void invalidateSelf() {
        super.invalidateSelf();
        wrapped.invalidateSelf();
    }

    @Override
    public void unscheduleSelf(Runnable what) {
        super.unscheduleSelf(what);
        wrapped.unscheduleSelf(what);
    }

    @Override
    public void scheduleSelf(Runnable what, long when) {
        super.scheduleSelf(what, when);
        wrapped.scheduleSelf(what, when);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.concat(matrix);
        wrapped.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(int i) {
        wrapped.setAlpha(i);
    }

    /**
     * 处理背景颜色，比如实现滤镜效果等
     *
     * @see {@link PorterDuff.Mode}
     */
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        wrapped.setColorFilter(colorFilter);
    }

    /**
     * 获取该{@link Drawable}对象的透明度信息
     *
     * @see {@link android.graphics.PixelFormat#OPAQUE}
     * @see {@link android.graphics.PixelFormat#TRANSLUCENT}
     * @see {@link android.graphics.PixelFormat#TRANSPARENT}
     * @see {@link android.graphics.PixelFormat#UNKNOWN}
     */
    @Override
    public int getOpacity() {
        return wrapped.getOpacity();
    }

    /**
     * 使这个{@link Drawable}变得状态不定，这个操作不能还原（变为不定后就不能变为原来的状态）
     * <p>
     * 一个状态不定的{@link Drawable}可以保证它不与其他任何一个{@link Drawable}共享它的状态，
     * 这对于你需要更改从同一资源加载来的{@link Drawable}的属性是非常有用
     * <p>
     * 默认情况下，所有的从同一资源（R.drawable.XXX）加载来的{@link Drawable}实例都共享一个共用的状态，
     * 如果你更改一个实例的状态，其他所有的实例都会收到相同的通知
     * <p>
     * 这个方法对于已经是{@code mutable}的{@link Drawable}没有效果
     *
     * @return
     */
    @Override
    public Drawable mutate() {
        if (!mutated && super.mutate() == this) {
            wrapped = wrapped.mutate();
            state = new State(state);
            mutated = true;
        }
        return this;
    }

    @Override
    public ConstantState getConstantState() {
        return state;
    }

    /**
     * 每个{@link Drawable}对象都关联一个{@link ConstantState}对象，这是为了保存{@link Drawable}
     * 对象的一些恒定不变的数据，如果从同一个资源中创建的{@link Drawable}对象，为了节约内存，
     * 它们会共享同一个{@link ConstantState}对象
     * <p>
     * 比如一个{@link android.graphics.drawable.ColorDrawable}对象，它会关联一个
     * {@link android.graphics.drawable.ColorDrawable.ColorState}对象，颜色值就保存在该对象中，
     * 如果修改{@link android.graphics.drawable.ColorDrawable}的颜色值，会修改到
     * {@link android.graphics.drawable.ColorDrawable.ColorState}的值，会导致和其关联的所有的
     * {@link android.graphics.drawable.ColorDrawable}的颜色都改变，在修改
     * {@link android.graphics.drawable.ColorDrawable}的属性时，需要先调用{@link Drawable#mutate()}
     * 让{@link Drawable}复制一个新的{@link android.graphics.drawable.Drawable.ConstantState}对象关联
     */
    static class State extends ConstantState {
        private final ConstantState wrapped;
        @Synthetic
        final int width;
        @Synthetic
        final int height;

        State(State other) {
            this(other.wrapped, other.width, other.height);
        }

        State(ConstantState wrapped, int width, int height) {
            this.wrapped = wrapped;
            this.width = width;
            this.height = height;
        }

        @Override
        public Drawable newDrawable() {
            return new FixedSizeDrawable(this, wrapped.newDrawable());
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new FixedSizeDrawable(this, wrapped.newDrawable(res));
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }
    }
}
