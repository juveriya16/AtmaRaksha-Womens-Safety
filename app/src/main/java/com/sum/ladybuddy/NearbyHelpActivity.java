package com.sum.ladybuddy;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyHelpActivity extends AppCompatActivity {

    private static final int REQ_PERM_LOCATION = 101;

    private ImageView btnBack;
    private TextView toggleBtnList, toggleBtnMap;
    private LinearLayout containerList, containerMap;
    private EditText etSearch;

    private TextView filterAll, filterPolice, filterHospital, filterPharmacy;
    private TextView chipRadius1km, chipRadius3km, chipRadius5km, chipRadius10km;

    private ProgressBar progressNearby;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private LinearLayout layoutDynamicContainer;

    private Button btnFast112, btnFast1091, btnOpenExternalMapAll;

    private MapView mapView;
    private boolean isMapReady = false;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 12.9716; // Default Bengaluru
    private double currentLng = 77.5946;
    private int selectedRadiusMeters = 3000; // 3km default
    private String selectedCategory = "ALL";

    private List<NearbySafetyHelper.SafetyPlace> loadedPlaces = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OpenStreetMap user agent initialization
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_nearby_help);

        btnBack = findViewById(R.id.btn_back_nearby);
        toggleBtnList = findViewById(R.id.toggle_btn_list);
        toggleBtnMap = findViewById(R.id.toggle_btn_map);
        containerList = findViewById(R.id.container_services_list);
        containerMap = findViewById(R.id.container_services_map);
        etSearch = findViewById(R.id.et_search_nearby);

        filterAll = findViewById(R.id.filter_all);
        filterPolice = findViewById(R.id.filter_police);
        filterHospital = findViewById(R.id.filter_hospital);
        filterPharmacy = findViewById(R.id.filter_pharmacy);

        chipRadius1km = findViewById(R.id.chip_radius_1km);
        chipRadius3km = findViewById(R.id.chip_radius_3km);
        chipRadius5km = findViewById(R.id.chip_radius_5km);
        chipRadius10km = findViewById(R.id.chip_radius_10km);

        progressNearby = findViewById(R.id.progress_nearby_loading);
        layoutEmpty = findViewById(R.id.layout_nearby_empty);
        tvEmptyTitle = findViewById(R.id.tv_nearby_empty_title);
        tvEmptySubtitle = findViewById(R.id.tv_nearby_empty_subtitle);
        layoutDynamicContainer = findViewById(R.id.layout_dynamic_places_container);

        btnFast112 = findViewById(R.id.btn_fast_112);
        btnFast1091 = findViewById(R.id.btn_fast_1091);
        btnOpenExternalMapAll = findViewById(R.id.btn_open_external_map_all);

        initOsmMapView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnBack.setOnClickListener(v -> finish());

        setupViewToggle();
        setupCategoryFilters();
        setupRadiusChips();
        setupSearchFiltering();
        setupQuickHelplines();

        requestLocationAndFetchPlaces();
    }

    private void initOsmMapView() {
        mapView = findViewById(R.id.mapview_nearby_help);
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
            mapView.getController().setZoom(14.5);
            mapView.getController().setCenter(new GeoPoint(currentLat, currentLng));
            isMapReady = true;
        }
    }

    private void requestLocationAndFetchPlaces() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERM_LOCATION);
        } else {
            fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                Location loc = task.getResult();
                if (loc != null) {
                    currentLat = loc.getLatitude();
                    currentLng = loc.getLongitude();
                    if (mapView != null) {
                        mapView.getController().setCenter(new GeoPoint(currentLat, currentLng));
                    }
                }
                fetchSafetyPlaces();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM_LOCATION) {
            requestLocationAndFetchPlaces();
        }
    }

    private void setupViewToggle() {
        toggleBtnList.setOnClickListener(v -> {
            toggleBtnList.setBackgroundResource(R.drawable.bg_pill_button_pink);
            toggleBtnList.setTextColor(Color.WHITE);

            toggleBtnMap.setBackgroundResource(android.R.color.transparent);
            toggleBtnMap.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            containerList.setVisibility(View.VISIBLE);
            containerMap.setVisibility(View.GONE);
        });

        toggleBtnMap.setOnClickListener(v -> {
            toggleBtnMap.setBackgroundResource(R.drawable.bg_pill_button_pink);
            toggleBtnMap.setTextColor(Color.WHITE);

            toggleBtnList.setBackgroundResource(android.R.color.transparent);
            toggleBtnList.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            containerList.setVisibility(View.GONE);
            containerMap.setVisibility(View.VISIBLE);

            if (mapView != null) {
                mapView.invalidate();
            }
        });
    }

    private void setupCategoryFilters() {
        filterAll.setOnClickListener(v -> selectCategoryFilter("ALL", filterAll));
        filterPolice.setOnClickListener(v -> selectCategoryFilter("POLICE", filterPolice));
        filterHospital.setOnClickListener(v -> selectCategoryFilter("HOSPITAL", filterHospital));
        filterPharmacy.setOnClickListener(v -> selectCategoryFilter("PHARMACY", filterPharmacy));
    }

    private void selectCategoryFilter(String category, TextView selectedView) {
        selectedCategory = category;

        TextView[] allChips = {filterAll, filterPolice, filterHospital, filterPharmacy};
        for (TextView t : allChips) {
            t.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            t.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
        }

        selectedView.setBackgroundResource(R.drawable.bg_pill_button_pink);
        selectedView.setTextColor(Color.WHITE);

        fetchSafetyPlaces();
    }

    private void setupRadiusChips() {
        chipRadius1km.setOnClickListener(v -> selectRadius(1000, chipRadius1km));
        chipRadius3km.setOnClickListener(v -> selectRadius(3000, chipRadius3km));
        chipRadius5km.setOnClickListener(v -> selectRadius(5000, chipRadius5km));
        chipRadius10km.setOnClickListener(v -> selectRadius(10000, chipRadius10km));
    }

    private void selectRadius(int radiusMeters, TextView selectedChip) {
        selectedRadiusMeters = radiusMeters;

        TextView[] chips = {chipRadius1km, chipRadius3km, chipRadius5km, chipRadius10km};
        for (TextView c : chips) {
            c.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            c.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
        }

        selectedChip.setBackgroundResource(R.drawable.bg_pill_button_pink);
        selectedChip.setTextColor(Color.WHITE);

        fetchSafetyPlaces();
    }

    private void fetchSafetyPlaces() {
        progressNearby.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutDynamicContainer.removeAllViews();

        NearbySafetyHelper.fetchNearbySafetyServices(this, currentLat, currentLng, selectedRadiusMeters, selectedCategory, new NearbySafetyHelper.PlacesCallback() {
            @Override
            public void onPlacesLoaded(List<NearbySafetyHelper.SafetyPlace> places) {
                progressNearby.setVisibility(View.GONE);
                loadedPlaces = places;

                if (places == null || places.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    tvEmptyTitle.setText("No Verified Facilities in " + (selectedRadiusMeters / 1000) + " km");
                    tvEmptySubtitle.setText("Try increasing search radius to 5 km or 10 km, or dial 112 for immediate dispatch.");
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    renderPlacesList(places);
                }

                renderPlacesOnMap();
            }

            @Override
            public void onError(String errorMessage) {
                progressNearby.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText("Safety Data Unavailable");
                tvEmptySubtitle.setText(errorMessage);
                renderPlacesOnMap();
            }
        });
    }

    private void renderPlacesList(List<NearbySafetyHelper.SafetyPlace> places) {
        layoutDynamicContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (NearbySafetyHelper.SafetyPlace p : places) {
            View view = inflater.inflate(R.layout.item_nearby_place, layoutDynamicContainer, false);

            ImageView ivIcon = view.findViewById(R.id.iv_place_icon);
            TextView tvName = view.findViewById(R.id.tv_place_name);
            TextView tvDistanceBadge = view.findViewById(R.id.tv_place_distance_badge);
            TextView tvAddress = view.findViewById(R.id.tv_place_address);
            TextView tvVerifiedTag = view.findViewById(R.id.tv_place_verified_tag);
            Button btnDirections = view.findViewById(R.id.btn_place_directions);
            Button btnCall = view.findViewById(R.id.btn_place_call);

            tvName.setText(p.name);
            tvAddress.setText(p.address);
            tvVerifiedTag.setText(p.badgeLabel);
            String openStatus = p.isOpenNow ? "Open 24/7" : "Operating Hours Recorded";
            tvDistanceBadge.setText(p.getFormattedDistance() + " • " + openStatus);

            if ("POLICE".equalsIgnoreCase(p.category)) {
                ivIcon.setImageResource(R.drawable.ic_shield_check);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_purple));
            } else if ("HOSPITAL".equalsIgnoreCase(p.category)) {
                ivIcon.setImageResource(R.drawable.ic_emergency_sos);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_sos_red));
            } else {
                ivIcon.setImageResource(R.drawable.ic_nearby_help);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_pink));
            }

            btnDirections.setOnClickListener(v -> {
                try {
                    Uri gmmIntentUri = Uri.parse("geo:" + p.latitude + "," + p.longitude + "?q=" + Uri.encode(p.name));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(NearbyHelpActivity.this, "Opening navigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            btnCall.setOnClickListener(v -> {
                promptDialService(p.name, p.phoneNumber);
            });

            layoutDynamicContainer.addView(view);
        }
    }

    private void renderPlacesOnMap() {
        if (!isMapReady || mapView == null) return;

        mapView.getOverlays().clear();

        GeoPoint userGeoPoint = new GeoPoint(currentLat, currentLng);
        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(userGeoPoint);
        userMarker.setTitle("Your Location");
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_record_dot));
        mapView.getOverlays().add(userMarker);

        List<GeoPoint> points = new ArrayList<>();
        points.add(userGeoPoint);

        for (NearbySafetyHelper.SafetyPlace p : loadedPlaces) {
            GeoPoint pGeo = new GeoPoint(p.latitude, p.longitude);
            points.add(pGeo);

            Marker m = new Marker(mapView);
            m.setPosition(pGeo);
            m.setTitle(p.name + " (" + p.getFormattedDistance() + ")");
            m.setSnippet(p.address);

            if ("POLICE".equalsIgnoreCase(p.category)) {
                m.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_shield_check));
            } else if ("HOSPITAL".equalsIgnoreCase(p.category)) {
                m.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_emergency_sos));
            } else {
                m.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nearby_help));
            }

            mapView.getOverlays().add(m);
        }

        if (points.size() > 1) {
            double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
            for (GeoPoint pt : points) {
                if (pt.getLatitude() < minLat) minLat = pt.getLatitude();
                if (pt.getLatitude() > maxLat) maxLat = pt.getLatitude();
                if (pt.getLongitude() < minLng) minLng = pt.getLongitude();
                if (pt.getLongitude() > maxLng) maxLng = pt.getLongitude();
            }
            BoundingBox box = new BoundingBox(maxLat + 0.002, maxLng + 0.002, minLat - 0.002, minLng - 0.002);
            mapView.zoomToBoundingBox(box, true, 70);
        } else {
            mapView.getController().setCenter(userGeoPoint);
            mapView.getController().setZoom(15.0);
        }

        mapView.invalidate();
    }

    private void setupSearchFiltering() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterPlacesLocally(s.toString().toLowerCase().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterPlacesLocally(String query) {
        if (loadedPlaces.isEmpty()) return;

        if (query.isEmpty()) {
            renderPlacesList(loadedPlaces);
            return;
        }

        List<NearbySafetyHelper.SafetyPlace> filtered = new ArrayList<>();
        for (NearbySafetyHelper.SafetyPlace p : loadedPlaces) {
            if (p.name.toLowerCase().contains(query) || p.address.toLowerCase().contains(query) || p.category.toLowerCase().contains(query)) {
                filtered.add(p);
            }
        }
        renderPlacesList(filtered);
    }

    private void setupQuickHelplines() {
        btnFast112.setOnClickListener(v -> promptDialService("National Emergency Helpline", "112"));
        btnFast1091.setOnClickListener(v -> promptDialService("Women In Distress Helpline", "1091"));

        if (btnOpenExternalMapAll != null) {
            btnOpenExternalMapAll.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:" + currentLat + "," + currentLng + "?q=" + Uri.encode("police station hospital"));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=police station")));
                }
            });
        }
    }

    private void promptDialService(String serviceName, final String phoneNumber) {
        new AlertDialog.Builder(this)
                .setTitle("Call " + serviceName + "?")
                .setMessage("Open dialer to call " + phoneNumber + "? (Toll-Free)")
                .setPositiveButton("Open Phone Dialer", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
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
