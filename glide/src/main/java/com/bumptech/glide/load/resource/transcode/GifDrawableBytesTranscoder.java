package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bytes.BytesResource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.util.ByteBufferUtil;

import java.nio.ByteBuffer;

/**
 * An {@link ResourceTranscoder} that converts {@link GifDrawable} into bytes by obtaining
 * the original bytes of the GIF from the {@link GifDrawable}.
 * <p>
 * {@link ResourceTranscoder}的实现类，将{@link GifDrawable}转换成通过其获取到的GIF的原始字节数组
 */
public class GifDrawableBytesTranscoder implements ResourceTranscoder<GifDrawable, byte[]> {
    @Override
    public Resource<byte[]> transcode(Resource<GifDrawable> toTranscode, Options options) {
        GifDrawable gifData = toTranscode.get();
        ByteBuffer byteBuffer = gifData.getBuffer();
        return new BytesResource(ByteBufferUtil.toBytes(byteBuffer));
    }
}
