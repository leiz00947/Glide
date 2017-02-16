package com.bumptech.glide.load.data.mediastore;

import android.database.Cursor;
import android.net.Uri;

/**
 * 根据缩略图的{@link Uri}来查询返回{@link Cursor}的接口
 */
interface ThumbnailQuery {
    Cursor query(Uri uri);
}
