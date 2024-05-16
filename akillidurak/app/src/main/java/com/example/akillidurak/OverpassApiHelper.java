package com.example.akillidurak;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.akillidurak.BusStop;
import com.example.akillidurak.OverpassResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OverpassApiHelper {
    private static final String TAG = "OverpassApiHelper";

    public interface BusStopsListener {
        void onBusStopsReceived(List<BusStop> busStops);
        void onError(String error);
    }

    public static void getBusStopsInKocaeli(Context context, BusStopsListener listener) {
        double minLat = 40.6165;
        double minLon = 29.9224;
        double maxLat = 41.1935;
        double maxLon = 30.2121;

        String overpassUrl = String.format(
                "https://overpass-api.de/api/interpreter?data=[out:json];node[\"highway\"=\"bus_stop\"](40.6165,29.9224,41.1935,30.2121);out body;",
                minLat, minLon, maxLat, maxLon);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(overpassUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error fetching bus stops", e);
                listener.onError("Error fetching bus stops: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onError("Unexpected response: " + response);
                    return;
                }

                String responseBody = response.body().string();
                Gson gson = new GsonBuilder().create();
                OverpassResponse overpassResponse = gson.fromJson(responseBody, OverpassResponse.class);

                if (overpassResponse != null && overpassResponse.getElements() != null) {
                    listener.onBusStopsReceived(overpassResponse.getElements());
                } else {
                    listener.onError("No bus stops found in Kocaeli");
                }
            }
        });
    }
}

