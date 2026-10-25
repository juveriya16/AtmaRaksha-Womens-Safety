package com.sum.ladybuddy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/**
 * FakeCallHelper
 * Displays the configuration dialog for scheduling simulated incoming decoy calls.
 */
public class FakeCallHelper {

    private static final int ALARM_REQUEST_CODE = 777;

    public static void showSetupDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        // Disclaimer Header Banner
        android.widget.TextView tvDisclaimer = new android.widget.TextView(context);
        tvDisclaimer.setText("🛡️ Simulated Decoy Call\nThis simulates an incoming call to help you safely leave uncomfortable or high-risk situations.\n• No actual calls are placed\n• No call logs or carrier charges\n• Completely private");
        tvDisclaimer.setTextColor(context.getResources().getColor(R.color.brand_purple));
        tvDisclaimer.setTextSize(12f);
        tvDisclaimer.setBackgroundResource(R.drawable.bg_pill_button_lavender);
        tvDisclaimer.setPadding(32, 24, 32, 24);
        layout.addView(tvDisclaimer);

        // Caller Preset Selector
        android.widget.TextView lblCaller = new android.widget.TextView(context);
        lblCaller.setText("Select Caller Name Preset:");
        lblCaller.setTextSize(13f);
        lblCaller.setTextColor(context.getResources().getColor(R.color.text_primary));
        lblCaller.setPadding(0, 24, 0, 8);
        layout.addView(lblCaller);

        final Spinner spinnerCaller = new Spinner(context);
        final String[] callerPresets = {"Mom", "Dad", "Boss", "Friend", "Police Helpline (112)", "Custom Name"};
        ArrayAdapter<String> callerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, callerPresets);
        spinnerCaller.setAdapter(callerAdapter);
        layout.addView(spinnerCaller);

        // Custom Caller Name Input (editable)
        final EditText inputCustomName = new EditText(context);
        inputCustomName.setHint("Custom Caller Name (e.g., Office)");
        inputCustomName.setText("Mom");
        inputCustomName.setPadding(24, 20, 24, 20);
        inputCustomName.setBackgroundResource(R.drawable.bg_input_box);
        inputCustomName.setVisibility(View.GONE);
        layout.addView(inputCustomName);

        // Custom Phone Number Input
        android.widget.TextView lblNumber = new android.widget.TextView(context);
        lblNumber.setText("Caller Phone Number:");
        lblNumber.setTextSize(13f);
        lblNumber.setTextColor(context.getResources().getColor(R.color.text_primary));
        lblNumber.setPadding(0, 16, 0, 8);
        layout.addView(lblNumber);

        final EditText inputNumber = new EditText(context);
        inputNumber.setText("+91 98765 43210");
        inputNumber.setPadding(24, 20, 24, 20);
        inputNumber.setBackgroundResource(R.drawable.bg_input_box);
        layout.addView(inputNumber);

        spinnerCaller.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = callerPresets[position];
                if ("Custom Name".equals(selected)) {
                    inputCustomName.setVisibility(View.VISIBLE);
                    inputCustomName.setText("");
                    inputCustomName.requestFocus();
                } else {
                    inputCustomName.setVisibility(View.GONE);
                    inputCustomName.setText(selected);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Delay Selector
        android.widget.TextView lblDelay = new android.widget.TextView(context);
        lblDelay.setText("Delay Before Call Rings:");
        lblDelay.setTextSize(13f);
        lblDelay.setTextColor(context.getResources().getColor(R.color.text_primary));
        lblDelay.setPadding(0, 16, 0, 8);
        layout.addView(lblDelay);

        final Spinner spinnerDelay = new Spinner(context);
        final String[] delayLabels = {"Now (Instant)", "10 Seconds", "30 Seconds", "1 Minute", "5 Minutes"};
        final int[] delaySeconds = {0, 10, 30, 60, 300};
        ArrayAdapter<String> delayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, delayLabels);
        spinnerDelay.setAdapter(delayAdapter);
        layout.addView(spinnerDelay);

        // Android Background note
        android.widget.TextView tvNote = new android.widget.TextView(context);
        tvNote.setText("ℹ️ Android note: If your screen is locked or app is closed, Android displays the call as a full-screen alert or heads-up incoming call notification.");
        tvNote.setTextColor(context.getResources().getColor(R.color.text_secondary));
        tvNote.setTextSize(11f);
        tvNote.setPadding(0, 16, 0, 8);
        layout.addView(tvNote);

        builder.setTitle("Schedule Fake Incoming Call");
        builder.setView(layout);

        builder.setPositiveButton("Schedule Decoy Call", (dialog, which) -> {
            String name = inputCustomName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                name = spinnerCaller.getSelectedItem().toString();
                if ("Custom Name".equals(name)) name = "Friend";
            }
            String number = inputNumber.getText().toString().trim();
            if (TextUtils.isEmpty(number)) number = "+91 98765 43210";

            int selectedDelayIndex = spinnerDelay.getSelectedItemPosition();
            int delaySec = delaySeconds[selectedDelayIndex];

            scheduleFakeCall(context, name, number, delaySec);
        });

        builder.setNeutralButton("Cancel Scheduled", (dialog, which) -> {
            cancelScheduledFakeCall(context);
            Toast.makeText(context, "Any pending simulated call cancelled", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    public static void scheduleFakeCall(Context context, String callerName, String callerNumber, int delaySeconds) {
        if (delaySeconds <= 0) {
            // Trigger immediately
            Intent intent = new Intent(context, FakeCallActivity.class);
            intent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, callerName);
            intent.putExtra(FakeCallActivity.EXTRA_CALLER_NUMBER, callerNumber);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, FakeCallReceiver.class);
            intent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, callerName);
            intent.putExtra(FakeCallActivity.EXTRA_CALLER_NUMBER, callerNumber);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags);

            long triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }

            Toast.makeText(context, "📞 Decoy call scheduled in " + delaySeconds + "s! You can lock phone or switch apps.", Toast.LENGTH_LONG).show();
        }
    }

    public static void cancelScheduledFakeCall(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, FakeCallReceiver.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE : PendingIntent.FLAG_NO_CREATE;
        PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags);
        if (pi != null) {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }
}
