package com.sum.ladybuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class OfflineSafetyActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout containerOfflineContacts;
    private TextView tvDeliveryStatus;

    private Button btnDial112, btnDial1091, btnDial181, btnDial108;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_safety);

        btnBack = findViewById(R.id.btn_back_offline);
        containerOfflineContacts = findViewById(R.id.container_offline_contacts);
        tvDeliveryStatus = findViewById(R.id.tv_offline_delivery_status);

        btnDial112 = findViewById(R.id.btn_dial_112);
        btnDial1091 = findViewById(R.id.btn_dial_1091);
        btnDial181 = findViewById(R.id.btn_dial_181);
        btnDial108 = findViewById(R.id.btn_dial_108);

        btnBack.setOnClickListener(v -> finish());

        setupEmergencyDials();
        loadOfflineContacts();
    }

    private void setupEmergencyDials() {
        btnDial112.setOnClickListener(v -> promptDial("National Emergency (112)", "112"));
        btnDial1091.setOnClickListener(v -> promptDial("Women Helpline (1091)", "1091"));
        btnDial181.setOnClickListener(v -> promptDial("Domestic Abuse Helpline (181)", "181"));
        btnDial108.setOnClickListener(v -> promptDial("Emergency Ambulance (108)", "108"));
    }

    private void loadOfflineContacts() {
        containerOfflineContacts.removeAllViews();
        List<TrustedContactModel> contacts = TrustedContactsManager.getTrustedContacts(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        int count = 0;
        for (final TrustedContactModel c : contacts) {
            if (TextUtils.isEmpty(c.getPhoneNumber())) continue;
            count++;

            View item = inflater.inflate(R.layout.item_nearby_place, containerOfflineContacts, false);
            TextView tvName = item.findViewById(R.id.tv_place_name);
            TextView tvDist = item.findViewById(R.id.tv_place_distance_badge);
            TextView tvAddr = item.findViewById(R.id.tv_place_address);
            TextView tvTag = item.findViewById(R.id.tv_place_verified_tag);
            Button btnDirections = item.findViewById(R.id.btn_place_directions);
            Button btnCall = item.findViewById(R.id.btn_place_call);

            tvName.setText(c.getName() + " (" + c.getRelationship() + ")");
            tvDist.setText("Cached Phone: " + c.getPhoneNumber());
            tvAddr.setText("Direct offline telephony available without data.");
            tvTag.setText("Guardian");

            btnDirections.setText("SMS Composer");
            btnCall.setText("Dial Directly");

            final String phone = c.getPhoneNumber();
            btnDirections.setOnClickListener(v -> openOfflineSmsComposer(phone));
            btnCall.setOnClickListener(v -> promptDial(c.getName(), phone));

            containerOfflineContacts.addView(item);
        }

        if (count == 0) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("⚠️ No emergency contacts saved yet. Please configure contacts in Trusted Contacts.");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyTv.setTextSize(12);
            emptyTv.setPadding(0, 12, 0, 12);
            containerOfflineContacts.addView(emptyTv);
        }
    }

    private void promptDial(String serviceName, final String phoneNumber) {
        new AlertDialog.Builder(this)
                .setTitle("Call " + serviceName + "?")
                .setMessage("Open phone dialer to call " + phoneNumber + " directly? Operates via standard GSM telecom without internet.")
                .setPositiveButton("Open Phone Dialer", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);

                    SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
                    dbHelper.logEvent("Offline Call", "Dialer Opened", "Opened phone dialer for " + serviceName + " (" + phoneNumber + ")", "Offline Handoff");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openOfflineSmsComposer(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + Uri.encode(phoneNumber)));
            intent.putExtra("sms_body", "[AtmaRaksha Offline Alert] I need help. Please contact me immediately. (Dispatched offline)");
            startActivity(intent);

            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Offline SMS", "Composer Handoff", "Opened SMS composer for guardian " + phoneNumber, "Handoff to Messages App");

            Toast.makeText(this, "Handoff to SMS app. Tap Send to dispatch.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open SMS composer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
