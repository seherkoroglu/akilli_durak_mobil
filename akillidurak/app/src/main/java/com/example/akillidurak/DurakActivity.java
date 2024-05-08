package com.example.akillidurak;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

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

        // En yakın durağı bul ve zoom yap
        LatLng nearestStop = findNearestStop(latitude, longitude);
        if (nearestStop != null) {
            float zoomLevel = 16.0f; // Or desired zoom level
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(nearestStop, zoomLevel);
            mMap.moveCamera(cameraUpdate);

            // Sesli bir şekilde yol tarifi yap
            String directions = "En yakın durağa yönlendiriliyorsunuz.";
            // Sesli yönlendirme kodu buraya gelecek
            speakDirections(directions);
        }
    }

    private LatLng findNearestStop(double latitude, double longitude) {
        // En yakın durağı bulmak için gerekli hesaplamalar

        return new LatLng(40.7128, -74.0060); // Örnek bir durağın koordinatları
    }

    private void speakDirections(String directions) {
        // Sesli yönlendirme için gerekli kod buraya gelecek
    }
}
