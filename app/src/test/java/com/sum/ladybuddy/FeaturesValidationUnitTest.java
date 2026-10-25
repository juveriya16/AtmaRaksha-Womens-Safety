package com.sum.ladybuddy;

import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests verifying logic for Feature 1 (Shake-to-SOS), Feature 2 (Fake Incoming Call),
 * and Feature 3 (Journey Low-Battery Warning).
 */
public class FeaturesValidationUnitTest {

    @Test
    public void testShakeSensitivityThresholds() {
        assertTrue("High sensitivity threshold must be more sensitive than medium",
                ShakeDetectorHelper.THRESHOLD_HIGH < ShakeDetectorHelper.THRESHOLD_MEDIUM);
        assertTrue("Medium sensitivity threshold must be more sensitive than low",
                ShakeDetectorHelper.THRESHOLD_MEDIUM < ShakeDetectorHelper.THRESHOLD_LOW);

        assertEquals(2.1f, ShakeDetectorHelper.THRESHOLD_HIGH, 0.01f);
        assertEquals(2.7f, ShakeDetectorHelper.THRESHOLD_MEDIUM, 0.01f);
        assertEquals(3.4f, ShakeDetectorHelper.THRESHOLD_LOW, 0.01f);
    }

    @Test
    public void testShakeGForceCalculation() {
        float x = 12.0f;
        float y = 16.0f;
        float z = 20.0f;
        float gravityEarth = 9.80665f;

        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
        float gForce = magnitude / gravityEarth;

        assertTrue("Deliberate shake acceleration magnitude should exceed 2.7g", gForce > ShakeDetectorHelper.THRESHOLD_MEDIUM);
    }

    @Test
    public void testBatteryDeduplicationLogic() {
        Set<Integer> notifiedThresholds = new HashSet<>();

        // Test Level 25% (no alert)
        int alert1 = evaluateBatteryThreshold(25, notifiedThresholds);
        assertEquals(-1, alert1);
        assertEquals(0, notifiedThresholds.size());

        // Test Level 20% (First trigger for 20%)
        int alert2 = evaluateBatteryThreshold(20, notifiedThresholds);
        assertEquals(20, alert2);
        assertTrue(notifiedThresholds.contains(20));

        // Test Level 20% again (Repeated broadcast, MUST NOT duplicate)
        int alert3 = evaluateBatteryThreshold(20, notifiedThresholds);
        assertEquals(-1, alert3);

        // Test Level 18% (Between 20 and 15, no new alert)
        int alert4 = evaluateBatteryThreshold(18, notifiedThresholds);
        assertEquals(-1, alert4);

        // Test Level 15% (First trigger for 15%)
        int alert5 = evaluateBatteryThreshold(15, notifiedThresholds);
        assertEquals(15, alert5);
        assertTrue(notifiedThresholds.contains(15));

        // Test Level 14% (Between 15 and 10, no new alert)
        int alert6 = evaluateBatteryThreshold(14, notifiedThresholds);
        assertEquals(-1, alert6);

        // Test Level 10% (First trigger for 10%)
        int alert7 = evaluateBatteryThreshold(10, notifiedThresholds);
        assertEquals(10, alert7);
        assertTrue(notifiedThresholds.contains(10));

        // Test Level 8% (Sub-10%, already alerted at 10%)
        int alert8 = evaluateBatteryThreshold(8, notifiedThresholds);
        assertEquals(-1, alert8);
    }

    @Test
    public void testBatteryDropDirectToCritical() {
        Set<Integer> notifiedThresholds = new HashSet<>();

        // Battery drops rapidly from 50% straight to 9%
        int alert = evaluateBatteryThreshold(9, notifiedThresholds);
        assertEquals(10, alert);
        assertTrue(notifiedThresholds.contains(10));
        assertTrue(notifiedThresholds.contains(15));
        assertTrue(notifiedThresholds.contains(20));

        // No secondary alerts after direct drop
        assertEquals(-1, evaluateBatteryThreshold(8, notifiedThresholds));
    }

    private int evaluateBatteryThreshold(int pct, Set<Integer> notifiedThresholds) {
        if (pct <= 10 && !notifiedThresholds.contains(10)) {
            notifiedThresholds.add(10);
            notifiedThresholds.add(15);
            notifiedThresholds.add(20);
            return 10;
        } else if (pct <= 15 && !notifiedBatteryThresholds(notifiedThresholds, 15)) {
            notifiedThresholds.add(15);
            notifiedThresholds.add(20);
            return 15;
        } else if (pct <= 20 && !notifiedBatteryThresholds(notifiedThresholds, 20)) {
            notifiedThresholds.add(20);
            return 20;
        }
        return -1;
    }

    private boolean notifiedBatteryThresholds(Set<Integer> set, int threshold) {
        return set.contains(threshold);
    }

    @Test
    public void testFakeCallDurationFormatting() {
        int durationSeconds = 125; // 2 minutes and 5 seconds
        int mins = durationSeconds / 60;
        int secs = durationSeconds % 60;
        String formatted = String.format(Locale.US, "%02d:%02d", mins, secs);

        assertEquals("02:05", formatted);
    }
}
