package com.sum.ladybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnGetStarted, btnLogin, btnDemo;
    private TextView tvPrivacyFootnote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnGetStarted = findViewById(R.id.btn_welcome_get_started);
        btnLogin = findViewById(R.id.btn_welcome_login);
        btnDemo = findViewById(R.id.btn_welcome_demo);
        tvPrivacyFootnote = findViewById(R.id.tv_welcome_privacy_footnote);

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        if (tvPrivacyFootnote != null) {
            tvPrivacyFootnote.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, PrivacyPolicyActivity.class));
            });
        }

        btnDemo.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_logged_in", true);
            if (!prefs.contains("user_name")) {
                editor.putString("user_name", "Juveriya Khan");
                editor.putString("user_phone", "+91 98765 43210");
                editor.putString("user_email", "juveriya@example.com");
                editor.putString("user_blood_group", "O+");
            }
            editor.apply();

            Toast.makeText(this, "Quick Demo Mode activated. Welcome!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
