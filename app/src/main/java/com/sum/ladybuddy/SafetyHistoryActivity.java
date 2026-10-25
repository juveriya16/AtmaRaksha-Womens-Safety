package com.sum.ladybuddy;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SafetyHistoryActivity extends AppCompatActivity {

    private static final String PREF_HISTORY = "AtmaRaksha_History_Prefs";
    private static final String KEY_SEEDED = "has_seeded_history_v1";

    private ImageView btnBack, btnNotificationBell;
    private Button btnClear, btnExport;
    private TextView tvCount, tvFilterLabel;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private LinearLayout listContainer, emptyView, btnFilter;
    private SafetyHistoryDbHelper dbHelper;
    private int currentFilterType = 0; // 0 = All, 1 = SOS, 2 = Journeys, 3 = Evidence

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_history);

        dbHelper = new SafetyHistoryDbHelper(this);

        btnBack = findViewById(R.id.btn_back_history);
        btnNotificationBell = findViewById(R.id.btn_notification_bell);
        btnClear = findViewById(R.id.btn_clear_history);
        btnExport = findViewById(R.id.btn_export_history);
        tvCount = findViewById(R.id.tv_total_events_count);
        tvFilterLabel = findViewById(R.id.tv_filter_label);
        btnFilter = findViewById(R.id.btn_history_filter);
        listContainer = findViewById(R.id.events_list_container);
        emptyView = findViewById(R.id.empty_history_view);
        tvEmptyTitle = findViewById(R.id.tv_empty_history_title);
        tvEmptySubtitle = findViewById(R.id.tv_empty_history_subtitle);

        btnBack.setOnClickListener(v -> finish());

        if (btnNotificationBell != null) {
            btnNotificationBell.setOnClickListener(v -> {
                Toast.makeText(this, "Active Protections: Guardian SOS Armed • Night Mode Ready", Toast.LENGTH_SHORT).show();
            });
        }

        btnClear.setOnClickListener(v -> promptClearHistory());
        btnExport.setOnClickListener(v -> exportSafetyReport());

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        seedInitialEventsOnceIfEmpty();
        loadHistoryEvents();

        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.ACTIVITY);
    }

    private void seedInitialEventsOnceIfEmpty() {
        SharedPreferences prefs = getSharedPreferences(PREF_HISTORY, MODE_PRIVATE);
        boolean hasSeeded = prefs.getBoolean(KEY_SEEDED, false);

        if (!hasSeeded && dbHelper.getEventsCount() == 0) {
            dbHelper.logEvent("SOS Alert", "SOS Alert Dispatched",
                    "Live GPS SMS dispatched to emergency circle contacts. Cellular status confirmed.", "Alert Sent");
            dbHelper.logEvent("Completed", "Completed Safe Journey",
                    "Safe Route: 25 min travel from Church Street to Pinewood Avenue. All safety check-ins completed.", "Safe Arrival");
            dbHelper.logEvent("Safe Route", "Safe Corridor Navigation",
                    "Travelled along well-lit corridor with CCTV coverage on Pinewood Avenue. No safety deviations.", "Completed");
            dbHelper.logEvent("Emergency Siren", "Siren Deterrent Activated",
                    "High-decibel audible alarm sounded to deter potential threats and alert surroundings.", "Active");
            dbHelper.logEvent("Check-In", "I'm Safe Check-In Confirmed",
                    "User confirmed safety check-in on monitored journey. Timer reset.", "Safe");
            dbHelper.logEvent("SOS Alert", "SOS Alert Cancelled",
                    "Location sharing stopped. User entered safety PIN and confirmed safety.", "Cancelled");

            prefs.edit().putBoolean(KEY_SEEDED, true).apply();
        }
    }

    private void showFilterDialog() {
        String[] options = new String[]{
                "All Safety History",
                "🚨 SOS Emergency Alerts",
                "🛣️ Monitored Journeys",
                "🎙️ Evidence Audio Recordings"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Safety Timeline");
        builder.setSingleChoiceItems(options, currentFilterType, (dialog, which) -> {
            currentFilterType = which;
            if (which == 0) tvFilterLabel.setText("All >");
            else if (which == 1) tvFilterLabel.setText("SOS Only");
            else if (which == 2) tvFilterLabel.setText("Journeys Only");
            else tvFilterLabel.setText("Evidence Only");

            loadHistoryEvents();
            dialog.dismiss();
        });
        builder.show();
    }

    private void loadHistoryEvents() {
        listContainer.removeAllViews();

        if (currentFilterType == 2) {
            renderJourneysList();
            return;
        } else if (currentFilterType == 3) {
            renderEvidenceList();
            return;
        }

        List<SafetyHistoryDbHelper.SafetyEvent> allEvents;
        try {
            allEvents = dbHelper.getAllEvents();
        } catch (Exception e) {
            allEvents = new ArrayList<>();
        }

        List<SafetyHistoryDbHelper.SafetyEvent> events = new ArrayList<>();

        if (allEvents != null) {
            for (SafetyHistoryDbHelper.SafetyEvent ev : allEvents) {
                if (currentFilterType == 1) {
                    if (isSosEvent(ev)) {
                        events.add(ev);
                    }
                } else {
                    events.add(ev);
                }
            }
        }

        if (events.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);

            if (currentFilterType == 1) {
                tvCount.setText("0 SOS Alerts");
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No SOS Alerts Recorded");
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Emergency triggers, SOS dispatches, and cancelled alerts will appear here.");
            } else {
                tvCount.setText("0 Safety Events");
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No Safety Events Recorded");
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText("SOS alerts, monitored journeys, check-ins, and deterrent sirens will appear here.");
            }
            return;
        }

        emptyView.setVisibility(View.GONE);
        listContainer.setVisibility(View.VISIBLE);

        int count = events.size();
        String label = (currentFilterType == 1) ? "SOS Alerts (" + count + ")" : "Timeline (" + count + " " + (count == 1 ? "event" : "events") + ")";
        tvCount.setText(label);

        for (final SafetyHistoryDbHelper.SafetyEvent event : events) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
            card.setLayoutParams(cardParams);
            card.setRadius(dpToPx(16));
            card.setCardElevation(dpToPx(2));

            boolean isSos = isSosEvent(event);
            boolean isCompleted = event.type != null && (event.type.contains("Completed") || (event.status != null && event.status.contains("Safe")));
            boolean isSiren = event.type != null && (event.type.contains("Siren") || (event.title != null && event.title.contains("Siren")));

            if (isSos) {
                card.setCardBackgroundColor(Color.parseColor("#FFF7F8"));
            } else {
                card.setCardBackgroundColor(Color.WHITE);
            }

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

            // Top Row: Icon + Title (Left) and Status Badge (Right)
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            String iconPrefix = getEventIcon(event.type, event.title);
            TextView tvType = new TextView(this);
            tvType.setText(iconPrefix + " " + (event.title != null ? event.title : "Safety Event"));
            tvType.setTextSize(14);
            tvType.setTextColor(isSos ? ContextCompat.getColor(this, R.color.color_sos_red) : ContextCompat.getColor(this, R.color.text_primary));
            tvType.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(tvType, typeParams);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(event.status != null ? event.status : "Logged");
            tvStatus.setTextSize(10);
            if (isSos) {
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setBackgroundResource(R.drawable.bg_pill_button_pink);
            } else if (isSiren) {
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
                tvStatus.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            } else {
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
                tvStatus.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            }
            tvStatus.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            topRow.addView(tvStatus);

            content.addView(topRow);

            // Event Details
            if (event.details != null && !event.details.trim().isEmpty()) {
                TextView tvDetails = new TextView(this);
                tvDetails.setText(event.details);
                tvDetails.setTextSize(12);
                tvDetails.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                detailParams.setMargins(0, dpToPx(6), 0, 0);
                content.addView(tvDetails, detailParams);
            }

            // Timestamp Row
            LinearLayout timeRow = new LinearLayout(this);
            timeRow.setOrientation(LinearLayout.HORIZONTAL);
            timeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams timeRowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timeRowParams.setMargins(0, dpToPx(8), 0, 0);
            timeRow.setLayoutParams(timeRowParams);

            TextView tvTime = new TextView(this);
            tvTime.setText("🕒 " + (event.timestamp != null ? event.timestamp : "Just now"));
            tvTime.setTextSize(11);
            tvTime.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            timeRow.addView(tvTime, timeParams);

            TextView tvViewDetails = new TextView(this);
            tvViewDetails.setText("View Details →");
            tvViewDetails.setTextSize(11);
            tvViewDetails.setTextColor(ContextCompat.getColor(this, R.color.brand_pink));
            tvViewDetails.setTypeface(null, android.graphics.Typeface.BOLD);
            tvViewDetails.setPadding(dpToPx(6), dpToPx(4), 0, dpToPx(4));
            tvViewDetails.setOnClickListener(v -> showEventDetailsDialog(event));
            timeRow.addView(tvViewDetails);

            content.addView(timeRow);

            // Connect content to CardView and CardView to listContainer
            card.addView(content);
            card.setOnClickListener(v -> showEventDetailsDialog(event));
            listContainer.addView(card);
        }
    }

    private void renderJourneysList() {
        List<JourneyRecord> journeys;
        try {
            journeys = dbHelper.getAllJourneys();
        } catch (Exception e) {
            journeys = new ArrayList<>();
        }

        if (journeys == null || journeys.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);
            tvCount.setText("0 Monitored Journeys");
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No Monitored Journeys Found");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Start a daily journey from the home screen to log active GPS routes, check-ins, and arrival times.");
            return;
        }

        emptyView.setVisibility(View.GONE);
        listContainer.setVisibility(View.VISIBLE);
        tvCount.setText("Monitored Journeys (" + journeys.size() + ")");

        for (final JourneyRecord j : journeys) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
            card.setLayoutParams(cardParams);
            card.setRadius(dpToPx(16));
            card.setCardElevation(dpToPx(2));
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

            // Top Row: Origin -> Destination + Status
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvRoute = new TextView(this);
            tvRoute.setText("🛣️ " + j.getOrigin() + " → " + j.getDestination());
            tvRoute.setTextSize(13);
            tvRoute.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvRoute.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams routeParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(tvRoute, routeParams);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(j.getStatus() != null ? j.getStatus() : "Completed");
            tvStatus.setTextSize(10);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
            tvStatus.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            tvStatus.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            topRow.addView(tvStatus);
            content.addView(topRow);

            // Details
            TextView tvStats = new TextView(this);
            tvStats.setText("• Check-ins Completed: " + j.getCheckInsCompleted() + " | Missed: " + j.getMissedCheckIns() +
                    "\n• Monitored Contacts: " + (j.getMonitoredContacts() != null ? j.getMonitoredContacts() : "None"));
            tvStats.setTextSize(12);
            tvStats.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            statsParams.setMargins(0, dpToPx(6), 0, 0);
            content.addView(tvStats, statsParams);

            // Timestamp
            TextView tvTime = new TextView(this);
            tvTime.setText("🕒 Started: " + j.getStartTime() + (j.getEndTime() != null ? " • Ended: " + j.getEndTime() : ""));
            tvTime.setTextSize(11);
            tvTime.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timeParams.setMargins(0, dpToPx(8), 0, 0);
            content.addView(tvTime, timeParams);

            card.addView(content);
            card.setOnClickListener(v -> showJourneyDetailsDialog(j));
            listContainer.addView(card);
        }
    }

    private void renderEvidenceList() {
        List<EvidenceRecording> recs;
        try {
            recs = dbHelper.getAllRecordings();
        } catch (Exception e) {
            recs = new ArrayList<>();
        }

        if (recs == null || recs.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);
            tvCount.setText("0 Evidence Recordings");
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No Audio Evidence Recorded");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Confidential audio evidence captured from the Evidence Vault will be archived here.");
            return;
        }

        emptyView.setVisibility(View.GONE);
        listContainer.setVisibility(View.VISIBLE);
        tvCount.setText("Audio Evidence (" + recs.size() + ")");

        for (final EvidenceRecording rec : recs) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
            card.setLayoutParams(cardParams);
            card.setRadius(dpToPx(16));
            card.setCardElevation(dpToPx(2));
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("🎙️ " + rec.getFilename());
            tvTitle.setTextSize(13);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(tvTitle, titleParams);

            TextView tvVault = new TextView(this);
            tvVault.setText("Secure Vault");
            tvVault.setTextSize(10);
            tvVault.setTextColor(ContextCompat.getColor(this, R.color.brand_pink));
            tvVault.setBackgroundResource(R.drawable.bg_pill_button_pink);
            tvVault.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            tvVault.setTypeface(null, android.graphics.Typeface.BOLD);
            topRow.addView(tvVault);
            content.addView(topRow);

            TextView tvMeta = new TextView(this);
            int dur = rec.getDurationSeconds();
            String durStr = String.format(Locale.getDefault(), "%02d:%02d", dur / 60, dur % 60);
            tvMeta.setText("🕒 " + rec.getTimestamp() + " • Duration: " + durStr + " • Size: " + (rec.getFileSizeBytes() / 1024) + " KB");
            tvMeta.setTextSize(11);
            tvMeta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            metaParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
            content.addView(tvMeta, metaParams);

            TextView tvOpen = new TextView(this);
            tvOpen.setText("Manage in Evidence Recorder →");
            tvOpen.setTextSize(11);
            tvOpen.setTextColor(ContextCompat.getColor(this, R.color.brand_pink));
            tvOpen.setTypeface(null, android.graphics.Typeface.BOLD);
            tvOpen.setOnClickListener(v -> startActivity(new Intent(this, EvidenceRecordingActivity.class)));
            content.addView(tvOpen);

            card.addView(content);
            listContainer.addView(card);
        }
    }

    private boolean isSosEvent(SafetyHistoryDbHelper.SafetyEvent ev) {
        if (ev == null) return false;
        String type = ev.type != null ? ev.type.toLowerCase(Locale.ROOT) : "";
        String title = ev.title != null ? ev.title.toLowerCase(Locale.ROOT) : "";
        return type.contains("sos") || type.contains("alert") || type.contains("discreet") || type.contains("voice")
                || title.contains("sos") || title.contains("alert") || title.contains("emergency");
    }

    private String getEventIcon(String type, String title) {
        String combined = ((type != null ? type : "") + " " + (title != null ? title : "")).toLowerCase(Locale.ROOT);
        if (combined.contains("sos") || combined.contains("emergency")) {
            return "🚨";
        } else if (combined.contains("journey") || combined.contains("route") || combined.contains("check-in") || combined.contains("completed")) {
            return "🛣️";
        } else if (combined.contains("siren")) {
            return "🔊";
        } else if (combined.contains("contact") || combined.contains("guardian")) {
            return "👥";
        } else if (combined.contains("hazard") || combined.contains("report")) {
            return "⚠️";
        } else if (combined.contains("offline")) {
            return "📡";
        } else if (combined.contains("night")) {
            return "🌙";
        } else if (combined.contains("discreet") || combined.contains("decoy")) {
            return "🔢";
        } else {
            return "🛡️";
        }
    }

    private void showEventDetailsDialog(SafetyHistoryDbHelper.SafetyEvent event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(event.title != null ? event.title : "Event Details");
        builder.setMessage("Category: " + event.type +
                "\nStatus: " + event.status +
                "\nTimestamp: " + event.timestamp +
                "\n\nDetails:\n" + (event.details != null ? event.details : "No additional notes."));
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showJourneyDetailsDialog(JourneyRecord j) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Journey Details");
        builder.setMessage("Origin: " + j.getOrigin() +
                "\nDestination: " + j.getDestination() +
                "\nStatus: " + j.getStatus() +
                "\nStarted: " + j.getStartTime() +
                "\nEnded: " + (j.getEndTime() != null ? j.getEndTime() : "In progress / Pending") +
                "\n\nSafety Check-ins Completed: " + j.getCheckInsCompleted() +
                "\nMissed Check-ins: " + j.getMissedCheckIns() +
                "\nGuardians Informed: " + (j.getMonitoredContacts() != null ? j.getMonitoredContacts() : "None"));
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void promptClearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear History Log");
        builder.setMessage("Are you sure you want to permanently clear all local safety records from this device?");
        builder.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.clearHistory();

                // Mark that user has seeded and cleared so sample events are not re-seeded
                SharedPreferences prefs = getSharedPreferences(PREF_HISTORY, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_SEEDED, true).apply();

                loadHistoryEvents();
                Toast.makeText(SafetyHistoryActivity.this, "Safety history cleared successfully.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void exportSafetyReport() {
        List<SafetyHistoryDbHelper.SafetyEvent> events = dbHelper.getAllEvents();
        List<JourneyRecord> journeys = dbHelper.getAllJourneys();
        List<EvidenceRecording> recs = dbHelper.getAllRecordings();

        boolean hasEvents = events != null && !events.isEmpty();
        boolean hasJourneys = journeys != null && !journeys.isEmpty();
        boolean hasRecs = recs != null && !recs.isEmpty();

        if (!hasEvents && !hasJourneys && !hasRecs) {
            Toast.makeText(this, "No records available to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🛡️ AtmaRaksha Comprehensive Safety Report\n");
        sb.append("Generated on: ").append(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date())).append("\n\n");

        // Monitored Journeys section
        if (hasJourneys) {
            sb.append("═════ MONITORED JOURNEYS (").append(journeys.size()).append(") ═════\n");
            for (JourneyRecord j : journeys) {
                sb.append("• ").append(j.getOrigin()).append(" → ").append(j.getDestination())
                        .append(" [Status: ").append(j.getStatus()).append("]\n")
                        .append("  Start: ").append(j.getStartTime())
                        .append(" | Check-ins: ").append(j.getCheckInsCompleted())
                        .append(" | Missed: ").append(j.getMissedCheckIns()).append("\n\n");
            }
        }

        // Evidence section
        if (hasRecs) {
            sb.append("═════ AUDIO EVIDENCE VAULT (").append(recs.size()).append(") ═════\n");
            for (EvidenceRecording r : recs) {
                sb.append("• ").append(r.getFilename()).append(" (").append(r.getTimestamp()).append(")\n")
                        .append("  Duration: ").append(r.getDurationSeconds()).append("s | Size: ").append(r.getFileSizeBytes() / 1024).append(" KB\n\n");
            }
        }

        // Safety Events section
        if (hasEvents) {
            sb.append("═════ SAFETY EVENTS TIMELINE (").append(events.size()).append(") ═════\n");
            for (SafetyHistoryDbHelper.SafetyEvent e : events) {
                sb.append("[").append(e.timestamp).append("] ").append(e.title).append(" (").append(e.status).append(")\n");
                if (e.details != null && !e.details.isEmpty()) {
                    sb.append("  Details: ").append(e.details).append("\n");
                }
                sb.append("\n");
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AtmaRaksha Comprehensive Safety Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Safety Log via"));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryEvents();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.ACTIVITY);
    }
}
