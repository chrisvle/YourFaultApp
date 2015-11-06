package me.chrisvle.yourfaultapp;

/**
 * Created by Chris on 10/12/15.
 */

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;


public class WatchListenerService extends WearableListenerService {

    private static final String START_ACTIVITY = "/start_activity";
    private static final String TWITTER_ACTIVITY = "/Twitter";

    private NotificationManagerCompat mNotificationManager;

    String place;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if( messageEvent.getPath().equalsIgnoreCase( START_ACTIVITY ) ) {

            // Vibration and Toast to show
            Vibrator v = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(300);
            Toast.makeText(getBaseContext(), "Viiiibrating!", Toast.LENGTH_LONG).show();


            String value = new String(messageEvent.getData(), StandardCharsets.UTF_8);

            // Parse message for location and distance
            String[] message = value.split(" ");
            String distance = message[message.length-2];
            int index = value.indexOf(distance);
            place = value.substring(0, index);
            String mag = message[message.length-1];

            String subtitle = distance + " Miles";

            Intent viewIntent = new Intent(this, TwitAndSensActivity.class);
            viewIntent.putExtra("Place", place);
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mNotificationManager = NotificationManagerCompat.from(this);
            int notificationID = 001;
            PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.parseColor("#FFDE00"));

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.earth_brown)
                            .setContentTitle(place)
                            .setContentText(subtitle)
                            .setColor(Color.parseColor("#593A27"))
                            .setContentIntent(viewPendingIntent)
                            .extend(new NotificationCompat.WearableExtender().setBackground(bitmap));


            mNotificationManager.notify(notificationID, notificationBuilder.build());
            Log.d("Past notify", "NOTIFICATION SHOULD BE LAUNCHED");

        } else if ( messageEvent.getPath().equalsIgnoreCase( TWITTER_ACTIVITY ) ) {
            Log.d("Received", "The message from the watch has been received");
            String description = new String(messageEvent.getData(), StandardCharsets.UTF_8);

            Intent showDesc = new Intent(this, TwitAndSensActivity.class );
            showDesc.putExtra("Description", description);
            showDesc.putExtra("Place", place);
            showDesc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            showDesc.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(showDesc);

        } else {
            super.onMessageReceived(messageEvent);
        }
    }
}
