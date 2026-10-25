package com.sum.ladybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmergencyPhone, etBlood, etPassword;
    private CheckBox cbTerms;
    private Button btnSignUp;
    private TextView tvLogin;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        btnBack = findViewById(R.id.btn_back_signup);
        etName = findViewById(R.id.et_signup_name);
        etPhone = findViewById(R.id.et_signup_phone);
        etEmergencyPhone = findViewById(R.id.et_signup_emergency_phone);
        etBlood = findViewById(R.id.et_signup_blood);
        etPassword = findViewById(R.id.et_signup_password);
        cbTerms = findViewById(R.id.cb_signup_terms);
        btnSignUp = findViewById(R.id.btn_do_signup);
        tvLogin = findViewById(R.id.tv_goto_login);

        btnBack.setOnClickListener(v -> finish());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String emergencyPhone = etEmergencyPhone.getText().toString().trim();
            String blood = etBlood.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Please enter your name");
                etName.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Please enter your phone number");
                etPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(emergencyPhone)) {
                etEmergencyPhone.setError("Please provide an emergency contact");
                etEmergencyPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password) || password.length() < 4) {
                etPassword.setError("Password should be at least 4 characters");
                etPassword.requestFocus();
                return;
            }

            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please accept the safety agreement to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save in demo SharedPreferences for seamless integration across all app features
            SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_name", name);
            editor.putString("user_phone", phone);
            editor.putString("user_blood_group", TextUtils.isEmpty(blood) ? "Not Specified" : blood);
            editor.putBoolean("is_logged_in", true);

            // Automatically register primary emergency contact if not already set
            String existingPhone1 = prefs.getString("phone1", "");
            if (TextUtils.isEmpty(existingPhone1)) {
                editor.putString("phone1", emergencyPhone);
            }
            editor.apply();

            // Log event in safety history
            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Account Registered", "Guardian Profile Activated", "Welcome " + name + "! Safety shield and emergency triggers are active.", "Active");

            Toast.makeText(SignUpActivity.this, "Safety Profile created successfully!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
