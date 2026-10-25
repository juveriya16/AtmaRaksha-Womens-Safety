package com.sum.ladybuddy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NearbySafetyHelper {

    private static final String TAG = "NearbySafetyHelper";
    private static final String USER_AGENT = "AtmaRaksha-WomenSafetyApp/6.0 (Android; OpenStreetMap Overpass API)";

    // Primary and mirror Overpass servers for high availability
    private static final String[] OVERPASS_ENDPOINTS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
    };

    public static class SafetyPlace {
        public String id;
        public String name;
        public String category; // POLICE, HOSPITAL, PHARMACY
        public String address;
        public int distanceMeters;
        public double latitude;
        public double longitude;
        public String phoneNumber;
        public boolean isOpenNow;
        public String badgeLabel;

        public String getFormattedDistance() {
            if (distanceMeters < 1000) {
                return distanceMeters + " m";
            } else {
                return String.format(Locale.US, "%.1f km", distanceMeters / 1000.0);
            }
        }
    }

    public interface PlacesCallback {
        void onPlacesLoaded(List<SafetyPlace> places);
        void onError(String errorMessage);
    }

    public static void fetchNearbySafetyServices(Context context, double userLat, double userLng,
                                                 int radiusMeters, String categoryFilter,
                                                 PlacesCallback callback) {
        new Thread(() -> {
            List<SafetyPlace> results = null;
            String errorMsg = null;

            // Query OpenStreetMap data via Overpass API (no paid API key required)
            for (String endpoint : OVERPASS_ENDPOINTS) {
                try {
                    results = queryOverpassApi(endpoint, userLat, userLng, radiusMeters, categoryFilter);
                    if (results != null) {
                        break; // Success
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Overpass query to " + endpoint + " failed: " + e.getMessage());
                    errorMsg = e.getMessage();
                }
            }

            if (results == null) {
                results = new ArrayList<>();
            }

            // Sort by distance ascending
            Collections.sort(results, new Comparator<SafetyPlace>() {
                @Override
                public int compare(SafetyPlace o1, SafetyPlace o2) {
                    return Integer.compare(o1.distanceMeters, o2.distanceMeters);
                }
            });

            List<SafetyPlace> finalResults = results;
            String finalError = errorMsg;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!finalResults.isEmpty()) {
                    callback.onPlacesLoaded(finalResults);
                } else if (finalError != null) {
                    callback.onError("OpenStreetMap safety query failed (" + finalError + "). You can call emergency helplines directly.");
                } else {
                    // Empty results within radius
                    callback.onPlacesLoaded(finalResults);
                }
            });
        }).start();
    }

    private static List<SafetyPlace> queryOverpassApi(String endpoint, double lat, double lng, int radius, String category) throws Exception {
        double radiusDegrees = radius / 111000.0;
        double south = lat - radiusDegrees;
        double west = lng - radiusDegrees;
        double north = lat + radiusDegrees;
        double east = lng + radiusDegrees;

        StringBuilder subQueries = new StringBuilder();

        if ("ALL".equalsIgnoreCase(category) || category == null || category.isEmpty()) {
            subQueries.append("node[\"amenity\"~\"police|hospital|pharmacy|clinic\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
            subQueries.append("way[\"amenity\"~\"police|hospital|pharmacy|clinic\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
        } else if ("POLICE".equalsIgnoreCase(category)) {
            subQueries.append("node[\"amenity\"=\"police\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
            subQueries.append("way[\"amenity\"=\"police\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
        } else if ("HOSPITAL".equalsIgnoreCase(category)) {
            subQueries.append("node[\"amenity\"~\"hospital|clinic\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
            subQueries.append("way[\"amenity\"~\"hospital|clinic\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
        } else if ("PHARMACY".equalsIgnoreCase(category)) {
            subQueries.append("node[\"amenity\"=\"pharmacy\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
            subQueries.append("node[\"healthcare\"=\"pharmacy\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
            subQueries.append("way[\"amenity\"=\"pharmacy\"](").append(south).append(",").append(west).append(",").append(north).append(",").append(east).append(");");
        }

        // Output JSON with timeout and center coordinates for ways
        String overpassQuery = "[out:json][timeout:15];(" + subQueries.toString() + ");out center 40;";
        String urlStr = endpoint + "?data=" + URLEncoder.encode(overpassQuery, "UTF-8");

        String json = executeHttpGet(urlStr);
        JSONObject root = new JSONObject(json);
        JSONArray elements = root.optJSONArray("elements");

        List<SafetyPlace> list = new ArrayList<>();
        if (elements != null) {
            for (int i = 0; i < elements.length(); i++) {
                JSONObject elem = elements.getJSONObject(i);
                double pLat = elem.optDouble("lat", Double.NaN);
                double pLng = elem.optDouble("lon", Double.NaN);

                // If element is a way, coordinates are inside the "center" object
                if (Double.isNaN(pLat) || Double.isNaN(pLng)) {
                    JSONObject center = elem.optJSONObject("center");
                    if (center != null) {
                        pLat = center.optDouble("lat", lat);
                        pLng = center.optDouble("lon", lng);
                    } else {
                        continue;
                    }
                }

                JSONObject tags = elem.optJSONObject("tags");
                if (tags == null) tags = new JSONObject();

                String amenity = tags.optString("amenity", "").toLowerCase();
                String healthcare = tags.optString("healthcare", "").toLowerCase();
                String name = tags.optString("name", "");

                SafetyPlace place = new SafetyPlace();
                place.id = String.valueOf(elem.optLong("id", i));
                place.latitude = pLat;
                place.longitude = pLng;
                place.distanceMeters = calculateDistanceMeters(lat, lng, pLat, pLng);

                // Determine category & display labels
                if ("police".equals(amenity)) {
                    place.category = "POLICE";
                    place.badgeLabel = "Police Station";
                    place.phoneNumber = tags.optString("phone", tags.optString("contact:phone", "100"));
                    if (name.isEmpty()) name = "Local Police Station / Outpost";
                } else if ("hospital".equals(amenity) || "clinic".equals(amenity) || "hospital".equals(healthcare)) {
                    place.category = "HOSPITAL";
                    place.badgeLabel = "hospital".equals(amenity) ? "Hospital / Trauma Center" : "Medical Clinic";
                    place.phoneNumber = tags.optString("phone", tags.optString("contact:phone", "108"));
                    if (name.isEmpty()) name = "Community Hospital & Trauma Care";
                } else {
                    place.category = "PHARMACY";
                    place.badgeLabel = "24/7 Pharmacy Safe Haven";
                    place.phoneNumber = tags.optString("phone", tags.optString("contact:phone", "112"));
                    if (name.isEmpty()) name = "Medical Pharmacy Safe Haven";
                }

                place.name = name;

                // Extract address information
                String houseNo = tags.optString("addr:housenumber", "");
                String street = tags.optString("addr:street", "");
                String city = tags.optString("addr:city", "");
                StringBuilder addrBuilder = new StringBuilder();
                if (!houseNo.isEmpty()) addrBuilder.append(houseNo).append(" ");
                if (!street.isEmpty()) addrBuilder.append(street);
                if (!city.isEmpty()) {
                    if (addrBuilder.length() > 0) addrBuilder.append(", ");
                    addrBuilder.append(city);
                }

                if (addrBuilder.length() > 0) {
                    place.address = addrBuilder.toString();
                } else {
                    place.address = "Near " + place.name + " (" + place.getFormattedDistance() + " away)";
                }

                String opening = tags.optString("opening_hours", "");
                place.isOpenNow = opening.isEmpty() || opening.contains("24/7") || !opening.contains("off");

                list.add(place);
            }
        }

        return list;
    }

    private static int calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return Math.round(results[0]);
    }

    private static String executeHttpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
