package com.bumptech.glide.samples;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Created by Zsago on 2017/2/22.
 *
 * @since 1.0
 */
public class TestActivity extends AppCompatActivity {
    TextView tvTest;
    ImageView ivTest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        tvTest = (TextView) findViewById(R.id.tv_test);
        ivTest = (ImageView) findViewById(R.id.iv_test);
        testDiskCacheDir();
        testLoadImage();
    }

    private void testDiskCacheDir() {
        File internalCacheDir = getCacheDir();
        File externalCacheDir = getExternalCacheDir();
        tvTest.append("internal cache dir:" + internalCacheDir.getPath() + "\n");
        tvTest.append("external cache dir:" + externalCacheDir.getPath());
    }

    private void testLoadImage() {
        Glide.with(this)
                .load("http://pic2.orsoon.com/2017/0118/20170118011032176.jpg")
                .into(ivTest);
    }
}
