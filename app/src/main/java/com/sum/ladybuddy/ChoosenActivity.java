package com.sum.ladybuddy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class ChoosenActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREF_NAME = "AtmaRaksha_Scanner_Prefs";
    private static final String KEY_SMOKE = "check_smoke";
    private static final String KEY_MIRROR = "check_mirror";
    private static final String KEY_SOCKETS = "check_sockets";
    private static final String KEY_CLOCK = "check_clock";
    private static final String KEY_BATHROOM = "check_bathroom";

    private SensorManager sensorManager;
    private Sensor magneticSensor;
    private boolean isScanning = true;

    private TextView tvReadingVal;
    private TextView tvStatusBadge;
    private ProgressBar pbGauge;
    private TextView tvSensorX, tvSensorY, tvSensorZ;
    private Button btnToggleScan;
    private ImageView btnBack;

    private CheckBox cbSmoke, cbMirror, cbSockets, cbClock, cbBathroom;
    private CardView cardChangingRoom, cardBedroom;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choosen);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Bind UI Elements
        tvReadingVal = findViewById(R.id.tv_sensor_reading_val);
        tvStatusBadge = findViewById(R.id.tv_sensor_status_badge);
        pbGauge = findViewById(R.id.pb_magnetic_gauge);
        tvSensorX = findViewById(R.id.tv_sensor_x);
        tvSensorY = findViewById(R.id.tv_sensor_y);
        tvSensorZ = findViewById(R.id.tv_sensor_z);
        btnToggleScan = findViewById(R.id.btn_toggle_scan);
        btnBack = findViewById(R.id.btn_back_scanner);

        cbSmoke = findViewById(R.id.cb_check_smoke);
        cbMirror = findViewById(R.id.cb_check_mirror);
        cbSockets = findViewById(R.id.cb_check_sockets);
        cbClock = findViewById(R.id.cb_check_clock);
        cbBathroom = findViewById(R.id.cb_check_bathroom);

        cardChangingRoom = findViewById(R.id.card_guide_changing_room);
        cardBedroom = findViewById(R.id.card_guide_bedroom);

        // Back Button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Initialize Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (magneticSensor == null) {
            if (tvStatusBadge != null) {
                tvStatusBadge.setText("⚠️ Magnetometer Not Found on Device");
            }
            if (btnToggleScan != null) {
                btnToggleScan.setEnabled(false);
                btnToggleScan.setText("Sensor Unavailable");
            }
        }

        // Toggle Sensor Scanning
        if (btnToggleScan != null) {
            btnToggleScan.setOnClickListener(v -> {
                if (isScanning) {
                    stopScanning();
                    btnToggleScan.setText("Resume Sensor Scan");
                    if (tvStatusBadge != null) {
                        tvStatusBadge.setText("⏸️ Sensor Paused");
                    }
                } else {
                    startScanning();
                    btnToggleScan.setText("Pause Sensor Scan");
                }
            });
        }

        // Setup Checkbox Persistence
        setupChecklistPersistence();

        // Guide Navigation
        if (cardChangingRoom != null) {
            cardChangingRoom.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(ChoosenActivity.this, ChangingRoom.class));
                } catch (Exception e) {
                    Toast.makeText(ChoosenActivity.this, "Changing room guide opening...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (cardBedroom != null) {
            cardBedroom.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(ChoosenActivity.this, Bedroom.class));
                } catch (Exception e) {
                    Toast.makeText(ChoosenActivity.this, "Bedroom guide opening...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupChecklistPersistence() {
        if (cbSmoke != null) {
            cbSmoke.setChecked(prefs.getBoolean(KEY_SMOKE, false));
            cbSmoke.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(KEY_SMOKE, isChecked).apply());
        }
        if (cbMirror != null) {
            cbMirror.setChecked(prefs.getBoolean(KEY_MIRROR, false));
            cbMirror.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(KEY_MIRROR, isChecked).apply());
        }
        if (cbSockets != null) {
            cbSockets.setChecked(prefs.getBoolean(KEY_SOCKETS, false));
            cbSockets.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(KEY_SOCKETS, isChecked).apply());
        }
        if (cbClock != null) {
            cbClock.setChecked(prefs.getBoolean(KEY_CLOCK, false));
            cbClock.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(KEY_CLOCK, isChecked).apply());
        }
        if (cbBathroom != null) {
            cbBathroom.setChecked(prefs.getBoolean(KEY_BATHROOM, false));
            cbBathroom.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(KEY_BATHROOM, isChecked).apply());
        }
    }

    private void startScanning() {
        if (sensorManager != null && magneticSensor != null) {
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
            isScanning = true;
        }
    }

    private void stopScanning() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            isScanning = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScanning && magneticSensor != null) {
            startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Magnetic field magnitude in microteslas (μT)
            double magnitude = Math.sqrt(x * x + y * y + z * z);

            if (tvReadingVal != null) {
                tvReadingVal.setText(String.format(Locale.getDefault(), "%.1f", magnitude));
            }

            if (tvSensorX != null) tvSensorX.setText(String.format(Locale.getDefault(), "X: %.1f", x));
            if (tvSensorY != null) tvSensorY.setText(String.format(Locale.getDefault(), "Y: %.1f", y));
            if (tvSensorZ != null) tvSensorZ.setText(String.format(Locale.getDefault(), "Z: %.1f", z));

            int progress = (int) Math.min(150, Math.round(magnitude));
            if (pbGauge != null) {
                pbGauge.setProgress(progress);
            }

            if (tvStatusBadge != null) {
                if (magnitude < 70) {
                    tvStatusBadge.setText("🟢 Ambient Baseline (Normal)");
                    tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
                } else if (magnitude < 110) {
                    tvStatusBadge.setText("🟡 Elevated Field (Electronic Proximity)");
                    tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_caution));
                } else {
                    tvStatusBadge.setText("🔴 High Magnetic Flux (Inspect Hardware)");
                    tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_emergency));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op for magnetometer reading
    }
}