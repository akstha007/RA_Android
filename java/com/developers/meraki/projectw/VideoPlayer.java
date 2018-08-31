package com.developers.meraki.projectw;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

public class VideoPlayer {
    private Context context;
    private VideoView videoView;


    VideoPlayer(Context context, VideoView videoView){
        this.context = context;
        this.videoView = videoView;
    }

    public void playVideo(String path) {
        android.widget.MediaController mediaController = new android.widget.MediaController(context);
        mediaController.setAnchorView(videoView);

        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(context, "Select video from galary!!!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.fromFile(file);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(uri);
        videoView.requestFocus();

        videoView.start();
    }

    public void pauseVideo(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                videoView.pause();
            }
        }, 1000);
    }
}
