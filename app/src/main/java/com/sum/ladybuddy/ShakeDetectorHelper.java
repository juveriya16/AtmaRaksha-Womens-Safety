package com.sum.ladybuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * ShakeDetectorHelper
 * Handles deliberate phone shake detection using the accelerometer.
 * Features:
 * - Dynamic sensitivity presets (Low, Medium, High)
 * - Multi-directional peak detection (prevents false triggers during walking/running)
 * - Debounce time window
 * - Test mode detection
 * - Cancellation countdown preferences
 */
public class ShakeDetectorHelper implements SensorEventListener {

    private static final String TAG = "ShakeDetector";

    public static final String PREF_SHAKE_ENABLED = "shake_sos_enabled";
    public static final String PREF_SHAKE_SENSITIVITY = "shake_sos_sensitivity"; // "low", "medium", "high"
    public static final String PREF_SHAKE_COUNTDOWN = "shake_sos_countdown_seconds"; // 3, 5, 10
    public static final String PREF_SHAKE_TEST_MODE = "shake_sos_test_mode"; // boolean

    // Sensitivity G-force thresholds
    public static final float THRESHOLD_HIGH = 2.1f;    // Highly sensitive (easier to trigger)
    public static final float THRESHOLD_MEDIUM = 2.7f;  // Balanced / Recommended
    public static final float THRESHOLD_LOW = 3.4f;     // Requires deliberate, heavy shaking

    // Direction changes required within time window
    private static final int REQUIRED_DIRECTION_CHANGES_MEDIUM = 3;
    private static final int REQUIRED_DIRECTION_CHANGES_LOW = 4;
    private static final int REQUIRED_DIRECTION_CHANGES_HIGH = 2;

    private static final int SHAKE_WINDOW_MS = 600;     // Time window for consecutive reversals
    private static final int DEBOUNCE_COOLDOWN_MS = 3000; // Minimum time between shake events

    public interface OnShakeListener {
        void onShake();
    }

    private final Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private OnShakeListener shakeListener;

    private float lastX = 0f, lastY = 0f, lastZ = 0f;
    private long lastTime = 0;
    private int directionChangeCount = 0;
    private long firstDirectionChangeTime = 0;
    private long lastShakeTimestamp = 0;
    private boolean isListening = false;

    public ShakeDetectorHelper(Context context) {
        this.context = context.getApplicationContext();
        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public static boolean isSensorAvailable(Context context) {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sm != null && sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }

    public static boolean isShakeSosEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_SHAKE_ENABLED, false);
    }

    public static void setShakeSosEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SHAKE_ENABLED, enabled).apply();
    }

    public static String getSensitivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        return prefs.getString(PREF_SHAKE_SENSITIVITY, "medium");
    }

    public static void setSensitivity(Context context, String sensitivity) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_SHAKE_SENSITIVITY, sensitivity).apply();
    }

    public static int getCountdownSeconds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        return prefs.getInt(PREF_SHAKE_COUNTDOWN, 5);
    }

    public static void setCountdownSeconds(Context context, int seconds) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_SHAKE_COUNTDOWN, seconds).apply();
    }

    public static boolean isTestModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_SHAKE_TEST_MODE, false);
    }

    public static void setTestModeEnabled(Context context, boolean testMode) {
        SharedPreferences prefs = context.getSharedPreferences("demo", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SHAKE_TEST_MODE, testMode).apply();
    }

    public boolean startListening(OnShakeListener listener) {
        if (accelerometer == null || sensorManager == null) {
            Log.w(TAG, "Accelerometer not available on this device");
            return false;
        }

        this.shakeListener = listener;
        resetState();
        // SENSOR_DELAY_UI gives a good balance between responsiveness and battery conservation
        isListening = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        Log.d(TAG, "Shake detection registered: " + isListening);
        return isListening;
    }

    public void stopListening() {
        if (sensorManager != null && isListening) {
            sensorManager.unregisterListener(this);
            isListening = false;
            Log.d(TAG, "Shake detection stopped");
        }
    }

    public boolean isListening() {
        return isListening;
    }

    private void resetState() {
        lastX = 0f;
        lastY = 0f;
        lastZ = 0f;
        lastTime = 0;
        directionChangeCount = 0;
        firstDirectionChangeTime = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        long now = System.currentTimeMillis();

        // Check debounce cooldown
        if (now - lastShakeTimestamp < DEBOUNCE_COOLDOWN_MS) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (lastTime == 0) {
            lastTime = now;
            lastX = x;
            lastY = y;
            lastZ = z;
            return;
        }

        long timeDelta = now - lastTime;
        if (timeDelta < 50) {
            // Skip sub-50ms jitter to avoid sensor noise
            return;
        }
        lastTime = now;

        // Determine active threshold & required direction changes based on sensitivity setting
        String sensitivity = getSensitivity(context);
        float threshold;
        int requiredChanges;

        if ("high".equalsIgnoreCase(sensitivity)) {
            threshold = THRESHOLD_HIGH;
            requiredChanges = REQUIRED_DIRECTION_CHANGES_HIGH;
        } else if ("low".equalsIgnoreCase(sensitivity)) {
            threshold = THRESHOLD_LOW;
            requiredChanges = REQUIRED_DIRECTION_CHANGES_LOW;
        } else {
            // Default medium
            threshold = THRESHOLD_MEDIUM;
            requiredChanges = REQUIRED_DIRECTION_CHANGES_MEDIUM;
        }

        // Calculate total acceleration magnitude normalized by earth gravity (1g = resting)
        float totalAccel = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

        // Check if movement exceeds threshold and reversed direction
        float deltaX = Math.abs(x - lastX);
        float deltaY = Math.abs(y - lastY);
        float deltaZ = Math.abs(z - lastZ);

        lastX = x;
        lastY = y;
        lastZ = z;

        if (totalAccel >= threshold || deltaX > (threshold * 3.5f) || deltaY > (threshold * 3.5f) || deltaZ > (threshold * 3.5f)) {
            if (firstDirectionChangeTime == 0) {
                firstDirectionChangeTime = now;
            }

            // Check if current reversal is within the valid shake time window
            if (now - firstDirectionChangeTime < SHAKE_WINDOW_MS) {
                directionChangeCount++;
                if (directionChangeCount >= requiredChanges) {
                    lastShakeTimestamp = now;
                    resetState();
                    Log.i(TAG, "Deliberate shake confirmed! G-force: " + totalAccel + ", sensitivity: " + sensitivity);
                    if (shakeListener != null) {
                        shakeListener.onShake();
                    }
                }
            } else {
                // Window expired, reset counter for new movement
                firstDirectionChangeTime = now;
                directionChangeCount = 1;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }
}
