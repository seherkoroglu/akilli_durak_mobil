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
    private double latitude;
    private double longitude;
    private String locationName;
    private static final int REQUEST_SPEECH_INPUT = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private ActivityResultLauncher<Intent> speechInputLauncher;
    private boolean locationPermissionGranted = false;
    private TextToSpeech tts;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            requestLocationPermission();
        }

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // TextToSpeech başlatıldı
            } else {
                Toast.makeText(this, "Sesli yönlendirme başlatılamadı", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonNavigate = findViewById(R.id.buttonNavigate);
        buttonNavigate.setOnClickListener(v -> {
            if (latitude != 0 && longitude != 0) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("locationName", locationName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Hedef konum bulunamadı", Toast.LENGTH_SHORT).show();
            }
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

    private void speakDirection(String directionText) {
        if (tts != null) {
            tts.speak(directionText, TextToSpeech.QUEUE_FLUSH, null, null);

            TextView textViewDirections = findViewById(R.id.textViewDirections);
            if (textViewDirections != null) {
                textViewDirections.setText(directionText);
            } else {
                Toast.makeText(this, "TextView bulunamadı", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Sesli yönlendirme motoru başlatılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void findDestination(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                latitude = address.getLatitude();
                longitude = address.getLongitude();
                this.locationName = address.getAddressLine(0);
                String destinationText = "Hedef konum: " + this.locationName;
                TextView textViewDestination = findViewById(R.id.textViewDirections);
                if (textViewDestination != null) {
                    textViewDestination.setText(destinationText);
                    speakDirection(destinationText);
                } else {
                    Toast.makeText(this, "TextView bulunamadı", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Konum bulunamadı", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Konum dönüştürme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
