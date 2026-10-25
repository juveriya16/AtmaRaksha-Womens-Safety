package com.sum.ladybuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class TrustedContactsManager {

    private static final String PREF_NAME = "demo";

    public static List<TrustedContactModel> getTrustedContacts(Context context) {
        List<TrustedContactModel> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String p1 = prefs.getString("phone1", "");
        String p2 = prefs.getString("phone2", "");
        String p3 = prefs.getString("phone3", "");
        String p4 = prefs.getString("phone4", "");

        String n1 = prefs.getString("name1", "Primary Guardian");
        String n2 = prefs.getString("name2", "Family Contact");
        String n3 = prefs.getString("name3", "Trusted Friend");
        String n4 = prefs.getString("name4", "Emergency Contact");

        boolean s1 = prefs.getBoolean("monitor_contact_1", true);
        boolean s2 = prefs.getBoolean("monitor_contact_2", true);
        boolean s3 = prefs.getBoolean("monitor_contact_3", false);
        boolean s4 = prefs.getBoolean("monitor_contact_4", false);

        if (!TextUtils.isEmpty(p1)) {
            list.add(new TrustedContactModel("1", n1, p1, "Primary Circle", s1));
        }
        if (!TextUtils.isEmpty(p2)) {
            list.add(new TrustedContactModel("2", n2, p2, "Family", s2));
        }
        if (!TextUtils.isEmpty(p3)) {
            list.add(new TrustedContactModel("3", n3, p3, "Friend", s3));
        }
        if (!TextUtils.isEmpty(p4)) {
            list.add(new TrustedContactModel("4", n4, p4, "Alternate Guardian", s4));
        }

        // If no contacts are configured yet, provide a placeholder guide item
        if (list.isEmpty()) {
            list.add(new TrustedContactModel("0", "Add Trusted Contact", "", "Configure in Settings", false));
        }

        return list;
    }

    public static List<TrustedContactModel> getContacts(Context context) {
        return getTrustedContacts(context);
    }

    public static void updateMonitoringPreference(Context context, String contactId, boolean selected) {
        setContactMonitoringSelected(context, contactId, selected);
    }

    public static void setContactMonitoringSelected(Context context, String contactId, boolean selected) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("monitor_contact_" + contactId, selected).apply();
    }

    public static List<String> getSelectedMonitoringNumbers(Context context) {
        List<String> numbers = new ArrayList<>();
        List<TrustedContactModel> contacts = getTrustedContacts(context);
        for (TrustedContactModel c : contacts) {
            if (c.isSelectedForMonitoring() && !TextUtils.isEmpty(c.getPhoneNumber())) {
                numbers.add(c.getPhoneNumber().trim());
            }
        }
        return numbers;
    }

    public static String getSelectedMonitoringSummary(Context context) {
        List<TrustedContactModel> contacts = getTrustedContacts(context);
        StringBuilder sb = new StringBuilder();
        for (TrustedContactModel c : contacts) {
            if (c.isSelectedForMonitoring() && !TextUtils.isEmpty(c.getPhoneNumber())) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.getName()).append(" (").append(c.getPhoneNumber()).append(")");
            }
        }
        return sb.length() > 0 ? sb.toString() : "None selected";
    }
}
