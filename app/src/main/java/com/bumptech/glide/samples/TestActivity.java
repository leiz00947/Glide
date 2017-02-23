package com.bumptech.glide.samples;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

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
        testCpu();
    }

    private void testDiskCacheDir() {
        File internalCacheDir = getCacheDir();
        File externalCacheDir = getExternalCacheDir();
        tvTest.append("internal cache dir:" + internalCacheDir.getPath());
        tvTest.append("\nexternal cache dir:" + externalCacheDir.getPath());
    }

    private void testLoadImage() {
        Glide.with(this)
                .load("http://pic2.orsoon.com/2017/0118/20170118011032176.jpg")
                .into(ivTest);
    }

    private void testCpu() {
        File[] cpus = null;
        try {
            File cpuInfo = new File("/sys/devices/system/cpu/");
            final Pattern cpuNamePattern = Pattern.compile("cpu[0-9]+");
            cpus = cpuInfo.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return cpuNamePattern.matcher(s).matches();
                }
            });
        } catch (Throwable t) {
            if (Log.isLoggable("TestActivity", Log.ERROR)) {
                Log.e("TestActivity", "Failed to calculate accurate cpu count", t);
            }
        }
        int cpuCount = cpus != null ? cpus.length : 0;
        int available = Runtime.getRuntime().availableProcessors();
        tvTest.append("\navailable=" + available + ",cpu count=" + cpuCount);
    }
}
