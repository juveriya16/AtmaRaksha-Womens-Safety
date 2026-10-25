package com.sum.ladybuddy;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * EmergencySirenManager
 * Robust, singleton-based emergency deterrent siren controller.
 * Plays high-volume siren audio from R.raw.police_siren, manages audio focus,
 * preserves user volume levels, handles interruptions gracefully, and supports UI listeners.
 */
public class EmergencySirenManager {

    private static final String TAG = "EmergencySirenManager";
    private static EmergencySirenManager instance;

    public interface SirenStateListener {
        void onSirenStateChanged(boolean isPlaying);
    }

    private final Context appContext;
    private final AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private ToneGenerator fallbackToneGen;
    private Handler fallbackHandler;
    private Runnable fallbackRunnable;
    private boolean isFallbackActive = false;

    private boolean isPlaying = false;
    private int originalMusicVolume = -1;
    private int originalAlarmVolume = -1;
    private AudioFocusRequest audioFocusRequest;
    private final List<SirenStateListener> listeners = new ArrayList<>();

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        Log.d(TAG, "Audio focus change: " + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stopSiren();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (mediaPlayer != null && isPlaying && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }
    };

    private EmergencySirenManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public static synchronized EmergencySirenManager getInstance(Context context) {
        if (instance == null) {
            instance = new EmergencySirenManager(context);
        }
        return instance;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void addListener(SirenStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onSirenStateChanged(isPlaying);
        }
    }

    public void removeListener(SirenStateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (SirenStateListener l : new ArrayList<>(listeners)) {
                try {
                    l.onSirenStateChanged(isPlaying);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        });
    }

    public synchronized void startSiren() {
        if (isPlaying) {
            return;
        }

        requestAudioFocusAndMaximizeVolume();

        try {
            // Attempt playback of real police_siren audio resource
            mediaPlayer = MediaPlayer.create(appContext, R.raw.police_siren);
            if (mediaPlayer != null) {
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()
                );
                mediaPlayer.setLooping(true);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + ", " + extra + ". Starting fallback tone.");
                    stopMediaPlayerOnly();
                    startFallbackToneSiren();
                    return true;
                });
                mediaPlayer.start();
                isPlaying = true;
                Log.d(TAG, "Emergency siren playback started via MediaPlayer");
            } else {
                Log.w(TAG, "police_siren resource returned null. Starting fallback tone siren.");
                startFallbackToneSiren();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start siren audio: " + t.getMessage(), t);
            startFallbackToneSiren();
        }

        // Log safety event to timeline
        try {
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(appContext);
            dbHelper.logEvent("Emergency Siren", "Siren Alarm Activated",
                    "High-decibel deterrence siren sounded to deter potential threats and alert surroundings.", "Active");
        } catch (Exception ignored) {}

        notifyListeners();
    }

    public synchronized void stopSiren() {
        if (!isPlaying) {
            return;
        }

        stopMediaPlayerOnly();
        stopFallbackToneSiren();
        restoreOriginalVolumeAndAbandonFocus();

        isPlaying = false;
        Log.d(TAG, "Emergency siren stopped");

        // Log safety event to timeline
        try {
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(appContext);
            dbHelper.logEvent("Emergency Siren", "Siren Alarm Stopped",
                    "Audible deterrent siren deactivated.", "Stopped");
        } catch (Exception ignored) {}

        notifyListeners();
    }

    public synchronized void toggleSiren() {
        if (isPlaying) {
            stopSiren();
        } else {
            startSiren();
        }
    }

    private void stopMediaPlayerOnly() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping mediaPlayer", e);
            mediaPlayer = null;
        }
    }

    /**
     * Fallback continuous alternating two-tone alarm in case raw file playback fails on device
     */
    private void startFallbackToneSiren() {
        try {
            if (fallbackToneGen == null) {
                fallbackToneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            }
            isPlaying = true;
            isFallbackActive = true;
            fallbackHandler = new Handler(Looper.getMainLooper());
            fallbackRunnable = new Runnable() {
                private boolean high = false;
                @Override
                public void run() {
                    if (!isFallbackActive || fallbackToneGen == null) return;
                    try {
                        fallbackToneGen.startTone(high ? ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK : ToneGenerator.TONE_PROP_BEEP2, 350);
                    } catch (Exception ignored) {}
                    high = !high;
                    fallbackHandler.postDelayed(this, 400);
                }
            };
            fallbackHandler.post(fallbackRunnable);
            Log.d(TAG, "Fallback ToneGenerator siren active");
        } catch (Exception e) {
            Log.e(TAG, "Error starting fallback tone siren", e);
        }
    }

    private void stopFallbackToneSiren() {
        isFallbackActive = false;
        if (fallbackHandler != null && fallbackRunnable != null) {
            fallbackHandler.removeCallbacks(fallbackRunnable);
            fallbackHandler = null;
            fallbackRunnable = null;
        }
        if (fallbackToneGen != null) {
            try {
                fallbackToneGen.release();
            } catch (Exception ignored) {}
            fallbackToneGen = null;
        }
    }

    private void requestAudioFocusAndMaximizeVolume() {
        if (audioManager == null) return;

        try {
            // Save original volume
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            // Boost to max safe volume for emergency deterrence
            int maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0);

            // Request Audio Focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build();

                audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring audio focus or setting volume", e);
        }
    }

    private void restoreOriginalVolumeAndAbandonFocus() {
        if (audioManager == null) return;

        try {
            // Restore previous user volume levels
            if (originalAlarmVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0);
            }
            if (originalMusicVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0);
            }

            // Abandon focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
                audioFocusRequest = null;
            } else {
                audioManager.abandonAudioFocus(focusChangeListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring volume or abandoning focus", e);
        }
    }
}
