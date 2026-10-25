package com.sum.ladybuddy;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceSosActivity extends AppCompatActivity {

    private static final int REQ_PERM_AUDIO = 301;
    private static final String PREF_PHRASE_KEY = "voice_activation_phrase";

    private ImageView btnBack;
    private FrameLayout layoutMicRing;
    private ImageView ivMicStatus;
    private TextView tvStatusIndicator, tvDetectedSnippet;
    private Button btnToggleListener;

    private EditText etActivationPhrase;
    private TextView chipPhrase1, chipPhrase2, chipPhrase3;
    private Button btnSavePhrase;
    private Button btnManualSosAlternative;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;
    private String configuredPhrase = "help me atmaraksha";
    private CountDownTimer emergencyTriggerTimer;
    private AlertDialog triggerConfirmDialog;
    private ToneGenerator toneGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_sos);

        btnBack = findViewById(R.id.btn_back_voice_sos);
        layoutMicRing = findViewById(R.id.layout_mic_pulsing_ring);
        ivMicStatus = findViewById(R.id.iv_mic_status);
        tvStatusIndicator = findViewById(R.id.tv_voice_status_indicator);
        tvDetectedSnippet = findViewById(R.id.tv_detected_speech_snippet);
        btnToggleListener = findViewById(R.id.btn_toggle_voice_listener);

        etActivationPhrase = findViewById(R.id.et_activation_phrase);
        chipPhrase1 = findViewById(R.id.chip_phrase_1);
        chipPhrase2 = findViewById(R.id.chip_phrase_2);
        chipPhrase3 = findViewById(R.id.chip_phrase_3);
        btnSavePhrase = findViewById(R.id.btn_save_phrase);
        btnManualSosAlternative = findViewById(R.id.btn_manual_sos_alternative);

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception ignored) {}

        btnBack.setOnClickListener(v -> finish());

        loadSavedPhrase();
        setupPhraseChips();
        setupManualSos();

        btnToggleListener.setOnClickListener(v -> {
            if (isListening) {
                stopListeningSession();
            } else {
                checkPermissionAndStartListening();
            }
        });
    }

    private void loadSavedPhrase() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        configuredPhrase = prefs.getString(PREF_PHRASE_KEY, "Help Me AtmaRaksha");
        etActivationPhrase.setText(configuredPhrase);
    }

    private void setupPhraseChips() {
        chipPhrase1.setOnClickListener(v -> etActivationPhrase.setText("Help Me AtmaRaksha"));
        chipPhrase2.setOnClickListener(v -> etActivationPhrase.setText("Emergency SOS"));
        chipPhrase3.setOnClickListener(v -> etActivationPhrase.setText("Safe Circle Help"));

        btnSavePhrase.setOnClickListener(v -> {
            String phrase = etActivationPhrase.getText().toString().trim();
            if (TextUtils.isEmpty(phrase)) {
                etActivationPhrase.setError("Please enter a valid phrase");
                return;
            }
            configuredPhrase = phrase;
            getSharedPreferences("demo", MODE_PRIVATE).edit().putString(PREF_PHRASE_KEY, configuredPhrase).apply();
            Toast.makeText(this, "Voice activation phrase saved: \"" + configuredPhrase + "\"", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupManualSos() {
        btnManualSosAlternative.setOnClickListener(v -> {
            startActivity(new Intent(this, SosEmergencyActivity.class));
        });
    }

    private void checkPermissionAndStartListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListeningSession();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Microphone Permission Required")
                    .setMessage("Voice-Activated SOS requires microphone access to match your emergency phrase hands-free. Audio is processed on-device and never saved without consent.")
                    .setPositiveButton("Grant Access", (d, w) -> {
                        ActivityCompat.requestPermissions(VoiceSosActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERM_AUDIO);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListeningSession();
            } else {
                Toast.makeText(this, "Microphone permission is required for Voice SOS.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startListeningSession() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition engine not available on this device.", Toast.LENGTH_LONG).show();
            return;
        }

        isListening = true;
        btnToggleListener.setText("Stop Voice Trigger Session");
        btnToggleListener.setBackgroundColor(getResources().getColor(R.color.color_sos_red));
        layoutMicRing.setBackgroundResource(R.drawable.bg_sos_ring);
        ivMicStatus.setColorFilter(getResources().getColor(R.color.color_sos_red));
        tvStatusIndicator.setText("🎤 Voice Trigger: Active & Listening");
        tvDetectedSnippet.setText("Listening for: \"" + configuredPhrase + "\"...");

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // If recognizer pauses or times out, loop listening session as long as user left it active
                if (isListening && speechRecognizer != null) {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onResults(Bundle results) {
                processSpeechMatches(results);
                if (isListening && speechRecognizer != null) {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                processSpeechMatches(partialResults);
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(speechRecognizerIntent);
        Toast.makeText(this, "Listening for: \"" + configuredPhrase + "\"", Toast.LENGTH_SHORT).show();
    }

    private void processSpeechMatches(Bundle bundle) {
        if (bundle == null) return;
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String spoken = matches.get(0);
            tvDetectedSnippet.setText("Detected: \"" + spoken + "\"");

            String normalizedSpoken = spoken.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
            String normalizedConfig = configuredPhrase.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();

            if (normalizedSpoken.contains(normalizedConfig) || normalizedSpoken.contains("help") || normalizedSpoken.contains("sos")) {
                triggerVoiceEmergencyConfirmation();
            }
        }
    }

    private void triggerVoiceEmergencyConfirmation() {
        stopListeningSession();

        try {
            if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400);
        } catch (Exception ignored) {}

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Voice SOS Phrase Detected!");
        builder.setMessage("Phrase \"" + configuredPhrase + "\" was matched. Activating Emergency SOS in 3 seconds...\n\nTap 'Cancel' if this was unintentional.");
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel Alert", (dialog, which) -> {
            if (emergencyTriggerTimer != null) emergencyTriggerTimer.cancel();
            Toast.makeText(VoiceSosActivity.this, "Voice emergency trigger cancelled.", Toast.LENGTH_SHORT).show();
        });

        triggerConfirmDialog = builder.create();
        triggerConfirmDialog.show();

        emergencyTriggerTimer = new CountDownTimer(3000, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish() {
                if (triggerConfirmDialog != null && triggerConfirmDialog.isShowing()) {
                    triggerConfirmDialog.dismiss();
                }
                Intent intent = new Intent(VoiceSosActivity.this, SosEmergencyActivity.class);
                intent.putExtra("trigger_source", "VOICE_ACTIVATION");
                startActivity(intent);
                finish();
            }
        }.start();
    }

    private void stopListeningSession() {
        isListening = false;
        btnToggleListener.setText("Arm Voice Trigger (Active Session)");
        btnToggleListener.setBackgroundResource(R.drawable.bg_gradient_pink_purple);
        layoutMicRing.setBackgroundResource(R.drawable.bg_circle_lavender);
        ivMicStatus.setColorFilter(getResources().getColor(R.color.brand_purple));
        tvStatusIndicator.setText("Voice Listener: Standby");

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningSession();
        if (emergencyTriggerTimer != null) emergencyTriggerTimer.cancel();
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
    }
}
