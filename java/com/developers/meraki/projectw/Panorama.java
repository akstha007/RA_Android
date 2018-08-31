package com.developers.meraki.projectw;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.Core.absdiff;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;

public class Panorama {
    private Context context;
    private static String LOG_TAG = "Panorama";
    private static PrefManager prefManager;

    private String[][] well_plate = {
            {"a", "y", "z", "d", "e", "f"},
            {"g", "h", "i", "j", "k", "l"},
            {"m", "n", "o", "p", "q", "r"},
            {"s", "t", "u", "v", "w", "x"}
    };

    private int[][] rawImages = {
            {R.raw.a, 1}, {R.raw.y, 25}, {R.raw.z, 26}, {R.raw.d, 4}, {R.raw.e, 5}, {R.raw.f, 6},
            {R.raw.g, 7}, {R.raw.h, 8}, {R.raw.i, 9}, {R.raw.j, 10}, {R.raw.k, 11}, {R.raw.l, 12},
            {R.raw.m, 13}, {R.raw.n, 14}, {R.raw.o, 15}, {R.raw.p, 16}, {R.raw.q, 17}, {R.raw.r, 18},
            {R.raw.s, 19}, {R.raw.t, 20}, {R.raw.u, 21}, {R.raw.v, 22}, {R.raw.w, 23}, {R.raw.x, 24}
    };

    Panorama(Context context) {
        this.context = context;
        prefManager = new PrefManager(context);
    }

    public Bitmap combineImagesHorizontally(Bitmap img1, Bitmap img2) {
        if (img1.getHeight() == img2.getHeight()) {
            Bitmap result = Bitmap.createBitmap(img1.getWidth() + img2.getWidth(), img1.getHeight(), img1.getConfig());
            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(img1, 0f, 0f, null);
            canvas.drawBitmap(img2, img1.getWidth(), 0f, null);
            return result;
        } else {
            return null;
        }
    }

    public Bitmap combineImagesVertically(Bitmap img1, Bitmap img2) {
        if (img1.getWidth() == img2.getWidth()) {
            Bitmap result = Bitmap.createBitmap(img1.getWidth(), img1.getHeight() + img2.getHeight(), img1.getConfig());
            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(img1, 0f, 0f, null);
            canvas.drawBitmap(img2, 0f, img1.getHeight(), null);
            return result;
        } else {
            return null;
        }
    }

    public Bitmap generatePanorama(String dirPath, String dirPathOut) {
        String imgPath = "row0.jpg";
        Bitmap res = null;
        File file = new File(dirPath, imgPath);
        if (file.exists()) {
            res = BitmapFactory.decodeFile(file.getAbsolutePath());
        } else {
            // create empty image
            res = createImage(1050, 210, Color.WHITE);
        }

        for (int j = 1; j < 4; j++) {
            imgPath = "row" + j + ".jpg";
            Bitmap bmp = null;
            file = new File(dirPath, imgPath);
            if (file.exists()) {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                // create empty image
                bmp = createImage(1050, 210, Color.WHITE);
            }

            res = combineImagesVertically(res, bmp);
        }

        //save each row images after converting res(Mat) to bitmap
        String rowImageName = "panorama";
        saveImage(dirPathOut, rowImageName, res);

        return res;
    }

    public void combineImagesInRow(String dirPath, String dirPathOut) {
        for (int i = 0; i < well_plate.length; i++) {
            //load first image of the row
            String imgPath = well_plate[i][0] + ".jpg";
            Bitmap res = null;
            File file = new File(dirPath, imgPath);
            if (file.exists()) {
                res = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                // create empty image
                res = createImage(210, 210, Color.WHITE);
            }

            for (int j = 1; j < well_plate[0].length; j++) {
                imgPath = well_plate[i][j] + ".jpg";
                Bitmap bmp = null;
                file = new File(dirPath, imgPath);
                if (file.exists()) {
                    bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                } else {
                    // create empty image
                    bmp = createImage(210, 210, Color.WHITE);
                }

                res = combineImagesHorizontally(res, bmp);
            }

            //save each row images after converting res(Mat) to bitmap
            String rowImageName = "row" + i;
            saveImage(dirPathOut, rowImageName, res);
        }
    }

    public Bitmap createImage(int width, int height, int color) {

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);

        paint.setColor(Color.rgb(0, 0, 0));
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(1, 1, 209, 209, paint);

        return bitmap;
    }

    public static void deleteOldFolder(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }
    }

    public static String getPublicAlbumStorageDir(String albumName, PrefManager prefManager) {
        // Get the directory for the user's public pictures directory.
        File file = null;

        if(prefManager.getIntValue("isFirstTime")==0){
            file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
        }else{
            String path = prefManager.getStringValue("root_path");
            file = new File(path, albumName);
        }

        if (file.exists()) {
            deleteOldFolder(file.getAbsolutePath());
        }
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file.getAbsolutePath();
    }

    public void saveImage(String path, String imgName, Bitmap bmp) {
        File myDir = new File(path);
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        String fname = imgName + ".jpg";
        File file = new File(myDir, fname);

        try {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            /*sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getListFiles(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();

        List<String> inFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    if (file.getName().endsWith(".jpg")) {
                        inFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return inFiles;
    }


    public Bitmap getContourArea(Bitmap bmp, boolean is_menu_image, String imgDecodableString, String tmp_well_dir, TextView txtResultWellPlate) {

        Map<String, String> map = new HashMap<String, String>();

        Mat src = new Mat();
        Mat resizeSrc = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src);

        int bmp_w = 240;
        int bmp_h = bmp_w * bmp.getHeight() / bmp.getWidth();

        //check if the image needs to be rotated clockwise or anti-clockwise
        String rotation = prefManager.getStringValue("rotation");

        if (bmp.getWidth() > bmp.getHeight()) {
            //rotate image by 90
            //Core.rotate(src, src, Core.ROTATE_90_COUNTERCLOCKWISE);
            if (rotation == "clockwise") {
                Core.rotate(src, src, Core.ROTATE_90_CLOCKWISE);
            } else if (rotation == "anticlockwise") {
                Core.rotate(src, src, Core.ROTATE_90_COUNTERCLOCKWISE);
            }
            bmp_w = 240;
            bmp_h = bmp_w * bmp.getWidth() / bmp.getHeight();
        }

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

        CompareResult cmp = new CompareResult();

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

                    float r_w = (float) img_w / x;
                    float r_h = (float) img_h / y;

                    //check if the rectangle is within center of the image
                    if (r_w > 1.5 && r_w < 2.5 && r_h > 1.5 && r_h < 2.5) {

                        Mat imCrop = new Mat(resizeSrc, rect);
                        cmp = compareImages(imCrop);

                        /*Imgproc.rectangle(resizeSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, .8), 2);
                        putText(resizeSrc, "" + sim, new Point(rect.x, rect.y),
                                Core.FONT_HERSHEY_DUPLEX, 1.0, new Scalar(0, 255, 0));
                        rectCrop = rect;*/

                        //check if the crop image is within image bounds
                        int a = x - well_w / 2;
                        int b = y - well_h / 2;
                        int c = x + well_w / 2;
                        int d = y + well_h / 2;
                        if (a > 0 && b > 0 && c < img_w && d < img_h) {
                            rectCrop = new Rect(x - well_w / 2, y - well_h / 2, well_w, well_h);
                        }
                    }

                }
            }
        }

        String result = "No wells found!";

        if (rectCrop != null) {
            Mat image_roi = new Mat(resizeSrc, rectCrop);
            bmp = Bitmap.createBitmap(image_roi.cols(), image_roi.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi, bmp);

            result = "Well no.: ";
            result += cmp.getWellName();
            result += "\nPath: " + imgDecodableString;

            //if menu selected is video save files to direcotry
            if (!is_menu_image) {
                //check if previous value is present and compare with its value before assignment.
                String imageName = cmp.getWellName();
                double minError = cmp.getSimilarity();

                if (map.containsKey(imageName)) {
                    double error = Double.parseDouble(map.get(imageName));
                    if (error > minError) {
                        map.put(imageName, "" + minError);
                        saveImage(tmp_well_dir, imageName, bmp);
                    }
                } else {
                    map.put(imageName, "" + minError);
                    saveImage(tmp_well_dir, imageName, bmp);
                }
            }
        }

        if (!is_menu_image) {
            return null;
        }

        txtResultWellPlate.setText(result);
        return bmp;
    }

    public void vid2Frame(String input, String output, int frame_rate, TextView txtResultWellPlate) {
        File file = new File(input);

        if (file.exists()) {
            Log.d(LOG_TAG, "File exists!! Inside vid2frame.");
        } else {
            Log.d(LOG_TAG, "NO file!!! Inside vid2frame.");
        }

        output = getPublicAlbumStorageDir(output, prefManager);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int frame_number = 0;

        try {
            retriever.setDataSource(file.getAbsolutePath());
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int videoDuration = Integer.parseInt(time)/1000;

            for (int i = 0; i < videoDuration; i += frame_rate) {
                Bitmap frame = retriever.getFrameAtTime(i*1000000, MediaMetadataRetriever.OPTION_CLOSEST);
                saveImage(output, "frame" + frame_number, frame);
                frame_number++;
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            try {
                txtResultWellPlate.setText("Frames created! " + frame_number);
                retriever.release();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }


    }

    public CompareResult compareImages(Mat img1) {
        //resize the images
        Mat resizeImg1 = new Mat();
        org.opencv.core.Size sz = new org.opencv.core.Size(50, 50);
        Imgproc.resize(img1, resizeImg1, sz);
        Imgproc.cvtColor(resizeImg1, resizeImg1, Imgproc.COLOR_RGBA2GRAY);
        adaptiveThreshold(resizeImg1, resizeImg1, 1, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 5);

        double minError = 200;
        String imageName = "";
        int sim = 0;

        CompareResult cmp = new CompareResult();

        for (int i = 0; i < rawImages.length; i++) {

            InputStream imageStream = context.getResources().openRawResource(rawImages[i][0]);
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
                imageName = "" + (char) (rawImages[i][1] + 'a' - 1);
            }
        }

        //cmp.set(imageName, minError);
        cmp.set(imageName, sim);
        return cmp;
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
