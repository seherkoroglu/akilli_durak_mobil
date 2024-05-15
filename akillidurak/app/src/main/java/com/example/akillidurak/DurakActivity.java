package com.example.akillidurak;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class DurakActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_durak);



        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Hedef konumu işaretle
        LatLng destinationLatLng = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

        // Haritayı hedef konuma odakla
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 15));
    }
    private LatLng findNearestStop(double latitude, double longitude) {
        // Burada en yakın durağı bulmak için gerekli hesaplamaları yapabilirsiniz
        // Örneğin, sabit bir durağın koordinatlarını kullanabiliriz
        return new LatLng(40.7128, -74.0060); // Örnek bir durağın koordinatları
    }

}
