package com.developers.meraki.projectw;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, View.OnTouchListener {

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
    static final int REQUEST_IMAGE_CAPTURE = 401;
    static final int REQUEST_TAKE_PHOTO = 402;
    static final int REQUEST_VIDEO_CAPTURE = 101;
    String mCurrentPhotoPath;
    private ImageButton btnClock, btnAnticlock;
    private int mCurrRotation = 0;

    @SuppressWarnings("unused")
    private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;

    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initIds();
        openDirectory();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.nav_camera:
                is_menu_image = true;
                dispatchTakePictureIntent();
                updateUI();
                break;

            case R.id.nav_video:
                is_menu_image = false;
                dispatchTakeVideoIntent();
                updateUI();
                break;

            case R.id.nav_gallery_pic:
                is_menu_image = true;
                loadImagefromGallery();
                updateUI();
                break;
            case R.id.nav_gallery_vid:
                is_menu_image = false;
                loadVideofromGallery();
                updateUI();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.nav_template:
                // go to template link
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1IaA-5iZ8s-VKB-TaCt-0_8ujucRkpYrqfQ1fkjrVpUM/edit?usp=sharing"));
                startActivity(browserIntent);
                break;
            case R.id.nav_share:
                shareApp();
                break;
            case R.id.nav_rateus:
                rateApp();
                break;
            case R.id.nav_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    public void shareApp() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "mScanner");
        String text = "Must for every student!\n\n";
        text += "http://play.google.com/store/apps/details?id=" + this.getPackageName();
        i.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(i, "Share via"));

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
            String path = Panorama.getPublicAlbumStorageDir("mScanner", prefManager);
            prefManager.setIntValue("isFirstTime", 1);
            prefManager.setIntValue("frame_rate", 2);
            prefManager.setStringValue("root_path", path);
            prefManager.setStringValue("rotation", "anticlockwise");
        }

        btnImage = (Button) findViewById(R.id.btnProcessImage);
        txtResultWellPlate = (TextView) findViewById(R.id.txtResultWellplate);
        image = (ImageView) findViewById(R.id.imgWellPlate);
        videoView = (VideoView) findViewById(R.id.videoView);
        btnClock = (ImageButton) findViewById(R.id.btnClockwise);
        btnAnticlock = (ImageButton) findViewById(R.id.btnAntiClockwise);

        btnImage.setOnClickListener(this);
        btnClock.setOnClickListener(this);
        btnAnticlock.setOnClickListener(this);
        image.setOnTouchListener(this);

        videoPlayer = new VideoPlayer(this, videoView);
        panorama = new Panorama(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnProcessImage:

                //long startTime = System.nanoTime();

                if (is_menu_image) {
                    processImage();
                } else {
                    processVideo();
                }

                /*long endTime = System.nanoTime();
                //divide by 1000000 to get milliseconds.
                long duration = (endTime - startTime)/1000000;
                txtResultWellPlate.setText("Duration: "+duration);
                Toast.makeText(this, "Duration: "+duration, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG,"Duration: "+duration);
                */
                break;

            case R.id.btnClockwise:
                if (is_menu_image) {
                    rotateImage(90);
                } else {
                    //videoView.setRotation(image.getRotation() + 90);
                }
                break;

            case R.id.btnAntiClockwise:
                if (is_menu_image) {
                    rotateImage(-90);
                } else {
                    //videoView.setRotation(image.getRotation() - 90);
                }
                break;
        }
    }

    private void rotateImage(int angle) {
        mCurrRotation %= 360;

        float fromRotation = mCurrRotation;
        float toRotation = mCurrRotation += angle;
        /*
        final RotateAnimation rotateAnim = new RotateAnimation(
                fromRotation, toRotation, image.getWidth()/2, image.getHeight()/2);

        rotateAnim.setDuration(1000); // Use 0 ms to rotate instantly
        rotateAnim.setFillAfter(true); // Must be true or the animation will reset

        image.startAnimation(rotateAnim);*/

        Mat src = new Mat();
        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src);

        if (angle == -90) {
            Core.rotate(src, src, Core.ROTATE_90_COUNTERCLOCKWISE);
        } else if (angle == 90) {
            Core.rotate(src, src, Core.ROTATE_90_CLOCKWISE);
        }

        //Utils.matToBitmap(src, bmp32);

        Bitmap bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bmp);

        image.setImageBitmap(bmp);

    }

    private void updateUI() {
        if (is_menu_image) {
            image.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);

            btnAnticlock.setVisibility(View.VISIBLE);
            btnClock.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            btnImage.setText("Generate Tile Image");
            btnAnticlock.setVisibility(View.GONE);
            btnClock.setVisibility(View.GONE);
        }
        btnImage.setVisibility(View.VISIBLE);
        txtResultWellPlate.setText("");
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

        //Toast.makeText(this, "W:" + bitmap.getWidth() + " H:" + bitmap.getHeight(), Toast.LENGTH_SHORT).show();
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

                //long startTime = System.nanoTime();
                getPanorama(files, true);

                panorama.combineImagesInRow(tmp_well_dir, tmp_out_dir);
                Bitmap bmp = panorama.generatePanorama(tmp_out_dir, tmp_panorama_dir);

                /*long endTime = System.nanoTime();
                //divide by 1000000 to get milliseconds.
                long duration = (endTime - startTime)/1000000;
                txtResultWellPlate.setText("Duration: "+duration);*/

                image.setImageBitmap(bmp);
                btnImage.setText("Process Image");
                btnImage.setClickable(true);
                btnImage.setVisibility(View.INVISIBLE);
                txtResultWellPlate.setText("Tile Image generated!\nLocation:"+tmp_panorama_dir);

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

            } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
                galleryAddPic();
                setPic();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);

                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imgDecodableString = cursor.getString(columnIndex);
                cursor.close();

                // Set the Image in ImageView after decoding the String
                Bitmap myBitmap = BitmapFactory.decodeFile(imgDecodableString);
                image.setImageBitmap(myBitmap);

            } else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            /*Uri videoUri = data.getData();
            mVideoView.setVideoURI(videoUri);*/

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

                videoPlayer.playVideo(vidDecodableString);
                videoPlayer.pauseVideo();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong!!! " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void getPanorama(List<String> files, boolean isFirst) {

        if (isFirst) {
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

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Intent takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d(LOG_TAG, ex.getMessage());
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.developers.meraki.projectw",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = image.getWidth();
        int targetH = image.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        image.setImageBitmap(bitmap);

        imgDecodableString = mCurrentPhotoPath;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "mScanner/DCIM");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(LOG_TAG, "Error! Failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        //Toast.makeText(this, "Path: "+mCurrentPhotoPath, Toast.LENGTH_SHORT).show();
        return image;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        image.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;

        dumpEvent(event);
        // Handle touch events here...

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(LOG_TAG, "mode=DRAG"); // write to LogCat
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted

                mode = NONE;
                Log.d(LOG_TAG, "mode=NONE");
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                oldDist = spacing(event);
                Log.d(LOG_TAG, "oldDist=" + oldDist);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(LOG_TAG, "mode=ZOOM");
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mode == DRAG)
                {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                }
                else if (mode == ZOOM)
                {
                    // pinch zooming
                    float newDist = spacing(event);
                    Log.d(LOG_TAG, "newDist=" + newDist);
                    if (newDist > 5f)
                    {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist; // setting the scaling of the
                        // matrix...if scale > 1 means
                        // zoom in...if scale < 1 means
                        // zoom out
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        image.setImageMatrix(matrix); // display the transformation on screen

        return true; // indicate event was handled
    }

    /*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

    private float spacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

    private void midPoint(PointF point, MotionEvent event)
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /** Show an event in the LogCat view, for debugging */
    private void dumpEvent(MotionEvent event)
    {
        String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE","POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP)
        {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }

        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++)
        {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }

        sb.append("]");
        Log.d("Touch Events ---------", sb.toString());
    }

}
