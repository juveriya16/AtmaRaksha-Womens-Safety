package com.sum.ladybuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class AboutUs extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        ImageView btnBack = findViewById(R.id.btn_back_about);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button button = findViewById(R.id.contactUs);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    composeEmail();
                }
            });
        }

        Button btnPolicy = findViewById(R.id.btn_open_privacy_policy);
        if (btnPolicy != null) {
            btnPolicy.setOnClickListener(v -> {
                startActivity(new Intent(AboutUs.this, PrivacyPolicyActivity.class));
            });
        }
    }

    public void composeEmail() {
        String subject = "Inquiry regarding AtmaRaksha Women Safety";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support.atmaraksha@example.com?subject=" + Uri.encode(subject)));

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}