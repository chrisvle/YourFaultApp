package me.chrisvle.yourfaultapp;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;

/**
 * Created by Chris on 10/12/15.
 */
public class FastEarthquakeWatcherService extends Service {

    private static HashSet<String> earthquakes;

    @Override
    public void onCreate() {
        super.onCreate();
        earthquakes = new HashSet<String>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Kick off new work to do
        createAndStartTimer();
        return START_STICKY;
    }

    private void createAndStartTimer() {
        CountDownTimer timer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        URL url;
                        HttpURLConnection urlConnection = null;
                        String converted = "";

                        String start = "http://earthquake.usgs.gov/fdsnws/event/1/";
                        String charset = java.nio.charset.StandardCharsets.UTF_8.name();
                        String query = start + "query?" + "format=" + "geojson" + "&" + "limit=" + 10;

                        try {
                            url = new URL(query);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.connect();
                            InputStream in = urlConnection.getInputStream();
                            converted = convertStreamToString(in);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (urlConnection != null) {
                                urlConnection.disconnect();
                            }
                        }

                        if (!converted.equals("")) {

                            JSONObject jsonobj = null;
                            JSONArray feature_arr = null;

                            try {
                                jsonobj = new JSONObject(converted);
                                feature_arr = jsonobj.getJSONArray("features");
                                for (int i = 0; i < feature_arr.length(); i++) {

                                    // Keep only new earthquakes
                                    String id = feature_arr.getJSONObject(i).getString("id");
                                    if (earthquakes.contains(id)) {
                                        continue;
                                    }
                                    Log.d("NEW", "A NEW EARTHQUAKE APPEARED!!!");
                                    earthquakes.add(id);

                                    // Get coordinates from each Earthquake
                                    JSONObject location = feature_arr.getJSONObject(i).getJSONObject("geometry");
                                    String lon = location.getJSONArray("coordinates").get(0).toString();
                                    String lat = location.getJSONArray("coordinates").get(1).toString();

                                    // Get location from each Earthquake
                                    String place = feature_arr.getJSONObject(i).getJSONObject("properties").getString("place");
                                    if (place.contains("of")) {
                                        int findOf = place.indexOf("of");
                                        place = feature_arr.getJSONObject(i).getJSONObject("properties")
                                                .getString("place").substring(findOf+3);
                                    }

                                    // Get magnitude from each Earthquake
                                    String mag = feature_arr.getJSONObject(i).getJSONObject("properties").getString("mag");

                                    Intent broadcast = new Intent("EarthquakeData");
                                    broadcast.putExtra("Place", place);
                                    broadcast.putExtra("Mag", mag);
                                    broadcast.putExtra("eLat", lat);
                                    broadcast.putExtra("eLon", lon);
                                    sendBroadcast(broadcast);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                createAndStartTimer();
            }
        };
        timer.start();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    // Code taken from https://developers.arcgis.com/android/sample-code/geojson-earthquake/
    public static String convertStreamToString(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder jsonstr = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String t = line + "\n";
                jsonstr.append(t);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonstr.toString();
    }
}
