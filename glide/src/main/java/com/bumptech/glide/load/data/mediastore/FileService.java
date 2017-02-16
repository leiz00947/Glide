package com.bumptech.glide.load.data.mediastore;

import java.io.File;

/**
 * 判断文件是否存在，获取文件长度和根据路径获取文件的类
 */
class FileService {
    public boolean exists(File file) {
        return file.exists();
    }

    public long length(File file) {
        return file.length();
    }

    public File get(String path) {
        return new File(path);
    }
}
