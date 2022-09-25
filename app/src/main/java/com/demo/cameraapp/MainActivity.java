package com.demo.cameraapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final int PERMISSION_REQUEST_CODE = 101;
    private String[] PERMISSIONS;
    private String imageName = "";
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link XML Components
        Button cameraBtn = findViewById(R.id.cameraBtn);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Array of Permissions
        PERMISSIONS = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        cameraBtn.setOnClickListener(v -> {
            if (hasPermission(MainActivity.this, PERMISSIONS))
                launchCamera();
            else
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        });
    }

    /**
     * Function to check if required permissions are granted or not
     * @param context Context
     * @param PERMISSIONS Array of Permissions
     * @return returns "True" if permission granted otherwise "False"
     */
    private boolean hasPermission(Context context, String... PERMISSIONS) {
        if (context != null && PERMISSIONS != null)
            for (String permission : PERMISSIONS)
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED)
                launchCamera();
            else{
                Toast.makeText(this, "Allow Permissions to capture and Save Image!!!", Toast.LENGTH_SHORT).show();
            }
    }

    /**
     * Function to launch camera
     */
    private void launchCamera() {
        Uri imageCollection;
        ContentResolver resolver = getContentResolver();

        imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        ContentValues contentValues = new ContentValues();
        imageName = System.currentTimeMillis() + ".jpg";
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "My Images");
        Uri imageUri = resolver.insert(imageCollection, contentValues);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraResultLauncher.launch(cameraIntent);
    }

    @SuppressLint("MissingPermission")
    ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if (location != null){
                                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                    List<Address> addressList = null;
                                    try {
                                        addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    // Get latitude and longitude
                                    assert addressList != null;
                                    double longitude = addressList.get(0).getLongitude();
                                    double latitude = addressList.get(0).getLatitude();

                                    // Set Exif Data in captured Image
                                    File file = new File(Environment.getExternalStorageDirectory(), "Pictures/My Images/" + imageName);
                                    setExifData(file,latitude,longitude, "Roshan Kumar");
                                }
                            });

                    Toast.makeText(this, "Image Saved!!!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    /**
     * Function to write meta data information in the given image file
     * @param file "Image File" - in which the exif data to be set
     * @param latitude latitude
     * @param longitude longitude
     * @param copyright copyright
     */
    private void setExifData(File file, double latitude, double longitude, String copyright){
        ExifInterface exif;

        try {
            exif = new ExifInterface(file);

            // Set Latitude & longitude
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(longitude));

            if (latitude > 0)
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            else
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");

            if (longitude > 0)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
            else
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");

            // Set Copyright
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, copyright);

            exif.saveAttributes();
        } catch (Exception e) {
            Log.e("MainActivity", e.getLocalizedMessage());
        }
    }
}