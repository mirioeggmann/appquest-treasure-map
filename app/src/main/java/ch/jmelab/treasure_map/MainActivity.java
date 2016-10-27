package ch.jmelab.treasure_map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements LocationListener {
    // Static variables
    private static final int REQUEST_APP_SETTINGS = 168;
    private static final String PREFERENCES_NAME = "Treasure_Map_Preferences";

    // Map view
    MapView map;
    IMapController controller;

    // Location manager
    LocationManager locationManager;
    String provider;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initalize android GPS
        if (initalizeLocationTrackerAndCheckPermission()) {
            // Load local map
            // Possibility of file picker here
            File mapFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/hsr.mbtiles");  // For example getFileFromUser();
            setUpMapView(mapFile);

            // Clear stored points
            prepareSharedPreferences();

            // Add listener to buttons
            addButtonListeners();

            // Finished build
            Log.i(getClass().toString(), "Application started successfully.");
        } else {
            Log.e(getClass().toString(), "Permission not granted! Exiting..");
        }
    }

    private boolean initalizeLocationTrackerAndCheckPermission() {
        // Check permission given
        String filePermission = "android.permission.WRITE_EXTERNAL_STORAGE";
        String gpsPermission = "android.permission.ACCESS_FINE_LOCATION";
        boolean granted = (checkCallingOrSelfPermission(gpsPermission) == PackageManager.PERMISSION_GRANTED && checkCallingOrSelfPermission(filePermission) == PackageManager.PERMISSION_GRANTED);

        // Ask for permissions if not granted yet
        if (!granted) {
            Log.e(getClass().toString(), "Missing permissions. Asking user to grant..");

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
                            Log.e(getClass().toString(), "User denied application permission.");
                        }
                    })
                    .show();
        }

        granted = (checkCallingOrSelfPermission(gpsPermission) == PackageManager.PERMISSION_GRANTED && checkCallingOrSelfPermission(filePermission) == PackageManager.PERMISSION_GRANTED);

        if (granted) {
            Log.d(getClass().toString(), "All permissions granted. Building location service..");

            // Update location listener
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            }

            // Check if GPS enabled
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.e(getClass().toString(), "GPS not enabled yet!");
                onProviderDisabled(provider);
            } else {
                Log.d(getClass().toString(), "GPS already enabled.");
            }

            // Get best provider
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);

            // Request location updates
            try {
                locationManager.requestLocationUpdates(provider, 10, 10, this);
            } catch (SecurityException e) {
                Log.e(getClass().toString(), e.getLocalizedMessage());
            }

            return true;
        }

        return false;
    }

    private void setUpMapView(File databaseFile) {
        // Check if map file exists
        if (!databaseFile.exists()) {
            Log.e(getClass().toString(), "Database file does not exist at: " + databaseFile.getAbsolutePath());
            return;
        } else {
            Log.d(getClass().toString(), "Opening database from: " + databaseFile.getAbsolutePath());
        }

        map = (MapView) findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);

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
    }

    private void prepareSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
        sharedPreferences.edit().apply();
    }

    private void addButtonListeners() {
        // Mark point button
        ImageButton markPointButton = (ImageButton) findViewById(R.id.imagebuttonpoint);
        markPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Check GPS connection
                    if (locationManager.getLastKnownLocation(provider) == null) {
                        Log.e(getClass().toString(), "Location unknown! Cannot set mark..");
                        return;
                    }

                    // Acccess shared preferences
                    SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();

                    // Add point
                    Log.i(getClass().toString(), "Adding location marrk at actual point: " + locationManager.getLastKnownLocation(provider).getLatitude() + "/" + locationManager.getLastKnownLocation(provider).getLongitude());
                    long timeStamp = System.currentTimeMillis() / 1000;
                    Log.d(getClass().toString(), "Latitude measured at: "+timeStamp);
                    preferencesEditor.putLong("Latitude at " + timeStamp, Double.doubleToRawLongBits(locationManager.getLastKnownLocation(provider).getLatitude()));
                    preferencesEditor.putLong("Altitude at " + timeStamp, Double.doubleToRawLongBits(locationManager.getLastKnownLocation(provider).getAltitude()));
                    Log.d(getClass().toString(), "Altitude measured at: "+timeStamp);
                    preferencesEditor.commit();
                    preferencesEditor.apply();
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

                                Intent intent = new Intent("ch.appquest.intent.LOG");

                                if (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                                    Toast.makeText(v.getContext(), "Logbook app not installed", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                JSONObject jsonMessage = new JSONObject();
                                try {
                                    // First part of json message
                                    jsonMessage.put("task", "Schatzkarte");
                                    JSONArray jsonPoints = new JSONArray();

                                    // Prerequests
                                    Map<String, ?> sharedPoints = sharedPreferences.getAll();
                                    List<Long> timestampList = new ArrayList<Long>();

                                    // For each saved location
                                    for (Map.Entry<String, ?> pointToSearch : sharedPoints.entrySet()) {
                                        // If timestamp not collected yet
                                        if (!timestampList.contains(Long.valueOf(pointToSearch.getKey().split(" ")[2]))) {
                                            // Construct array object
                                            JSONObject singlePoint = new JSONObject();

                                            // Determine if lat/lon
                                            if (pointToSearch.getKey().contains("Latitude")) {
                                                for (Map.Entry<String, ?> pointToCheck : sharedPoints.entrySet()) {
                                                    // On lon match
                                                    if (pointToCheck.getKey().contains("Altitude") && Long.valueOf(pointToCheck.getKey().split(" ")[2]) == Long.valueOf(pointToSearch.getKey().split(" ")[2])) {
                                                        // Add lat/lon to json location object
                                                        Log.d(getClass().toString(), "Exporting point: " + pointToSearch.getValue() + "/" + pointToCheck.getValue());

                                                        // Add lat/lon to json location object
                                                        singlePoint.put("lat", Double.longBitsToDouble((Long) pointToSearch.getValue()) * 1000000);
                                                        singlePoint.put("lon", Double.longBitsToDouble((Long) pointToCheck.getValue()) * 1000000);

                                                        // Add locations to array
                                                        jsonPoints.put(singlePoint);

                                                        // Break search
                                                        break;
                                                    }
                                                }
                                            } else {
                                                for (Map.Entry<String, ?> pointToCheck : sharedPoints.entrySet()) {
                                                    // On lat match
                                                    if (pointToCheck.getKey().contains("Latitude") && Long.valueOf(pointToCheck.getKey().split(" ")[2]) == Long.valueOf(pointToSearch.getKey().split(" ")[2])) {
                                                        // Add lat/lon to json location object
                                                        Log.d(getClass().toString(), "Exporting point: " + pointToCheck.getValue() + "/" + pointToSearch.getValue());

                                                        // Add lat/lon to json location object
                                                        singlePoint.put("lat", Double.longBitsToDouble((Long) pointToCheck.getValue()) * 1000000);
                                                        singlePoint.put("lon", Double.longBitsToDouble((Long) pointToSearch.getValue()) * 1000000);

                                                        // Add locations to array
                                                        jsonPoints.put(singlePoint);

                                                        // Break search
                                                        break;
                                                    }
                                                }
                                            }

                                            // Add timestamp to list of handled ones
                                            timestampList.add(Long.valueOf(pointToSearch.getKey().split(" ")[2]));
                                        }
                                    }

                                    // Add array to message
                                    jsonMessage.put("points", jsonPoints);
                                } catch (JSONException e) {
                                    Log.e(getClass().toString(), e.getLocalizedMessage());
                                }

                                // Convert to string
                                String message = jsonMessage.toString();

                                // Add to intent
                                intent.putExtra("ch.appquest.logmessage", message);

                                // Export data
                                Log.i(getClass().toString(), "Exporting data to logbook: " + message);
                                startActivity(intent);

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
        initalizeLocationTrackerAndCheckPermission();
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

    @Override
    public void onProviderEnabled(String provider) {
        // Update location listener
        Log.i(getClass().toString(), "GPS provider enabled.");
        initalizeLocationTrackerAndCheckPermission();
    }
}
