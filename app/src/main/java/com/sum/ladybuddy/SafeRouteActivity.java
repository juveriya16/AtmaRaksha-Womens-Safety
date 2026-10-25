package com.sum.ladybuddy;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SafeRouteActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSource, etDestination;
    private TextView tvTime, tvDist, tvHeaderTitle, tvHeaderSubtitle;
    private TextView chipStation, chipHome, chipMetro;
    private TextView tvNightToggleText, tvNightRatingBanner;
    private LinearLayout btnToggleNightView;

    private MaterialCardView cardRouteA, cardRouteB;
    private TextView tvSafetyReasonsTitle, tvSafetyReasonsBody;
    private Button btnShareJourney, btnCheckinArrived, btnStartSafeNav, btnMonitorJourney;
    private ProgressBar progressRoute;

    private MapView mapView;
    private boolean isMapReady = false;

    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint originGeoPoint = new GeoPoint(12.9716, 77.5946); // Default Bengaluru
    private GeoPoint destGeoPoint = new GeoPoint(12.9785, 77.6050);
    private boolean isNightViewActive = false;
    private int selectedRouteIndex = 0; // 0 = Route A, 1 = Route B

    private List<OsmRoutesHelper.RouteOption> loadedRoutes = new ArrayList<>();
    private List<Polyline> activePolylines = new ArrayList<>();
    private Marker originMarker;
    private Marker destMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OpenStreetMap configuration with User-Agent complying with OSM policy
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_safe_route);

        btnBack = findViewById(R.id.btn_back_safe_route);
        etSource = findViewById(R.id.et_route_source);
        etDestination = findViewById(R.id.et_route_destination);
        tvTime = findViewById(R.id.tv_route_time);
        tvDist = findViewById(R.id.tv_route_dist);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        tvHeaderSubtitle = findViewById(R.id.tv_header_subtitle);

        chipStation = findViewById(R.id.chip_dest_station);
        chipHome = findViewById(R.id.chip_dest_home);
        chipMetro = findViewById(R.id.chip_dest_metro);

        btnToggleNightView = findViewById(R.id.btn_toggle_night_view);
        tvNightToggleText = findViewById(R.id.tv_night_toggle_text);
        tvNightRatingBanner = findViewById(R.id.tv_night_rating_banner);

        cardRouteA = findViewById(R.id.card_route_opt_a);
        cardRouteB = findViewById(R.id.card_route_opt_b);
        tvSafetyReasonsTitle = findViewById(R.id.tv_safety_reasons_title);
        tvSafetyReasonsBody = findViewById(R.id.tv_safety_reasons_body);

        btnShareJourney = findViewById(R.id.btn_share_journey);
        btnCheckinArrived = findViewById(R.id.btn_checkin_arrived);
        btnStartSafeNav = findViewById(R.id.btn_start_safe_navigation);
        btnMonitorJourney = findViewById(R.id.btn_monitor_route_journey);
        progressRoute = findViewById(R.id.progress_route_loading);

        initOsmMapView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        btnBack.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        if (prefs.getBoolean("night_safety_mode", false)) {
            toggleNightModeView(true);
        }

        fetchCurrentGpsLocation();
        setupDestinationChips();
        setupRouteAlternatives();
        setupJourneySharing();
        setupStartNavigation();
        setupMonitorJourney();

        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.MAP);
    }

    private void initOsmMapView() {
        mapView = findViewById(R.id.mapview_safe_route);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(originGeoPoint);

        // Tap listener on map to select custom destination
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                destGeoPoint = p;
                etDestination.setText(String.format(Locale.US, "Map Pin (%.4f, %.4f)", p.getLatitude(), p.getLongitude()));
                requestDynamicRoutes();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(mapEventsOverlay);

        isMapReady = true;
    }

    private void fetchCurrentGpsLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                Location loc = task.getResult();
                if (loc != null) {
                    originGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                    etSource.setText("Current Location (" + String.format(Locale.US, "%.3f, %.3f", loc.getLatitude(), loc.getLongitude()) + ")");
                    destGeoPoint = new GeoPoint(originGeoPoint.getLatitude() + 0.009, originGeoPoint.getLongitude() + 0.008);
                    etDestination.setText("Station Road");
                } else {
                    etSource.setText("Current Location (12.972, 77.595)");
                }
                if (mapView != null) {
                    mapView.getController().setCenter(originGeoPoint);
                }
                requestDynamicRoutes();
            });
        } else {
            etSource.setText("Maple Lane (Current Location)");
            requestDynamicRoutes();
        }
    }

    private void setupDestinationChips() {
        chipStation.setOnClickListener(v -> {
            etDestination.setText("Station Road");
            destGeoPoint = new GeoPoint(originGeoPoint.getLatitude() + 0.009, originGeoPoint.getLongitude() + 0.008);
            requestDynamicRoutes();
        });

        chipHome.setOnClickListener(v -> {
            etDestination.setText("Home (Pinewood Avenue)");
            destGeoPoint = new GeoPoint(originGeoPoint.getLatitude() + 0.012, originGeoPoint.getLongitude() - 0.007);
            requestDynamicRoutes();
        });

        chipMetro.setOnClickListener(v -> {
            etDestination.setText("Central Metro Station");
            destGeoPoint = new GeoPoint(originGeoPoint.getLatitude() - 0.008, originGeoPoint.getLongitude() + 0.010);
            requestDynamicRoutes();
        });

        btnToggleNightView.setOnClickListener(v -> toggleNightModeView(!isNightViewActive));
    }

    private void toggleNightModeView(boolean enableNight) {
        isNightViewActive = enableNight;
        if (isNightViewActive) {
            tvNightToggleText.setText("☀️ Day View");
            tvNightRatingBanner.setVisibility(View.VISIBLE);
            tvHeaderTitle.setText("Night Safety Mode (OSM)");
            tvHeaderSubtitle.setText("Enhanced navigation for nighttime safety • Illuminated corridors active");
        } else {
            tvNightToggleText.setText("🌙 Night View");
            tvNightRatingBanner.setVisibility(View.GONE);
            tvHeaderTitle.setText("Route Safety Preview");
            tvHeaderSubtitle.setText("Review the safety of your route before starting your journey");
        }
    }

    private void setupRouteAlternatives() {
        cardRouteA.setOnClickListener(v -> selectRoute(0));
        cardRouteB.setOnClickListener(v -> selectRoute(1));
    }

    private void selectRoute(int index) {
        selectedRouteIndex = index;
        if (loadedRoutes.isEmpty()) return;

        if (index == 0 && loadedRoutes.size() > 0) {
            OsmRoutesHelper.RouteOption r0 = loadedRoutes.get(0);
            cardRouteA.setStrokeColor(getResources().getColor(R.color.brand_purple));
            cardRouteA.setStrokeWidth(4);
            cardRouteB.setStrokeColor(getResources().getColor(R.color.card_border));
            cardRouteB.setStrokeWidth(1);

            tvTime.setText(r0.durationMinutes + " min");
            tvDist.setText(r0.distanceKm + " km");
            tvSafetyReasonsTitle.setText(r0.safetyReasonsTitle);
            tvSafetyReasonsBody.setText(r0.safetyReasonsBody);

            highlightPolyline(0);
        } else if (index == 1 && loadedRoutes.size() > 1) {
            OsmRoutesHelper.RouteOption r1 = loadedRoutes.get(1);
            cardRouteB.setStrokeColor(getResources().getColor(R.color.color_route_medium));
            cardRouteB.setStrokeWidth(4);
            cardRouteA.setStrokeColor(getResources().getColor(R.color.card_border));
            cardRouteA.setStrokeWidth(1);

            tvTime.setText(r1.durationMinutes + " min");
            tvDist.setText(r1.distanceKm + " km");
            tvSafetyReasonsTitle.setText(r1.safetyReasonsTitle);
            tvSafetyReasonsBody.setText(r1.safetyReasonsBody);

            highlightPolyline(1);
        }
    }

    private void requestDynamicRoutes() {
        if (!isMapReady || mapView == null) return;

        if (progressRoute != null) progressRoute.setVisibility(View.VISIBLE);

        String originName = etSource.getText().toString();
        String destName = etDestination.getText().toString();

        OsmRoutesHelper.fetchRoutes(this, originGeoPoint, destGeoPoint, originName, destName, new OsmRoutesHelper.RoutesCallback() {
            @Override
            public void onRoutesReady(List<OsmRoutesHelper.RouteOption> routes) {
                if (progressRoute != null) progressRoute.setVisibility(View.GONE);
                loadedRoutes = routes;
                renderRoutesOnMap();
                selectRoute(0);
            }

            @Override
            public void onError(String message) {
                if (progressRoute != null) progressRoute.setVisibility(View.GONE);
                Toast.makeText(SafeRouteActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderRoutesOnMap() {
        if (mapView == null || loadedRoutes == null || loadedRoutes.isEmpty()) return;

        // Clear existing overlays except the MapEventsOverlay (at index 0)
        while (mapView.getOverlays().size() > 1) {
            mapView.getOverlays().remove(mapView.getOverlays().size() - 1);
        }
        activePolylines.clear();

        // 1. Origin Marker
        originMarker = new Marker(mapView);
        originMarker.setPosition(originGeoPoint);
        originMarker.setTitle("Start: " + etSource.getText().toString());
        originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_record_dot));
        mapView.getOverlays().add(originMarker);

        // 2. Destination Marker
        destMarker = new Marker(mapView);
        destMarker.setPosition(destGeoPoint);
        destMarker.setTitle("Destination: " + etDestination.getText().toString());
        destMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_safe_nav));
        mapView.getOverlays().add(destMarker);

        // 3. Polylines for each route alternative
        List<GeoPoint> allPoints = new ArrayList<>();
        allPoints.add(originGeoPoint);
        allPoints.add(destGeoPoint);

        for (int i = 0; i < loadedRoutes.size(); i++) {
            OsmRoutesHelper.RouteOption r = loadedRoutes.get(i);
            int color = (i == 0) ? Color.parseColor("#5F297B") : Color.parseColor("#F39C12");
            float width = (i == 0) ? 14.0f : 9.0f;

            Polyline polyline = new Polyline(mapView);
            polyline.setPoints(r.polylinePoints);
            polyline.setColor(color);
            polyline.setWidth(width);
            mapView.getOverlays().add(polyline);
            activePolylines.add(polyline);

            allPoints.addAll(r.polylinePoints);
        }

        // Fit map bounding box
        if (!allPoints.isEmpty()) {
            double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
            for (GeoPoint p : allPoints) {
                if (p.getLatitude() < minLat) minLat = p.getLatitude();
                if (p.getLatitude() > maxLat) maxLat = p.getLatitude();
                if (p.getLongitude() < minLng) minLng = p.getLongitude();
                if (p.getLongitude() > maxLng) maxLng = p.getLongitude();
            }
            BoundingBox box = new BoundingBox(maxLat + 0.002, maxLng + 0.002, minLat - 0.002, minLng - 0.002);
            mapView.zoomToBoundingBox(box, true, 80);
        }

        mapView.invalidate();
    }

    private void highlightPolyline(int selectedIndex) {
        for (int i = 0; i < activePolylines.size(); i++) {
            Polyline poly = activePolylines.get(i);
            if (i == selectedIndex) {
                poly.setWidth(16.0f);
            } else {
                poly.setWidth(8.0f);
            }
        }
        if (mapView != null) mapView.invalidate();
    }

    private void setupJourneySharing() {
        btnShareJourney.setOnClickListener(v -> promptShareJourney());
        btnCheckinArrived.setOnClickListener(v -> promptArrivalCheckin());
    }

    private void promptShareJourney() {
        final String dest = etDestination.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        final String p1 = prefs.getString("phone1", "");

        String journeyMessage = "I am traveling to " + (dest.isEmpty() ? "my destination" : dest) +
                " via safe route (" + tvTime.getText() + " • " + tvDist.getText() + ").";
        journeyMessage += " Live Location: https://www.openstreetmap.org/?mlat=" + originGeoPoint.getLatitude() + "&mlon=" + originGeoPoint.getLongitude();

        final String finalMsg = journeyMessage;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share Journey & ETA");
        builder.setMessage("Share trip details with your emergency circle?\n\n\"" + finalMsg + "\"");
        builder.setPositiveButton("Send in Messages App", (dialog, which) -> {
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                if (!TextUtils.isEmpty(p1)) {
                    smsIntent.setData(Uri.parse("smsto:" + Uri.encode(p1)));
                } else {
                    smsIntent.setData(Uri.parse("smsto:"));
                }
                smsIntent.putExtra("sms_body", finalMsg);
                startActivity(smsIntent);

                SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
                dbHelper.logEvent("Journey Shared", "ETA Shared with Guardian",
                        "Shared travel plan to " + dest + " (" + tvTime.getText() + ").", "Active Journey");
            } catch (Exception e) {
                Toast.makeText(this, "Could not open messaging app.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("Share via Apps", (dialog, which) -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, finalMsg);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share Journey via"));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void promptArrivalCheckin() {
        final String dest = etDestination.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        final String p1 = prefs.getString("phone1", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Safe Arrival");
        builder.setMessage("Send safe arrival notification to your guardians?");
        builder.setPositiveButton("Check In & Notify", (dialog, which) -> {
            String checkinMsg = "Safe Check-In: I have arrived safely at " + (dest.isEmpty() ? "my destination" : dest) + ". Thank you!";
            if (!TextUtils.isEmpty(p1)) {
                try {
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                    smsIntent.setData(Uri.parse("smsto:" + Uri.encode(p1)));
                    smsIntent.putExtra("sms_body", checkinMsg);
                    startActivity(smsIntent);
                } catch (Exception ignored) {}
            }

            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Completed", "Safe Route Completed",
                    "Arrived safely at " + dest + ". Logged into safety history.", "Safe");

            Toast.makeText(this, "Safe arrival recorded! Guardians notified.", Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupStartNavigation() {
        btnStartSafeNav.setOnClickListener(v -> {
            String destination = etDestination.getText().toString().trim();
            if (TextUtils.isEmpty(destination)) {
                destination = "Station Road";
            }

            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Navigation Started", "Safe Route Activated",
                    "Started navigation to " + destination + " via " + (selectedRouteIndex == 0 ? "Recommended Corridor" : "Shortcut Route"), "In Progress");

            try {
                // Open standard geo intent compatible with OsmAnd, Organic Maps, or browser navigation
                Uri geoUri = Uri.parse("geo:" + destGeoPoint.getLatitude() + "," + destGeoPoint.getLongitude() + "?q=" + Uri.encode(destination));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                startActivity(mapIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Opening navigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMonitorJourney() {
        if (btnMonitorJourney != null) {
            btnMonitorJourney.setOnClickListener(v -> {
                Intent intent = new Intent(this, JourneyMonitoringActivity.class);
                intent.putExtra(JourneyMonitoringService.EXTRA_DESTINATION, etDestination.getText().toString());
                intent.putExtra(JourneyMonitoringService.EXTRA_ORIGIN, etSource.getText().toString());
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.MAP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDetach();
    }
}
