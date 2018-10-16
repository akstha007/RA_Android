package com.developers.meraki.projectw;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class VideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraBasicFragment.newInstance())
                    .commit();
        }
    }

}