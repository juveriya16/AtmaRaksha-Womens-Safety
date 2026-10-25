package com.sum.ladybuddy;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvAvatar, tvName, tvPhone, tvBlood;
    private Button btnEditProfile, btnLogout;
    private SwitchCompat switchNight, switchPowerBtn, switchSiren;
    private SwitchCompat switchShakeSos, switchShakeTestMode, switchBatteryAlerts, switchBatterySms;
    private View rowShakeSensitivity, rowShakeCountdown;
    private TextView tvShakeSensitivityValue, tvShakeCountdownValue;
    private TextView linkContacts, linkHelplines, linkSelfDef, linkAbout, linkClearHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        btnBack = findViewById(R.id.btn_back_profile);
        tvAvatar = findViewById(R.id.tv_avatar_initials);
        tvName = findViewById(R.id.tv_profile_name);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvBlood = findViewById(R.id.tv_profile_blood);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);

        switchNight = findViewById(R.id.switch_pref_night);
        switchPowerBtn = findViewById(R.id.switch_pref_power_btn);
        switchSiren = findViewById(R.id.switch_pref_siren);

        // Feature 1: Shake-to-SOS Views
        switchShakeSos = findViewById(R.id.switch_pref_shake_sos);
        rowShakeSensitivity = findViewById(R.id.row_shake_sensitivity);
        tvShakeSensitivityValue = findViewById(R.id.tv_shake_sensitivity_value);
        rowShakeCountdown = findViewById(R.id.row_shake_countdown);
        tvShakeCountdownValue = findViewById(R.id.tv_shake_countdown_value);
        switchShakeTestMode = findViewById(R.id.switch_pref_shake_test_mode);

        // Feature 3: Journey Low-Battery Views
        switchBatteryAlerts = findViewById(R.id.switch_pref_battery_alerts);
        switchBatterySms = findViewById(R.id.switch_pref_battery_sms);

        linkContacts = findViewById(R.id.link_trusted_contacts);
        linkHelplines = findViewById(R.id.link_helplines);
        linkSelfDef = findViewById(R.id.link_self_def);
        linkAbout = findViewById(R.id.link_about_policy);
        linkClearHistory = findViewById(R.id.link_clear_history);

        btnBack.setOnClickListener(v -> finish());

        loadUserProfile();

        // Safety Preference Switches
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        switchNight.setChecked(prefs.getBoolean("night_safety_mode", false));
        switchPowerBtn.setChecked(prefs.getBoolean("power_btn_sos_enabled", true));
        switchSiren.setChecked(prefs.getBoolean("siren_on_sos", true));

        switchNight.setOnCheckedChangeListener((bv, isChecked) -> {
            prefs.edit().putBoolean("night_safety_mode", isChecked).apply();
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Night Safety", isChecked ? "Night Mode Activated" : "Night Mode Standby",
                    isChecked ? "Armed high-decibel alarm & rapid GPS" : "Returned to standard safety mode", "Active");
            Toast.makeText(this, isChecked ? "🌙 Night Safety Mode armed" : "Night Safety Mode deactivated", Toast.LENGTH_SHORT).show();
        });

        switchPowerBtn.setOnCheckedChangeListener((bv, isChecked) -> {
            prefs.edit().putBoolean("power_btn_sos_enabled", isChecked).apply();
            try {
                if (isChecked) {
                    startService(new Intent(getApplicationContext(), ScreenOnOffBackgroundService.class));
                    Toast.makeText(this, "Triple-click power button SOS listener activated", Toast.LENGTH_SHORT).show();
                } else {
                    stopService(new Intent(getApplicationContext(), ScreenOnOffBackgroundService.class));
                    Toast.makeText(this, "Triple-click power button listener stopped", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ignored) {}
        });

        switchSiren.setOnCheckedChangeListener((bv, isChecked) -> {
            prefs.edit().putBoolean("siren_on_sos", isChecked).apply();
        });

        setupShakeSosPreferences();
        setupBatteryProtectionPreferences();

        // Quick Links
        linkContacts.setOnClickListener(v -> startActivity(new Intent(this, TrustedContactsActivity.class)));
        linkHelplines.setOnClickListener(v -> startActivity(new Intent(this, Helpline.class)));
        linkSelfDef.setOnClickListener(v -> startActivity(new Intent(this, SelfDef.class)));
        linkAbout.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        if (linkClearHistory != null) {
            linkClearHistory.setOnClickListener(v -> promptClearHistory());
        }

        // Edit Profile Dialog
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Logout
        btnLogout.setOnClickListener(v -> promptLogout());

        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.PROFILE);
    }

    private void setupShakeSosPreferences() {
        boolean sensorAvailable = ShakeDetectorHelper.isSensorAvailable(this);
        if (!sensorAvailable) {
            if (switchShakeSos != null) {
                switchShakeSos.setChecked(false);
                switchShakeSos.setEnabled(false);
            }
            if (rowShakeSensitivity != null) rowShakeSensitivity.setEnabled(false);
            if (rowShakeCountdown != null) rowShakeCountdown.setEnabled(false);
            if (switchShakeTestMode != null) switchShakeTestMode.setEnabled(false);
            if (tvShakeSensitivityValue != null) tvShakeSensitivityValue.setText("No Sensor");
            return;
        }

        boolean shakeEnabled = ShakeDetectorHelper.isShakeSosEnabled(this);
        if (switchShakeSos != null) {
            switchShakeSos.setChecked(shakeEnabled);
            switchShakeSos.setOnCheckedChangeListener((bv, isChecked) -> {
                ShakeDetectorHelper.setShakeSosEnabled(this, isChecked);
                SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
                dbHelper.logEvent("Settings", isChecked ? "Shake-to-SOS Armed" : "Shake-to-SOS Disabled",
                        "User toggled Shake-to-SOS to " + (isChecked ? "Active" : "Disabled"), "Settings");

                try {
                    startService(new Intent(getApplicationContext(), ScreenOnOffBackgroundService.class));
                } catch (Exception ignored) {}

                Toast.makeText(this, isChecked ? "📳 Shake-to-SOS armed" : "Shake-to-SOS disabled", Toast.LENGTH_SHORT).show();
            });
        }

        updateSensitivityUi();
        if (rowShakeSensitivity != null) {
            rowShakeSensitivity.setOnClickListener(v -> showSensitivityDialog());
        }

        updateCountdownUi();
        if (rowShakeCountdown != null) {
            rowShakeCountdown.setOnClickListener(v -> showCountdownDialog());
        }

        if (switchShakeTestMode != null) {
            switchShakeTestMode.setChecked(ShakeDetectorHelper.isTestModeEnabled(this));
            switchShakeTestMode.setOnCheckedChangeListener((bv, isChecked) -> {
                ShakeDetectorHelper.setTestModeEnabled(this, isChecked);
                if (isChecked) {
                    Toast.makeText(this, "🧪 Test Shake Mode ON. Shake phone to test without sending real alerts.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Real SOS armed for deliberate shake triggers.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateSensitivityUi() {
        if (tvShakeSensitivityValue == null) return;
        String sens = ShakeDetectorHelper.getSensitivity(this);
        if ("high".equalsIgnoreCase(sens)) {
            tvShakeSensitivityValue.setText("High ▾");
        } else if ("low".equalsIgnoreCase(sens)) {
            tvShakeSensitivityValue.setText("Low ▾");
        } else {
            tvShakeSensitivityValue.setText("Medium ▾");
        }
    }

    private void showSensitivityDialog() {
        final String[] options = {"Medium (Recommended)", "Low (Heavy deliberate shake)", "High (Sensitive)"};
        final String[] values = {"medium", "low", "high"};

        String current = ShakeDetectorHelper.getSensitivity(this);
        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Shake Detection Sensitivity")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    ShakeDetectorHelper.setSensitivity(this, values[which]);
                    updateSensitivityUi();
                    Toast.makeText(this, "Sensitivity set to " + options[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCountdownUi() {
        if (tvShakeCountdownValue == null) return;
        int seconds = ShakeDetectorHelper.getCountdownSeconds(this);
        tvShakeCountdownValue.setText(seconds + "s ▾");
    }

    private void showCountdownDialog() {
        final String[] options = {"5 Seconds (Recommended)", "3 Seconds (Quick)", "10 Seconds (Extra cancellation buffer)"};
        final int[] values = {5, 3, 10};

        int current = ShakeDetectorHelper.getCountdownSeconds(this);
        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Shake-to-SOS Cancellation Window")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    ShakeDetectorHelper.setCountdownSeconds(this, values[which]);
                    updateCountdownUi();
                    Toast.makeText(this, "Cancellation countdown set to " + values[which] + " seconds", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBatteryProtectionPreferences() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);

        if (switchBatteryAlerts != null) {
            switchBatteryAlerts.setChecked(prefs.getBoolean("journey_battery_alerts_enabled", true));
            switchBatteryAlerts.setOnCheckedChangeListener((bv, isChecked) -> {
                prefs.edit().putBoolean("journey_battery_alerts_enabled", isChecked).apply();
                Toast.makeText(this, isChecked ? "🔋 Low-Battery Warnings active during journeys" : "Low-Battery Warnings disabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (switchBatterySms != null) {
            switchBatterySms.setChecked(prefs.getBoolean("journey_battery_sms_contacts", true));
            switchBatterySms.setOnCheckedChangeListener((bv, isChecked) -> {
                prefs.edit().putBoolean("journey_battery_sms_contacts", isChecked).apply();
                Toast.makeText(this, isChecked ? "📨 Guardians will receive SMS when battery drops" : "Guardian battery SMS disabled", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        String name = prefs.getString("user_name", "Ananya Sharma");
        String phone = prefs.getString("user_phone", "+91 98765 43210");
        String blood = prefs.getString("user_blood_group", "O+");

        tvName.setText(name);
        tvPhone.setText(phone);
        tvBlood.setText("Blood: " + blood);

        // Generate avatar initials
        if (!TextUtils.isEmpty(name)) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                String initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
                tvAvatar.setText(initials);
            } else if (parts.length == 1 && parts[0].length() > 0) {
                tvAvatar.setText(parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase());
            }
        }
    }

    private void showEditProfileDialog() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Personal & Medical Safety Profile");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final EditText inputName = new EditText(this);
        inputName.setHint("Full Name (e.g. Ananya Sharma)");
        inputName.setText(prefs.getString("user_name", ""));
        inputName.setPadding(24, 24, 24, 24);
        inputName.setBackgroundResource(R.drawable.bg_input_box);
        inputName.setTextColor(getResources().getColor(R.color.text_primary));
        inputName.setHintTextColor(getResources().getColor(R.color.text_tertiary));
        layout.addView(inputName);

        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 16, 0, 0);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("Primary Phone (e.g. +91 98765 43210)");
        inputPhone.setText(prefs.getString("user_phone", ""));
        inputPhone.setPadding(24, 24, 24, 24);
        inputPhone.setBackgroundResource(R.drawable.bg_input_box);
        inputPhone.setTextColor(getResources().getColor(R.color.text_primary));
        inputPhone.setHintTextColor(getResources().getColor(R.color.text_tertiary));
        inputPhone.setLayoutParams(lp);
        layout.addView(inputPhone);

        final EditText inputBlood = new EditText(this);
        inputBlood.setHint("Blood Group (e.g. O+, B+, A-)");
        inputBlood.setText(prefs.getString("user_blood_group", ""));
        inputBlood.setPadding(24, 24, 24, 24);
        inputBlood.setBackgroundResource(R.drawable.bg_input_box);
        inputBlood.setTextColor(getResources().getColor(R.color.text_primary));
        inputBlood.setHintTextColor(getResources().getColor(R.color.text_tertiary));
        inputBlood.setLayoutParams(lp);
        layout.addView(inputBlood);

        final EditText inputMedical = new EditText(this);
        inputMedical.setHint("Medical Notes / Allergies (Optional)");
        inputMedical.setText(prefs.getString("user_medical_notes", ""));
        inputMedical.setPadding(24, 24, 24, 24);
        inputMedical.setBackgroundResource(R.drawable.bg_input_box);
        inputMedical.setTextColor(getResources().getColor(R.color.text_primary));
        inputMedical.setHintTextColor(getResources().getColor(R.color.text_tertiary));
        inputMedical.setLayoutParams(lp);
        layout.addView(inputMedical);

        builder.setView(layout);
        builder.setPositiveButton("Save Details", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = inputName.getText().toString().trim();
                String newPhone = inputPhone.getText().toString().trim();
                String newBlood = inputBlood.getText().toString().trim();
                String newMedical = inputMedical.getText().toString().trim();

                SharedPreferences.Editor editor = prefs.edit();
                if (!TextUtils.isEmpty(newName)) editor.putString("user_name", newName);
                if (!TextUtils.isEmpty(newPhone)) editor.putString("user_phone", newPhone);
                if (!TextUtils.isEmpty(newBlood)) editor.putString("user_blood_group", newBlood);
                editor.putString("user_medical_notes", newMedical);
                editor.apply();

                loadUserProfile();
                Toast.makeText(ProfileSettingsActivity.this, "Safety profile updated successfully!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void promptClearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Safety History?");
        builder.setMessage("This will wipe all local emergency event records and safety activity entries from this device.");
        builder.setPositiveButton("Clear Log", (d, w) -> {
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.clearHistory();
            Toast.makeText(this, "Safety activity history cleared.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void promptLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Out");
        builder.setMessage("Are you sure you want to log out? Your configured emergency numbers and safety logs will be safely preserved on this device.");
        builder.setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
                prefs.edit().putBoolean("is_logged_in", false).apply();

                Intent intent = new Intent(ProfileSettingsActivity.this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.PROFILE);
    }
}
