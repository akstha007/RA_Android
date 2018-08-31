package com.developers.meraki.projectw;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.absdiff;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.putText;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnImage;
    private TextView txtResultWellPlate;
    private String imgDecodableString = null;
    private static int RESULT_LOAD_IMG = 1;
    private static final int RESULT_IMAGE_PATH = 2;
    ImageView image;

    static {
        System.loadLibrary("opencv_java3");
    }

    private int[][] rawImages = {
            {R.raw.a, 1}, {R.raw.y, 25}, {R.raw.z, 26}, {R.raw.d, 4}, {R.raw.e, 5}, {R.raw.f, 6},
            {R.raw.g, 7}, {R.raw.h, 8}, {R.raw.i, 9}, {R.raw.j, 10}, {R.raw.k, 11}, {R.raw.l, 12},
            {R.raw.m, 13}, {R.raw.n, 14}, {R.raw.o, 15}, {R.raw.p, 16}, {R.raw.q, 17}, {R.raw.r, 18},
            {R.raw.s, 19}, {R.raw.t, 20}, {R.raw.u, 21}, {R.raw.v, 22}, {R.raw.w, 23}, {R.raw.x, 24}
    };

    private int[][] rawImagesAlpha = {
            {R.raw.a, R.raw.y, R.raw.z, R.raw.d, R.raw.e, R.raw.f},
            {R.raw.g, R.raw.h, R.raw.i, R.raw.j, R.raw.k, R.raw.l},
            {R.raw.m, R.raw.n, R.raw.o, R.raw.p, R.raw.q, R.raw.r},
            {R.raw.s, R.raw.t, R.raw.u, R.raw.v, R.raw.w, R.raw.x}
    };

    private int[][] rawImagesNum = {
            {1, 25, 26, 4, 5, 6},
            {7, 8, 9, 10, 11, 12},
            {13, 14, 15, 16, 17, 18},
            {19, 20, 21, 22, 23, 24}
    };

    private int iindex = 0, jindex = 0;
    private boolean isfirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initIds();
    }

    private void initIds() {
        btnImage = (Button) findViewById(R.id.btnProcessImage);
        txtResultWellPlate = (TextView) findViewById(R.id.txtResultWellplate);
        image = (ImageView) findViewById(R.id.imgWellPlate);

        btnImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnProcessImage:
                processImage();
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
            case R.id.menu_media:
                loadImagefromGallery();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

        Bitmap bittmap = getContourArea(bitmap);
        image.setImageBitmap(bittmap);
    }

    public void loadImagefromGallery() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
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

                //Toast.makeText(this, "Path: " + imagePath, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private Bitmap getContourArea(Bitmap bmp) {

        Mat src = new Mat();
        Mat resizeSrc = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src);

        int bmp_w = 240;
        int bmp_h = bmp_w * bmp.getHeight() / bmp.getWidth();

        Mat gray = new Mat();
        org.opencv.core.Size sz = new org.opencv.core.Size(bmp_w, bmp_h);
        Imgproc.resize(src, resizeSrc, sz);
        Imgproc.cvtColor(resizeSrc, gray, Imgproc.COLOR_RGBA2GRAY);
        adaptiveThreshold(gray, gray, 200, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 5);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        double minArea = 1000, maxArea = 3000;

        Rect rectCrop = null;
        int well_w = 210, well_h = 210;
        int img_w = gray.width(), img_h = gray.height();
        String sim = "";

        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Mat contour = contours.get(contourIdx);
            double contourArea = Imgproc.contourArea(contour);

            if (contourArea > minArea && contourArea < maxArea) {

                Rect rect = Imgproc.boundingRect(contours.get(contourIdx));

                int h = rect.height;
                int w = rect.width;
                //int y = rect.y + h / 2;
                //int x = rect.x + w / 2;
                int y = rect.y + 25;
                int x = rect.x + 20;

                //check size of rectangle
                if (h > 30 && h < 50 && w > 30 && w < 50) {

                    float r_w = (float)img_w / x;
                    float r_h = (float)img_h / y;

                    //check if the rectangle is within center of the image
                    if(r_w > 1.5 && r_w < 2.5 && r_h > 1.5 && r_h < 2.5) {

                        Mat imCrop = new Mat(resizeSrc, rect);
                        sim = compareImages(imCrop);

                        /*Imgproc.rectangle(resizeSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, .8), 2);
                        putText(resizeSrc, "" + sim, new Point(rect.x, rect.y),
                                Core.FONT_HERSHEY_DUPLEX, 1.0, new Scalar(0, 255, 0));
                        rectCrop = rect;*/
                        rectCrop = new Rect(x - well_w / 2, y - well_h / 2, well_w, well_h);
                    }

                }
            }
        }

        String result="No wells found!";

        if (rectCrop != null) {
            Mat image_roi = new Mat(resizeSrc, rectCrop);
            bmp = Bitmap.createBitmap(image_roi.cols(), image_roi.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi, bmp);

            result = "Well no.: ";
            result += sim;
            result += "\nPath: " + imgDecodableString;
        }

        txtResultWellPlate.setText(result);

        return bmp;
    }

    private String compareImages(Mat img1) {

        //resize the images
        Mat resizeImg1 = new Mat();
        org.opencv.core.Size sz = new org.opencv.core.Size(50, 50);
        Imgproc.resize(img1, resizeImg1, sz);
        Imgproc.cvtColor(resizeImg1, resizeImg1, Imgproc.COLOR_RGBA2GRAY);
        adaptiveThreshold(resizeImg1, resizeImg1, 1, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 5);

        double minError = 200;
        String imageName = "";

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                InputStream imageStream = this.getResources().openRawResource(rawImagesAlpha[i][j]);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                Mat img2 = new Mat();
                Utils.bitmapToMat(bitmap, img2);
                Imgproc.resize(img2, img2, sz);

                Mat resizeImg2 = new Mat();
                Imgproc.resize(img2, resizeImg2, sz);
                Imgproc.cvtColor(resizeImg2, resizeImg2, Imgproc.COLOR_RGBA2GRAY);
                adaptiveThreshold(resizeImg2, resizeImg2, 1, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 5);

                Mat s1 = new Mat();
                absdiff(resizeImg1, resizeImg2, s1);       // |I1 - I2|
                s1.convertTo(s1, CvType.CV_32F);  // cannot make a square on 8 bits
                s1 = s1.mul(s1);           // |I1 - I2|^2

                Scalar s = Core.sumElems(s1);        // sum elements per channel
                double sse = s.val[0] + s.val[1] + s.val[2]; // sum channels
                double mse = 0;
                if (sse <= 1e-10) // for small values return zero
                    mse = 0;
                else {
                    mse = sse / (double) (resizeImg1.channels() * resizeImg1.total());
                    //mse = 10.0 * Math.log10((255 * 255) / mse);
                }
                if (minError > mse) {
                    minError = mse;
                    imageName = "" + (char) (rawImagesNum[i][j] + 'A' - 1);
                    iindex = i - 1;
                    jindex = j - 1;
                    isfirst = false;
                }
            }

        }
        return imageName;
    }
}
