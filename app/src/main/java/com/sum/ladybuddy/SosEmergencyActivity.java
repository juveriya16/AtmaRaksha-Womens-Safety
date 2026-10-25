package com.sum.ladybuddy;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class SosEmergencyActivity extends AppCompatActivity {

    private static final String ACTION_SOS_SENT = "com.sum.ladybuddy.SOS_SENT_STATUS";
    private static final String ACTION_SOS_DELIVERED = "com.sum.ladybuddy.SOS_DELIVERED_STATUS";

    private TextView tvCountdown, tvContactsSummary, tvGpsStatus;
    private Button btnCancel, btnConfirmNow;
    private Button btnToggleSiren;
    private TextView tvSirenStatus;
    private EmergencySirenManager sirenManager;
    private EmergencySirenManager.SirenStateListener sirenStateListener;
    private CountDownTimer countDownTimer;
    private ToneGenerator toneGen;
    private FusedLocationProviderClient fusedLocationClient;
    private String acquiredLocation = "";
    private boolean isSosTriggered = false;
    private BroadcastReceiver sosStatusReceiver;

    public static final String EXTRA_SHAKE_TRIGGERED = "EXTRA_SHAKE_TRIGGERED";
    public static final String EXTRA_COUNTDOWN_SECONDS = "EXTRA_COUNTDOWN_SECONDS";

    private boolean isShakeTriggered = false;
    private int countdownSeconds = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_emergency);

        isShakeTriggered = getIntent().getBooleanExtra(EXTRA_SHAKE_TRIGGERED, false);
        countdownSeconds = getIntent().getIntExtra(EXTRA_COUNTDOWN_SECONDS, 5);
        if (countdownSeconds <= 0) countdownSeconds = 5;

        sirenManager = EmergencySirenManager.getInstance(this);

        tvCountdown = findViewById(R.id.tv_countdown_seconds);
        tvContactsSummary = findViewById(R.id.tv_contacts_summary);
        tvGpsStatus = findViewById(R.id.tv_current_gps_status);
        btnCancel = findViewById(R.id.btn_cancel_sos);
        btnConfirmNow = findViewById(R.id.btn_confirm_now);
        btnToggleSiren = findViewById(R.id.btn_toggle_siren);
        tvSirenStatus = findViewById(R.id.tv_siren_status);

        TextView tvSubtitle = findViewById(R.id.tv_sos_subtitle);
        TextView tvInstruction = findViewById(R.id.tv_countdown_instruction);

        if (isShakeTriggered) {
            if (tvSubtitle != null) {
                tvSubtitle.setText("⚠️ Shake-to-SOS Detected • Countdown Active");
            }
            if (tvInstruction != null) {
                tvInstruction.setText("Deliberate shake detected! Tap Cancel within " + countdownSeconds + " seconds if accidental.");
            }
        } else if (tvInstruction != null) {
            tvInstruction.setText("To prevent accidental alarms, you have " + countdownSeconds + " seconds to cancel.");
        }

        tvCountdown.setText(String.valueOf(countdownSeconds));

        sirenStateListener = this::updateSirenUi;
        sirenManager.addListener(sirenStateListener);

        if (btnToggleSiren != null) {
            btnToggleSiren.setOnClickListener(v -> sirenManager.toggleSiren());
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception ignored) {}

        displayConfiguredContacts();
        preFetchLocation();
        registerSosDeliveryReceiver();

        // Configurable safety confirmation countdown
        countDownTimer = new CountDownTimer(countdownSeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000) + 1;
                tvCountdown.setText(String.valueOf(seconds));
                try {
                    if (toneGen != null) {
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("0");
                if (!isSosTriggered && !isFinishing()) {
                    triggerEmergencyAlert();
                }
            }
        }.start();

        // Cancel button: instantly stops alarm and cancels alert
        btnCancel.setOnClickListener(v -> cancelEmergencyAlert());

        // Confirm now button: bypasses remaining countdown
        btnConfirmNow.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            triggerEmergencyAlert();
        });
    }

    private void displayConfiguredContacts() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        String p1 = prefs.getString("phone1", "");
        String p2 = prefs.getString("phone2", "");
        String p3 = prefs.getString("phone3", "");
        String p4 = prefs.getString("phone4", "");

        int count = 0;
        if (!TextUtils.isEmpty(p1)) count++;
        if (!TextUtils.isEmpty(p2)) count++;
        if (!TextUtils.isEmpty(p3)) count++;
        if (!TextUtils.isEmpty(p4)) count++;

        if (count > 0) {
            tvContactsSummary.setText("Alert will be dispatched to " + count + " trusted guardian(s)");
        } else {
            tvContactsSummary.setText("⚠️ No emergency numbers configured yet. You will be prompted to call 112.");
        }
    }

    private void preFetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location loc = task.getResult();
                    if (loc != null) {
                        acquiredLocation = loc.getLatitude() + "," + loc.getLongitude();
                        tvGpsStatus.setText("GPS Fixed: " + acquiredLocation);
                    } else {
                        tvGpsStatus.setText("Acquiring GPS fix via network...");
                    }
                }
            });
        } else {
            tvGpsStatus.setText("Location permission not granted. GPS link omitted.");
        }
    }

    private void cancelEmergencyAlert() {
        if (sirenManager != null) {
            sirenManager.stopSiren();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        if (isShakeTriggered) {
            dbHelper.logEvent("Shake-to-SOS", "Shake-to-SOS Cancelled",
                    "User cancelled Shake-to-SOS within " + countdownSeconds + "-second countdown window. Status Safe.", "Cancelled");
            Toast.makeText(this, "Shake-to-SOS cancelled. You are safe.", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.logEvent("SOS Alert", "SOS Cancelled",
                    "User cancelled SOS within " + countdownSeconds + "-second window. Status Safe.", "Cancelled");
            Toast.makeText(this, "Emergency SOS cancelled. You are safe.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void updateSirenUi(boolean isPlaying) {
        if (btnToggleSiren == null) return;
        if (isPlaying) {
            btnToggleSiren.setText("🛑 STOP EMERGENCY SIREN (ACTIVE)");
            btnToggleSiren.setBackgroundResource(R.drawable.bg_pill_button_red);
            if (tvSirenStatus != null) {
                tvSirenStatus.setText("Loud deterrence alarm blasting at maximum volume!");
                tvSirenStatus.setTextColor(ContextCompat.getColor(this, R.color.color_sos_red));
            }
        } else {
            btnToggleSiren.setText("🔊 START EMERGENCY SIREN");
            btnToggleSiren.setBackgroundResource(R.drawable.bg_pill_button_pink);
            if (tvSirenStatus != null) {
                tvSirenStatus.setText("Sound high-decibel alarm without sending SOS SMS");
                tvSirenStatus.setTextColor(ContextCompat.getColor(this, R.color.night_text_secondary));
            }
        }
    }

    private void triggerEmergencyAlert() {
        if (isSosTriggered) return;
        isSosTriggered = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        String p1 = prefs.getString("phone1", "");
        String p2 = prefs.getString("phone2", "");
        String p3 = prefs.getString("phone3", "");
        String p4 = prefs.getString("phone4", "");
        String userMsg = prefs.getString("msg", "EMERGENCY! I need immediate help. Please reach me.");

        String fullMessage = userMsg;
        if (!TextUtils.isEmpty(acquiredLocation)) {
            fullMessage += " Live GPS: https://maps.google.com/?q=" + acquiredLocation;
        }

        List<String> targetNumbers = new ArrayList<>();
        if (!TextUtils.isEmpty(p1)) targetNumbers.add(p1.trim());
        if (!TextUtils.isEmpty(p2)) targetNumbers.add(p2.trim());
        if (!TextUtils.isEmpty(p3)) targetNumbers.add(p3.trim());
        if (!TextUtils.isEmpty(p4)) targetNumbers.add(p4.trim());

        boolean hasDirectSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        int directDispatchedCount = 0;

        if (hasDirectSmsPermission && !targetNumbers.isEmpty()) {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SOS_SENT), flags);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SOS_DELIVERED), flags);

            for (String number : targetNumbers) {
                try {
                    smsManager.sendTextMessage(number, null, fullMessage, sentPI, deliveredPI);
                    directDispatchedCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // If direct SMS was not permitted or failed, fall back to opening the standard SMS composer
        if (directDispatchedCount == 0 && !targetNumbers.isEmpty()) {
            try {
                Intent composerIntent = new Intent(Intent.ACTION_SENDTO);
                composerIntent.setData(Uri.parse("smsto:" + Uri.encode(targetNumbers.get(0))));
                composerIntent.putExtra("sms_body", fullMessage);
                composerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(composerIntent);
            } catch (Exception ignored) {}
        }

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        String eventType = isShakeTriggered ? "Shake-to-SOS" : "SOS Alert";
        String eventTitle = isShakeTriggered ? "Shake-to-SOS Dispatched" : "SOS Alert Triggered";
        dbHelper.logEvent(eventType, eventTitle,
                "Emergency dispatched (" + (directDispatchedCount > 0 ? (directDispatchedCount + " via Direct SMS") : "Opened via SMS Composer") +
                        ") with GPS: " + acquiredLocation, "Alert Active");

        showPostSosDialog(directDispatchedCount, p1, fullMessage);
    }

    private void showPostSosDialog(int directSentCount, final String primaryPhone, final String fullMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 SOS Emergency Dispatched");

        String statusExplanation;
        if (directSentCount > 0) {
            statusExplanation = "Direct emergency SMS dispatched to " + directSentCount + " contact(s). Carrier delivery confirmation in progress.";
        } else if (!TextUtils.isEmpty(primaryPhone)) {
            statusExplanation = "Opening emergency SMS composer with your live GPS location for primary guardian (" + primaryPhone + ").";
        } else {
            statusExplanation = "No trusted contacts were configured. Please call national emergency (112) immediately.";
        }

        builder.setMessage(statusExplanation + "\n\nWould you like to dial emergency services now?");
        builder.setCancelable(false);

        if (!TextUtils.isEmpty(primaryPhone)) {
            builder.setPositiveButton("Call Guardian (" + primaryPhone + ")", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + primaryPhone));
                startActivity(intent);
                finish();
            });
        }

        builder.setNeutralButton("Call 112 (National)", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:112"));
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("I am Safe • Close", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });

        builder.show();
    }

    private void registerSosDeliveryReceiver() {
        sosStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(SosEmergencyActivity.this);
                if (ACTION_SOS_SENT.equals(action)) {
                    if (getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(context, "Carrier confirmed: SOS emergency SMS transmitted!", Toast.LENGTH_LONG).show();
                        dbHelper.logEvent("SOS Carrier", "SMS Carrier Transmitted", "Cellular carrier confirmed transmission.", "Sent");
                    } else {
                        Toast.makeText(context, "Carrier returned dispatch code: " + getResultCode(), Toast.LENGTH_SHORT).show();
                    }
                } else if (ACTION_SOS_DELIVERED.equals(action)) {
                    Toast.makeText(context, "Recipient handset received SOS alert!", Toast.LENGTH_SHORT).show();
                    dbHelper.logEvent("SOS Carrier", "SMS Delivered", "Guardian handset confirmed receipt.", "Delivered");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SOS_SENT);
        filter.addAction(ACTION_SOS_DELIVERED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(sosStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(sosStatusReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sirenManager != null && sirenManager.isPlaying()) {
            sirenManager.stopSiren();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sirenManager != null) {
            sirenManager.removeListener(sirenStateListener);
            sirenManager.stopSiren();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
        if (sosStatusReceiver != null) {
            try {
                unregisterReceiver(sosStatusReceiver);
            } catch (Exception ignored) {}
        }
    }
}
