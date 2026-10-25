package com.sum.ladybuddy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityReportsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView btnOpenReportDialog;
    private LinearLayout containerReportsList;
    private MapView mapView;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 12.9716;
    private double currentLng = 77.5946;
    private SafetyHistoryDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure osmdroid user agent & preferences
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName() + " AtmaRaksha Safety App");

        setContentView(R.layout.activity_community_reports);

        btnBack = findViewById(R.id.btn_back_reports);
        btnOpenReportDialog = findViewById(R.id.btn_open_report_dialog);
        containerReportsList = findViewById(R.id.container_community_reports_list);

        mapView = findViewById(R.id.mapview_community_reports);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(14.0);
        GeoPoint startPoint = new GeoPoint(currentLat, currentLng);
        mapController.setCenter(startPoint);

        dbHelper = new SafetyHistoryDbHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnBack.setOnClickListener(v -> finish());
        btnOpenReportDialog.setOnClickListener(v -> showSubmitReportDialog());

        fetchLocation();
        loadReportsList();
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                Location loc = task.getResult();
                if (loc != null) {
                    currentLat = loc.getLatitude();
                    currentLng = loc.getLongitude();
                    if (mapView != null) {
                        mapView.getController().animateTo(new GeoPoint(currentLat, currentLng));
                        renderMapMarkers(dbHelper.getAllReports());
                    }
                }
            });
        }
    }

    private void loadReportsList() {
        containerReportsList.removeAllViews();
        List<CommunityReport> reports = dbHelper.getAllReports();

        if (reports == null || reports.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No community hazard reports in your immediate vicinity. Area appears clear!");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyTv.setTextSize(12);
            emptyTv.setPadding(0, 20, 0, 20);
            containerReportsList.addView(emptyTv);
            renderMapMarkers(null);
            return;
        }

        for (final CommunityReport r : reports) {
            CardView card = new CardView(this);
            card.setCardElevation(2);
            card.setRadius(24);
            card.setCardBackgroundColor(getResources().getColor(R.color.white));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(24, 24, 24, 24);

            // Title and Status
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(r.getTitle());
            tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
            tvTitle.setTextSize(14);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvTitle.setLayoutParams(titleLp);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(r.getStatus());
            tvStatus.setTextSize(9);
            tvStatus.setPadding(12, 4, 12, 4);
            if (r.getStatus().contains("Verified")) {
                tvStatus.setBackgroundResource(R.drawable.bg_pill_button_pink);
                tvStatus.setTextColor(getResources().getColor(R.color.white));
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_pill_button_lavender);
                tvStatus.setTextColor(getResources().getColor(R.color.brand_purple));
            }

            header.addView(tvTitle);
            header.addView(tvStatus);
            layout.addView(header);

            // Description
            TextView tvDesc = new TextView(this);
            tvDesc.setText(r.getDescription());
            tvDesc.setTextColor(getResources().getColor(R.color.text_secondary));
            tvDesc.setTextSize(11);
            tvDesc.setPadding(0, 8, 0, 8);
            layout.addView(tvDesc);

            // Footer (Timestamp and Moderation button)
            LinearLayout footer = new LinearLayout(this);
            footer.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvTime = new TextView(this);
            tvTime.setText("📍 " + r.getAddress() + " • " + r.getTimestamp());
            tvTime.setTextColor(getResources().getColor(R.color.text_tertiary));
            tvTime.setTextSize(10);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvTime.setLayoutParams(timeLp);

            TextView btnFlag = new TextView(this);
            btnFlag.setText("🚩 Flag / Report");
            btnFlag.setTextColor(getResources().getColor(R.color.color_route_medium));
            btnFlag.setTextSize(10);
            btnFlag.setPadding(8, 4, 8, 4);
            btnFlag.setOnClickListener(v -> {
                dbHelper.flagReport(r.getId());
                Toast.makeText(this, "Thank you. Report flagged for moderation review.", Toast.LENGTH_SHORT).show();
            });

            footer.addView(tvTime);
            footer.addView(btnFlag);
            layout.addView(footer);

            card.addView(layout);
            containerReportsList.addView(card);
        }

        renderMapMarkers(reports);
    }

    private void renderMapMarkers(List<CommunityReport> reports) {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        // 1. User position marker
        GeoPoint userPt = new GeoPoint(currentLat, currentLng);
        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(userPt);
        userMarker.setTitle("Your GPS Location");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_record_dot));
        mapView.getOverlays().add(userMarker);

        // 2. Incident & Hazard report markers
        if (reports != null) {
            for (CommunityReport r : reports) {
                Marker m = new Marker(mapView);
                m.setPosition(new GeoPoint(r.getLatitude(), r.getLongitude()));
                m.setTitle(r.getTitle());
                m.setSnippet(r.getStatus() + " • " + r.getDescription());
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                if (r.getStatus().contains("Verified")) {
                    m.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_shield_check));
                } else {
                    m.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_alert_contacts));
                }

                mapView.getOverlays().add(m);
            }
        }

        mapView.invalidate();
    }

    private void showSubmitReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit Anonymous Safety Hazard");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Incident Title (e.g. Unlit Alleyway, Dark Underpass)");
        inputTitle.setPadding(20, 20, 20, 20);
        inputTitle.setBackgroundResource(R.drawable.bg_input_box);
        inputTitle.setTextSize(13);
        layout.addView(inputTitle);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 0);

        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Description & specific hazard details...");
        inputDesc.setPadding(20, 20, 20, 20);
        inputDesc.setBackgroundResource(R.drawable.bg_input_box);
        inputDesc.setTextSize(13);
        inputDesc.setLayoutParams(lp);
        layout.addView(inputDesc);

        final EditText inputAddr = new EditText(this);
        inputAddr.setHint("Street / Landmark (e.g. 5th Main Corner)");
        inputAddr.setPadding(20, 20, 20, 20);
        inputAddr.setBackgroundResource(R.drawable.bg_input_box);
        inputAddr.setTextSize(13);
        inputAddr.setLayoutParams(lp);
        layout.addView(inputAddr);

        TextView tvNotice = new TextView(this);
        tvNotice.setText("ℹ️ Submissions are labeled as 'Community Reported • Unverified' and auto-expire in 30 days. Your phone number and identity remain completely anonymous.");
        tvNotice.setTextSize(10);
        tvNotice.setTextColor(getResources().getColor(R.color.text_secondary));
        tvNotice.setPadding(0, 16, 0, 0);
        layout.addView(tvNotice);

        builder.setView(layout);

        builder.setPositiveButton("Submit Report", (dialog, which) -> {
            String title = inputTitle.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            String addr = inputAddr.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Please enter an incident title.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentTime = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
            CommunityReport report = new CommunityReport(
                    0, title, "COMMUNITY_HAZARD",
                    !TextUtils.isEmpty(desc) ? desc : "Caution advised in this area.",
                    currentLat + (Math.random() - 0.5) * 0.005,
                    currentLng + (Math.random() - 0.5) * 0.005,
                    !TextUtils.isEmpty(addr) ? addr : "Nearby Crossroad",
                    currentTime, "Community Reported • Unverified", 1, 0
            );

            dbHelper.insertReport(report);
            dbHelper.logEvent("Hazard Report", "Community Hazard Submitted",
                    "Submitted anonymous report: " + title + " at " + addr, "Submitted");

            Toast.makeText(this, "Anonymous report submitted. Thank you for protecting the community!", Toast.LENGTH_LONG).show();
            loadReportsList();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        loadReportsList();
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
