package com.bumptech.glide.manager;

import com.bumptech.glide.RequestManager;

import java.util.Collections;
import java.util.Set;

/**
 * A {@link RequestManagerTreeNode} that returns no relatives.
 * <p>
 * {@link RequestManagerTreeNode}的实现类，返回一个空的{@link Set<RequestManager>}
 */
final class EmptyRequestManagerTreeNode implements RequestManagerTreeNode {
    @Override
    public Set<RequestManager> getDescendants() {
        return Collections.emptySet();
    }
}
