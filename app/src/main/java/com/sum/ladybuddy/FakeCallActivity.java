package com.sum.ladybuddy;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * FakeCallActivity
 * Displays a realistic incoming call screen and active connected call screen.
 * Intended as a safe, discreet decoy to help women exit uncomfortable situations.
 * Strictly simulated:
 * - No actual phone calls placed
 * - No carrier charges
 * - No call logs accessed or created
 */
public class FakeCallActivity extends AppCompatActivity {

    public static final String EXTRA_CALLER_NAME = "extra_caller_name";
    public static final String EXTRA_CALLER_NUMBER = "extra_caller_number";
    public static final String EXTRA_AUTO_ANSWER = "extra_auto_answer";

    private TextView tvAvatarInitials, tvCallerName, tvCallerNumber, tvCallStatus;
    private LinearLayout layoutIncomingControls, layoutActiveCallControls;
    private ImageButton btnDeclineCall, btnAnswerCall, btnEndCall;
    private LinearLayout btnMute, btnKeypad, btnSpeaker;
    private ImageView ivMuteIcon, ivSpeakerIcon;
    private TextView tvMuteLabel, tvSpeakerLabel;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private int callDurationSeconds = 0;
    private boolean isCallActive = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;

    private String callerName = "Mom";
    private String callerNumber = "+91 98765 43210";

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            callDurationSeconds++;
            int mins = callDurationSeconds / 60;
            int secs = callDurationSeconds % 60;
            String timeStr = String.format(Locale.US, "%02d:%02d", mins, secs);
            if (tvCallStatus != null) {
                tvCallStatus.setText(timeStr);
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure window for lockscreen and full-screen incoming call display
        configureWindowForLockscreen();

        setContentView(R.layout.activity_fake_incoming_call);

        // Read intent extras
        if (getIntent() != null) {
            String nameExtra = getIntent().getStringExtra(EXTRA_CALLER_NAME);
            if (!TextUtils.isEmpty(nameExtra)) callerName = nameExtra;

            String numExtra = getIntent().getStringExtra(EXTRA_CALLER_NUMBER);
            if (!TextUtils.isEmpty(numExtra)) callerNumber = numExtra;
        }

        initViews();
        bindCallerData();

        boolean autoAnswer = getIntent() != null && getIntent().getBooleanExtra(EXTRA_AUTO_ANSWER, false);
        if (autoAnswer) {
            answerCall();
        } else {
            startRinging();
        }
    }

    private void configureWindowForLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initViews() {
        tvAvatarInitials = findViewById(R.id.tv_call_avatar_initials);
        tvCallerName = findViewById(R.id.tv_caller_name);
        tvCallerNumber = findViewById(R.id.tv_caller_number);
        tvCallStatus = findViewById(R.id.tv_call_status);

        layoutIncomingControls = findViewById(R.id.layout_incoming_controls);
        layoutActiveCallControls = findViewById(R.id.layout_active_call_controls);

        btnDeclineCall = findViewById(R.id.btn_decline_call);
        btnAnswerCall = findViewById(R.id.btn_answer_call);
        btnEndCall = findViewById(R.id.btn_end_call);

        btnMute = findViewById(R.id.btn_call_mute);
        btnKeypad = findViewById(R.id.btn_call_keypad);
        btnSpeaker = findViewById(R.id.btn_call_speaker);

        ivMuteIcon = findViewById(R.id.iv_call_mute_icon);
        ivSpeakerIcon = findViewById(R.id.iv_call_speaker_icon);
        tvMuteLabel = findViewById(R.id.tv_call_mute_label);
        tvSpeakerLabel = findViewById(R.id.tv_call_speaker_label);

        btnDeclineCall.setOnClickListener(v -> declineCall());
        btnAnswerCall.setOnClickListener(v -> answerCall());
        btnEndCall.setOnClickListener(v -> endCall());

        // In-call decoy controls
        btnMute.setOnClickListener(v -> toggleMute());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnKeypad.setOnClickListener(v -> {
            Toast.makeText(this, "Simulated Keypad: Beep", Toast.LENGTH_SHORT).show();
        });
    }

    private void bindCallerData() {
        tvCallerName.setText(callerName);
        tvCallerNumber.setText(callerNumber);

        // Derive avatar initial
        if (!TextUtils.isEmpty(callerName)) {
            tvAvatarInitials.setText(String.valueOf(callerName.trim().charAt(0)).toUpperCase());
        } else {
            tvAvatarInitials.setText("C");
        }
    }

    private void startRinging() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 1000};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRinging() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
        } catch (Exception ignored) {}

        try {
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception ignored) {}
    }

    private void answerCall() {
        stopRinging();
        isCallActive = true;
        callDurationSeconds = 0;

        layoutIncomingControls.setVisibility(View.GONE);
        layoutActiveCallControls.setVisibility(View.VISIBLE);

        tvCallStatus.setText("00:00");
        tvCallStatus.setBackgroundResource(R.drawable.bg_pill_button_pink);
        tvCallStatus.setTextColor(ContextCompat.getColor(this, R.color.white));

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (ivMuteIcon != null) {
            ivMuteIcon.setColorFilter(isMuted ? ContextCompat.getColor(this, R.color.color_sos_red) : ContextCompat.getColor(this, R.color.white));
        }
        if (tvMuteLabel != null) {
            tvMuteLabel.setText(isMuted ? "Muted" : "Mute");
        }
        Toast.makeText(this, isMuted ? "Microphone muted (Simulated)" : "Microphone active", Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        if (ivSpeakerIcon != null) {
            ivSpeakerIcon.setColorFilter(isSpeakerOn ? ContextCompat.getColor(this, R.color.brand_pink) : ContextCompat.getColor(this, R.color.white));
        }
        if (tvSpeakerLabel != null) {
            tvSpeakerLabel.setText(isSpeakerOn ? "Speaker ON" : "Speaker");
        }
        Toast.makeText(this, isSpeakerOn ? "Speakerphone active (Simulated)" : "Earpiece active", Toast.LENGTH_SHORT).show();
    }

    private void declineCall() {
        stopRinging();
        logCallEvent("Declined", 0);
        Toast.makeText(this, "Simulated call dismissed", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void endCall() {
        stopRinging();
        timerHandler.removeCallbacks(timerRunnable);
        logCallEvent("Completed", callDurationSeconds);

        int mins = callDurationSeconds / 60;
        int secs = callDurationSeconds % 60;
        String dur = String.format(Locale.US, "%02d:%02d", mins, secs);

        Toast.makeText(this, "Decoy call ended (" + dur + "). You are safe.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void logCallEvent(String status, int duration) {
        try {
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            int mins = duration / 60;
            int secs = duration % 60;
            String durStr = String.format(Locale.US, "%02d:%02d", mins, secs);

            dbHelper.logEvent("Decoy Call", "Fake Call Used (" + callerName + ")",
                    "Simulated call from " + callerName + " (" + callerNumber + ") ended after " + durStr + ". Status Safe.", status);
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (isCallActive) {
            endCall();
        } else {
            declineCall();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRinging();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
