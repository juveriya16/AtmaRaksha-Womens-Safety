package com.sum.ladybuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class LocationTrace extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Forward directly to OpenStreetMap-powered SafeRouteActivity
        startActivity(new Intent(this, SafeRouteActivity.class));
        finish();
    }

    // Method to open external map navigation with open standard geo intent
    private void openNavigation(String source, String destination) {
        try {
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(destination));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Uri uri = Uri.parse("https://www.openstreetmap.org/search?query=" + Uri.encode(destination));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
}
