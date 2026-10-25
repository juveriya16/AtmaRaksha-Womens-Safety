package com.sum.ladybuddy;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TrustedContactsActivity extends AppCompatActivity {

    private static final int REQ_PICK_1 = 1001;
    private static final int REQ_PICK_2 = 1002;
    private static final int REQ_PICK_3 = 1003;
    private static final int REQ_PICK_4 = 1004;
    private static final int REQ_PERM_SMS = 201;

    private static final String ACTION_SMS_SENT = "com.sum.ladybuddy.SMS_SENT_STATUS";
    private static final String ACTION_SMS_DELIVERED = "com.sum.ladybuddy.SMS_DELIVERED_STATUS";

    private EditText etPhone1, etPhone2, etPhone3, etPhone4, etMessage;
    private Button btnPick1, btnPick2, btnPick3, btnPick4;
    private Button btnSave, btnTestSms;
    private ImageView btnBack;

    private BroadcastReceiver smsStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contacts);

        btnBack = findViewById(R.id.btn_back_contacts);
        etPhone1 = findViewById(R.id.et_contact_phone1);
        etPhone2 = findViewById(R.id.et_contact_phone2);
        etPhone3 = findViewById(R.id.et_contact_phone3);
        etPhone4 = findViewById(R.id.et_contact_phone4);
        etMessage = findViewById(R.id.et_sos_custom_message);

        btnPick1 = findViewById(R.id.btn_pick_contact1);
        btnPick2 = findViewById(R.id.btn_pick_contact2);
        btnPick3 = findViewById(R.id.btn_pick_contact3);
        btnPick4 = findViewById(R.id.btn_pick_contact4);

        btnSave = findViewById(R.id.btn_save_all_contacts);
        btnTestSms = findViewById(R.id.btn_send_test_sms);

        btnBack.setOnClickListener(v -> finish());

        // Load existing saved values from SharedPreferences
        loadSavedPreferences();

        // Contact Pickers
        btnPick1.setOnClickListener(v -> pickContactFromPhone(REQ_PICK_1));
        btnPick2.setOnClickListener(v -> pickContactFromPhone(REQ_PICK_2));
        btnPick3.setOnClickListener(v -> pickContactFromPhone(REQ_PICK_3));
        btnPick4.setOnClickListener(v -> pickContactFromPhone(REQ_PICK_4));

        // Save
        btnSave.setOnClickListener(v -> savePreferences());

        // Test SMS with explicit confirmation and modern Android composer fallback
        btnTestSms.setOnClickListener(v -> promptTestSmsConfirmation());

        registerSmsFeedbackReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedPreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsStatusReceiver != null) {
            try {
                unregisterReceiver(smsStatusReceiver);
            } catch (Exception ignored) {}
        }
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        etPhone1.setText(prefs.getString("phone1", ""));
        etPhone2.setText(prefs.getString("phone2", ""));
        etPhone3.setText(prefs.getString("phone3", ""));
        etPhone4.setText(prefs.getString("phone4", ""));
        etMessage.setText(prefs.getString("msg", "I am in danger, please come fast..."));
    }

    private void savePreferences() {
        String p1 = sanitizePhoneNumber(etPhone1.getText().toString());
        String p2 = sanitizePhoneNumber(etPhone2.getText().toString());
        String p3 = sanitizePhoneNumber(etPhone3.getText().toString());
        String p4 = sanitizePhoneNumber(etPhone4.getText().toString());
        String msg = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(msg)) {
            msg = "I am in danger, please come fast...";
        }

        SharedPreferences prefs = getSharedPreferences("demo", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("phone1", p1);
        editor.putString("phone2", p2);
        editor.putString("phone3", p3);
        editor.putString("phone4", p4);
        editor.putString("msg", msg);
        editor.commit();

        SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
        dbHelper.logEvent("Contacts Updated", "Emergency Circle Saved",
                "Updated emergency circle contacts (" + (TextUtils.isEmpty(p1) ? "None" : p1) + ") and SOS message.", "Saved");

        Toast.makeText(this, "Emergency contacts & SOS message saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private String sanitizePhoneNumber(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        // Preserve leading + for international format, remove other non-digits
        boolean hasPlus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^0-9]", "");
        return hasPlus ? ("+" + digits) : digits;
    }

    private void pickContactFromPhone(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open phone contacts.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String number = cursor.getString(numberIndex);
                    cursor.close();

                    if (number != null) {
                        number = sanitizePhoneNumber(number);
                        if (requestCode == REQ_PICK_1) etPhone1.setText(number);
                        else if (requestCode == REQ_PICK_2) etPhone2.setText(number);
                        else if (requestCode == REQ_PICK_3) etPhone3.setText(number);
                        else if (requestCode == REQ_PICK_4) etPhone4.setText(number);
                    }
                }
            }
        }
    }

    private void promptTestSmsConfirmation() {
        final String p1 = sanitizePhoneNumber(etPhone1.getText().toString());
        if (TextUtils.isEmpty(p1)) {
            etPhone1.setError("Please enter Contact 1 first");
            etPhone1.requestFocus();
            Toast.makeText(this, "Please enter Contact 1 first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Test Safety SMS");
        builder.setMessage("Send a test safety verification message to Primary Guardian (" + p1 + ")?\n\nChoose 'Send in Messages App' to review the message before dispatching, or 'Direct SMS' if permission is granted.");
        
        builder.setPositiveButton("Open in Messages App", (dialog, which) -> {
            openSmsComposer(p1, "[AtmaRaksha Safety Test] This is a verification test from your guardian contact. Live SOS readiness is confirmed.");
        });

        builder.setNeutralButton("Direct SMS", (dialog, which) -> {
            executeDirectSms(p1);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openSmsComposer(String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + Uri.encode(phoneNumber)));
            intent.putExtra("sms_body", message);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(this);
            dbHelper.logEvent("Test SMS", "Composer Opened", "Prepared test verification SMS for " + phoneNumber + " in default SMS app.", "Pending User Send");

            Toast.makeText(this, "Review and tap send in your SMS app.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open SMS composer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void executeDirectSms(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQ_PERM_SMS);
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            String testMessage = "[AtmaRaksha Safety Test] This is a verification test from your guardian contact. Live SOS readiness is confirmed.";

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_SENT), flags);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_DELIVERED), flags);

            smsManager.sendTextMessage(phoneNumber, null, testMessage, sentPI, deliveredPI);
            Toast.makeText(this, "Dispatching SMS... waiting for carrier response.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Graceful fallback to composer if direct send fails
            Toast.makeText(this, "Direct SMS unavailable: " + e.getMessage() + ". Opening Messages app...", Toast.LENGTH_SHORT).show();
            openSmsComposer(phoneNumber, "[AtmaRaksha Safety Test] This is a verification test from your guardian contact. Live SOS readiness is confirmed.");
        }
    }

    private void registerSmsFeedbackReceiver() {
        smsStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                SafetyHistoryDbHelper dbHelper = new SafetyHistoryDbHelper(TrustedContactsActivity.this);
                if (ACTION_SMS_SENT.equals(action)) {
                    int resultCode = getResultCode();
                    if (resultCode == Activity.RESULT_OK) {
                        Toast.makeText(context, "Carrier confirmed: Test SMS dispatched successfully!", Toast.LENGTH_LONG).show();
                        dbHelper.logEvent("Test SMS", "Direct SMS Dispatched", "Carrier confirmed dispatch of test SMS.", "Delivered to Network");
                    } else {
                        Toast.makeText(context, "Carrier error: SMS could not be sent (Code: " + resultCode + ").", Toast.LENGTH_LONG).show();
                        dbHelper.logEvent("Test SMS", "SMS Send Failure", "Carrier returned code " + resultCode, "Failed");
                    }
                } else if (ACTION_SMS_DELIVERED.equals(action)) {
                    Toast.makeText(context, "Recipient carrier confirmed SMS delivered!", Toast.LENGTH_SHORT).show();
                    dbHelper.logEvent("Test SMS", "SMS Delivered", "Recipient handset confirmed receipt.", "Delivered");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SMS_SENT);
        filter.addAction(ACTION_SMS_DELIVERED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(smsStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsStatusReceiver, filter);
        }
    }
}
