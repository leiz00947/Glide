package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;

/**
 * 任务启动监听接口
 */
interface EngineJobListener {
    /**
     * 任务启动完成的回调接口
     */
    void onEngineJobComplete(Key key, EngineResource<?> resource);

    /**
     * 任务启动失败的回调接口
     */
    void onEngineJobCancelled(EngineJob engineJob, Key key);
}
