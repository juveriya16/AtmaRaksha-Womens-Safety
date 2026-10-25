package com.sum.ladybuddy;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JourneyMonitoringService extends Service {

    private static final String TAG = "JourneyService";

    public static final String ACTION_START_JOURNEY = "com.sum.ladybuddy.ACTION_START_JOURNEY";
    public static final String ACTION_CHECKIN_SAFE = "com.sum.ladybuddy.ACTION_CHECKIN_SAFE";
    public static final String ACTION_EXTEND_TIMER = "com.sum.ladybuddy.ACTION_EXTEND_TIMER";
    public static final String ACTION_END_JOURNEY = "com.sum.ladybuddy.ACTION_END_JOURNEY";
    public static final String ACTION_EMERGENCY_SOS = "com.sum.ladybuddy.ACTION_EMERGENCY_SOS";

    public static final String ACTION_JOURNEY_BROADCAST = "com.sum.ladybuddy.JOURNEY_STATUS_UPDATE";

    public static final String EXTRA_DESTINATION = "extra_destination";
    public static final String EXTRA_ORIGIN = "extra_origin";
    public static final String EXTRA_ETA_MINUTES = "extra_eta_minutes";
    public static final String EXTRA_CHECKIN_INTERVAL_MINUTES = "extra_checkin_interval_minutes";

    private static final String CHANNEL_ID_JOURNEY = "atmaraksha_journey_channel";
    private static final String CHANNEL_ID_ALERT = "atmaraksha_journey_alert_channel";
    private static final int NOTIFICATION_ID_FOREGROUND = 801;
    private static final int NOTIFICATION_ID_ESCALATION = 802;
    private static final int NOTIFICATION_ID_BATTERY = 809;

    private BroadcastReceiver batteryReceiver;
    private final Set<Integer> notifiedBatteryThresholds = new HashSet<>();

    public static boolean isJourneyRunning = false;
    public static String currentDestination = "";
    public static String currentOrigin = "";
    public static String currentStatus = "ACTIVE"; // ACTIVE, CHECK_IN_REQUIRED, ALERT_SENT, COMPLETED, CANCELLED
    public static long secondsRemaining = 0;
    public static long checkinSecondsRemaining = 0;
    public static int checkinsCompletedCount = 0;
    public static int missedCheckinsCount = 0;
    public static double lastLatitude = 0.0;
    public static double lastLongitude = 0.0;
    public static long journeyDbId = -1;

    public static boolean isJourneyActive() {
        return isJourneyRunning;
    }

    public static String getActiveDestination() {
        return currentDestination != null ? currentDestination : "";
    }

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private CountDownTimer journeyTimer;
    private CountDownTimer checkinTimer;
    private CountDownTimer gracePeriodTimer;
    private boolean isGracePeriodActive = false;

    private int totalEtaMinutes = 30;
    private int checkinIntervalMinutes = 15;
    private String startTimeFormatted = "";

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START_JOURNEY.equals(action)) {
            String dest = intent.getStringExtra(EXTRA_DESTINATION);
            String origin = intent.getStringExtra(EXTRA_ORIGIN);
            int eta = intent.getIntExtra(EXTRA_ETA_MINUTES, 30);
            int interval = intent.getIntExtra(EXTRA_CHECKIN_INTERVAL_MINUTES, 15);

            startJourneyMonitoring(origin, dest, eta, interval);
        } else if (ACTION_CHECKIN_SAFE.equals(action)) {
            handleUserCheckinSafe();
        } else if (ACTION_EXTEND_TIMER.equals(action)) {
            extendJourneyTimer(15);
        } else if (ACTION_END_JOURNEY.equals(action)) {
            endJourney(true);
        } else if (ACTION_EMERGENCY_SOS.equals(action)) {
            triggerEmergencySosFromJourney();
        }

        return START_STICKY;
    }

    private void startJourneyMonitoring(String origin, String dest, int etaMinutes, int checkinInterval) {
        isJourneyRunning = true;
        currentOrigin = !TextUtils.isEmpty(origin) ? origin : "Current Location";
        currentDestination = !TextUtils.isEmpty(dest) ? dest : "Destination";
        totalEtaMinutes = Math.max(5, etaMinutes);
        checkinIntervalMinutes = Math.max(3, checkinInterval);
        currentStatus = "ACTIVE";
        checkinsCompletedCount = 0;
        missedCheckinsCount = 0;
        isGracePeriodActive = false;

        secondsRemaining = totalEtaMinutes * 60L;
        checkinSecondsRemaining = checkinIntervalMinutes * 60L;
        startTimeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        // Log journey entry to database
        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        JourneyRecord record = new JourneyRecord(
                0, currentOrigin, currentDestination, startTimeFormatted, "",
                totalEtaMinutes, totalEtaMinutes + " min", "ACTIVE", 0, 0,
                TrustedContactsManager.getSelectedMonitoringSummary(this),
                "Live Journey Monitoring Active"
        );
        journeyDbId = dbHelper.insertJourney(record);
        dbHelper.logEvent("Journey Started", "Monitoring to " + currentDestination,
                "Started trip from " + currentOrigin + " (ETA " + totalEtaMinutes + " min). Safety check-in every " + checkinIntervalMinutes + " min.",
                "Active");

        // Start Foreground Service with sticky notification
        startForeground(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification());

        // Start location updates
        startLocationUpdates();

        // Start Journey Timers
        startTimers();

        // Start Journey Battery Monitoring (Feature 3)
        notifiedBatteryThresholds.clear();
        registerBatteryMonitor();

        broadcastJourneyUpdate();
    }

    private void startTimers() {
        if (journeyTimer != null) journeyTimer.cancel();
        if (checkinTimer != null) checkinTimer.cancel();
        if (gracePeriodTimer != null) gracePeriodTimer.cancel();

        // Check-in timer countdown
        checkinTimer = new CountDownTimer(checkinSecondsRemaining * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                checkinSecondsRemaining = millisUntilFinished / 1000L;
                if (secondsRemaining > 0) secondsRemaining--;

                // If 2 minutes remaining, send a friendly pre-reminder
                if (checkinSecondsRemaining == 120) {
                    sendPreCheckinNotification();
                }

                updateForegroundNotification();
                broadcastJourneyUpdate();
            }

            @Override
            public void onFinish() {
                checkinSecondsRemaining = 0;
                startCheckinEscalation();
            }
        }.start();
    }

    private void sendPreCheckinNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent safeIntent = new Intent(this, JourneyMonitoringService.class);
        safeIntent.setAction(ACTION_CHECKIN_SAFE);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent piSafe = PendingIntent.getService(this, 1, safeIntent, piFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_JOURNEY)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle("⏰ AtmaRaksha Check-In in 2 Minutes")
                .setContentText("Keep your safety circle assured. Tap 'I\\'m Safe' anytime.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_shield_check, "I'm Safe", piSafe);

        nm.notify(NOTIFICATION_ID_FOREGROUND + 5, builder.build());
    }

    private void startCheckinEscalation() {
        currentStatus = "CHECK_IN_REQUIRED";
        isGracePeriodActive = true;
        broadcastJourneyUpdate();

        // Phase 1: High priority alert notification with alarm sound & vibration
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            Intent openAppIntent = new Intent(this, JourneyMonitoringActivity.class);
            int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent piOpen = PendingIntent.getActivity(this, 0, openAppIntent, piFlags);

            Intent safeIntent = new Intent(this, JourneyMonitoringService.class);
            safeIntent.setAction(ACTION_CHECKIN_SAFE);
            PendingIntent piSafe = PendingIntent.getService(this, 11, safeIntent, piFlags);

            Intent sosIntent = new Intent(this, JourneyMonitoringService.class);
            sosIntent.setAction(ACTION_EMERGENCY_SOS);
            PendingIntent piSos = PendingIntent.getService(this, 12, sosIntent, piFlags);

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder alertBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
                    .setSmallIcon(R.drawable.ic_notification_bell)
                    .setContentTitle("⚠️ AtmaRaksha Check-In Required!")
                    .setContentText("Are you safe? 60-second grace countdown active before guardian alert.")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 500, 250, 500, 250, 500})
                    .setContentIntent(piOpen)
                    .addAction(R.drawable.ic_shield_check, "I'm Safe", piSafe)
                    .addAction(R.drawable.ic_emergency_sos, "Trigger SOS", piSos)
                    .setAutoCancel(false)
                    .setOngoing(true);

            nm.notify(NOTIFICATION_ID_ESCALATION, alertBuilder.build());
        }

        // Phase 2: Start 60-second grace period countdown
        gracePeriodTimer = new CountDownTimer(60000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                checkinSecondsRemaining = millisUntilFinished / 1000L;
                broadcastJourneyUpdate();
            }

            @Override
            public void onFinish() {
                isGracePeriodActive = false;
                executeGuardianEscalationAlert();
            }
        }.start();
    }

    private void executeGuardianEscalationAlert() {
        currentStatus = "ALERT_SENT";
        missedCheckinsCount++;

        // Dismiss escalation countdown notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID_ESCALATION);
        }

        // Notify selected trusted contacts via SMS with last known location
        List<String> guardians = TrustedContactsManager.getSelectedMonitoringNumbers(this);
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        String locPart = (lastLatitude != 0.0 && lastLongitude != 0.0)
                ? (" Last GPS: https://maps.google.com/?q=" + lastLatitude + "," + lastLongitude)
                : " GPS: Fix pending.";

        String escalationMessage = "[AtmaRaksha Safety Notice] " +
                "Precautionary Alert: The user missed their safety check-in on the journey to " + currentDestination +
                " at " + timeStr + "." + locPart +
                " Note: Missed check-ins do not prove an emergency. Please call or verify their safety.";

        boolean hasDirectSms = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        int dispatched = 0;

        if (hasDirectSms && !guardians.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            for (String phone : guardians) {
                try {
                    smsManager.sendTextMessage(phone, null, escalationMessage, null, null);
                    dispatched++;
                } catch (Exception e) {
                    Log.e(TAG, "Escalation SMS error to " + phone + ": " + e.getMessage());
                }
            }
        }

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        dbHelper.logEvent("Journey Escalation", "Precautionary Alert Dispatched",
                "Missed check-in countdown. Precautionary SMS alert dispatched to " + dispatched + " guardian(s). " + locPart,
                "Alert Sent");

        if (journeyDbId > 0) {
            dbHelper.updateJourneyStatus(journeyDbId, "ALERT_SENT", null, checkinsCompletedCount, missedCheckinsCount);
        }

        updateForegroundNotification();
        broadcastJourneyUpdate();
    }

    public void handleUserCheckinSafe() {
        if (!isJourneyRunning) return;

        if (gracePeriodTimer != null) {
            gracePeriodTimer.cancel();
        }
        isGracePeriodActive = false;

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID_ESCALATION);
        }

        checkinsCompletedCount++;
        currentStatus = "ACTIVE";
        checkinSecondsRemaining = checkinIntervalMinutes * 60L;

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        dbHelper.logEvent("Check-In", "I'm Safe Confirmed",
                "User checked in safely during journey to " + currentDestination + ". Check-in #" + checkinsCompletedCount,
                "Safe");

        if (journeyDbId > 0) {
            dbHelper.updateJourneyStatus(journeyDbId, "ACTIVE", null, checkinsCompletedCount, missedCheckinsCount);
        }

        // Restart checkin countdown
        startTimers();
        updateForegroundNotification();
        broadcastJourneyUpdate();
    }

    public void extendJourneyTimer(int additionalMinutes) {
        if (!isJourneyRunning) return;

        totalEtaMinutes += additionalMinutes;
        secondsRemaining += (additionalMinutes * 60L);
        checkinSecondsRemaining = Math.max(checkinSecondsRemaining, 10 * 60L);

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        dbHelper.logEvent("Journey Extended", "Timer Extended by " + additionalMinutes + " min",
                "User extended expected arrival time to " + currentDestination + " (New ETA: +" + additionalMinutes + " min)",
                "Active");

        updateForegroundNotification();
        broadcastJourneyUpdate();
    }

    public void endJourney(boolean wasCompleted) {
        isJourneyRunning = false;
        currentStatus = wasCompleted ? "COMPLETED" : "CANCELLED";

        if (journeyTimer != null) journeyTimer.cancel();
        if (checkinTimer != null) checkinTimer.cancel();
        if (gracePeriodTimer != null) gracePeriodTimer.cancel();
        stopLocationUpdates();

        String endTimeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        if (journeyDbId > 0) {
            dbHelper.updateJourneyStatus(journeyDbId, currentStatus, endTimeFormatted, checkinsCompletedCount, missedCheckinsCount);
        }
        dbHelper.logEvent("Journey Ended", "Journey " + currentStatus,
                "Trip to " + currentDestination + " finished. Check-ins: " + checkinsCompletedCount + ", Missed: " + missedCheckinsCount,
                wasCompleted ? "Safe" : "Cancelled");

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID_ESCALATION);
            nm.cancel(NOTIFICATION_ID_BATTERY);
        }

        // Stop battery monitoring when journey ends
        unregisterBatteryMonitor();
        notifiedBatteryThresholds.clear();

        broadcastJourneyUpdate();
        stopForeground(true);
        stopSelf();
    }

    private void triggerEmergencySosFromJourney() {
        Intent sosIntent = new Intent(this, SosEmergencyActivity.class);
        sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sosIntent);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location loc : locationResult.getLocations()) {
                    if (loc != null) {
                        lastLatitude = loc.getLatitude();
                        lastLongitude = loc.getLongitude();
                        broadcastJourneyUpdate();
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private Notification buildForegroundNotification() {
        Intent openAppIntent = new Intent(this, JourneyMonitoringActivity.class);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent piOpen = PendingIntent.getActivity(this, 0, openAppIntent, piFlags);

        Intent safeIntent = new Intent(this, JourneyMonitoringService.class);
        safeIntent.setAction(ACTION_CHECKIN_SAFE);
        PendingIntent piSafe = PendingIntent.getService(this, 1, safeIntent, piFlags);

        Intent extendIntent = new Intent(this, JourneyMonitoringService.class);
        extendIntent.setAction(ACTION_EXTEND_TIMER);
        PendingIntent piExtend = PendingIntent.getService(this, 2, extendIntent, piFlags);

        Intent endIntent = new Intent(this, JourneyMonitoringService.class);
        endIntent.setAction(ACTION_END_JOURNEY);
        PendingIntent piEnd = PendingIntent.getService(this, 3, endIntent, piFlags);

        long checkinMins = checkinSecondsRemaining / 60;
        long checkinSecs = checkinSecondsRemaining % 60;
        String checkinTimeStr = String.format(Locale.US, "%02d:%02d", checkinMins, checkinSecs);

        String title = "🛡️ Journey Active to " + currentDestination;
        String content = "Check-in in " + checkinTimeStr + " • Live safety circle active";
        if (isGracePeriodActive) {
            title = "⚠️ Check-in Required (" + checkinSecondsRemaining + "s)";
            content = "Tap 'I'm Safe' to confirm safety with guardians.";
        } else if ("ALERT_SENT".equals(currentStatus)) {
            title = "📢 Precautionary Guardian Alert Sent";
            content = "Tap to update status or mark safe.";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID_JOURNEY)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(piOpen)
                .addAction(R.drawable.ic_shield_check, "I'm Safe", piSafe)
                .addAction(R.drawable.ic_safe_nav, "+15 Min", piExtend)
                .addAction(R.drawable.ic_arrow_back, "End Trip", piEnd)
                .build();
    }

    private void updateForegroundNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && isJourneyRunning) {
            nm.notify(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel journeyChannel = new NotificationChannel(
                    CHANNEL_ID_JOURNEY,
                    "Live Journey Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            journeyChannel.setDescription("Shows ongoing journey progress and safety check-in countdowns.");
            nm.createNotificationChannel(journeyChannel);

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ID_ALERT,
                    "Journey Check-In Escalation",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("High-priority safety check-in reminders and grace period alerts.");
            alertChannel.enableVibration(true);
            alertChannel.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 500});
            nm.createNotificationChannel(alertChannel);
        }
    }

    private void broadcastJourneyUpdate() {
        Intent intent = new Intent(ACTION_JOURNEY_BROADCAST);
        intent.putExtra("is_running", isJourneyRunning);
        intent.putExtra("destination", currentDestination);
        intent.putExtra("origin", currentOrigin);
        intent.putExtra("status", currentStatus);
        intent.putExtra("seconds_remaining", secondsRemaining);
        intent.putExtra("checkin_seconds_remaining", checkinSecondsRemaining);
        intent.putExtra("checkins_completed", checkinsCompletedCount);
        intent.putExtra("missed_checkins", missedCheckinsCount);
        intent.putExtra("is_grace_period", isGracePeriodActive);
        intent.putExtra("latitude", lastLatitude);
        intent.putExtra("longitude", lastLongitude);
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isJourneyRunning = false;
        if (journeyTimer != null) journeyTimer.cancel();
        if (checkinTimer != null) checkinTimer.cancel();
        if (gracePeriodTimer != null) gracePeriodTimer.cancel();
        stopLocationUpdates();

        unregisterBatteryMonitor();
        notifiedBatteryThresholds.clear();
    }

    private void registerBatteryMonitor() {
        if (batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !isJourneyRunning) return;
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (level >= 0 && scale > 0) {
                        int pct = (level * 100) / scale;
                        checkBatteryThreshold(pct);
                    }
                }
            };
            try {
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                registerReceiver(batteryReceiver, filter);
                Log.d(TAG, "Battery monitoring active for journey.");
            } catch (Exception e) {
                Log.e(TAG, "Failed registering battery receiver: " + e.getMessage(), e);
            }
        }
    }

    private void unregisterBatteryMonitor() {
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
                Log.d(TAG, "Battery monitoring unregistered.");
            } catch (Exception ignored) {}
            batteryReceiver = null;
        }
    }

    private void checkBatteryThreshold(int pct) {
        if (!isJourneyRunning) return;

        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        boolean alertsEnabled = prefs.getBoolean("journey_battery_alerts_enabled", true);
        if (!alertsEnabled) return;

        // Deduplicate: check which threshold was crossed without repeat alerts
        int triggeredThreshold = -1;
        if (pct <= 10 && !notifiedBatteryThresholds.contains(10)) {
            triggeredThreshold = 10;
            notifiedBatteryThresholds.add(10);
            notifiedBatteryThresholds.add(15);
            notifiedBatteryThresholds.add(20);
        } else if (pct <= 15 && !notifiedBatteryThresholds.contains(15)) {
            triggeredThreshold = 15;
            notifiedBatteryThresholds.add(15);
            notifiedBatteryThresholds.add(20);
        } else if (pct <= 20 && !notifiedBatteryThresholds.contains(20)) {
            triggeredThreshold = 20;
            notifiedBatteryThresholds.add(20);
        }

        if (triggeredThreshold == -1) return;

        // 1. Send warning notification to user
        sendBatteryAlertNotification(pct, triggeredThreshold);

        // 2. Dispatch SMS to monitoring contacts if enabled
        boolean notifySms = prefs.getBoolean("journey_battery_sms_contacts", true);
        boolean hasSmsPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        String locDetails;
        if (lastLatitude != 0.0 && lastLongitude != 0.0) {
            locDetails = "Last GPS: https://maps.google.com/?q=" + lastLatitude + "," + lastLongitude;
        } else {
            locDetails = "GPS location acquiring or omitted";
        }

        String smsText = "[AtmaRaksha Safety Notice] Low Battery Alert: User's phone battery dropped to " +
                pct + "% during active journey to " + currentDestination + ". " + locDetails + " at " + timeStr + ". Please keep track.";

        int smsDispatched = 0;
        if (notifySms && hasSmsPerm) {
            List<String> guardians = TrustedContactsManager.getSelectedMonitoringNumbers(this);
            if (!guardians.isEmpty()) {
                SmsManager smsManager = SmsManager.getDefault();
                for (String number : guardians) {
                    try {
                        smsManager.sendTextMessage(number, null, smsText, null, null);
                        smsDispatched++;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed sending low battery SMS: " + e.getMessage());
                    }
                }
            }
        }

        // 3. Log event into Safety History database
        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        String details = "Battery reached " + pct + "% en route to " + currentDestination + ". " +
                (notifySms ? (hasSmsPerm ? ("Dispatched SMS to " + smsDispatched + " guardian(s).") : "SMS delivery omitted (SEND_SMS permission not granted).") : "Guardian SMS notifications disabled.") +
                " " + locDetails;
        dbHelper.logEvent("Low Battery", "Journey Battery Warning (" + pct + "%)", details, "Warning");
    }

    private void sendBatteryAlertNotification(int pct, int threshold) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent openIntent = new Intent(this, JourneyMonitoringActivity.class);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 909, openIntent, piFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle("⚠️ AtmaRaksha Low Battery: " + pct + "%")
                .setContentText("Battery reached " + pct + "% during your journey to " + currentDestination + ". Conserve power or alert guardians.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(NOTIFICATION_ID_BATTERY, builder.build());
    }
}
