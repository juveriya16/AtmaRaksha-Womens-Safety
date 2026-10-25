package com.sum.ladybuddy;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class BottomNavHelper {

    public enum Tab {
        HOME, MAP, SOS, ACTIVITY, PROFILE
    }

    public static void setupBottomNav(final Activity activity, Tab currentTab) {
        View tabHome = activity.findViewById(R.id.tab_home);
        View tabMap = activity.findViewById(R.id.tab_map);
        View tabSos = activity.findViewById(R.id.tab_sos);
        View tabActivity = activity.findViewById(R.id.tab_activity);
        View tabProfile = activity.findViewById(R.id.tab_profile);

        if (tabHome == null) return;

        ImageView iconHome = activity.findViewById(R.id.icon_home);
        TextView labelHome = activity.findViewById(R.id.label_home);

        ImageView iconMap = activity.findViewById(R.id.icon_map);
        TextView labelMap = activity.findViewById(R.id.label_map);

        ImageView iconActivity = activity.findViewById(R.id.icon_activity);
        TextView labelActivity = activity.findViewById(R.id.label_activity);

        ImageView iconProfile = activity.findViewById(R.id.icon_profile);
        TextView labelProfile = activity.findViewById(R.id.label_profile);

        int activeColor = ContextCompat.getColor(activity, R.color.brand_pink);
        int inactiveColor = ContextCompat.getColor(activity, R.color.nav_item_inactive);

        // Reset all
        if (iconHome != null) iconHome.setColorFilter(inactiveColor);
        if (labelHome != null) labelHome.setTextColor(inactiveColor);

        if (iconMap != null) iconMap.setColorFilter(inactiveColor);
        if (labelMap != null) labelMap.setTextColor(inactiveColor);

        if (iconActivity != null) iconActivity.setColorFilter(inactiveColor);
        if (labelActivity != null) labelActivity.setTextColor(inactiveColor);

        if (iconProfile != null) iconProfile.setColorFilter(inactiveColor);
        if (labelProfile != null) labelProfile.setTextColor(inactiveColor);

        // Highlight current
        switch (currentTab) {
            case HOME:
                if (iconHome != null) iconHome.setColorFilter(activeColor);
                if (labelHome != null) labelHome.setTextColor(activeColor);
                break;
            case MAP:
                if (iconMap != null) iconMap.setColorFilter(activeColor);
                if (labelMap != null) labelMap.setTextColor(activeColor);
                break;
            case ACTIVITY:
                if (iconActivity != null) iconActivity.setColorFilter(activeColor);
                if (labelActivity != null) labelActivity.setTextColor(activeColor);
                break;
            case PROFILE:
                if (iconProfile != null) iconProfile.setColorFilter(activeColor);
                if (labelProfile != null) labelProfile.setTextColor(activeColor);
                break;
            case SOS:
                break;
        }

        // Click listeners
        tabHome.setOnClickListener(v -> {
            if (currentTab != Tab.HOME) {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        });

        tabMap.setOnClickListener(v -> {
            if (currentTab != Tab.MAP) {
                Intent intent = new Intent(activity, SafeRouteActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        });

        tabSos.setOnClickListener(v -> {
            Intent intent = new Intent(activity, SosEmergencyActivity.class);
            activity.startActivity(intent);
        });

        tabActivity.setOnClickListener(v -> {
            if (currentTab != Tab.ACTIVITY) {
                Intent intent = new Intent(activity, SafetyHistoryActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        });

        tabProfile.setOnClickListener(v -> {
            if (currentTab != Tab.PROFILE) {
                Intent intent = new Intent(activity, ProfileSettingsActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        });
    }
}
