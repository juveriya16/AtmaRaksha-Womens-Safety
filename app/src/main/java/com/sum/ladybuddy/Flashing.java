package com.sum.ladybuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;

public class Flashing extends AppCompatActivity {
    private ImageView imageviewflashing;
    private ObjectAnimator anim;
    private EmergencySirenManager sirenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashing);

        sirenManager = EmergencySirenManager.getInstance(this);

        imageviewflashing = findViewById(R.id.imageactivity);
        Button btnStop = findViewById(R.id.btn_stop_alarm);

        if (btnStop != null) {
            btnStop.setOnClickListener(v -> {
                sirenManager.stopSiren();
                finish();
            });
        }

        sirenManager.startSiren();
        startLights();
    }

    @SuppressLint("WrongConstant")
    public void startLights() {
        if (imageviewflashing != null) {
            // High visibility pink-red and royal purple flashing strobe
            anim = ObjectAnimator.ofInt(imageviewflashing, "BackgroundColor",
                    Color.parseColor("#FE4E8C"), Color.parseColor("#5F297B"));
            anim.setDuration(120);
            anim.setEvaluator(new ArgbEvaluator());
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            anim.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSirenAndLights();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSirenAndLights();
    }

    private void stopSirenAndLights() {
        try {
            if (anim != null && anim.isRunning()) {
                anim.cancel();
            }
            if (sirenManager != null) {
                sirenManager.stopSiren();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        stopSirenAndLights();
        super.onBackPressed();
    }
}
