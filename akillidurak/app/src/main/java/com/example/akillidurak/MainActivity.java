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
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private TextToSpeech textToSpeech;
    private double latitude;
    private double longitude;
    private String locationName;

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private ActivityResultLauncher<Intent> speechInputLauncher;
    private boolean locationPermissionGranted = false;
    private static final int SPEECH_TIMEOUT_MILLISECONDS = 5000;
    private Handler speechTimeoutHandler;
    private boolean speechInputReceived = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this, this);

        speechInputLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    speechInputReceived = true; // Ses girişi alındı
                    speechTimeoutHandler.removeCallbacksAndMessages(null); // Timeout işlemi iptal edilir
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


        speechTimeoutHandler = new Handler();

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

        findViewById(R.id.buttonVoiceInput).setOnClickListener(v -> getDestinationFromVoice());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            requestLocationPermission();
        }

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
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            speakWelcomeMessageAndPrompt();

        }
    }

    private void getDestinationFromVoice() {
        if (!locationPermissionGranted) {
            requestLocationPermission();
            return;
        }

        speakWelcomeMessageAndPrompt();
    }

    private void speakWelcomeMessageAndPrompt() {
        String welcomeMessage = "Hoş geldiniz. Hedef konumu söyleyin lütfen...";
        textToSpeech.setLanguage(Locale.getDefault());
        textToSpeech.speak(welcomeMessage, TextToSpeech.QUEUE_FLUSH, null, "welcome");
        listenForSpeechInput();
    }

    private void listenForSpeechInput() {
        speechTimeoutHandler.postDelayed(() -> {
            if (!speechInputReceived) {
                speakPromptAgain();
            }
        }, SPEECH_TIMEOUT_MILLISECONDS);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            try {
                speechInputLauncher.launch(intent);
            } catch (Exception e) {

            }
        }, SPEECH_TIMEOUT_MILLISECONDS);
    }
    private static final int PROMPT_DELAY_MILLISECONDS = 6000; // 6 saniye bekletme süresi

    private void speakPromptAgain() {
        new Handler().postDelayed(() -> {
        String promptAgainMessage = "Hedef konumu söylemediğiniz için tekrar soruyorum...";
        textToSpeech.setLanguage(Locale.getDefault());
        textToSpeech.speak(promptAgainMessage, TextToSpeech.QUEUE_FLUSH, null, "promptAgain");



            listenForSpeechInput();
        }, PROMPT_DELAY_MILLISECONDS);
    }


    // Hedef konumu bul
    private void findDestination(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                latitude = address.getLatitude();
                longitude = address.getLongitude();
                this.locationName = address.getAddressLine(0);
                // Diğer sayfaya yönlendir
                navigateToMapActivity();
            } else {
                String promptAgainMessage = "Geçersiz konum, lütfen başka bir konum söyleyin";
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.speak(promptAgainMessage, TextToSpeech.QUEUE_FLUSH, null, "promptAgain");
            }
        } catch (IOException e) {
            String promptAgainMessage = "Konum Dönüştürme Hatası";
            textToSpeech.setLanguage(Locale.getDefault());
            textToSpeech.speak(promptAgainMessage, TextToSpeech.QUEUE_FLUSH, null, "promptAgain");
        }
    }

    // Diğer sayfaya yönlendir
    private void navigateToMapActivity() {
        if (latitude != 0 && longitude != 0) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("locationName", locationName);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Hedef konum bulunamadı", Toast.LENGTH_SHORT).show();
        }
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


    protected void onDestroy() {
        super.onDestroy();
        // Uygulama kapatıldığında TextToSpeech nesnesini serbest bırak
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechTimeoutHandler != null) {
            speechTimeoutHandler.removeCallbacksAndMessages(null);
        }
    }
    }




