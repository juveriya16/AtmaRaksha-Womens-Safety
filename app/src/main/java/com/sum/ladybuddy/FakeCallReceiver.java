package com.sum.ladybuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * FakeCallReceiver
 * BroadcastReceiver triggered by AlarmManager to launch the simulated call
 * when a delay was configured (10s, 30s, 1m, 5m).
 */
public class FakeCallReceiver extends BroadcastReceiver {

    public static final String ACTION_TRIGGER_FAKE_CALL = "com.sum.ladybuddy.ACTION_TRIGGER_FAKE_CALL";
    public static final String ACTION_DISMISS_FAKE_CALL = "com.sum.ladybuddy.ACTION_DISMISS_FAKE_CALL";
    public static final String CHANNEL_FAKECALL = "atmaraksha_fakecall_channel";
    public static final int NOTIF_FAKECALL_ID = 888;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (ACTION_DISMISS_FAKE_CALL.equals(action)) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_FAKECALL_ID);
            }
            return;
        }

        String callerName = intent.getStringExtra(FakeCallActivity.EXTRA_CALLER_NAME);
        String callerNumber = intent.getStringExtra(FakeCallActivity.EXTRA_CALLER_NUMBER);
        if (callerName == null) callerName = "Mom";
        if (callerNumber == null) callerNumber = "+91 98765 43210";

        createNotificationChannel(context);

        // Intent to launch FakeCallActivity in full screen
        Intent callIntent = new Intent(context, FakeCallActivity.class);
        callIntent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, callerName);
        callIntent.putExtra(FakeCallActivity.EXTRA_CALLER_NUMBER, callerNumber);
        callIntent.putExtra(FakeCallActivity.EXTRA_AUTO_ANSWER, false);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent fullScreenPI = PendingIntent.getActivity(context, 101, callIntent, flags);

        // Intent for Answer Action
        Intent answerIntent = new Intent(context, FakeCallActivity.class);
        answerIntent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, callerName);
        answerIntent.putExtra(FakeCallActivity.EXTRA_CALLER_NUMBER, callerNumber);
        answerIntent.putExtra(FakeCallActivity.EXTRA_AUTO_ANSWER, true);
        answerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent answerPI = PendingIntent.getActivity(context, 102, answerIntent, flags);

        // Intent for Dismiss Action
        Intent dismissIntent = new Intent(context, FakeCallReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS_FAKE_CALL);
        PendingIntent dismissPI = PendingIntent.getBroadcast(context, 103, dismissIntent, flags);

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FAKECALL)
                .setSmallIcon(R.drawable.ic_phone_dial)
                .setContentTitle("Incoming Call")
                .setContentText(callerName + " (" + callerNumber + ")")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(true)
                .setSound(ringtoneUri)
                .setVibrate(new long[]{0, 1000, 1000})
                .setFullScreenIntent(fullScreenPI, true)
                .setContentIntent(fullScreenPI)
                .addAction(R.drawable.ic_phone_dial, "Answer", answerPI)
                .addAction(R.drawable.ic_call_end, "Decline", dismissPI);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_FAKECALL_ID, builder.build());
        }

        // Direct Activity Launch (if allowed by system / screen on)
        try {
            context.startActivity(callIntent);
        } catch (Exception e) {
            Log.w("FakeCallReceiver", "Direct activity start blocked by OS; full screen intent notification will display: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_FAKECALL,
                    "Simulated Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Full-screen simulated incoming decoy calls");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 1000});

            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            channel.setSound(ringtoneUri, audioAttributes);

            nm.createNotificationChannel(channel);
        }
    }
}
