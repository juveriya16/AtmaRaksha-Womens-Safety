package com.sum.ladybuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SafetyHistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "atmaraksha_safety.db";
    private static final int DATABASE_VERSION = 2;

    // Table 1: Safety Events
    public static final String TABLE_EVENTS = "safety_events";
    public static final String COL_EVENT_ID = "id";
    public static final String COL_EVENT_TYPE = "event_type";
    public static final String COL_EVENT_TITLE = "title";
    public static final String COL_EVENT_DETAILS = "details";
    public static final String COL_EVENT_TIMESTAMP = "timestamp";
    public static final String COL_EVENT_STATUS = "status";

    // Table 2: Journeys
    public static final String TABLE_JOURNEYS = "journeys";
    public static final String COL_JOURNEY_ID = "id";
    public static final String COL_JOURNEY_ORIGIN = "origin";
    public static final String COL_JOURNEY_DESTINATION = "destination";
    public static final String COL_JOURNEY_START_TIME = "start_time";
    public static final String COL_JOURNEY_END_TIME = "end_time";
    public static final String COL_JOURNEY_DURATION = "duration_minutes";
    public static final String COL_JOURNEY_ETA = "eta";
    public static final String COL_JOURNEY_STATUS = "status";
    public static final String COL_JOURNEY_CHECKINS_COMPLETED = "checkins_completed";
    public static final String COL_JOURNEY_MISSED_CHECKINS = "missed_checkins";
    public static final String COL_JOURNEY_CONTACTS_SHARED = "contacts_shared";
    public static final String COL_JOURNEY_ROUTE_SUMMARY = "route_summary";

    // Table 3: Community Reports
    public static final String TABLE_REPORTS = "community_reports";
    public static final String COL_REPORT_ID = "id";
    public static final String COL_REPORT_TITLE = "title";
    public static final String COL_REPORT_CATEGORY = "category";
    public static final String COL_REPORT_DESCRIPTION = "description";
    public static final String COL_REPORT_LAT = "latitude";
    public static final String COL_REPORT_LNG = "longitude";
    public static final String COL_REPORT_ADDRESS = "address";
    public static final String COL_REPORT_TIMESTAMP = "timestamp";
    public static final String COL_REPORT_STATUS = "status";
    public static final String COL_REPORT_UPVOTES = "upvotes";
    public static final String COL_REPORT_FLAGS = "flags";

    // Table 4: Evidence Recordings
    public static final String TABLE_RECORDINGS = "evidence_recordings";
    public static final String COL_REC_ID = "id";
    public static final String COL_REC_FILENAME = "filename";
    public static final String COL_REC_PATH = "filepath";
    public static final String COL_REC_DURATION = "duration_seconds";
    public static final String COL_REC_SIZE = "file_size_bytes";
    public static final String COL_REC_TIMESTAMP = "timestamp";
    public static final String COL_REC_NOTES = "notes";

    public SafetyHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Safety Events table
        String createEventsTable = "CREATE TABLE " + TABLE_EVENTS + " (" +
                COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EVENT_TYPE + " TEXT, " +
                COL_EVENT_TITLE + " TEXT, " +
                COL_EVENT_DETAILS + " TEXT, " +
                COL_EVENT_TIMESTAMP + " TEXT, " +
                COL_EVENT_STATUS + " TEXT" +
                ")";
        db.execSQL(createEventsTable);

        // Create Journeys table
        String createJourneysTable = "CREATE TABLE " + TABLE_JOURNEYS + " (" +
                COL_JOURNEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_JOURNEY_ORIGIN + " TEXT, " +
                COL_JOURNEY_DESTINATION + " TEXT, " +
                COL_JOURNEY_START_TIME + " TEXT, " +
                COL_JOURNEY_END_TIME + " TEXT, " +
                COL_JOURNEY_DURATION + " INTEGER, " +
                COL_JOURNEY_ETA + " TEXT, " +
                COL_JOURNEY_STATUS + " TEXT, " +
                COL_JOURNEY_CHECKINS_COMPLETED + " INTEGER, " +
                COL_JOURNEY_MISSED_CHECKINS + " INTEGER, " +
                COL_JOURNEY_CONTACTS_SHARED + " TEXT, " +
                COL_JOURNEY_ROUTE_SUMMARY + " TEXT" +
                ")";
        db.execSQL(createJourneysTable);

        // Create Community Reports table
        String createReportsTable = "CREATE TABLE " + TABLE_REPORTS + " (" +
                COL_REPORT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REPORT_TITLE + " TEXT, " +
                COL_REPORT_CATEGORY + " TEXT, " +
                COL_REPORT_DESCRIPTION + " TEXT, " +
                COL_REPORT_LAT + " REAL, " +
                COL_REPORT_LNG + " REAL, " +
                COL_REPORT_ADDRESS + " TEXT, " +
                COL_REPORT_TIMESTAMP + " TEXT, " +
                COL_REPORT_STATUS + " TEXT, " +
                COL_REPORT_UPVOTES + " INTEGER, " +
                COL_REPORT_FLAGS + " INTEGER" +
                ")";
        db.execSQL(createReportsTable);

        // Create Evidence Recordings table
        String createRecordingsTable = "CREATE TABLE " + TABLE_RECORDINGS + " (" +
                COL_REC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REC_FILENAME + " TEXT, " +
                COL_REC_PATH + " TEXT, " +
                COL_REC_DURATION + " INTEGER, " +
                COL_REC_SIZE + " INTEGER, " +
                COL_REC_TIMESTAMP + " TEXT, " +
                COL_REC_NOTES + " TEXT" +
                ")";
        db.execSQL(createRecordingsTable);

        // Prepopulate with an initial safety shield event
        insertInitialEvent(db, "System Ready", "AtmaRaksha Protection Activated",
                "Emergency shield, journey monitor, and safety services initialized.", "Active");

        // Prepopulate initial community reports with clear unverified / verified labels
        prepopulateSampleReports(db);
    }

    private void insertInitialEvent(SQLiteDatabase db, String type, String title, String details, String status) {
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_TYPE, type);
        values.put(COL_EVENT_TITLE, title);
        values.put(COL_EVENT_DETAILS, details);
        String currentTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        values.put(COL_EVENT_TIMESTAMP, currentTime);
        values.put(COL_EVENT_STATUS, status);
        db.insert(TABLE_EVENTS, null, values);
    }

    private void prepopulateSampleReports(SQLiteDatabase db) {
        String currentTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());

        ContentValues r1 = new ContentValues();
        r1.put(COL_REPORT_TITLE, "Dim Lighting Near Metro Gate 2");
        r1.put(COL_REPORT_CATEGORY, "POOR_LIGHTING");
        r1.put(COL_REPORT_DESCRIPTION, "Streetlights flickering for 200m along side alleyway. Caution advised after 8 PM.");
        r1.put(COL_REPORT_LAT, 12.9716);
        r1.put(COL_REPORT_LNG, 77.5946);
        r1.put(COL_REPORT_ADDRESS, "Station Road, Gate 2");
        r1.put(COL_REPORT_TIMESTAMP, currentTime);
        r1.put(COL_REPORT_STATUS, "Community Reported • Unverified");
        r1.put(COL_REPORT_UPVOTES, 14);
        r1.put(COL_REPORT_FLAGS, 0);
        db.insert(TABLE_REPORTS, null, r1);

        ContentValues r2 = new ContentValues();
        r2.put(COL_REPORT_TITLE, "24/7 Women Helpdesk & PCR Van");
        r2.put(COL_REPORT_CATEGORY, "SAFE_HAVEN");
        r2.put(COL_REPORT_DESCRIPTION, "Active police patrol van stationed regularly with emergency first-aid kit.");
        r2.put(COL_REPORT_LAT, 12.9750);
        r2.put(COL_REPORT_LNG, 77.5980);
        r2.put(COL_REPORT_ADDRESS, "MG Circle Crossroad");
        r2.put(COL_REPORT_TIMESTAMP, currentTime);
        r2.put(COL_REPORT_STATUS, "Verified Emergency Post");
        r2.put(COL_REPORT_UPVOTES, 32);
        r2.put(COL_REPORT_FLAGS, 0);
        db.insert(TABLE_REPORTS, null, r2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createJourneysTable = "CREATE TABLE IF NOT EXISTS " + TABLE_JOURNEYS + " (" +
                    COL_JOURNEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_JOURNEY_ORIGIN + " TEXT, " +
                    COL_JOURNEY_DESTINATION + " TEXT, " +
                    COL_JOURNEY_START_TIME + " TEXT, " +
                    COL_JOURNEY_END_TIME + " TEXT, " +
                    COL_JOURNEY_DURATION + " INTEGER, " +
                    COL_JOURNEY_ETA + " TEXT, " +
                    COL_JOURNEY_STATUS + " TEXT, " +
                    COL_JOURNEY_CHECKINS_COMPLETED + " INTEGER, " +
                    COL_JOURNEY_MISSED_CHECKINS + " INTEGER, " +
                    COL_JOURNEY_CONTACTS_SHARED + " TEXT, " +
                    COL_JOURNEY_ROUTE_SUMMARY + " TEXT" +
                    ")";
            db.execSQL(createJourneysTable);

            String createReportsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_REPORTS + " (" +
                    COL_REPORT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_REPORT_TITLE + " TEXT, " +
                    COL_REPORT_CATEGORY + " TEXT, " +
                    COL_REPORT_DESCRIPTION + " TEXT, " +
                    COL_REPORT_LAT + " REAL, " +
                    COL_REPORT_LNG + " REAL, " +
                    COL_REPORT_ADDRESS + " TEXT, " +
                    COL_REPORT_TIMESTAMP + " TEXT, " +
                    COL_REPORT_STATUS + " TEXT, " +
                    COL_REPORT_UPVOTES + " INTEGER, " +
                    COL_REPORT_FLAGS + " INTEGER" +
                    ")";
            db.execSQL(createReportsTable);

            String createRecordingsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_RECORDINGS + " (" +
                    COL_REC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_REC_FILENAME + " TEXT, " +
                    COL_REC_PATH + " TEXT, " +
                    COL_REC_DURATION + " INTEGER, " +
                    COL_REC_SIZE + " INTEGER, " +
                    COL_REC_TIMESTAMP + " TEXT, " +
                    COL_REC_NOTES + " TEXT" +
                    ")";
            db.execSQL(createRecordingsTable);
        }
    }

    // --- Safety Events Methods ---
    public void logEvent(String type, String title, String details, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_TYPE, type);
        values.put(COL_EVENT_TITLE, title);
        values.put(COL_EVENT_DETAILS, details);
        String currentTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        values.put(COL_EVENT_TIMESTAMP, currentTime);
        values.put(COL_EVENT_STATUS, status);
        db.insert(TABLE_EVENTS, null, values);
        db.close();
    }

    public void insertSafetyEvent(String type, String title, String details, String status) {
        logEvent(type, title, details, status);
    }

    public static class SafetyEvent {
        public long id;
        public String type;
        public String title;
        public String details;
        public String timestamp;
        public String status;

        public SafetyEvent(long id, String type, String title, String details, String timestamp, String status) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.details = details;
            this.timestamp = timestamp;
            this.status = status;
        }
    }

    public List<SafetyEvent> getAllEvents() {
        List<SafetyEvent> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " ORDER BY " + COL_EVENT_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TYPE));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TITLE));
                String details = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_DETAILS));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TIMESTAMP));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_STATUS));
                list.add(new SafetyEvent(id, type, title, details, timestamp, status));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // --- Journeys Methods ---
    public long insertJourney(JourneyRecord journey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_JOURNEY_ORIGIN, journey.getOrigin());
        cv.put(COL_JOURNEY_DESTINATION, journey.getDestination());
        cv.put(COL_JOURNEY_START_TIME, journey.getStartTime());
        cv.put(COL_JOURNEY_END_TIME, journey.getEndTime());
        cv.put(COL_JOURNEY_DURATION, journey.getDurationMinutes());
        cv.put(COL_JOURNEY_ETA, journey.getEta());
        cv.put(COL_JOURNEY_STATUS, journey.getStatus());
        cv.put(COL_JOURNEY_CHECKINS_COMPLETED, journey.getCheckinsCompleted());
        cv.put(COL_JOURNEY_MISSED_CHECKINS, journey.getMissedCheckins());
        cv.put(COL_JOURNEY_CONTACTS_SHARED, journey.getContactsShared());
        cv.put(COL_JOURNEY_ROUTE_SUMMARY, journey.getRouteSummary());
        long id = db.insert(TABLE_JOURNEYS, null, cv);
        db.close();
        return id;
    }

    public void updateJourneyStatus(long journeyId, String status, String endTime, int checkins, int missed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_JOURNEY_STATUS, status);
        if (endTime != null) {
            cv.put(COL_JOURNEY_END_TIME, endTime);
        }
        cv.put(COL_JOURNEY_CHECKINS_COMPLETED, checkins);
        cv.put(COL_JOURNEY_MISSED_CHECKINS, missed);
        db.update(TABLE_JOURNEYS, cv, COL_JOURNEY_ID + " = ?", new String[]{String.valueOf(journeyId)});
        db.close();
    }

    public List<JourneyRecord> getAllJourneys() {
        List<JourneyRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_JOURNEYS + " ORDER BY " + COL_JOURNEY_ID + " DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_JOURNEY_ID));
                String origin = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_ORIGIN));
                String dest = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_DESTINATION));
                String start = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_START_TIME));
                String end = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_END_TIME));
                int dur = cursor.getInt(cursor.getColumnIndexOrThrow(COL_JOURNEY_DURATION));
                String eta = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_ETA));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_STATUS));
                int checkins = cursor.getInt(cursor.getColumnIndexOrThrow(COL_JOURNEY_CHECKINS_COMPLETED));
                int missed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_JOURNEY_MISSED_CHECKINS));
                String contacts = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_CONTACTS_SHARED));
                String summary = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOURNEY_ROUTE_SUMMARY));
                list.add(new JourneyRecord(id, origin, dest, start, end, dur, eta, status, checkins, missed, contacts, summary));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    // --- Community Reports Methods ---
    public long insertReport(CommunityReport report) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_REPORT_TITLE, report.getTitle());
        cv.put(COL_REPORT_CATEGORY, report.getCategory());
        cv.put(COL_REPORT_DESCRIPTION, report.getDescription());
        cv.put(COL_REPORT_LAT, report.getLatitude());
        cv.put(COL_REPORT_LNG, report.getLongitude());
        cv.put(COL_REPORT_ADDRESS, report.getAddress());
        cv.put(COL_REPORT_TIMESTAMP, report.getTimestamp());
        cv.put(COL_REPORT_STATUS, report.getStatus());
        cv.put(COL_REPORT_UPVOTES, report.getUpvotes());
        cv.put(COL_REPORT_FLAGS, report.getFlags());
        long id = db.insert(TABLE_REPORTS, null, cv);
        db.close();
        return id;
    }

    public List<CommunityReport> getAllReports() {
        List<CommunityReport> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_REPORTS + " ORDER BY " + COL_REPORT_ID + " DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REPORT_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPORT_TITLE));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPORT_CATEGORY));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPORT_DESCRIPTION));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_REPORT_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_REPORT_LNG));
                String addr = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPORT_ADDRESS));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPORT_TIMESTAMP));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPORT_STATUS));
                int up = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REPORT_UPVOTES));
                int flags = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REPORT_FLAGS));
                list.add(new CommunityReport(id, title, cat, desc, lat, lng, addr, time, status, up, flags));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public void flagReport(long reportId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_REPORTS + " SET " + COL_REPORT_FLAGS + " = " + COL_REPORT_FLAGS + " + 1 WHERE " + COL_REPORT_ID + " = " + reportId);
        db.close();
    }

    // --- Evidence Recordings Methods ---
    public long insertRecording(EvidenceRecording rec) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_REC_FILENAME, rec.getFilename());
        cv.put(COL_REC_PATH, rec.getFilePath());
        cv.put(COL_REC_DURATION, rec.getDurationSeconds());
        cv.put(COL_REC_SIZE, rec.getFileSizeBytes());
        cv.put(COL_REC_TIMESTAMP, rec.getTimestamp());
        cv.put(COL_REC_NOTES, rec.getNotes());
        long id = db.insert(TABLE_RECORDINGS, null, cv);
        db.close();
        return id;
    }

    public List<EvidenceRecording> getAllRecordings() {
        List<EvidenceRecording> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECORDINGS + " ORDER BY " + COL_REC_ID + " DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REC_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_REC_FILENAME));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COL_REC_PATH));
                int dur = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REC_DURATION));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REC_SIZE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COL_REC_TIMESTAMP));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_REC_NOTES));
                list.add(new EvidenceRecording(id, name, path, dur, size, time, notes));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public void deleteRecording(long recId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORDINGS, COL_REC_ID + " = ?", new String[]{String.valueOf(recId)});
        db.close();
    }

    // --- Clear & Cleanup Methods ---
    public void clearAllEvents() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, null, null);
        db.close();
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, null, null);
        db.delete(TABLE_JOURNEYS, null, null);
        db.close();
    }

    public int getEventsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EVENTS, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }
}
