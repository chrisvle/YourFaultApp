package me.chrisvle.yourfaultapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.AppSession;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.MediaEntity;
import com.twitter.sdk.android.core.models.Search;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.SearchService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Stack;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Chris on 10/13/15.
 */
public class TwitterActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TWITTER_KEY = "uhyUdPmce4k26RkabHGDPjaZ6";
    private static final String TWITTER_SECRET = "evr3aRYEUInfq5DyXK9POL2kTIUPpwCvKj8as1DFax2dgY91Sd";

    private static Stack<String> images = new Stack<>();
    private static Stack<String> descriptions = new Stack<>();

    private static String prevPlace;
    private String place;
    private static boolean populated;

    private static final String TWITTER_ACTIVITY = "/Twitter";

    private static GoogleApiClient mApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Twitter", "I made it to the twitter");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();

        Intent intent = getIntent();
        place = intent.getStringExtra("Place");
        if (place != null) {
            Log.d("place", place);
        }

        if (prevPlace == null || !place.equalsIgnoreCase(prevPlace)) {
            getImage(place);
            prevPlace = place;
            populated = false;
        }

        Log.d("size of stack", Integer.toString(images.size()));

        ImageView i = (ImageView) findViewById(R.id.imageView);
        String url = "";
        if (populated) {
            url = images.pop();
            i.setImageBitmap(getBitmapFromURL(url + ":large"));
            i.setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }

    public void getImage(String query) {
        Log.d("inside Twitter", "Inside get image");
        String[] loc = query.split(" ");
        final String place = loc[loc.length-1];
        TwitterCore.getInstance().logInGuest(new Callback<AppSession>() {
            @Override
            public void success(Result<AppSession> appSessionResult) {

                AppSession session = appSessionResult.data;

                TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient(session);
                SearchService searchService = twitterApiClient.getSearchService();
                searchService.tweets(place, null, null, null, null, 100, null, null, null, null, new Callback<Search>() {
                    @Override
                    public void success(Result<Search> result) {
                        Log.d("Successful search", "Got inside the search service");
                        extractFromTweet(result);
                    }

                    @Override
                    public void failure(TwitterException e) {
                        Log.d("FAILED", "failed to connect");
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(getBaseContext(), "Could not get guest Twitter session", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void extractFromTweet(Result<Search> result) {
        populated = true;
        List<Tweet> tweets = result.data.tweets;
        for (Tweet t : tweets) {
            List<MediaEntity> medias = t.entities.media;
            if (medias != null) {
                for (MediaEntity m : medias) {
                    String mUrl = m.mediaUrl;
                    if (!mUrl.equals("")) {
                        Log.d("Image", mUrl);
                        images.push(mUrl);
                        Log.d("TWEET TEXT", t.text);
                        descriptions.push(t.text);
                    }
                }
            }
        }
        updateUI();
    }

    private void updateUI () {
        sendMessage(TWITTER_ACTIVITY, descriptions.pop());
        String url = images.pop() + ":large";
        Log.d("What is my url?", url);
        ImageView i = (ImageView) findViewById(R.id.imageView);
        i.setImageBitmap(getBitmapFromURL(url));
        i.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    // Stackoverflow
    public static Bitmap getBitmapFromURL(String src) {
        Bitmap bitMap = null;
        try {
            URL url = new URL(src);
            InputStream is = url.openConnection().getInputStream();
            bitMap = BitmapFactory.decodeStream(is);
            Log.d("Bitmap", "Successful host and bitmap");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("return", bitMap.toString());
        return bitMap;
    }

    public static void sendMessage( final String path, final String text) {
        Log.d("SENDING MOBILE", "Sending a message from TWITTER");
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    Log.d("Node", "Found Node inside mobile MAIN");
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
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
    public void onDestroy() {
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
