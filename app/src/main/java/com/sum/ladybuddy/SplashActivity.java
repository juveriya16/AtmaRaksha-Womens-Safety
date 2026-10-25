package com.sum.ladybuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {
    private LottieAnimationView lottieAnimationView;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_splash);
            lottieAnimationView = findViewById(R.id.lottie_layer_name);

            if (lottieAnimationView != null) {
                lottieAnimationView.addAnimatorListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        navigateToNextScreen();
                    }
                });
            }
        } catch (Throwable t) {
            android.util.Log.e("SplashActivity", "Splash layout inflation error, proceeding to next screen", t);
            navigateToNextScreen();
            return;
        }

        // Safety timeout fallback: if animation stalls or doesn't end after 2.5 seconds
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNextScreen();
            }
        }, 2500);
    }

    private synchronized void navigateToNextScreen() {
        if (hasNavigated) return;
        hasNavigated = true;

        try {
            SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

            Intent intent;
            if (isLoggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }
            startActivity(intent);
            finish();
        } catch (Throwable t) {
            android.util.Log.e("SplashActivity", "Navigation error: " + t.getMessage(), t);
            try {
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                finish();
            } catch (Throwable ignored) {}
        }
    }
}