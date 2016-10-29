package ch.jmelab.treasure_map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

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
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements LocationListener {
    // Static values
    private static final String PREFERENCES_NAME = "Treasure_Map_Preferences";

    // Permission and service manager
    private LocationHandler locationHandler;

    // Map view
    private MapView map;
    private IMapController controller;
    List<OverlayItem> overlayItemArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initalize application check
        locationHandler = new LocationHandler(this);

        // Clear stored points
        prepareSharedPreferences();

        locationHandler.checkPreRequests();
    }

    public void continueApplicationSetUp() {
        // Load local map
        // Possible locations are download ore storage
//        String mapPath = Environment.getExternalStorageDirectory() + "/Android/data/net.osmand/files/Switzerland_europe.obf";
        String mapPath = Environment.getExternalStorageDirectory() + "/hsr.mbtiles";
//            String mapPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/hsr.mbtiles";

        // Possibility of file picker here
        File mapFile = new File(mapPath);  // For example getFileFromUser();
        setUpMapView(mapFile);

        // Add listener to buttons
        addButtonListener();

        locationHandler.requestLocationUpdates(this);

        // Finished build
        Log.i(getClass().toString(), "Application started successfully.");
    }

    private void prepareSharedPreferences() {
        // Remove all saved values
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit().clear().apply();
    }

    private void setUpMapView(File databaseFile) {
        // Catch sql exceptions
        try {
            // Show map view
            map = (MapView) findViewById(R.id.mapview);
            map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

            // Map settings
            map.setMultiTouchControls(true);
            map.setBuiltInZoomControls(true);

            // Create map controller (to zoom/center map)
            controller = map.getController();
            controller.setZoom(18);

            // Die TileSource beschreibt die Eigenschaften der Kacheln die wir anzeigen
            XYTileSource treasureMapTileSource = new XYTileSource("mbtiles", ResourceProxy.string.offline_mode, 1, 20, 256, ".png", new String[]{});

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

            // Create overlay for markers
            overlayItemArray = new ArrayList<>();
            ItemizedIconOverlay<OverlayItem> itemItemizedIconOverlay = new ItemizedIconOverlay<>(this, overlayItemArray, null);
            map.getOverlays().add(itemItemizedIconOverlay);
        } catch (SQLiteCantOpenDatabaseException e) {
            Log.e(getClass().toString(), e.getLocalizedMessage());
            new AlertDialog.Builder(this)
                    .setMessage("Could not find any map file at the specified location. Please make sure you placed it in your default \"external\" storage.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Kill process
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .show();
        }
    }

    private void addButtonListener() {
        // Mark point button
        ImageButton markPointButton = (ImageButton) findViewById(R.id.imagebuttonpoint);
        markPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Check GPS connection
                    if (locationHandler.getLocationManager().getLastKnownLocation(locationHandler.getBestProvider()) == null) {
                        Log.e(getClass().toString(), "Location unknown! Cannot reach GPS..");
                        Toast.makeText(v.getContext(), "Location unknown! Cannot reach GPS..", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Get location
                    double lastLat = locationHandler.getLocationManager().getLastKnownLocation(locationHandler.getBestProvider()).getLatitude();
                    double lastLong = locationHandler.getLocationManager().getLastKnownLocation(locationHandler.getBestProvider()).getAltitude();

                    // Save location
                    saveLocationInPreferences(lastLat, lastLong);
//                    saveLocationOnMap(lastLat, lastLong);
                } catch (SecurityException e) {
                    Log.e(getClass().toString(), e.getLocalizedMessage());
                }
            }
        });

        // Export button
        ImageButton exportButton = (ImageButton) findViewById(R.id.imagebuttonexport);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // User must confirm export
                new AlertDialog.Builder(v.getContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Export data")
                        .setMessage("Are you sure you want to export your collected data now?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Open applicaiton settings
                                Log.i(getClass().toString(), "Confirmed exporting marks to logbook..");

                                // Create log factory
                                // Save collected data to log
                                LogFactory logFactory = new LogFactory(getApplicationContext());
                                startActivity(logFactory.getLogActivity((HashMap<String, ?>) getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getAll()));

                                // Inform user
                                Toast.makeText(v.getContext(), "Exported data to logbook", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(getClass().toString(), "Canceled exporting marks to logbook..");
                                return;
                            }
                        })
                        .show();
            }
        });
    }

    private void saveLocationInPreferences(double lastLat, double lastLong) {
        // Acccess shared preferences
        SharedPreferences.Editor preferencesEditor = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit();

        // Add point to preferences
        Log.i(getClass().toString(), "Adding location marker at actual point: " + lastLat + "/" + lastLong);
        long timeStamp = System.currentTimeMillis() / 1000;
        preferencesEditor.putLong("Latitude at " + timeStamp, Double.doubleToRawLongBits(lastLat));
        preferencesEditor.putLong("Altitude at " + timeStamp, Double.doubleToRawLongBits(lastLong));

        // Save point
        preferencesEditor.commit();
        Log.d(getClass().toString(), "Collected " + getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getAll().size() / 2 + " locations.");
    }

//    private void saveLocationOnMap(double lastLat, double lastLong) {
//        // Add point to map
//        GeoPoint currentLocation = new GeoPoint((int) (lastLat * 1E6), (int) (lastLong * 1E6));
//        OverlayItem myLocationOverlayItem = new OverlayItem("Marker", "Important location", currentLocation);
//        overlayItemArray.add(myLocationOverlayItem);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantedResults) {
        locationHandler.onRequestPermissionsResult(requestCode, permissions, grantedResults);
    }

    @Override
    public void onLocationChanged(Location location) {
        // Center map
        Log.d(getClass().toString(), "Centering to: " + location.getLatitude() + " / " + location.getLongitude());
        controller.setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Update location listener
        Log.d(getClass().toString(), "Provider status changed. Updating provider..");
        locationHandler.checkPreRequests();
    }

    @Override
    public void onProviderDisabled(final String provider) {
        // Ask for permissions if not granted yet
        Log.e(getClass().toString(), "GPS provider disabled. Enabling..");
        new AlertDialog.Builder(this)
                .setMessage("This application required GPS service to be enabled. Enable now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent gpsSettings = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsSettings);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Reask
                        onProviderDisabled(provider);
                    }
                })
                .setNeutralButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Kill process
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Update location listener
        Log.i(getClass().toString(), "GPS provider enabled.");
        locationHandler.checkPreRequests();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (locationHandler.isGPSServiceEnabled()) {
            locationHandler.requestLocationUpdates(this);
        }
    }
}
