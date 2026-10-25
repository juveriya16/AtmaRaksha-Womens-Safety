package com.sum.ladybuddy;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Services :- Services in Android are a special component that facilitates an application to run in the background in order
 * to perform long-running operation tasks. The prime aim of a service is to ensure that the application remains
 * active in the background so that the user can operate multiple applications at the same time.
 * A service is started when an application component, such as an activity, starts it by calling startService().
 */

public class ScreenOnOffBackgroundService extends Service {

    /**
     * BroadcastReceiver :- Broadcast in android is the system-wide events that can occur when the device starts,
     * when a message is received on the device or when incoming calls are received, or when a device goes to airplane mode,
     * etc. Broadcast Receivers are used to respond to these system-wide events.
     */

    public BroadcastReceiver screenOnOffReceiver = null;
    private ShakeDetectorHelper shakeDetector = null;

    final static String ACTION = "NotifyServiceAction";
    final static String STOP_SERVICE_BROADCAST_KEY = "StopServiceBroadcastKey";
    final static int RQS_STOP_SERVICE = 1;
    private static final String CHANNEL_SHAKE = "atmaraksha_shake_channel";
    private static final int NOTIF_SHAKE_ID = 911;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkAndRestartShakeListener();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createShakeNotificationChannel();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.setPriority(100);
        screenOnOffReceiver = new ScreenOnOffReceiver();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenOnOffReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(screenOnOffReceiver, intentFilter);
            }
            Log.d(ScreenOnOffReceiver.SCREEN_TOGGLE_TAG, "Service onCreate: screenOnOffReceiver is registered.");
        } catch (Exception e) {
            Log.e(ScreenOnOffReceiver.SCREEN_TOGGLE_TAG, "Failed to register screenOnOffReceiver: " + e.getMessage(), e);
        }

        checkAndRestartShakeListener();
    }

    private void checkAndRestartShakeListener() {
        if (ShakeDetectorHelper.isShakeSosEnabled(this)) {
            if (shakeDetector == null) {
                shakeDetector = new ShakeDetectorHelper(this);
            }
            if (!shakeDetector.isListening()) {
                shakeDetector.startListening(this::onShakeDetected);
            }
        } else if (shakeDetector != null && shakeDetector.isListening()) {
            shakeDetector.stopListening();
        }
    }

    private void onShakeDetected() {
        boolean isTest = ShakeDetectorHelper.isTestModeEnabled(this);
        int countdown = ShakeDetectorHelper.getCountdownSeconds(this);

        if (isTest) {
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Shake-to-SOS (Test)", "Test Shake Detected",
                    "Sensor test confirmed. Sensitivity: " + ShakeDetectorHelper.getSensitivity(this) + ". No emergency alerts dispatched.", "Safe");
            showShakeNotification("🔔 Shake Detected! (Test Mode)", "Sensitivity is working properly. No emergency alerts or SMS were dispatched.", true);
        } else {
            // Intent to launch SosEmergencyActivity
            Intent sosIntent = new Intent(this, SosEmergencyActivity.class);
            sosIntent.putExtra(SosEmergencyActivity.EXTRA_SHAKE_TRIGGERED, true);
            sosIntent.putExtra(SosEmergencyActivity.EXTRA_COUNTDOWN_SECONDS, countdown);
            sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            try {
                startActivity(sosIntent);
            } catch (Exception e) {
                Log.w(ScreenOnOffReceiver.SCREEN_TOGGLE_TAG, "Background activity launch restricted: " + e.getMessage());
            }

            // Also post high-priority alert with FullScreenIntent for lockscreen / Android 10+ compliance
            showShakeNotification("⚠️ Shake-to-SOS Triggered!", "Deliberate shake detected. Tap to cancel within " + countdown + "s.", false);
        }
    }

    private void showShakeNotification(String title, String message, boolean isTest) {
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent intent = isTest ? new Intent(this, ProfileSettingsActivity.class) : new Intent(this, SosEmergencyActivity.class);
        if (!isTest) {
            intent.putExtra(SosEmergencyActivity.EXTRA_SHAKE_TRIGGERED, true);
            intent.putExtra(SosEmergencyActivity.EXTRA_COUNTDOWN_SECONDS, ShakeDetectorHelper.getCountdownSeconds(this));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 101, intent, flags);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, CHANNEL_SHAKE)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setDefaults(android.app.Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);

        if (!isTest) {
            builder.setFullScreenIntent(pi, true);
        }

        nm.notify(NOTIF_SHAKE_ID, builder.build());
    }

    private void createShakeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    CHANNEL_SHAKE,
                    "Shake-to-SOS Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Immediate alerts when deliberate phone shake is detected");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 400, 200, 400});
            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (shakeDetector != null) {
            shakeDetector.stopListening();
            shakeDetector = null;
        }

        if (screenOnOffReceiver != null) {
            try {
                unregisterReceiver(screenOnOffReceiver);
                Log.d(ScreenOnOffReceiver.SCREEN_TOGGLE_TAG, "Service onDestroy: screenOnOffReceiver is unregistered.");
            } catch (Exception e) {
                Log.e(ScreenOnOffReceiver.SCREEN_TOGGLE_TAG, "Failed to unregister screenOnOffReceiver: " + e.getMessage(), e);
            }
            screenOnOffReceiver = null;
        }
    }
}