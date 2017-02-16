package com.bumptech.glide.signature;

import com.bumptech.glide.load.Key;

import java.security.MessageDigest;

/**
 * An empty key that is always equal to all other empty keys.
 * <p>
 * {@link Key}的无操作实现类
 */
public final class EmptySignature implements Key {
    private static final EmptySignature EMPTY_KEY = new EmptySignature();

    public static EmptySignature obtain() {
        return EMPTY_KEY;
    }

    private EmptySignature() {
        // Empty.
    }

    @Override
    public String toString() {
        return "EmptySignature";
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        // Do nothing.
    }
}
