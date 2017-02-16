package com.bumptech.glide.load.resource.file;

import com.bumptech.glide.load.resource.SimpleResource;

import java.io.File;

/**
 * A simple {@link com.bumptech.glide.load.engine.Resource} that wraps a {@link File}.
 * <p>
 * 封装一个{@link File}的{@link com.bumptech.glide.load.engine.Resource}类
 */
public class FileResource extends SimpleResource<File> {
    public FileResource(File file) {
        super(file);
    }
}
