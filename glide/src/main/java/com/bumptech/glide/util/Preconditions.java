package com.bumptech.glide.util;

import android.text.TextUtils;

import java.util.Collection;

/**
 * Contains common assertions.
 * <p>
 * 一般的判断的工具方法(如判断是否为空等)
 */
public final class Preconditions {

    private Preconditions() {
        // Utility class.
    }

    public static void checkArgument(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 验证传参是否为空
     *
     * @return 若传参不为空，则返回传参对象，否则抛出异常
     */
    public static <T> T checkNotNull(T arg) {
        return checkNotNull(arg, "Argument must not be null");
    }

    public static <T> T checkNotNull(T arg, String message) {
        if (arg == null) {
            throw new NullPointerException(message);
        }
        return arg;
    }

    public static String checkNotEmpty(String string) {
        if (TextUtils.isEmpty(string)) {
            throw new IllegalArgumentException("Must not be null or empty");
        }
        return string;
    }

    public static <T extends Collection<Y>, Y> T checkNotEmpty(T collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Must not be empty.");
        }
        return collection;
    }
}
