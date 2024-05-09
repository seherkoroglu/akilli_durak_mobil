package com.example.akillidurak;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private LatLng userLocation;
    private LatLng destinationLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button buttonNavigate = findViewById(R.id.buttonDurak);
        buttonNavigate.setOnClickListener(v -> {
            // Kullanıcının ve hedef konumunun alındığından emin ol
            if (userLocation != null && destinationLocation != null) {
                Uri uri = Uri.parse("https://www.google.com/maps/dir/" + userLocation.latitude + "," + userLocation.longitude + "/" + destinationLocation.latitude + "," + destinationLocation.longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage("com.google.android.apps.maps");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(MapActivity.this, "Konumlar henüz belirlenmedi.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Kullanıcının konumunu almak için izin iste
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Kullanıcının konumunu al
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Kullanıcının konumu alındı
                        userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(userLocation).title("Kullanıcı Konumu"));

                        // Hedef konumu al
                        Intent intent = getIntent();
                        double destinationLatitude = intent.getDoubleExtra("latitude", 0);
                        double destinationLongitude = intent.getDoubleExtra("longitude", 0);
                        destinationLocation = new LatLng(destinationLatitude, destinationLongitude);
                        mMap.addMarker(new MarkerOptions().position(destinationLocation).title("Hedef Konum"));

                        // Kamera ayarlarını yaparak kullanıcının ve hedef konumun olduğu alana odaklan
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(userLocation);
                        builder.include(destinationLocation);
                        LatLngBounds bounds = builder.build();
                        int padding = 100; // Haritanın kenarları ile işaretler arasındaki boşluk
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        mMap.moveCamera(cameraUpdate);
                    }
                });
    }

}
