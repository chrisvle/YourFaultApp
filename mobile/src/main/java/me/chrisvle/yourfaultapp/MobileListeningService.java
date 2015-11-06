package me.chrisvle.yourfaultapp;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

/**
 * Created by Chris on 10/13/15.
 */
public class MobileListeningService extends WearableListenerService {
    private static final String SHAKE_ACTIVITY = "/shake";
    private static final String EXIT_ACTIVITY = "/exit";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("Received", "MMMMMMMMMM");
        if( messageEvent.getPath().equalsIgnoreCase( SHAKE_ACTIVITY ) ) {
            Log.d("Received", "The message from the watch has been received");
            String place = new String(messageEvent.getData(), StandardCharsets.UTF_8);

            Intent getImages = new Intent(this, TwitterActivity.class );
            getImages.putExtra("Place", place);
            getImages.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getImages.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(getImages);
        }
        else if (messageEvent.getPath().equalsIgnoreCase(EXIT_ACTIVITY)) {
            Log.d("Received", "The message to EXIT");
            Intent list = new Intent(this, MobileMain.class );
            list.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            list.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(list);
        }

    }
}
