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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.akillidurak.BusStop;
import com.google.android.gms.maps.GoogleMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private TextToSpeech textToSpeech;
    private double latitude;
    private double longitude;
    private String locationName;
    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private ActivityResultLauncher<Intent> speechInputLauncher;
    private boolean locationPermissionGranted = false;
    private static final int SPEECH_TIMEOUT_MILLISECONDS = 10000;
    private Handler speechTimeoutHandler;
    private boolean speechInputReceived = false;
    private LocationManager locationManager;
    private String provider;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this, this);

        // ActivityResultLauncher başlatılıyor
        speechInputLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        List<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String destination = matches.get(0); // İlk eşleşmeyi al
                            EditText editTextDestination = findViewById(R.id.destination);
                            editTextDestination.setText(destination);

                            // Kullanıcıdan giriş alındı, döngüden çık
                            speechInputReceived = true;

                            // Kullanıcının hedef konumunu findNearestBusStop fonksiyonuna iletmek için gecikmeli çağrı yap
                            Handler handler = new Handler();
                            handler.postDelayed(() -> {
                                findNearestBusStop(destination);
                            }, 1000); // 1 saniye gecikme ekleyin (istenilen zaman aralığını ayarlayabilirsiniz)
                        }
                    }
                });

        // Haritayı otomatik açmak için EditText üzerinde değişiklik dinleyicisi ekleniyor
        EditText editTextDestination = findViewById(R.id.destination);
        editTextDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    // Kullanıcının hedef konumunu findNearestBusStop fonksiyonuna iletmek için gecikmeli çağrı yap
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        findNearestBusStop(s.toString());
                    }, 1000); // 1 saniye gecikme ekleyin (istenilen zaman aralığını ayarlayabilirsiniz)
                }
            }
        });
    }

    private void listenForSpeechInput() {
        // Döngü başlatılıyor
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            try {
                speechInputLauncher.launch(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, SPEECH_TIMEOUT_MILLISECONDS);
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

    private void findNearestBusStop(String destination) {
        // Konum izinlerini kontrol et
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Kullanıcının bulunduğu konumu al
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            Location userLocation = locationManager.getLastKnownLocation(provider);

            // Overpass API'den rotanın herhangi bir noktasındaki otobüs duraklarını al
            OverpassApiHelper.getBusStopsInKocaeli(this, new OverpassApiHelper.BusStopsListener() {
                @Override
                public void onBusStopsReceived(List<BusStop> busStops) {
                    if (busStops.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Yakınlarda otobüs durağı bulunamadı", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Kullanıcının bulunduğu konuma en yakın otobüs durağını bul
                    BusStop nearestBusStop = null;
                    float minDistance = Float.MAX_VALUE;
                    for (BusStop busStop : busStops) {
                        Location busStopLocation = new Location("");
                        busStopLocation.setLatitude(busStop.getLat());
                        busStopLocation.setLongitude(busStop.getLon());

                        // Kullanıcının mevcut konumu ile otobüs durağı arasındaki mesafeyi hesapla
                        float distance = userLocation.distanceTo(busStopLocation);

                        // Eğer bu mesafe, şu ana kadar en küçük mesafeden daha küçükse, bu otobüs durağını en yakın olarak belirle
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestBusStop = busStop;
                        }
                    }

                    if (nearestBusStop != null) {
                        // En yakın otobüs durağına Google Haritalar ile yürüyerek yönlendir
                        Uri uri = Uri.parse("google.navigation:q=" + nearestBusStop.getLat() + "," + nearestBusStop.getLon() + "&mode=w");

                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.setPackage("com.google.android.apps.maps");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);



                    } else {
                        Toast.makeText(MainActivity.this, "Yakınlarda otobüs durağı bulunamadı", Toast.LENGTH_SHORT).show();
                    }
                }


                @Override
                public void onError(String error) {
                    Log.e("FindBusStopError", "Error retrieving bus stops: " + error);
                    Toast.makeText(MainActivity.this, "Hata: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Konum izinleri iste
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onInit(int status) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);

        EditText editTextSource = findViewById(R.id.source);

        // Kullanıcının bulunduğu konumu otomatik olarak al
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (!addresses.isEmpty()) {
                        String currentLocation = addresses.get(0).getAddressLine(0);
                        editTextSource.setText(currentLocation);
                        // Konumu sesli olarak söyle
                        String welcome = "Welcome, your current location is " + currentLocation;
                        textToSpeech.setLanguage(Locale.getDefault());
                        textToSpeech.speak(welcome, TextToSpeech.QUEUE_FLUSH, null, "welcome");
                        // Hedef konumu sormak için mesajı sesli olarak söyle
                        String prompt = "Please tell me your destination";
                        textToSpeech.speak(prompt, TextToSpeech.QUEUE_ADD, null, "prompt");
                        // Konuşma girişini beklemek için dinleme işlemini başlat
                        listenForSpeechInput();


                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        } else {
            // Konum izni verilmediyse, izin iste
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
}