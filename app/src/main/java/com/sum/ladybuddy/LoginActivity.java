package com.sum.ladybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin, btnDemoFill;
    private TextView tvForgot;
    private View llGoToSignUp;
    private ImageView btnBack, ivTogglePassword;
    private View btnSocialGoogle, btnSocialFacebook, btnSocialPhone;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnBack = findViewById(R.id.btn_back_login);
        etIdentifier = findViewById(R.id.et_login_identifier);
        etPassword = findViewById(R.id.et_login_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        btnLogin = findViewById(R.id.btn_action_login);
        btnDemoFill = findViewById(R.id.btn_demo_autofill);
        tvForgot = findViewById(R.id.tv_forgot_password);
        llGoToSignUp = findViewById(R.id.ll_go_to_signup);

        btnSocialGoogle = findViewById(R.id.btn_social_google);
        btnSocialFacebook = findViewById(R.id.btn_social_facebook);
        btnSocialPhone = findViewById(R.id.btn_social_phone);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (ivTogglePassword != null) {
            ivTogglePassword.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ivTogglePassword.setAlpha(0.6f);
                    isPasswordVisible = false;
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ivTogglePassword.setAlpha(1.0f);
                    isPasswordVisible = true;
                }
                etPassword.setSelection(etPassword.getText().length());
            });
        }

        if (llGoToSignUp != null) {
            llGoToSignUp.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            });
        }

        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> {
                Toast.makeText(this, "A secure password reset link has been dispatched to your email/phone.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDemoFill != null) {
            btnDemoFill.setOnClickListener(v -> {
                etIdentifier.setText("juveriya.khan@example.com");
                etPassword.setText("AtmaRaksha2026");
                Toast.makeText(this, "Demo credentials populated! Tap Log In to enter.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSocialGoogle != null) {
            btnSocialGoogle.setOnClickListener(v -> {
                etIdentifier.setText("juveriya.google@example.com");
                etPassword.setText("GoogleOAuthVerified");
                Toast.makeText(this, "Authenticating with Google Account...", Toast.LENGTH_SHORT).show();
                btnLogin.performClick();
            });
        }

        if (btnSocialFacebook != null) {
            btnSocialFacebook.setOnClickListener(v -> {
                etIdentifier.setText("juveriya.fb@example.com");
                etPassword.setText("FacebookOAuthVerified");
                Toast.makeText(this, "Authenticating with Facebook...", Toast.LENGTH_SHORT).show();
                btnLogin.performClick();
            });
        }

        if (btnSocialPhone != null) {
            btnSocialPhone.setOnClickListener(v -> {
                etIdentifier.setText("+91 98765 43210");
                etPassword.setText("OtpSecureCode99");
                Toast.makeText(this, "Authenticating with Mobile Phone OTP...", Toast.LENGTH_SHORT).show();
                btnLogin.performClick();
            });
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String identifier = etIdentifier.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(identifier)) {
                    etIdentifier.setError("Please enter your email or phone");
                    etIdentifier.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    etPassword.setError("Please enter your password");
                    etPassword.requestFocus();
                    return;
                }

                // Save login state in SharedPreferences
                SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                if (!prefs.contains("user_name")) {
                    editor.putString("user_name", "Juveriya");
                    editor.putString("user_phone", identifier);
                    editor.putString("user_email", identifier.contains("@") ? identifier : "juveriya@example.com");
                    editor.putString("user_blood_group", "O+");
                }
                editor.apply();

                SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
                dbHelper.logEvent("Auth", "User Logged In", "Signed in as " + prefs.getString("user_name", "Juveriya"), "Active");

                Toast.makeText(LoginActivity.this, "Welcome, " + prefs.getString("user_name", "Juveriya") + "!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
