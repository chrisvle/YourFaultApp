package me.chrisvle.yourfaultapp;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;

public class MobileMain extends ListActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private BroadcastReceiver mReceiver;
    private IntentFilter filter;

    private static GoogleApiClient mGoogleApiClient;
    private boolean firstLocationRequest = true;
    private static int count;

    private String mLon;
    private String mLat;

    private String eLon;
    private String eLat;
    private String place;
    private int distance;
    private String mag;

    private String message;

    private static final String START_ACTIVITY = "/start_activity";
    private static HashMap<String, String> earthquakes = new HashMap<String, String>();

    private static ListView list;
    private static ArrayList<String> itemnames = new ArrayList<String>();
    private static ArrayList<String> mags = new ArrayList<String>();
    private static ArrayList<String> distances = new ArrayList<String>();

    private static CustomListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquakes);

        // Check if GPS is on, thanks Stack Overflow
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(provider != null ){
            if(! provider.contains("gps")){
                // Notify users and show settings if they want to enable GPS
                new AlertDialog.Builder(this)
                        .setMessage("GPS is switched off. enable?")
                        .setPositiveButton("Enable GPS", new DialogInterface.OnClickListener(){

                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, 5);
                            }
                        })
                        .setNegativeButton("Don't do it", new DialogInterface.OnClickListener(){

                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        }

        count = 0;

        if (firstLocationRequest) {
            Toast.makeText(this, "Updating most recent earthquakes...", Toast.LENGTH_LONG).show();
        }

        adapter = new CustomListAdapter(this, itemnames, mags, distances);
        list = (ListView)findViewById(android.R.id.list);
        list.setAdapter(adapter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                place = extras.getString("Place");
                mag = extras.getString("Mag");
                eLat = extras.getString("eLat");
                eLon = extras.getString("eLon");

                String listMessage = place + " " + mag;

                distance = (int) distance(Double.parseDouble(mLat), Double.parseDouble(mLon), Double.parseDouble(eLat), Double.parseDouble(eLon));

                updateUI(listMessage, Integer.toString(distance));
                message = place + " " + Double.toString(distance) + " " + mag;

                earthquakes.put(listMessage, eLat + " " + eLon + " " + Double.toString(distance) + " " + mag);
                count++;
                if (count >= 10) {
                    sendMessage(START_ACTIVITY, message);

                    Intent openMaps = new Intent(getBaseContext(), GoogleMapsActivity.class);
                    openMaps.putExtra("Latitude", eLat);
                    openMaps.putExtra("Longitude", eLon);
                    openMaps.putExtra("Place", place);
                    openMaps.putExtra("Distance", Integer.toString(distance));
                    openMaps.putExtra("Mag", mag);
                    startActivity(openMaps);
                }
            }
        };

        filter = new IntentFilter();
        filter.addAction("EarthquakeData");
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String listing = adapter.getItemName(position);
        String[] all_details = earthquakes.get(listing).split(" ");

        String[] full_title = listing.split(" ");
        String title = "";
        for (int i = 0; i < full_title.length-1; i++) {
            title = title + full_title[i] + " ";
        }

        Intent makeMap = new Intent(this, GoogleMapsActivity.class);
        String lat = all_details[0];
        String lon = all_details[1];
        String distance = all_details[2];
        String mag = all_details[3];
        Log.d("CLICK", lat);
        Log.d("CLICK", lon);
        Log.d("CLICK", listing);
        makeMap.putExtra("Latitude", lat);
        makeMap.putExtra("Longitude", lon);
        makeMap.putExtra("Place", title);
        makeMap.putExtra("Distance", distance);
        makeMap.putExtra("Mag", mag);
        startActivity(makeMap);

        sendMessage(START_ACTIVITY, message);

    }


    // Special thanks to Stackoverflow
    public static double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75; // miles (or 6371.0 kilometers)
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
    }

    public static void sendMessage( final String path, final String text) {
        Log.d("send", "Sending message");
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    Log.d("Node", "Found Node inside mobile MAIN");
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        registerReceiver(mReceiver, filter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
        unregisterReceiver(mReceiver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connResult) {
    }

    @Override
    public void onConnected(Bundle bundle) {

        // Build a request for continual location updates
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(300000)
                .setFastestInterval(10000);

        // Send request for location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.getStatus().isSuccess()) {
                            Log.d("SUCCESS", "Successfully requested");
                        } else {
                            Log.e("FAILED", status.getStatusMessage());
                        }
                    }
                });
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location object", location.toString());
        mLat = String.valueOf(location.getLatitude());
        mLon = String.valueOf(location.getLongitude());
        if (firstLocationRequest) {
            Intent i = new Intent(this, FastEarthquakeWatcherService.class);
            startService(i);
            firstLocationRequest = false;
        }
    }

    private void updateUI(String listMessage, String distance) {
        Log.d("changing UI", listMessage);
        String[] split = listMessage.split(" ");
        String mag = split[split.length-1];
        int index = listMessage.indexOf(mag);
        String title = listMessage.substring(0, index);
        itemnames.add(0, title);
        mags.add(0, mag);
        distances.add(0, distance);
        adapter.notifyDataSetChanged();
    }

}