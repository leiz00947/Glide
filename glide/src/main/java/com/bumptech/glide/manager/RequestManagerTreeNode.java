package com.bumptech.glide.manager;

import com.bumptech.glide.RequestManager;

import java.util.Set;

/**
 * Provides access to the relatives of a RequestManager based on the current context. The context
 * hierarchy is provided by nesting in Activity and Fragments; the application context does not
 * provide access to any other RequestManagers hierarchically.
 * <p>
 * 提供基于当前Context相关联的一个RequestManager的使用权，Context需要由Activity和Fragment提供的，而非
 * 由Application提供
 */
public interface RequestManagerTreeNode {
    /**
     * Returns all descendant {@link RequestManager}s relative to the context of the current
     * {@link RequestManager}.
     * <p>
     * 返回基于当前{@link android.content.Context}的所有嵌套的{@link RequestManager}集合
     */
    Set<RequestManager> getDescendants();
}
