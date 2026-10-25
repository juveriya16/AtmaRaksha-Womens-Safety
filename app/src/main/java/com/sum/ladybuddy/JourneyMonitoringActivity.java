package com.sum.ladybuddy;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

public class JourneyMonitoringActivity extends AppCompatActivity {

    private ImageView btnBack, btnHistoryShortcut;
    private LinearLayout layoutSetup, layoutActive;

    // Setup View components
    private EditText etOrigin, etDestination;
    private TextView chipHome, chipOffice, chipMetro;
    private TextView chipEta15, chipEta30, chipEta45, chipEta60;
    private RadioGroup rgInterval;
    private LinearLayout containerContactCheckboxes;
    private CheckBox cbConsent;
    private Button btnStartJourney;
    private TextView btnManageContacts;

    // Active View components
    private LinearLayout bannerStatus;
    private View dotStatusIndicator;
    private TextView tvActiveStatusText, tvActiveTripSummary;
    private TextView tvCheckinTimerDisplay, tvEtaCountdownDisplay, tvGuardiansSummary;
    private Button btnImSafe, btnExtendTimer, btnEndJourney, btnEmergencySos;
    private TextView btnStopLiveSharing;
    private MapView mapView;
    private Marker liveLocationMarker;

    private FusedLocationProviderClient fusedLocationClient;
    private int selectedEtaMinutes = 30;
    private int selectedCheckinInterval = 15;
    private BroadcastReceiver journeyUpdateReceiver;
    private GeoPoint currentGpsGeoPoint = new GeoPoint(12.9716, 77.5946); // Default Bengaluru

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OpenStreetMap user agent initialization
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_journey_monitoring);

        // Header
        btnBack = findViewById(R.id.btn_back_journey);
        btnHistoryShortcut = findViewById(R.id.btn_journey_history_shortcut);

        // Views
        layoutSetup = findViewById(R.id.layout_journey_setup);
        layoutActive = findViewById(R.id.layout_journey_active);

        // Setup Controls
        etOrigin = findViewById(R.id.et_setup_origin);
        etDestination = findViewById(R.id.et_setup_destination);
        chipHome = findViewById(R.id.chip_quick_home);
        chipOffice = findViewById(R.id.chip_quick_office);
        chipMetro = findViewById(R.id.chip_quick_metro);

        chipEta15 = findViewById(R.id.chip_eta_15);
        chipEta30 = findViewById(R.id.chip_eta_30);
        chipEta45 = findViewById(R.id.chip_eta_45);
        chipEta60 = findViewById(R.id.chip_eta_60);
        rgInterval = findViewById(R.id.rg_checkin_interval);

        containerContactCheckboxes = findViewById(R.id.container_contacts_checkboxes);
        cbConsent = findViewById(R.id.cb_journey_consent);
        btnStartJourney = findViewById(R.id.btn_start_protected_journey);
        btnManageContacts = findViewById(R.id.btn_manage_contacts);

        // Active Controls
        bannerStatus = findViewById(R.id.banner_journey_status);
        dotStatusIndicator = findViewById(R.id.dot_status_indicator);
        tvActiveStatusText = findViewById(R.id.tv_active_status_text);
        tvActiveTripSummary = findViewById(R.id.tv_active_trip_summary);
        tvCheckinTimerDisplay = findViewById(R.id.tv_checkin_timer_display);
        tvEtaCountdownDisplay = findViewById(R.id.tv_eta_countdown_display);
        tvGuardiansSummary = findViewById(R.id.tv_guardians_monitoring_summary);

        btnImSafe = findViewById(R.id.btn_action_im_safe);
        btnExtendTimer = findViewById(R.id.btn_action_extend_timer);
        btnEndJourney = findViewById(R.id.btn_action_end_journey);
        btnEmergencySos = findViewById(R.id.btn_action_emergency_sos);
        btnStopLiveSharing = findViewById(R.id.btn_stop_live_sharing);

        initOsmMapView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnBack.setOnClickListener(v -> finish());
        btnHistoryShortcut.setOnClickListener(v -> startActivity(new Intent(this, SafetyHistoryActivity.class)));
        btnManageContacts.setOnClickListener(v -> startActivity(new Intent(this, TrustedContactsActivity.class)));

        // Handle Intent extras passed from SafeRouteActivity
        String passedDest = getIntent().getStringExtra(JourneyMonitoringService.EXTRA_DESTINATION);
        String passedOrigin = getIntent().getStringExtra(JourneyMonitoringService.EXTRA_ORIGIN);
        if (passedDest != null && !passedDest.isEmpty()) etDestination.setText(passedDest);
        if (passedOrigin != null && !passedOrigin.isEmpty()) etOrigin.setText(passedOrigin);

        setupEtaChips();
        setupIntervalRadio();
        setupDestinationQuickChips();
        setupStartButton();
        setupActiveControls();
        registerStatusBroadcastReceiver();

        fetchCurrentLocationForSetup();
    }

    private void initOsmMapView() {
        mapView = findViewById(R.id.mapview_journey_active);
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(currentGpsGeoPoint);
        }
    }

    private void fetchCurrentLocationForSetup() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                Location loc = task.getResult();
                if (loc != null) {
                    currentGpsGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                    if (etOrigin.getText().toString().isEmpty()) {
                        etOrigin.setText("Current Location (" + String.format(Locale.US, "%.3f, %.3f", loc.getLatitude(), loc.getLongitude()) + ")");
                    }
                    updateMapMarker(currentGpsGeoPoint);
                }
            });
        }
    }

    private void updateMapMarker(GeoPoint point) {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        liveLocationMarker = new Marker(mapView);
        liveLocationMarker.setPosition(point);
        liveLocationMarker.setTitle("Live Location");
        liveLocationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_record_dot));
        mapView.getOverlays().add(liveLocationMarker);

        mapView.getController().setCenter(point);
        mapView.invalidate();
    }

    private void setupDestinationQuickChips() {
        chipHome.setOnClickListener(v -> etDestination.setText("Home"));
        chipOffice.setOnClickListener(v -> etDestination.setText("Workplace"));
        chipMetro.setOnClickListener(v -> etDestination.setText("Central Station"));
    }

    private void setupEtaChips() {
        chipEta15.setOnClickListener(v -> selectEtaChip(15, chipEta15));
        chipEta30.setOnClickListener(v -> selectEtaChip(30, chipEta30));
        chipEta45.setOnClickListener(v -> selectEtaChip(45, chipEta45));
        chipEta60.setOnClickListener(v -> selectEtaChip(60, chipEta60));
    }

    private void selectEtaChip(int minutes, TextView selectedView) {
        selectedEtaMinutes = minutes;
        TextView[] chips = {chipEta15, chipEta30, chipEta45, chipEta60};
        for (TextView t : chips) {
            t.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            t.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
        }
        selectedView.setBackgroundResource(R.drawable.bg_pill_button_pink);
        selectedView.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void setupIntervalRadio() {
        rgInterval.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_interval_10) selectedCheckinInterval = 10;
            else if (checkedId == R.id.rb_interval_15) selectedCheckinInterval = 15;
            else if (checkedId == R.id.rb_interval_20) selectedCheckinInterval = 20;
        });
    }

    private void populateContactsList() {
        containerContactCheckboxes.removeAllViews();
        List<TrustedContactModel> contacts = TrustedContactsManager.getContacts(this);

        if (contacts.isEmpty()) {
            TextView emptyNotice = new TextView(this);
            emptyNotice.setText("No emergency contacts saved yet. Tap 'Manage Safety Circle' below to add guardians.");
            emptyNotice.setTextSize(12);
            emptyNotice.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            emptyNotice.setPadding(0, 10, 0, 10);
            containerContactCheckboxes.addView(emptyNotice);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (TrustedContactModel c : contacts) {
            View itemView = inflater.inflate(R.layout.item_trusted_contact_checkbox, containerContactCheckboxes, false);
            CheckBox cb = itemView.findViewById(R.id.cb_contact_monitoring);
            TextView tvName = itemView.findViewById(R.id.tv_contact_name);
            TextView tvPhone = itemView.findViewById(R.id.tv_contact_phone);

            tvName.setText(c.getName() + " (" + c.getRelationship() + ")");
            tvPhone.setText(c.getPhoneNumber());
            cb.setChecked(c.isMonitoringEnabled());

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                TrustedContactsManager.updateMonitoringPreference(this, c.getId(), isChecked);
            });

            containerContactCheckboxes.addView(itemView);
        }
    }

    private void setupStartButton() {
        btnStartJourney.setOnClickListener(v -> {
            String origin = etOrigin.getText().toString().trim();
            String destination = etDestination.getText().toString().trim();

            if (destination.isEmpty()) {
                Toast.makeText(this, "Please enter your destination.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbConsent.isChecked()) {
                Toast.makeText(this, "Explicit consent is required to share live location with selected guardians.", Toast.LENGTH_LONG).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                return;
            }

            Intent serviceIntent = new Intent(this, JourneyMonitoringService.class);
            serviceIntent.setAction(JourneyMonitoringService.ACTION_START_JOURNEY);
            serviceIntent.putExtra(JourneyMonitoringService.EXTRA_DESTINATION, destination);
            serviceIntent.putExtra(JourneyMonitoringService.EXTRA_ORIGIN, origin.isEmpty() ? "Current Location" : origin);
            serviceIntent.putExtra(JourneyMonitoringService.EXTRA_ETA_MINUTES, selectedEtaMinutes);
            serviceIntent.putExtra(JourneyMonitoringService.EXTRA_CHECKIN_INTERVAL_MINUTES, selectedCheckinInterval);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(this, "Daily Journey Monitoring Activated (OSM Live Tracking)", Toast.LENGTH_SHORT).show();
            switchToActiveMode(destination, origin);
        });
    }

    private void setupActiveControls() {
        btnImSafe.setOnClickListener(v -> {
            Intent intent = new Intent(this, JourneyMonitoringService.class);
            intent.setAction(JourneyMonitoringService.ACTION_CHECKIN_SAFE);
            startService(intent);
            Toast.makeText(this, "Check-In Confirmed: You are safe!", Toast.LENGTH_SHORT).show();
        });

        btnExtendTimer.setOnClickListener(v -> {
            Intent intent = new Intent(this, JourneyMonitoringService.class);
            intent.setAction(JourneyMonitoringService.ACTION_EXTEND_TIMER);
            startService(intent);
            Toast.makeText(this, "Check-in timer extended by 15 minutes.", Toast.LENGTH_SHORT).show();
        });

        btnEndJourney.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("End Journey Monitoring?")
                    .setMessage("This will stop live GPS location tracking and notify your guardians that you have arrived safely.")
                    .setPositiveButton("Arrived Safely", (dialog, which) -> {
                        Intent intent = new Intent(this, JourneyMonitoringService.class);
                        intent.setAction(JourneyMonitoringService.ACTION_END_JOURNEY);
                        startService(intent);
                        switchToSetupMode();
                        Toast.makeText(this, "Journey Completed Safely!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnEmergencySos.setOnClickListener(v -> {
            Intent intent = new Intent(this, JourneyMonitoringService.class);
            intent.setAction(JourneyMonitoringService.ACTION_EMERGENCY_SOS);
            startService(intent);
            startActivity(new Intent(this, SosEmergencyActivity.class));
        });

        btnStopLiveSharing.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Stop Live Location Broadcast?")
                    .setMessage("Live GPS broadcast to your contacts will be paused immediately.")
                    .setPositiveButton("Pause Sharing", (dialog, which) -> {
                        Intent intent = new Intent(this, JourneyMonitoringService.class);
                        intent.setAction(JourneyMonitoringService.ACTION_END_JOURNEY);
                        startService(intent);
                        switchToSetupMode();
                        Toast.makeText(this, "Location Sharing Stopped", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void registerStatusBroadcastReceiver() {
        journeyUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (JourneyMonitoringService.ACTION_JOURNEY_BROADCAST.equals(intent.getAction())) {
                    long etaRemaining = intent.getLongExtra("eta_remaining", 0);
                    long checkinRemaining = intent.getLongExtra("checkin_remaining", 0);
                    String status = intent.getStringExtra("status");
                    String destination = intent.getStringExtra("destination");
                    String origin = intent.getStringExtra("origin");
                    double lat = intent.getDoubleExtra("latitude", 0.0);
                    double lng = intent.getDoubleExtra("longitude", 0.0);

                    if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
                        switchToSetupMode();
                    } else {
                        switchToActiveMode(destination, origin);
                        updateActiveDisplay(status, etaRemaining, checkinRemaining);
                        if (lat != 0.0 && lng != 0.0) {
                            currentGpsGeoPoint = new GeoPoint(lat, lng);
                            updateMapMarker(currentGpsGeoPoint);
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(JourneyMonitoringService.ACTION_JOURNEY_BROADCAST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(journeyUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(journeyUpdateReceiver, filter);
        }
    }

    private void refreshViewMode() {
        if (JourneyMonitoringService.isJourneyRunning) {
            switchToActiveMode(JourneyMonitoringService.currentDestination, JourneyMonitoringService.currentOrigin);
            updateActiveDisplay(JourneyMonitoringService.currentStatus,
                    JourneyMonitoringService.secondsRemaining,
                    JourneyMonitoringService.checkinSecondsRemaining);
            if (JourneyMonitoringService.lastLatitude != 0.0) {
                currentGpsGeoPoint = new GeoPoint(JourneyMonitoringService.lastLatitude, JourneyMonitoringService.lastLongitude);
                updateMapMarker(currentGpsGeoPoint);
            }
        } else {
            switchToSetupMode();
        }
    }

    private void switchToSetupMode() {
        layoutSetup.setVisibility(View.VISIBLE);
        layoutActive.setVisibility(View.GONE);
    }

    private void switchToActiveMode(String destination, String origin) {
        layoutSetup.setVisibility(View.GONE);
        layoutActive.setVisibility(View.VISIBLE);

        tvActiveTripSummary.setText(origin + " → " + destination);
        tvGuardiansSummary.setText("👥 Monitored by: " + TrustedContactsManager.getSelectedMonitoringSummary(this));
    }

    private void updateActiveDisplay(String status, long etaSecs, long checkinSecs) {
        int cMins = (int) (checkinSecs / 60);
        int cSecs = (int) (checkinSecs % 60);
        tvCheckinTimerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", cMins, cSecs));

        int tMins = (int) (etaSecs / 60);
        tvEtaCountdownDisplay.setText(tMins + " min left");

        tvGuardiansSummary.setText("👥 Monitored by: " + TrustedContactsManager.getSelectedMonitoringSummary(this));

        if ("CHECK_IN_REQUIRED".equals(status)) {
            dotStatusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.color_sos_red));
            tvActiveStatusText.setText("⚠️ CHECK-IN REQUIRED • GRACE PERIOD ACTIVE");
            tvActiveStatusText.setTextColor(ContextCompat.getColor(this, R.color.color_sos_red));
        } else if ("ALERT_SENT".equals(status)) {
            dotStatusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.color_warning));
            tvActiveStatusText.setText("📢 PRECAUTIONARY ALERT SENT TO GUARDIANS");
            tvActiveStatusText.setTextColor(ContextCompat.getColor(this, R.color.color_warning));
        } else {
            dotStatusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.color_success));
            tvActiveStatusText.setText("STATUS: ACTIVE • LIVE LOCATION SHARING ON");
            tvActiveStatusText.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        refreshViewMode();
        populateContactsList();
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
        if (journeyUpdateReceiver != null) {
            try {
                unregisterReceiver(journeyUpdateReceiver);
            } catch (Exception ignored) {}
        }
    }
}
