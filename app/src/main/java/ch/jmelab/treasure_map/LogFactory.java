package ch.jmelab.treasure_map;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Timon Borter on 29.10.2016.
 */

public class LogFactory {
    private Context context;
    private Intent logBook;

    public LogFactory(Context context) {
        // Save private variables
        this.context = context;

        // Check if logbook is installed
        logBook = new Intent("ch.appquest.intent.LOG");
        if (context.getPackageManager().queryIntentActivities(logBook, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            Toast.makeText(context, "Logbook app not installed", Toast.LENGTH_LONG).show();
            return;
        }
    }

    public Intent getLogActivity(HashMap<String, ?> logValues) {
        // Create log message
        JSONObject jsonMessage = new JSONObject();
        try {
            // First part of json message
            jsonMessage.put("task", "Schatzkarte");
            JSONArray jsonPoints = new JSONArray();

            // Prerequests
            List<Long> timestampList = new ArrayList<Long>();

            // For each saved location
            for (Map.Entry<String, ?> pointToSearch : logValues.entrySet()) {
                // If timestamp not collected yet
                if (!timestampList.contains(Long.valueOf(pointToSearch.getKey().split(" ")[2]))) {
                    // Construct array object
                    JSONObject singlePoint = new JSONObject();

                    // Determine if lat/lon
                    if (pointToSearch.getKey().contains("Latitude")) {
                        for (Map.Entry<String, ?> pointToCheck : logValues.entrySet()) {
                            // On lon match
                            if (pointToCheck.getKey().contains("Altitude") && Long.valueOf(pointToCheck.getKey().split(" ")[2]).equals(Long.valueOf(pointToSearch.getKey().split(" ")[2]))) {
                                Log.d(getClass().toString(), "Data from " + pointToSearch.getKey() + " matches " + pointToCheck.getKey());

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
                        for (Map.Entry<String, ?> pointToCheck : logValues.entrySet()) {
                            // On lat match
                            if (pointToCheck.getKey().contains("Latitude") && Long.valueOf(pointToCheck.getKey().split(" ")[2]).equals(Long.valueOf(pointToSearch.getKey().split(" ")[2]))) {
                                Log.d(getClass().toString(), "Data from " + pointToSearch.getKey() + " matches " + pointToCheck.getKey());

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

        // Add collected data
        Log.i(getClass().toString(), "Collected log data: " + jsonMessage.toString());
        logBook.putExtra("ch.appquest.logmessage", jsonMessage.toString());

        // Log
        return logBook;
    }
}
