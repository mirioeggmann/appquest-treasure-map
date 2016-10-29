package ch.jmelab.treasure_map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by Timon Borter on 29.10.2016.
 */

public class LocationHandler {
    // Static values
    private static final String STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final String GPS_PERMISSION = "android.permission.ACCESS_FINE_LOCATION";
    private static final int GPS_PERMISSION_CODE = 2;

    // Location manager
    private LocationManager locationManager;
    private String provider;

    public LocationManager getLocationManager() {
        return this.locationManager;
    }

    public String getBestProvider() {
        return this.provider;
    }

    // "Super" class
    private MainActivity mainActivity;

    public LocationHandler(MainActivity sup3r) {
        this.mainActivity = sup3r;
    }

    public void checkPreRequests() {
        // Check application permissions
        checkGPSPermission();
    }

    private void checkGPSPermission() {
        if (mainActivity.checkCallingOrSelfPermission(GPS_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(getClass().toString(), "GPS permissions not granted yeted!");
            ActivityCompat.requestPermissions(mainActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    GPS_PERMISSION_CODE);
        } else {
            Log.d(getClass().toString(), "GPS permissions already granted!");
            checkStoragePermission();
        }
    }

    private void checkStoragePermission() {
        // Check storage access
        if (mainActivity.checkCallingOrSelfPermission(STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(getClass().toString(), "Storage permissions not granted yet!");
            ActivityCompat.requestPermissions(mainActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            Log.d(getClass().toString(), "Storage permissions already granted!");
            if (isGPSServiceEnabled()) {
                mainActivity.continueApplicationSetUp();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantedResult) {
        switch (requestCode) {
            case GPS_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantedResult.length > 0
                        && grantedResult[0] == PackageManager.PERMISSION_GRANTED) {
                    checkStoragePermission();
                } else {
                    new AlertDialog.Builder(mainActivity)
                            .setMessage("This application requires GPS permission!")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Kill process
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            })
                            .show();
                }
                return;
            }
            case STORAGE_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantedResult.length > 0
                        && grantedResult[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isGPSServiceEnabled()) {
                        mainActivity.continueApplicationSetUp();
                    }
                } else {
                    new AlertDialog.Builder(mainActivity)
                            .setMessage("This application requires Storage permission!")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Kill process
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            })
                            .show();
                }
                return;
            }
        }
    }

    public boolean isGPSServiceEnabled() {
        // Update location listener
        if (locationManager == null) {
            locationManager = (android.location.LocationManager) mainActivity.getSystemService(mainActivity.LOCATION_SERVICE);
        }

        // Check if GPS enabled
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Log.e(getClass().toString(), "GPS not enabled yet!");
            mainActivity.onProviderDisabled(provider);
            return false;
        }

        Log.d(getClass().toString(), "GPS already enabled.");
        return true;
    }

    public void requestLocationUpdates(LocationListener listener) {
        // Get best provider
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);

        // Request location updates
        try {
            locationManager.requestLocationUpdates(provider, 10, 10, listener);
        } catch (SecurityException e) {
            Log.e(getClass().toString(), e.getLocalizedMessage());
        }
    }
}
