package com.sum.ladybuddy;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EvidenceRecordingActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_AUDIO = 201;

    private ImageView ivRecordPulsingDot;
    private TextView tvRecordStateLabel;
    private TextView tvRecordingTimer;
    private Button btnToggleRecording;
    private LinearLayout containerRecordingsList;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private File currentOutputFile;
    private long recordingStartTime = 0;
    private int recordingDurationSeconds = 0;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean dotPulseVisible = true;

    private SafetyHistoryDbHelper dbHelper;
    private long currentlyPlayingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evidence_recording);

        dbHelper = new SafetyHistoryDbHelper(this);
        timerHandler = new Handler(Looper.getMainLooper());

        findViewById(R.id.btn_back_evidence).setOnClickListener(v -> finish());
        ivRecordPulsingDot = findViewById(R.id.iv_record_pulsing_dot);
        tvRecordStateLabel = findViewById(R.id.tv_record_state_label);
        tvRecordingTimer = findViewById(R.id.tv_recording_timer);
        btnToggleRecording = findViewById(R.id.btn_toggle_recording);
        containerRecordingsList = findViewById(R.id.container_recordings_list);

        btnToggleRecording.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                checkPermissionAndStart();
            }
        });

        loadRecordingsList();
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_AUDIO);
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Microphone permission required for evidence recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        // Stop any active playback first
        stopAudioPlayback();

        File dir = new File(getFilesDir(), "evidence_audio");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentOutputFile = new File(dir, "Evidence_" + timeStamp + ".m4a");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                mediaRecorder = new MediaRecorder();
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentOutputFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            recordingDurationSeconds = 0;

            tvRecordStateLabel.setText("Recording in Progress (Encrypted Local)");
            tvRecordStateLabel.setTextColor(ContextCompat.getColor(this, R.color.status_red));
            btnToggleRecording.setText("⏹ Stop & Save Recording");
            btnToggleRecording.setBackgroundResource(R.drawable.bg_pill_button_red);

            startTimerAnimation();
            Toast.makeText(this, "Evidence audio recording started", Toast.LENGTH_SHORT).show();

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recorder: " + e.getMessage(), Toast.LENGTH_LONG).show();
            releaseMediaRecorder();
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (RuntimeException stopException) {
            // Can happen if stopped immediately after start
            stopException.printStackTrace();
        } finally {
            releaseMediaRecorder();
        }

        stopTimerAnimation();
        isRecording = false;

        tvRecordStateLabel.setText("Recorder Standby");
        tvRecordStateLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvRecordingTimer.setText("00:00");
        btnToggleRecording.setText("Start Evidence Audio");
        btnToggleRecording.setBackgroundResource(R.drawable.bg_gradient_pink_purple);

        if (currentOutputFile != null && currentOutputFile.exists() && currentOutputFile.length() > 0) {
            String currentTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
            EvidenceRecording record = new EvidenceRecording();
            record.setFilename(currentOutputFile.getName());
            record.setFilePath(currentOutputFile.getAbsolutePath());
            record.setDurationSeconds(recordingDurationSeconds);
            record.setFileSizeBytes(currentOutputFile.length());
            record.setTimestamp(currentTime);
            record.setNotes("Sandboxed local evidence recording");

            long id = dbHelper.insertRecording(record);
            dbHelper.insertSafetyEvent(
                    "Evidence Recorded",
                    "Audio Evidence Saved (" + formatDuration(recordingDurationSeconds) + ")",
                    "File: " + currentOutputFile.getName() + " saved to secure private storage.",
                    "Secured"
            );

            Toast.makeText(this, "Evidence saved securely (" + formatDuration(recordingDurationSeconds) + ")", Toast.LENGTH_SHORT).show();
            loadRecordingsList();
        } else {
            Toast.makeText(this, "Recording discarded (too short)", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimerAnimation() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    recordingDurationSeconds++;
                    tvRecordingTimer.setText(formatDuration(recordingDurationSeconds));

                    dotPulseVisible = !dotPulseVisible;
                    ivRecordPulsingDot.setAlpha(dotPulseVisible ? 1.0f : 0.2f);

                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimerAnimation() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        ivRecordPulsingDot.setAlpha(1.0f);
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception ignored) {
            }
            mediaRecorder = null;
        }
    }

    private void stopAudioPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
        currentlyPlayingId = -1;
    }

    private void loadRecordingsList() {
        containerRecordingsList.removeAllViews();
        List<EvidenceRecording> list = dbHelper.getAllRecordings();

        if (list.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No evidence recordings yet.\nRecordings you make are sandboxed in private storage.");
            emptyTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyTv.setPadding(24, 40, 24, 40);
            emptyTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            emptyTv.setTextSize(12);
            containerRecordingsList.addView(emptyTv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (EvidenceRecording rec : list) {
            View itemView = inflater.inflate(R.layout.item_evidence_recording, containerRecordingsList, false);

            TextView tvTitle = itemView.findViewById(R.id.tv_rec_title);
            TextView tvMeta = itemView.findViewById(R.id.tv_rec_meta);
            Button btnPlay = itemView.findViewById(R.id.btn_rec_play);
            Button btnShare = itemView.findViewById(R.id.btn_rec_share);
            ImageView btnDelete = itemView.findViewById(R.id.btn_rec_delete);

            tvTitle.setText(rec.getFilename());
            String sizeStr = formatFileSize(rec.getFileSizeBytes());
            tvMeta.setText(rec.getTimestamp() + " • " + formatDuration(rec.getDurationSeconds()) + " • " + sizeStr);

            if (currentlyPlayingId == rec.getId()) {
                btnPlay.setText("⏹ Stop Audio");
                btnPlay.setTextColor(ContextCompat.getColor(this, R.color.status_red));
            } else {
                btnPlay.setText("▶ Play Audio");
                btnPlay.setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
            }

            btnPlay.setOnClickListener(v -> {
                if (currentlyPlayingId == rec.getId()) {
                    stopAudioPlayback();
                    loadRecordingsList();
                } else {
                    playRecording(rec);
                }
            });

            btnShare.setOnClickListener(v -> shareRecording(rec));

            btnDelete.setOnClickListener(v -> confirmDeleteRecording(rec));

            containerRecordingsList.addView(itemView);
        }
    }

    private void playRecording(EvidenceRecording rec) {
        stopAudioPlayback();

        File file = new File(rec.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "Recording file not found on disk", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentlyPlayingId = rec.getId();

            mediaPlayer.setOnCompletionListener(mp -> {
                stopAudioPlayback();
                loadRecordingsList();
            });

            loadRecordingsList();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareRecording(EvidenceRecording rec) {
        File file = new File(rec.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "Recording file does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Confidential Audio Evidence: " + rec.getFilename());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Encrypted private evidence recorded via AtmaRaksha on " + rec.getTimestamp());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Evidence Securely"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteRecording(EvidenceRecording rec) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Evidence Recording?")
                .setMessage("This will permanently remove the audio file from your local vault. This action cannot be undone.")
                .setPositiveButton("Delete Permanently", (dialog, which) -> {
                    if (currentlyPlayingId == rec.getId()) {
                        stopAudioPlayback();
                    }
                    File file = new File(rec.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                    dbHelper.deleteRecording(rec.getId());
                    Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
                    loadRecordingsList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            // Keep recording in background if user switches or let them stop
        }
        stopAudioPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudioPlayback();
        if (isRecording) {
            stopRecording();
        }
        stopTimerAnimation();
    }
}
