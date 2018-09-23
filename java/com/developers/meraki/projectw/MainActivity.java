package com.developers.meraki.projectw;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.opencv.core.Core.absdiff;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private Button btnImage;
    private TextView txtResultWellPlate;
    private String imgDecodableString = null;
    private String vidDecodableString = null;
    private VideoView videoView;
    private static int RESULT_LOAD_IMG = 1;
    private static int RESULT_IMAGE_PATH = 2;
    private static int RESULT_LOAD_VIDEO = 3;
    private ImageView image;
    private MediaController m;
    private boolean is_menu_image = true;
    private String LOG_TAG = "MainActivity";
    private List<String> files;
    private String tmp_well_dir, tmp_out_dir, tmp_panorama_dir;
    private VideoPlayer videoPlayer;
    private Map<String, String> map = new HashMap<String, String>();
    private Panorama panorama;
    private PrefManager prefManager;
    private final int REQUEST_DIR_PERMISSION = 101;

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initIds();
        openDirectory();
    }

    private void openDirectory() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }
    }

    private void initIds() {
        prefManager = new PrefManager(this);
        if (prefManager.getIntValue("isFirstTime") == 0) {
            //first time
            String path = Panorama.getPublicAlbumStorageDir("ProjectX", prefManager);
            prefManager.setIntValue("isFirstTime", 1);
            prefManager.setIntValue("frame_rate", 2);
            prefManager.setStringValue("root_path", path);
            prefManager.setStringValue("rotation", "clockwise");
        }

        btnImage = (Button) findViewById(R.id.btnProcessImage);
        txtResultWellPlate = (TextView) findViewById(R.id.txtResultWellplate);
        image = (ImageView) findViewById(R.id.imgWellPlate);
        videoView = (VideoView) findViewById(R.id.videoView);

        btnImage.setOnClickListener(this);

        videoPlayer = new VideoPlayer(this, videoView);
        panorama = new Panorama(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnProcessImage:
                if (is_menu_image) {
                    processImage();
                } else {
                    processVideo();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_camera:
                Intent intent = new Intent(getApplicationContext(), Camera2Activity.class);
                startActivityForResult(intent, RESULT_IMAGE_PATH);
                return true;

            case R.id.menu_image:
                is_menu_image = true;
                loadImagefromGallery();
                updateUI();
                return true;

            case R.id.menu_video:
                is_menu_image = false;
                loadVideofromGallery();
                updateUI();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateUI() {
        if (is_menu_image) {
            image.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
        } else {
            image.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            btnImage.setText("Generate Panorama");
        }
        btnImage.setVisibility(View.VISIBLE);
        txtResultWellPlate.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu - this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void processImage() {
        if (imgDecodableString == null || imgDecodableString.isEmpty() || imgDecodableString.equalsIgnoreCase("")) {
            Toast.makeText(getApplicationContext(), "Error! Image not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        txtResultWellPlate.setText("Processing...");

        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();

        Bitmap bittmap = panorama.getContourArea(bitmap, is_menu_image, imgDecodableString, tmp_well_dir, txtResultWellPlate, true);
        image.setImageBitmap(bittmap);
    }

    private void processVideo() {
        if (vidDecodableString == null || vidDecodableString.isEmpty() || vidDecodableString.equalsIgnoreCase("")) {
            Toast.makeText(getApplicationContext(), "Error! Image not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnImage.setText("Processing...");
        btnImage.setClickable(false);
        videoPlayer.pauseVideo();

        final int frate = prefManager.getIntValue("frame_rate");

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String path = panorama.getPublicAlbumStorageDir("frames", prefManager);
                panorama.vid2Frame(vidDecodableString, "frames", frate, txtResultWellPlate);

                files = panorama.getListFiles(path);
                if (files == null) {
                    Toast.makeText(getApplicationContext(), "Error! Images not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                getPanorama(files,true);
                //getPanorama(files,false);

                panorama.combineImagesInRow(tmp_well_dir, tmp_out_dir);
                Bitmap bmp = panorama.generatePanorama(tmp_out_dir, tmp_panorama_dir);

                image.setImageBitmap(bmp);
                btnImage.setText("Process Image");
                btnImage.setClickable(true);
                btnImage.setVisibility(View.INVISIBLE);
                //txtResultWellPlate.setText("Panorama Image generated!");

                image.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.GONE);
            }
        }, 1000);
    }

    public void loadImagefromGallery() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    public void loadVideofromGallery() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {

                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);

                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgDecodableString = cursor.getString(columnIndex);
                cursor.close();

                // Set the Image in ImageView after decoding the String
                Bitmap myBitmap = BitmapFactory.decodeFile(imgDecodableString);
                image.setImageBitmap(myBitmap);

                //getContourArea(myBitmap);

            } else if (requestCode == RESULT_IMAGE_PATH && resultCode == RESULT_OK && data != null) {
                String imagePath = data.getStringExtra("ImagePath");
                File imgFile = new File(imagePath);
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                image.setImageBitmap(myBitmap);
                imgDecodableString = imgFile.getAbsolutePath();

                //getContourArea(myBitmap);

                //Toast.makeText(this, "Path-img: " + imagePath, Toast.LENGTH_LONG).show();
            } else if (requestCode == RESULT_LOAD_VIDEO && resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Video.Media.DATA};

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);

                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                vidDecodableString = cursor.getString(columnIndex);
                cursor.close();

                //String path = Environment.getExternalStorageDirectory().toString() + "/Pictures/img/";
                //files = panorama.getListFiles(path);

                //Toast.makeText(this, "Path: " + path, Toast.LENGTH_LONG).show();

                videoPlayer.playVideo(vidDecodableString);
                videoPlayer.pauseVideo();

            }

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong!!! " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void getPanorama(List<String> files, boolean isFirst) {

        if(isFirst) {
            tmp_well_dir = "tmp_well_dir/";
            tmp_out_dir = "tmp_out_dir/";
            tmp_panorama_dir = "tmp_panorama_dir/";

            tmp_well_dir = panorama.getPublicAlbumStorageDir(tmp_well_dir, prefManager);
            tmp_out_dir = panorama.getPublicAlbumStorageDir(tmp_out_dir, prefManager);
            tmp_panorama_dir = panorama.getPublicAlbumStorageDir(tmp_panorama_dir, prefManager);
        }
        if (files != null) {
            for (String filepath : files) {
                File imgFile = new File(filepath);
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imgDecodableString = imgFile.getAbsolutePath();
                panorama.getContourArea(myBitmap, is_menu_image, imgDecodableString, tmp_well_dir, txtResultWellPlate, isFirst);
            }
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_DIR_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_DIR_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    //permission not granted show error dialog
                    showDialog();
                }
                return;
            }
        }
    }

    public void showDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Permissions")
                .setMessage("Permission Denied! App may not work properly if permission is not granted.")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        requestCameraPermission();
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_dir)
                .show();
    }

}
