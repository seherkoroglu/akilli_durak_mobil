package com.example.akillidurak;

import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;


import android.graphics.Color;
import android.widget.Toast;
import com.google.maps.android.PolyUtil;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

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
    private void createRoute(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        String origin = startLatitude + "," + startLongitude;
        String destination = endLatitude + "," + endLongitude;
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + "AIzaSyDIlinQT4_h-6fPapG6J_Ush0fQfjfVV_Y";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray routes = response.getJSONArray("routes");
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String encodedPolyline = overviewPolyline.getString("points");

                            List<LatLng> decodedPath = PolyUtil.decode(encodedPolyline);

                            // Çizim işlemleri
                            drawRoute(decodedPath);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MapActivity.this, "Rota oluşturma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MapActivity.this, "Rota oluşturma hatası: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // RequestQueue'ye ekle
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    // Çizim işlemleri için ayrı bir metot
    private void drawRoute(List<LatLng> decodedPath) {
        PolylineOptions options = new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);

        for (int i = 0; i < decodedPath.size(); i++) {
            LatLng point = decodedPath.get(i);
            options.add(point);
        }

        mMap.addPolyline(options);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button buttonNavigate = findViewById(R.id.buttonDurak);
        buttonNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, DurakActivity.class);

            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Kullanıcının konumunu al
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Kullanıcının konumu alındı
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            // Kullanıcının konumunu haritada işaretle
                            mMap.addMarker(new MarkerOptions().position(userLocation).title("Kullanıcı Konumu"));

                            // Hedef konumu al
                            Intent intent = getIntent();
                            double destinationLatitude = intent.getDoubleExtra("latitude", 0);
                            double destinationLongitude = intent.getDoubleExtra("longitude", 0);
                            LatLng destinationLocation = new LatLng(destinationLatitude, destinationLongitude);

                            // Hedef konumu haritada işaretle
                            mMap.addMarker(new MarkerOptions().position(destinationLocation).title("Hedef Konum"));

                            // Kamera ayarlarını yaparak kullanıcının ve hedef konumun olduğu alana odaklan
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(userLocation);
                            builder.include(destinationLocation);
                            LatLngBounds bounds = builder.build();
                            int padding = 100; // Haritanın kenarları ile işaretler arasındaki boşluk
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            mMap.moveCamera(cameraUpdate);
                            createRoute(userLocation.latitude, userLocation.longitude, destinationLatitude, destinationLongitude);


                        }
                    });
        }
    }



}
