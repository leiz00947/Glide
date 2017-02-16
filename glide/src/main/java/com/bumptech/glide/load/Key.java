package com.bumptech.glide.load;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * An interface that uniquely identifies some put of data. Implementations must implement {@link
 * Object#equals(Object)} and {@link Object#hashCode()}. Implementations are generally expected to
 * add all uniquely identifying information used in in {@link Object#equals(Object)}} and
 * {@link Object#hashCode()}} to the given {@link MessageDigest} in {@link
 * #updateDiskCacheKey(MessageDigest)}}, although this requirement is not as strict
 * for partial cache key signatures.
 * <p>
 * 给存放的数据提供一个唯一标识的接口
 */
public interface Key {
    String STRING_CHARSET_NAME = "UTF-8";
    Charset CHARSET = Charset.forName(STRING_CHARSET_NAME);

    /**
     * Adds all uniquely identifying information to the given digest.
     * <p>
     * Note - Using {@link MessageDigest#reset()} inside of this method will result
     * in undefined behavior.
     */
    void updateDiskCacheKey(MessageDigest messageDigest);

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
