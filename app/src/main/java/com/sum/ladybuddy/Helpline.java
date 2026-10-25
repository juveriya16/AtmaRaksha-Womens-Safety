package com.sum.ladybuddy;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class Helpline extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helpline);

        ImageView btnBack = findViewById(R.id.btn_back_helpline);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View nationalHelpline = findViewById(R.id.national_helpline);
        View womenHelpline = findViewById(R.id.women_helpline);
        View distressHelpline = findViewById(R.id.distress_helpline);
        View ncwHelpline = findViewById(R.id.ncw_helpline);
        View ambulanceHelpline = findViewById(R.id.ambulance_helpline);
        View childlineHelpline = findViewById(R.id.childline_helpline);

        if (nationalHelpline != null) nationalHelpline.setOnClickListener(v -> confirmAndDial("National Emergency (Police/Fire/Medical)", "112"));
        if (womenHelpline != null) womenHelpline.setOnClickListener(v -> confirmAndDial("Women's Helpline (All India)", "181"));
        if (distressHelpline != null) distressHelpline.setOnClickListener(v -> confirmAndDial("Women in Distress (Police)", "1091"));
        if (ncwHelpline != null) ncwHelpline.setOnClickListener(v -> confirmAndDial("NCW Domestic Abuse Helpline", "7827170170"));
        if (ambulanceHelpline != null) ambulanceHelpline.setOnClickListener(v -> confirmAndDial("Ambulance & Trauma Care", "108"));
        if (childlineHelpline != null) childlineHelpline.setOnClickListener(v -> confirmAndDial("Childline Emergency", "1098"));
    }

    private void confirmAndDial(String serviceTitle, final String phoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Call " + serviceTitle + "?");
        builder.setMessage("Open your phone dialer to connect to official emergency number " + phoneNumber + "?\n\nCalls are free and toll-free across India.");
        builder.setPositiveButton("Open Phone Dialer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
