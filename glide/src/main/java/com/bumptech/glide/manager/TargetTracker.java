package com.bumptech.glide.manager;

import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Holds the set of {@link Target}s currently active for a
 * {@link com.bumptech.glide.RequestManager} and forwards on lifecycle events.
 * <p>
 * 持有一个管理{@link Target}生命周期的集合
 */
public final class TargetTracker implements LifecycleListener {
    /**
     * 用来存放所有持有{@link com.bumptech.glide.request.Request}的{@link Target}对象的集合
     */
    private final Set<Target<?>> targets =
            Collections.newSetFromMap(new WeakHashMap<Target<?>, Boolean>());

    public void track(Target<?> target) {
        targets.add(target);
    }

    /**
     * 当{@link Target}所持有的{@link com.bumptech.glide.request.Request}被清除时，就会调用该方法
     */
    public void untrack(Target<?> target) {
        targets.remove(target);
    }

    @Override
    public void onStart() {
        for (Target<?> target : Util.getSnapshot(targets)) {
            target.onStart();
        }
    }

    @Override
    public void onStop() {
        for (Target<?> target : Util.getSnapshot(targets)) {
            target.onStop();
        }
    }

    @Override
    public void onDestroy() {
        for (Target<?> target : Util.getSnapshot(targets)) {
            target.onDestroy();
        }
    }

    public List<Target<?>> getAll() {
        return new ArrayList<>(targets);
    }

    public void clear() {
        targets.clear();
    }
}
