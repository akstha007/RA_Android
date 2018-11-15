package com.developers.meraki.projectw;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "SettingsActivity";
    private EditText editFrameRate;
    private static TextView txtDirectory;
    private ImageButton imgBrowse;
    private Spinner spinnerRotation;
    private String rotation;
    private int DIR_REQUEST_CODE = 1;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initIds();
        setDisplay();
    }

    private void setDisplay() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.array_rotation, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRotation.setAdapter(adapter);

        String rotation = prefManager.getStringValue("rotation");
        if (rotation.equalsIgnoreCase("0")) {
            spinnerRotation.setSelection(0);
        } else if (rotation.equalsIgnoreCase("90")) {
            spinnerRotation.setSelection(1);
        }else if (rotation.equalsIgnoreCase("180")) {
            spinnerRotation.setSelection(2);
        }else if (rotation.equalsIgnoreCase("-90")) {
            spinnerRotation.setSelection(3);
        }
    }

    private void initIds() {
        prefManager = new PrefManager(this);

        editFrameRate = (EditText) findViewById(R.id.editFrameRate);
        txtDirectory = (TextView) findViewById(R.id.txtDirectory);
        imgBrowse = (ImageButton) findViewById(R.id.btnBrowse);
        spinnerRotation = (Spinner) findViewById(R.id.spinnerRotation);

        imgBrowse.setOnClickListener(this);
        spinnerRotation.setOnItemSelectedListener(this);

        //set values got from prefmanager
        int frate = prefManager.getIntValue("frame_rate");
        editFrameRate.setText("" + frate);

        String path = prefManager.getStringValue("root_path");
        txtDirectory.setText(path);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        saveData();
        //this.finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnBrowse:
                //loadDir();
                break;

        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        if (parent.getItemAtPosition(pos).toString().equalsIgnoreCase("0")) {
            //No rotation
            rotation = "0";
        } else if (parent.getItemAtPosition(pos).toString().equalsIgnoreCase("90")) {
            //clockwise 90
            rotation = "90";
        }else if (parent.getItemAtPosition(pos).toString().equalsIgnoreCase("180")) {
            //clockwise 180
            rotation = "180";
        }else if (parent.getItemAtPosition(pos).toString().equalsIgnoreCase("-90")) {
            //anticlockwise 90
            rotation = "-90";
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void saveData() {
        String frameRate = editFrameRate.getText().toString();
        int fRate = Integer.parseInt(frameRate);
        if (fRate < 0 || fRate > 50) {
            editFrameRate.setFocusable(true);
            Toast.makeText(this, "Select valid frame rate (1 to 50).", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = txtDirectory.getText().toString();
        //Toast.makeText(this, "Settings: f::" + frameRate + ":p:" + path + ":r:" + rotation, Toast.LENGTH_SHORT).show();

        //save values to prefmanager
        prefManager.setIntValue("frame_rate", fRate);
        prefManager.setStringValue("root_path", path);
        prefManager.setStringValue("rotation", rotation);

        this.finish();
    }

    private void loadDir() {
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);

        startActivityForResult(intent, DIR_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == DIR_REQUEST_CODE && resultCode == RESULT_OK && null != data) {
                Uri uri = data.getData();
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                        DocumentsContract.getTreeDocumentId(uri));
                String str = docUri.getPath();

                String path = getPath(this, docUri);

                String[] paths = str.split(":");
                //String path = paths.length >= 3 ? paths[2] : "";
                if (path != null) {
                    //path = Panorama.getPublicAlbumStorageDir(path);
                }
                txtDirectory.setText(uri.getPath()+" == "+str+ " Path: " + path);

            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong!!! " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + split[1];
                } else {
                    String SdcardPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
                    String dir = SdcardPath.substring(SdcardPath.lastIndexOf('/') + 1);
                    String[] trimmed = SdcardPath.split(dir);
                    String sdcardPath = trimmed[0];
                    return sdcardPath + "/" + split[1];
                    //return Environment.getExternalStoragePublicDirectory(null) + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
