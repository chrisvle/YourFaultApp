package me.chrisvle.yourfaultapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private int lat;
    private int lon;
    private LatLng place;
    private BitmapDescriptor marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the name, distance, and magnitude to display on the map
        Intent i = getIntent();
        lat = (int) Double.parseDouble(i.getStringExtra("Latitude"));
        lon = (int) Double.parseDouble(i.getStringExtra("Longitude"));
        String title = i.getStringExtra("Place");
        String distance = i.getStringExtra("Distance");
        String mag = i.getStringExtra("Mag");
        place = new LatLng(lat, lon);
        setContentView(R.layout.activity_google_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        TextView l = (TextView) findViewById(R.id.location);
        l.setText(title);

        TextView d = (TextView) findViewById(R.id.distance);
        d.setText("Distance: " + distance + " M    " + "Magnitude: " + mag);

        double size = Double.parseDouble(mag);
        if (size < 1.0) {
            marker = BitmapDescriptorFactory.fromResource(R.drawable.low);
        } else if (size < 2.0) {
            marker = BitmapDescriptorFactory.fromResource(R.drawable.low_med);
        } else {
            marker = BitmapDescriptorFactory.fromResource(R.drawable.low_big);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.addMarker(new MarkerOptions().position(place).icon(marker));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(place));
    }
}
