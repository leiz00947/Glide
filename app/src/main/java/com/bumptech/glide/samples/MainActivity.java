package com.bumptech.glide.samples;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bumptech.glide.samples.contacturi.ContactUriActivity;
import com.bumptech.glide.samples.flickr.FlickrSearchActivity;
import com.bumptech.glide.samples.gallery.GalleryActivity;
import com.bumptech.glide.samples.giphy.GiphyActivity;
import com.bumptech.glide.samples.svg.SvgActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button btnSvg = (Button) findViewById(R.id.btn_svg);
        Button btnGiphy = (Button) findViewById(R.id.btn_giphy);
        Button btnFlickr = (Button) findViewById(R.id.btn_flickr);
        Button btnGallery = (Button) findViewById(R.id.btn_gallery);
        Button btnContactUri = (Button) findViewById(R.id.btn_contacturi);
        Button btnTest = (Button) findViewById(R.id.btn_test);

        btnSvg.setOnClickListener(this);
        btnGiphy.setOnClickListener(this);
        btnFlickr.setOnClickListener(this);
        btnGallery.setOnClickListener(this);
        btnContactUri.setOnClickListener(this);
        btnTest.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_svg:
                skipActivity(SvgActivity.class);
                break;
            case R.id.btn_giphy:
                skipActivity(GiphyActivity.class);
                break;
            case R.id.btn_flickr:
                skipActivity(FlickrSearchActivity.class);
                break;
            case R.id.btn_gallery:
                skipActivity(GalleryActivity.class);
                break;
            case R.id.btn_contacturi:
                skipActivity(ContactUriActivity.class);
                break;
            case R.id.btn_test:
                skipActivity(TestActivity.class);
                break;
        }
    }

    private void skipActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }
}
