package com.bumptech.glide.samples.gallery;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.samples.R;

/**
 * Displays a {@link HorizontalGalleryFragment}.
 */
public class GalleryActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH);
    }
}
