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
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.log10;
import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.divide;
import static org.opencv.core.Core.mean;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.threshold;

public class Panorama {
    private Context context;
    private static String LOG_TAG = "Panorama";
    private static PrefManager prefManager;
    private Map<String, String> map = new HashMap<String, String>();

    private String[][] well_plate = {
            {"a", "y", "z", "d", "e", "f"},
            {"g", "h", "i", "j", "k", "l"},
            {"m", "n", "o", "p", "q", "r"},
            {"s", "t", "u", "v", "w", "x"}
    };

    private String[] well_plate_value = {
            "a", "y", "z", "+", "e", "f",
            "g", "h", "i", "j", "k", "&",
            "m", "?", "$", "p", "9", "r",
            "#", "t", "%", "v", "w", "*"
    };

    private int[][] rawImages = {
            {R.raw.a, 11}, {R.raw.y, 12}, {R.raw.z, 13}, {R.raw.d, 14}, {R.raw.e, 15}, {R.raw.f, 16},
            {R.raw.g, 21}, {R.raw.h, 22}, {R.raw.i, 23}, {R.raw.j, 24}, {R.raw.k, 25}, {R.raw.l, 26},
            {R.raw.m, 31}, {R.raw.n, 32}, {R.raw.o, 33}, {R.raw.p, 34}, {R.raw.q, 35}, {R.raw.r, 36},
            {R.raw.s, 41}, {R.raw.t, 42}, {R.raw.u, 43}, {R.raw.v, 44}, {R.raw.w, 45}, {R.raw.x, 46}
    };

    /*private int[][] rawImages = {
            {R.raw.a, 1}, {R.raw.y, 25}, {R.raw.z, 26}, {R.raw.d, 4}, {R.raw.e, 5}, {R.raw.f, 6},
            {R.raw.g, 7}, {R.raw.h, 8}, {R.raw.i, 9}, {R.raw.j, 10}, {R.raw.k, 11}, {R.raw.l, 12},
            {R.raw.m, 13}, {R.raw.n, 14}, {R.raw.o, 15}, {R.raw.p, 16}, {R.raw.q, 17}, {R.raw.r, 18},
            {R.raw.s, 19}, {R.raw.t, 20}, {R.raw.u, 21}, {R.raw.v, 22}, {R.raw.w, 23}, {R.raw.x, 24}
    };*/

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

        //delete map values
        map.clear();

        return res;
    }

    public void combineImagesInRow(String dirPath, String dirPathOut) {
        int rowLen = well_plate.length;
        int colLen = well_plate[0].length;

        for (int i = 0; i < rowLen; i++) {
            //load first image of the row
            int r = rawImages[i * colLen][1] / 10;
            int c = rawImages[i * colLen][1] % 10;
            String imgPath = "R-" + r + " C-" + c + " ( " + well_plate_value[i * colLen] + " )" + ".jpg";
            //String imgPath = well_plate[i][0] + ".jpg";
            Bitmap res = null;
            File file = new File(dirPath, imgPath);
            if (file.exists()) {
                res = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                // create empty image
                res = createImage(210, 210, Color.WHITE);
            }

            for (int j = 1; j < colLen; j++) {
                r = rawImages[i * colLen + j][1] / 10;
                c = rawImages[i * colLen + j][1] % 10;
                imgPath = "R-" + r + " C-" + c + " ( " + well_plate_value[i * colLen + j] + " )" + ".jpg";
                //imgPath = well_plate[i][j] + ".jpg";
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

        if (prefManager.getIntValue("isFirstTime") == 0) {
            file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
        } else {
            String path = prefManager.getStringValue("root_path");
            file = new File(path, albumName);

            if (file.exists()) {
                deleteOldFolder(file.getAbsolutePath());
            }
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


    public Bitmap getContour(Bitmap bmp, boolean is_menu_image, String imgDecodableString, String tmp_well_dir, TextView txtResultWellPlate) {
        Bitmap bitmap = null;
        for (int i = 80; i < 150; i = i + 10) {
            bitmap = getContourArea(bmp, is_menu_image, imgDecodableString, tmp_well_dir, txtResultWellPlate, true);
        }

        return bitmap;
    }


    public Bitmap getContourArea(Bitmap bmp, boolean is_menu_image, String imgDecodableString,
                                 String tmp_well_dir, TextView txtResultWellPlate, boolean isFirst) {

        Mat src = new Mat();
        Mat resizeSrc = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src);

        //16:9 240
        //4:3 320
        int aspect = 240;

        float ratio = (float) bmp.getWidth() / bmp.getHeight();

        if (ratio < 1) {
            ratio = 1 / ratio;
        }
        if (ratio < 1.6) {
            aspect = 320;
        }

        int bmp_w = aspect;
        int bmp_h = bmp_w * bmp.getHeight() / bmp.getWidth();

        //check if the image needs to be rotated clockwise or anti-clockwise
        String rotation = prefManager.getStringValue("rotation");

        if (bmp.getWidth() > bmp.getHeight()) {
            //manual rotation done by user in home page
            //use it for videos only
            if (!is_menu_image) {
                if (rotation.equalsIgnoreCase("anticlockwise")) {
                    Core.rotate(src, src, Core.ROTATE_90_COUNTERCLOCKWISE);
                } else if (rotation.equalsIgnoreCase("clockwise")) {
                    Core.rotate(src, src, Core.ROTATE_90_CLOCKWISE);
                }
            }

            bmp_w = aspect;
            bmp_h = bmp_w * bmp.getWidth() / bmp.getHeight();
        }

        Mat gray = new Mat();
        Mat binary = new Mat();
        Size sz = new Size(bmp_w, bmp_h);
        Imgproc.resize(src, resizeSrc, sz);
        Imgproc.cvtColor(resizeSrc, gray, Imgproc.COLOR_RGBA2GRAY);
        //Imgproc.threshold(gray, binary, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        adaptiveThreshold(gray, binary, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 11, 5);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        double minArea = 1000, maxArea = 3000;

        Rect rectCrop = null;
        int well_w = 210, well_h = 210;
        int img_w = gray.width(), img_h = gray.height();

        CompareResult cmp = new CompareResult();

        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

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
                        cmp = compareImages(imCrop, isFirst);

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
            //Mat image_roi = new Mat(binary, rectCrop);
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
                    double score = Double.parseDouble(map.get(imageName));
                    if (score < minError) {
                        map.put(imageName, "" + minError);
                        saveImage(tmp_well_dir, imageName, bmp);
                    }
                } else {
                    map.put(imageName, "" + minError);
                    saveImage(tmp_well_dir, imageName, bmp);
                }
            }
        }/*else{
            Mat image_roi = binary;
            bmp = Bitmap.createBitmap(image_roi.cols(), image_roi.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi, bmp);
        }*/

        if (!is_menu_image) {
            return null;
        }

        txtResultWellPlate.setText(result);
        return bmp;
    }

    public CompareResult compareImages(Mat img1, Boolean isFirst) {
        //resize the images
        Mat resizeImg1 = new Mat();
        org.opencv.core.Size sz = new org.opencv.core.Size(50, 50);
        Imgproc.resize(img1, resizeImg1, sz);
        Imgproc.cvtColor(resizeImg1, resizeImg1, Imgproc.COLOR_RGBA2GRAY);
        //adaptiveThreshold(resizeImg1, resizeImg1, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 11, 5);
        //Imgproc.threshold(resizeImg1, resizeImg1, 90, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgproc.threshold(resizeImg1, resizeImg1, 90, 255, Imgproc.THRESH_BINARY);

        double minError = 0;
        String imageName = "";

        CompareResult cmp = new CompareResult();

        for (int i = 0; i < rawImages.length; i++) {

            //String imgname = "" + (char) (rawImages[i][1] + 'a' - 1);
            int r = rawImages[i][1] / 10;
            int c = rawImages[i][1] % 10;
            String imgname = "R-" + r + " C-" + c + " ( " + well_plate_value[i] + " )";
            if (!isFirst && map.containsKey(imgname)) {
                continue;
            }

            InputStream imageStream = context.getResources().openRawResource(rawImages[i][0]);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            Mat img2 = new Mat();
            Utils.bitmapToMat(bitmap, img2);
            Imgproc.resize(img2, img2, sz);

            Mat resizeImg2 = new Mat();
            Imgproc.resize(img2, resizeImg2, sz);
            Imgproc.cvtColor(resizeImg2, resizeImg2, Imgproc.COLOR_RGBA2GRAY);
            //adaptiveThreshold(resizeImg2, resizeImg2, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 11, 5);
            //Imgproc.threshold(resizeImg2, resizeImg2, 80, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            Imgproc.threshold(resizeImg2, resizeImg2, 80, 255, Imgproc.THRESH_BINARY);


           /* Mat s1 = new Mat();
            double score = 0;
            absdiff(resizeImg1, resizeImg2, s1);
            s1.convertTo(s1, CvType.CV_32F);

            Scalar s = Core.sumElems(s1);

            //Scalar s = getMSSIM(resizeImg1,resizeImg2);

            score = s.val[0] + s.val[1] + s.val[2];
            score = score / (50 * 50 * 255);
            score = 1- score;*/

            double score = getPSNR(resizeImg1, resizeImg2);
            if (minError < score) {
                minError = score;
                //imageName = "" + (char) (rawImages[i][1] + 'a' - 1);
                //imageName = "" + rawImages[i][1] + " ( " + well_plate_value[i] + " )";
                r = rawImages[i][1] / 10;
                c = rawImages[i][1] % 10;
                imageName = "R-" + r + " C-" + c + " ( " + well_plate_value[i] + " )";
            }
        }

        cmp.set(imageName, minError);
        return cmp;
    }


    double getPSNR(Mat I1, Mat I2) {

        int z = 5;
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < z; j++) {
                I1.put(i, j, 0);
                I1.put(i, 49 - j, 0);
                I1.put(j, i, 0);
                I1.put(49 - j, i, 0);
            }
        }

        z = 5;

        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < z; j++) {
                I2.put(i, j, 0);
                I2.put(i, 49 - j, 0);
                I2.put(j, i, 0);
                I2.put(49 - j, i, 0);
            }
        }

        Mat s1 = new Mat();
        absdiff(I1, I2, s1);       // |I1 - I2|
        s1.convertTo(s1, CvType.CV_32F);  // cannot make a square on 8 bits
        s1 = s1.mul(s1);           // |I1 - I2|^2

        Scalar s = Core.sumElems(s1);         // sum elements per channel

        double sse = s.val[0] + s.val[1] + s.val[2]; // sum channels

        if (sse <= 1e-10) // for small values return zero
            return 0;
        else {
            double mse = sse / (double) (I1.channels() * I1.total());
            double psnr = 10.0 * log10((255 * 255) / mse);
            return psnr;
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
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
            int videoDuration = Integer.parseInt(time) / 1000;

            for (int i = 0; i < videoDuration; i += frame_rate) {
                Bitmap frame = retriever.getFrameAtTime(i * 1000000, MediaMetadataRetriever.OPTION_CLOSEST);
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

    Scalar getMSSIM(Mat I1, Mat I2) {
        double C1 = 6.5025, C2 = 58.5225;
        /***************************** INITS **********************************/

        I1.convertTo(I1, CvType.CV_32F);           // cannot calculate on one byte large values
        I2.convertTo(I2, CvType.CV_32F);

        Mat I2_2 = I2.mul(I2);        // I2^2
        Mat I1_2 = I1.mul(I1);        // I1^2
        Mat I1_I2 = I1.mul(I2);        // I1 * I2

        /*************************** END INITS **********************************/

        Mat mu1 = new Mat();
        Mat mu2 = new Mat();   // PRELIMINARY COMPUTING
        GaussianBlur(I1, mu1, new Size(11, 11), 1.5);
        GaussianBlur(I2, mu2, new Size(11, 11), 1.5);

        Mat mu1_2 = mu1.mul(mu1);
        Mat mu2_2 = mu2.mul(mu2);
        Mat mu1_mu2 = mu1.mul(mu2);

        Mat sigma1_2 = new Mat();
        Mat sigma2_2 = new Mat();
        Mat sigma12 = new Mat();

        GaussianBlur(I1_2, sigma1_2, new Size(11, 11), 1.5);
        Core.subtract(sigma1_2, mu1_2, sigma1_2);

        GaussianBlur(I2_2, sigma2_2, new Size(11, 11), 1.5);
        Core.subtract(sigma2_2, mu2_2, sigma2_2);

        GaussianBlur(I1_I2, sigma12, new Size(11, 11), 1.5);
        Core.subtract(sigma12, mu1_mu2, sigma12);

        ///////////////////////////////// FORMULA ////////////////////////////////
        Mat t1 = new Mat();
        Mat t2 = new Mat();
        Mat t3 = new Mat();

        Scalar alpha = new Scalar(2);
        //t1 = 2 * mu1_mu2 + C1;
        Core.multiply(mu1_mu2, alpha, t1);
        Scalar sc1 = new Scalar(C1);
        Core.add(t1, sc1, t1);

        //t2 = 2 * sigma12 + C2;
        Core.multiply(sigma12, alpha, t2);
        Scalar sc2 = new Scalar(C2);
        Core.add(t2, sc2, t2);

        t3 = t1.mul(t2);              // t3 = ((2*mu1_mu2 + C1).*(2*sigma12 + C2))

        //t1 = mu1_2 + mu2_2 + C1;
        Core.add(mu1_2, mu2_2, t1);
        Core.add(t1, sc1, t1);

        //t2 = sigma1_2 + sigma2_2 + C2;
        Core.add(sigma1_2, sigma2_2, t2);
        Core.add(t2, sc2, t2);

        t1 = t1.mul(t2);               // t1 =((mu1_2 + mu2_2 + C1).*(sigma1_2 + sigma2_2 + C2))

        Mat ssim_map = new Mat();
        divide(t3, t1, ssim_map);      // ssim_map =  t3./t1;

        Scalar mssim = mean(ssim_map); // mssim = average of ssim map

        //return mssim.val[0];

        return mssim;
    }

}
