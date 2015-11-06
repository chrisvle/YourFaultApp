package me.chrisvle.yourfaultapp;

/**
 * Created by Chris on 10/12/15.
 */

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class TwitAndSensActivity extends WearableActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String EXIT_ACTIVITY = "/exit";
    private static final String SHAKE_ACTIVITY = "/shake";

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 400;

    public static GoogleApiClient mApiClient;

    private static String place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Intent i = getIntent();
        String description = i.getStringExtra("Description");
        place = i.getStringExtra("Place");

        if (description != null && !description.equals("")) {
            TextView tv = (TextView) findViewById(R.id.description);
            String shortened = description.substring(0,25);
            tv.setText(shortened+"...");
        }

        ImageButton button = (ImageButton) findViewById(R.id.cancel);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage(EXIT_ACTIVITY, "Back to Listview");
                finish();
                System.exit(0);
            }
        });



        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();


    }

    // Thank you stackoverflow
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long currTime = System.currentTimeMillis();

            if ((currTime - lastUpdate) > 100) {
                long timeDiff = (currTime - lastUpdate);
                lastUpdate = currTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / timeDiff * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Toast.makeText(getBaseContext(), "Shaaaaking!", Toast.LENGTH_LONG) .show();
                    sendMessage(SHAKE_ACTIVITY, place);
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public static void sendMessage( final String path, final String text ) {
        Log.d("send", "Sending message");
        new Thread( new Runnable() {
            @Override
            public void run() {
                Log.d("Sending", "Sending a message from the watch");
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    Log.d("Node", "found a node inside WATCH MAIN");
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                    Log.d("result", result.toString());
                }
            }
        }).start();
        Log.d("after", "after running thread is called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApiClient.disconnect();
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
