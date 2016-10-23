package ch.jmelab.treasure_map;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MBTilesFileArchive;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.File;

public class MainActivity extends Activity implements LocationListener {
    // Static variables
    private static final int REQUEST_APP_SETTINGS = 168;
    private static final int FILE_SELECT_CODE = 0;

    // Map view
    MapView map;
    IMapController controller;

    // Location manager
    LocationManager locationManager;
    String provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load local map
        // Possibility of file picker here
        setUpMapView(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/hsr.mbtiles"));

        // Initalize android GPS
        initalizeLocationTrackerAndCheckPermission();

        // Add listener to buttons
        addButtonListeners();
    }

    private void setUpMapView(File databaseFile) {
        // Check if map file exists
        if (!databaseFile.exists()) {
            Log.e(getClass().toString(), "Database file does not exist at: " + databaseFile.getAbsolutePath());
            return;
        }

        map = (MapView) findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);

        controller = map.getController();
        controller.setZoom(18);

        // Die TileSource beschreibt die Eigenschaften der Kacheln die wir anzeigen
        XYTileSource treasureMapTileSource = new XYTileSource("mbtiles", ResourceProxy.string.offline_mode, 1, 20, 256, ".png", "http://127.0.0.1");

        /* Das verwenden von mbtiles ist leider ein wenig aufwändig, wir müssen
         * unsere XYTileSource in verschiedene Klassen 'verpacken' um sie dann
         * als TilesOverlay über der Grundkarte anzuzeigen.
         */
        MapTileModuleProviderBase treasureMapModuleProvider = new MapTileFileArchiveProvider(new SimpleRegisterReceiver(this),
                treasureMapTileSource, new IArchiveFile[]{MBTilesFileArchive.getDatabaseFileArchive(databaseFile)});

        MapTileProviderBase treasureMapProvider = new MapTileProviderArray(treasureMapTileSource, null,
                new MapTileModuleProviderBase[]{treasureMapModuleProvider});

        TilesOverlay treasureMapTilesOverlay = new TilesOverlay(treasureMapProvider, getBaseContext());
        treasureMapTilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);

        // Jetzt können wir den Overlay zu unserer Karte hinzufügen:
        map.getOverlays().add(treasureMapTilesOverlay);
    }

    private void initalizeLocationTrackerAndCheckPermission() {
        // Check permission given
        String filePermission = "android.permission.WRITE_EXTERNAL_STORAGE";
        String gpsPermission = "android.permission.ACCESS_FINE_LOCATION";
        boolean granted = (checkCallingOrSelfPermission(gpsPermission) == PackageManager.PERMISSION_GRANTED && checkCallingOrSelfPermission(filePermission) == PackageManager.PERMISSION_GRANTED);

        // Ask for permissions if not granted yet
        if (!granted) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Application permissions")
                    .setMessage("This app requires permission to read storage data and access GPS location. Do you want to change your Settings now?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Open applicaiton settings
                            Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                            myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivityForResult(myAppSettings, REQUEST_APP_SETTINGS);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Exit applicaiton on no
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .show();
        }

        if (granted) {
            // Update location listener
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                provider = locationManager.getBestProvider(criteria, true);
            }

            try {
                locationManager.requestLocationUpdates(provider, 10, 10, this);
            } catch (SecurityException e) {
                Log.e(getClass().toString(), e.getLocalizedMessage());
            }
        }
    }

    private void addButtonListeners() {
        // Mark point button

        // Export button
    }

    @Override
    public void onLocationChanged(Location location) {
        // Center map
        Log.d(getClass().toString(), "Centering to: " + location.getLatitude() + " / " + location.getLongitude());
        controller.setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        // Update location listener
        Log.i(getClass().toString(), "GPS provider enabled.");
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);
        }

        try {
            locationManager.requestLocationUpdates(provider, 10, 10, this);
        } catch (SecurityException e) {
            Log.e(getClass().toString(), e.getLocalizedMessage());
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Ask for permissions if not granted yet
        Log.e(getClass().toString(), "GPS provider disabled. Checking settings..");
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Activate GPS")
                .setMessage("This app requires GPS enabled. Do you want to change your Settings now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open GPS settings on yes
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Exit applicaiton on no
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .show();
    }
}
