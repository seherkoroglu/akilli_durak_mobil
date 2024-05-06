package com.example.akillidurak;
import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SPEECH_INPUT = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private ActivityResultLauncher<Intent> speechInputLauncher;
    private boolean locationPermissionGranted = false;
    private TextToSpeech tts; // TextToSpeech nesnesini sınıf düzeyinde tanımlıyoruz

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechInputLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK && data != null) {
                        ArrayList<String> speechResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speechResults != null && !speechResults.isEmpty()) {
                            String input = speechResults.get(0);
                            if (locationPermissionGranted) {
                                findDestination(input);
                            } else {
                                requestLocationPermission();
                            }
                        }
                    }
                });

        findViewById(R.id.buttonVoiceInput).setOnClickListener(v -> startSpeechToText());

        // Konum izni kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            requestLocationPermission();
        }

        // TextToSpeech nesnesini başlat
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Başarılı durumda bir şeyler yapabilirsiniz
            } else {
                // Hata durumunda kullanıcıya bilgi ver
                Toast.makeText(this, "Sesli yönlendirme başlatılamadı", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonNavigate = findViewById(R.id.buttonNavigate);
        buttonNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);

            // Intent'i başlatın
            startActivity(intent);

        });
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            } else {
                Toast.makeText(this, "Konum izni reddedildi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Mikrofon simgesine tıklandığında sesli giriş işlemini başlat
    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Konumunuzu söyleyin...");

        try {
            speechInputLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cihazınız sesli girişi desteklemiyor", Toast.LENGTH_SHORT).show();
        }
    }

    // Kullanıcının girdiği konumu haritada bul ve yönlendirme yap
    private void findDestination(String input) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(input, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                double destinationLatitude = address.getLatitude();
                double destinationLongitude = address.getLongitude();

                LatLng destination = new LatLng(destinationLatitude, destinationLongitude);

                LatLng nearestStation = findNearestStation(destination);
                if (nearestStation != null) {
                    String directionText = getDirectionText(destination, nearestStation);
                    speakDirection(directionText);
                }
            } else {
                Toast.makeText(this, "Belirtilen konum bulunamadı", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void speakDirection(String directionText) {
        if (tts != null) {
            tts.speak(directionText, TextToSpeech.QUEUE_FLUSH, null, null);
            TextView textViewDirections = findViewById(R.id.textViewDirections);
            textViewDirections.setText(directionText);
        } else {
            Toast.makeText(this, "Sesli yönlendirme motoru başlatılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private String getDirectionText(LatLng destination, LatLng nearestStation) {
        double destLat = destination.latitude;
        double destLng = destination.longitude;
        double stationLat = nearestStation.latitude;
        double stationLng = nearestStation.longitude;

        String directionText = "Hedefe yönlendiriliyorsunuz.";
        directionText += " En yakın durak koordinatları: (" + stationLat + ", " + stationLng + ")";

        return directionText;
    }

    private LatLng findNearestStation(LatLng destination) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location userLocation = locationManager.getLastKnownLocation(provider);

            if (userLocation != null) {
                LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                double minDistance = Double.MAX_VALUE;
                LatLng nearestStation = null;

                List<LatLng> stationList = new ArrayList<>();

                for (LatLng stationLocation : stationList) {
                    double distance = distanceBetween(userLatLng, stationLocation);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestStation = stationLocation;
                    }
                }

                return nearestStation;
            }
        } else {
            Toast.makeText(this, "Konum izni verilmedi", Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    private double distanceBetween(LatLng point1, LatLng point2) {
        Location location1 = new Location("");
        location1.setLatitude(point1.latitude);
        location1.setLongitude(point1.longitude);

        Location location2 = new Location("");
        location2.setLatitude(point2.latitude);
        location2.setLongitude(point2.longitude);

        return location1.distanceTo(location2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TextToSpeech nesnesinin kaynaklarını serbest bırak
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

}
