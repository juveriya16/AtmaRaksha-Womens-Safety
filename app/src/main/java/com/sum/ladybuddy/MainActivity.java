package com.sum.ladybuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private CardView locTrack, cardNearbyHelp, settingsCard, privacyScannerCard, liveJourneyCard, selfDefCard, helplineCard, safetyHistoryCard;
    private CardView cardJourneyBanner, cardVoiceSos, cardEvidenceVault, cardDiscreetDecoy, cardCommunityReports, cardOfflineShield, cardFakeCall;
    private ShakeDetectorHelper shakeDetector;
    private TextView tvJourneyBannerTitle, tvJourneyBannerSubtitle, tvJourneyBannerBadge;
    private LinearLayout btnHeroSos;
    private LinearLayout btnQuickCall112, btnQuickAlertContacts;
    private LinearLayout btnQuickSiren;
    private TextView tvQuickSirenText;
    private ImageView ivQuickSirenIcon;
    private EmergencySirenManager sirenManager;
    private EmergencySirenManager.SirenStateListener sirenListener;
    private ImageView btnMenuHamburger, btnNotificationBell;
    private TextView tvGreeting, tvHeaderLocationText, tvSeeAllServices;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Safe background service initialization for power-button SOS
        try {
            Intent backgroundService = new Intent(getApplicationContext(), ScreenOnOffBackgroundService.class);
            this.startService(backgroundService);
            Log.d(ScreenOnOffReceiver.SCREEN_TOGGLE_TAG, "Activity onCreate background service started");
        } catch (Throwable t) {
            Log.e("MainActivity", "Service start error: " + t.getMessage(), t);
        }

        // Initialize Views
        btnMenuHamburger = findViewById(R.id.btn_menu_hamburger);
        btnNotificationBell = findViewById(R.id.btn_notification_bell);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvHeaderLocationText = findViewById(R.id.tv_header_location_text);
        tvSeeAllServices = findViewById(R.id.tv_see_all_services);

        btnHeroSos = findViewById(R.id.btn_hero_sos);
        btnQuickCall112 = findViewById(R.id.btn_quick_call_112);
        btnQuickAlertContacts = findViewById(R.id.btn_quick_alert_contacts);
        btnQuickSiren = findViewById(R.id.btn_quick_siren);
        tvQuickSirenText = findViewById(R.id.tv_quick_siren_text);
        ivQuickSirenIcon = findViewById(R.id.iv_quick_siren_icon);

        sirenManager = EmergencySirenManager.getInstance(this);
        sirenListener = this::updateQuickSirenUi;
        sirenManager.addListener(sirenListener);

        // Journey Banner
        cardJourneyBanner = findViewById(R.id.card_journey_monitoring_banner);
        tvJourneyBannerTitle = findViewById(R.id.tv_journey_banner_title);
        tvJourneyBannerSubtitle = findViewById(R.id.tv_journey_banner_subtitle);
        tvJourneyBannerBadge = findViewById(R.id.tv_journey_banner_badge);

        locTrack = findViewById(R.id.location_trace);
        cardNearbyHelp = findViewById(R.id.card_nearby_help);
        settingsCard = findViewById(R.id.Settings);
        privacyScannerCard = findViewById(R.id.Currentlocation);
        liveJourneyCard = findViewById(R.id.card_live_journey);
        selfDefCard = findViewById(R.id.self_def);
        helplineCard = findViewById(R.id.helpline);
        safetyHistoryCard = findViewById(R.id.card_safety_history);

        // New Feature Cards
        cardVoiceSos = findViewById(R.id.card_voice_sos);
        cardEvidenceVault = findViewById(R.id.card_evidence_vault);
        cardDiscreetDecoy = findViewById(R.id.card_discreet_decoy);
        cardCommunityReports = findViewById(R.id.card_community_reports);
        cardOfflineShield = findViewById(R.id.card_offline_shield);
        cardFakeCall = findViewById(R.id.card_fake_call);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupGreeting();
        setupLocationTracker();
        setupEmergencyActions();
        setupServiceCards();
        setupHeaderMenu();

        // Setup Consistent 5-Tab Bottom Navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.HOME);

        // Check Permissions and Privacy Policy on first run
        checkPermissionsAndPolicy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupGreeting();
        setupLocationTracker();
        updateJourneyBannerState();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.HOME);

        // Resume shake detection if enabled
        if (ShakeDetectorHelper.isShakeSosEnabled(this)) {
            if (shakeDetector == null) {
                shakeDetector = new ShakeDetectorHelper(this);
            }
            shakeDetector.startListening(this::handleActivityShakeTrigger);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shakeDetector != null && shakeDetector.isListening()) {
            shakeDetector.stopListening();
        }
    }

    private void handleActivityShakeTrigger() {
        if (ShakeDetectorHelper.isTestModeEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("🔔 Test Shake Detected!")
                    .setMessage("Accelerometer shake sensor is working properly (Sensitivity: " +
                            ShakeDetectorHelper.getSensitivity(this).toUpperCase() + ").\n\nNo emergency alerts or SMS were dispatched because Test Mode is ON.")
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Intent intent = new Intent(this, SosEmergencyActivity.class);
            intent.putExtra(SosEmergencyActivity.EXTRA_SHAKE_TRIGGERED, true);
            intent.putExtra(SosEmergencyActivity.EXTRA_COUNTDOWN_SECONDS, ShakeDetectorHelper.getCountdownSeconds(this));
            startActivity(intent);
        }
    }

    private void updateJourneyBannerState() {
        if (cardJourneyBanner == null) return;

        if (JourneyMonitoringService.isJourneyActive()) {
            if (tvJourneyBannerTitle != null) {
                String dest = JourneyMonitoringService.getActiveDestination();
                tvJourneyBannerTitle.setText("Active Journey to " + (dest.isEmpty() ? "Destination" : dest));
            }
            if (tvJourneyBannerSubtitle != null) {
                tvJourneyBannerSubtitle.setText("Live tracking active • Tap to check in, extend or end journey");
            }
            if (tvJourneyBannerBadge != null) {
                tvJourneyBannerBadge.setText("LIVE TRACKING");
                tvJourneyBannerBadge.setBackgroundResource(R.drawable.bg_pill_button_pink);
                tvJourneyBannerBadge.setTextColor(ContextCompat.getColor(this, R.color.brand_pink));
            }
        } else {
            if (tvJourneyBannerTitle != null) {
                tvJourneyBannerTitle.setText("Daily Journey Monitoring");
            }
            if (tvJourneyBannerSubtitle != null) {
                tvJourneyBannerSubtitle.setText("Live GPS tracking, periodic check-in timer & guardian alerts");
            }
            if (tvJourneyBannerBadge != null) {
                tvJourneyBannerBadge.setText("PROTECTED");
                tvJourneyBannerBadge.setBackgroundResource(R.drawable.bg_pill_button_lavender);
                tvJourneyBannerBadge.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
            }
        }
    }

    private void setupGreeting() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        String userName = prefs.getString("user_name", "Juveriya");
        if (tvGreeting != null) {
            tvGreeting.setText("Hi, " + userName + " 👋");
        }
    }

    private void setupLocationTracker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location != null && tvHeaderLocationText != null) {
                        try {
                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String locality = addresses.get(0).getLocality();
                                String subLocality = addresses.get(0).getSubLocality();
                                if (subLocality != null && !subLocality.isEmpty()) {
                                    tvHeaderLocationText.setText("📍 " + subLocality + ", " + locality);
                                } else if (locality != null && !locality.isEmpty()) {
                                    tvHeaderLocationText.setText("📍 " + locality);
                                } else {
                                    tvHeaderLocationText.setText(String.format(Locale.US, "📍 %.3f, %.3f", location.getLatitude(), location.getLongitude()));
                                }
                            } else {
                                tvHeaderLocationText.setText(String.format(Locale.US, "📍 %.3f, %.3f", location.getLatitude(), location.getLongitude()));
                            }
                        } catch (Exception e) {
                            tvHeaderLocationText.setText(String.format(Locale.US, "📍 %.3f, %.3f", location.getLatitude(), location.getLongitude()));
                        }
                    } else if (tvHeaderLocationText != null) {
                        tvHeaderLocationText.setText("📍 Bengaluru, Karnataka (Active)");
                    }
                }
            });
        } else if (tvHeaderLocationText != null) {
            tvHeaderLocationText.setText("📍 Enable GPS for live safety");
        }
    }

    private void setupEmergencyActions() {
        // Large Center Hero SOS Button
        if (btnHeroSos != null) {
            btnHeroSos.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, SosEmergencyActivity.class));
            });
        }

        // Quick Call 112 with Confirmation
        if (btnQuickCall112 != null) {
            btnQuickCall112.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Emergency Call 112")
                        .setMessage("Do you want to dial 112 (National Emergency Helpline)?")
                        .setPositiveButton("Call Now", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:112"));
                                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    startActivity(intent);
                                } else {
                                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
                                    startActivity(dialIntent);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Quick Alert Contacts Action
        if (btnQuickAlertContacts != null) {
            btnQuickAlertContacts.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, TrustedContactsActivity.class));
            });
        }

        // Quick Siren Deterrent Action
        if (btnQuickSiren != null) {
            btnQuickSiren.setOnClickListener(v -> {
                sirenManager.toggleSiren();
            });
            btnQuickSiren.setOnLongClickListener(v -> {
                startActivity(new Intent(MainActivity.this, Flashing.class));
                return true;
            });
        }
    }

    private void updateQuickSirenUi(boolean isPlaying) {
        if (btnQuickSiren == null || tvQuickSirenText == null) return;
        if (isPlaying) {
            btnQuickSiren.setBackgroundResource(R.drawable.bg_pill_button_red);
            tvQuickSirenText.setText("🛑 STOP SIREN ALARM (ACTIVE)");
            tvQuickSirenText.setTextColor(ContextCompat.getColor(this, R.color.white));
            if (ivQuickSirenIcon != null) {
                ivQuickSirenIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
            }
        } else {
            btnQuickSiren.setBackgroundResource(R.drawable.bg_pill_button_lavender);
            tvQuickSirenText.setText("🚨 Sound Emergency Siren Alarm");
            tvQuickSirenText.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
            if (ivQuickSirenIcon != null) {
                ivQuickSirenIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_sos_red));
            }
        }
    }

    private void setupServiceCards() {
        // Journey Banner Shortcut
        if (cardJourneyBanner != null) {
            cardJourneyBanner.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, JourneyMonitoringActivity.class)));
        }

        // 1. Safe Route Card
        if (locTrack != null) {
            locTrack.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SafeRouteActivity.class)));
        }

        // 2. Nearby Help Card
        if (cardNearbyHelp != null) {
            cardNearbyHelp.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NearbyHelpActivity.class)));
        }

        // 3. Trusted Contacts Card
        if (settingsCard != null) {
            settingsCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TrustedContactsActivity.class)));
        }

        // 4. Privacy Scanner (Detect Hidden Cameras) Card
        if (privacyScannerCard != null) {
            privacyScannerCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ChoosenActivity.class)));
        }

        // 5. Daily Journey Monitoring Card
        if (liveJourneyCard != null) {
            liveJourneyCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, JourneyMonitoringActivity.class)));
        }

        // 6. Self Defence Learning Card
        if (selfDefCard != null) {
            selfDefCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SelfDef.class)));
        }

        // 7. Women Helpline Card
        if (helplineCard != null) {
            helplineCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Helpline.class)));
        }

        // 8. Safety History Timeline Card
        if (safetyHistoryCard != null) {
            safetyHistoryCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SafetyHistoryActivity.class)));
        }

        // 9. Voice-Activated SOS Card
        if (cardVoiceSos != null) {
            cardVoiceSos.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VoiceSosActivity.class)));
        }

        // 10. Private Evidence Recorder Card
        if (cardEvidenceVault != null) {
            cardEvidenceVault.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EvidenceRecordingActivity.class)));
        }

        // 11. Discreet Mode Decoy Card
        if (cardDiscreetDecoy != null) {
            cardDiscreetDecoy.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DiscreetModeActivity.class)));
        }

        // 12. Community Safety Reports Card
        if (cardCommunityReports != null) {
            cardCommunityReports.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CommunityReportsActivity.class)));
        }

        // 13. Offline Safety Shield Card
        if (cardOfflineShield != null) {
            cardOfflineShield.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, OfflineSafetyActivity.class)));
        }

        // 14. Fake Incoming Call Decoy Card (Feature 2)
        if (cardFakeCall != null) {
            cardFakeCall.setOnClickListener(v -> FakeCallHelper.showSetupDialog(MainActivity.this));
        }

        // "See All ->" shortcut
        if (tvSeeAllServices != null) {
            tvSeeAllServices.setOnClickListener(v -> {
                Toast.makeText(this, "Showing all 14 AtmaRaksha Safety Services", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupHeaderMenu() {
        if (btnNotificationBell != null) {
            btnNotificationBell.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, SafetyHistoryActivity.class));
            });
        }

        if (btnMenuHamburger != null) {
            btnMenuHamburger.setOnClickListener(v -> {
                String[] menuOptions = {
                        "👤 Profile & Medical Details",
                        "🛣️ Daily Journey Monitoring",
                        "🎙️ Voice-Activated SOS",
                        "🎙️ Private Evidence Vault",
                        "🧮 Discreet Decoy Calculator",
                        "📡 Offline Safety Shield",
                        "🗺️ Community Safety Reports",
                        "🛡️ Privacy Scanner (Spy Camera)",
                        "📞 Women Helpline Directory",
                        "🔒 Privacy Policy & Consent",
                        "ℹ️ About AtmaRaksha",
                        "🚪 Log Out"
                };

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("AtmaRaksha Menu")
                        .setItems(menuOptions, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class));
                                    break;
                                case 1:
                                    startActivity(new Intent(MainActivity.this, JourneyMonitoringActivity.class));
                                    break;
                                case 2:
                                    startActivity(new Intent(MainActivity.this, VoiceSosActivity.class));
                                    break;
                                case 3:
                                    startActivity(new Intent(MainActivity.this, EvidenceRecordingActivity.class));
                                    break;
                                case 4:
                                    startActivity(new Intent(MainActivity.this, DiscreetModeActivity.class));
                                    break;
                                case 5:
                                    startActivity(new Intent(MainActivity.this, OfflineSafetyActivity.class));
                                    break;
                                case 6:
                                    startActivity(new Intent(MainActivity.this, CommunityReportsActivity.class));
                                    break;
                                case 7:
                                    startActivity(new Intent(MainActivity.this, ChoosenActivity.class));
                                    break;
                                case 8:
                                    startActivity(new Intent(MainActivity.this, Helpline.class));
                                    break;
                                case 9:
                                    startActivity(new Intent(MainActivity.this, PrivacyPolicyActivity.class));
                                    break;
                                case 10:
                                    startActivity(new Intent(MainActivity.this, AboutUs.class));
                                    break;
                                case 11:
                                    confirmLogout();
                                    break;
                            }
                        })
                        .show();
            });
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of AtmaRaksha?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
                    prefs.edit().putBoolean("is_logged_in", false).apply();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkPermissionsAndPolicy() {
        // 1. Enforce Privacy Policy Acceptance without bypassing consent
        if (!PrivacyPolicyActivity.isPolicyAccepted(this)) {
            Intent policyIntent = new Intent(this, PrivacyPolicyActivity.class);
            policyIntent.putExtra("open_main_on_accept", false);
            startActivity(policyIntent);
            return;
        }

        // 2. Check permissions once without repeatedly annoying the user with popups
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        boolean permissionsPrompted = prefs.getBoolean("permissions_prompted_once", false);
        if (!permissionsPrompted) {
            prefs.edit().putBoolean("permissions_prompted_once", true).apply();
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.SEND_SMS
                    }, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sirenManager != null) {
            sirenManager.removeListener(sirenListener);
        }
    }
}