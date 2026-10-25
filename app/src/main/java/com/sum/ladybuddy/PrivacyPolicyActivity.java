package com.sum.ladybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Dedicated, scrollable Privacy Policy and Consent screen.
 * Persists user consent with version tracking to avoid repeated prompts,
 * while ensuring explicit, transparent agreement before accessing sensitive safety features.
 */
public class PrivacyPolicyActivity extends AppCompatActivity {

    public static final int CURRENT_POLICY_VERSION = 1;
    public static final String PREF_POLICY_VERSION = "privacy_policy_accepted_version";
    public static final String PREF_POLICY_TIMESTAMP = "privacy_accepted_timestamp";
    public static final String PREF_POLICY_ACCEPTED = "privacy_accepted";

    private CheckBox cbAccept;
    private Button btnAccept, btnDecline;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        cbAccept = findViewById(R.id.cb_accept_policy);
        btnAccept = findViewById(R.id.btn_accept_policy);
        btnDecline = findViewById(R.id.btn_decline_policy);
        btnBack = findViewById(R.id.btn_back_policy);

        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        int acceptedVersion = prefs.getInt(PREF_POLICY_VERSION, 0);

        // If previously accepted, pre-check and enable
        if (acceptedVersion >= CURRENT_POLICY_VERSION) {
            cbAccept.setChecked(true);
            btnAccept.setEnabled(true);
            btnAccept.setText("Policy Already Accepted (Continue)");
        }

        cbAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAccept.setEnabled(isChecked);
            if (isChecked) {
                btnAccept.setAlpha(1.0f);
            } else {
                btnAccept.setAlpha(0.6f);
            }
        });

        btnAccept.setOnClickListener(v -> {
            if (cbAccept.isChecked()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREF_POLICY_VERSION, CURRENT_POLICY_VERSION);
                editor.putLong(PREF_POLICY_TIMESTAMP, System.currentTimeMillis());
                editor.putBoolean(PREF_POLICY_ACCEPTED, true);
                editor.apply();

                SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
                dbHelper.logEvent("Privacy Consent", "Policy Accepted",
                        "User accepted Privacy Policy & Terms of Protection (v" + CURRENT_POLICY_VERSION + ").", "Active");

                Toast.makeText(this, "Privacy consent recorded securely. Welcome to AtmaRaksha!", Toast.LENGTH_SHORT).show();

                setResult(RESULT_OK);
                if (getIntent().getBooleanExtra("open_main_on_accept", false)) {
                    startActivity(new Intent(this, MainActivity.class));
                }
                finish();
            } else {
                Toast.makeText(this, "Please review and check the acceptance box to proceed.", Toast.LENGTH_LONG).show();
            }
        });

        btnDecline.setOnClickListener(v -> {
            Toast.makeText(this, "Privacy acceptance is required to activate guardian safety features.", Toast.LENGTH_LONG).show();
            finish();
        });

        btnBack.setOnClickListener(v -> {
            if (acceptedVersion >= CURRENT_POLICY_VERSION) {
                finish();
            } else {
                Toast.makeText(this, "Please accept the safety privacy policy to continue.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Helper method to check if the user has accepted the latest policy version.
     */
    public static boolean isPolicyAccepted(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences("demo", MODE_PRIVATE);
        return prefs.getInt(PREF_POLICY_VERSION, 0) >= CURRENT_POLICY_VERSION;
    }
}
