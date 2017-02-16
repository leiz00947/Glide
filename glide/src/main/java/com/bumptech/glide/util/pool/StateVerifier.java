package com.bumptech.glide.util.pool;

import com.bumptech.glide.util.Synthetic;

/**
 * Verifies that the job is not in the recycled state.
 * <p>
 * 验证并非回收状态
 */
public abstract class StateVerifier {
    private static final boolean DEBUG = false;

    /**
     * Creates a new {@link StateVerifier} instance.
     * <p>
     * 创建一个新的{@link StateVerifier}实例
     */
    public static StateVerifier newInstance() {
        if (DEBUG) {
            return new DebugStateVerifier();
        } else {
            return new DefaultStateVerifier();
        }
    }

    private StateVerifier() {
    }

    /**
     * Throws an exception if we believe our object is recycled and inactive
     * (i.e. is currently in an object pool).
     * <p>
     * 若断定对象被回收或者对象正处于闲置状态,那么就抛出一个异常
     */
    public abstract void throwIfRecycled();

    /**
     * Sets whether or not our object is recycled.
     * <p>
     * 设置对象是否被回收
     */
    abstract void setRecycled(boolean isRecycled);

    private static class DefaultStateVerifier extends StateVerifier {
        private volatile boolean isReleased;

        @Synthetic
        DefaultStateVerifier() {
        }

        @Override
        public void throwIfRecycled() {
            if (isReleased) {
                throw new IllegalStateException("Already released");
            }
        }

        @Override
        public void setRecycled(boolean isRecycled) {
            this.isReleased = isRecycled;
        }
    }

    private static class DebugStateVerifier extends StateVerifier {
        // Keeps track of the stack trace where our state was set to recycled.
        private volatile RuntimeException recycledAtStackTraceException;

        @Synthetic
        DebugStateVerifier() {
        }

        @Override
        public void throwIfRecycled() {
            if (recycledAtStackTraceException != null) {
                throw new IllegalStateException("Already released", recycledAtStackTraceException);
            }
        }

        @Override
        void setRecycled(boolean isRecycled) {
            if (isRecycled) {
                this.recycledAtStackTraceException = new RuntimeException("Released");
            } else {
                this.recycledAtStackTraceException = null;
            }
        }
    }
}
