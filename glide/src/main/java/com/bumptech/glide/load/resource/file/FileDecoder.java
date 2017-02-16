package com.bumptech.glide.load.resource.file;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.File;

/**
 * A simple {@link com.bumptech.glide.load.ResourceDecoder} that creates resource for a given
 * {@link File}.
 * <p>
 * {@link ResourceDecoder}的实现类，为给定的{@link File}创建{@link ResourceDecoder}
 */
public class FileDecoder implements ResourceDecoder<File, File> {

    @Override
    public boolean handles(File source, Options options) {
        return true;
    }

    @Override
    public Resource<File> decode(File source, int width, int height, Options options) {
        return new FileResource(source);
    }
}
