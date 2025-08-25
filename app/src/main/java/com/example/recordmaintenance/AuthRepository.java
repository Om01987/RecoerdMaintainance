package com.example.recordmaintenance;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public AuthRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "Error opening database", e);
        }
    }

    /**
     * Verify admin credentials against TblUserMaster
     */
    public boolean verifyAdmin(String userId, String password) {
        if (database == null) {
            Log.e(TAG, "Database is null");
            return false;
        }

        String hashedPassword = hash(password);
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USER_MASTER +
                " WHERE " + DatabaseHelper.USER_ID + " = ? AND " +
                DatabaseHelper.USER_PASSWORD + " = ?";

        Cursor cursor = null;
        try {
            cursor = database.rawQuery(query, new String[]{userId, hashedPassword});
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Admin verification result: " + count);
                return count > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying admin", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Get admin user details by UserID
     */
    public AdminUser getAdminByUserId(String userId) {
        if (database == null) return null;

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_USER_MASTER +
                " WHERE " + DatabaseHelper.USER_ID + " = ?";

        Cursor cursor = null;
        AdminUser admin = null;

        try {
            cursor = database.rawQuery(query, new String[]{userId});
            if (cursor.moveToFirst()) {
                admin = new AdminUser();
                admin.setMastCode(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.USER_MAST_CODE)));
                admin.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.USER_ID)));
                admin.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.USER_PASSWORD)));
                admin.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.USER_NAME)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting admin by userId", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return admin;
    }

    /**
     * Update admin password
     */
    public boolean updateAdminPassword(String userId, String newPassword) {
        if (database == null) return false;

        String hashedPassword = hash(newPassword);
        String whereClause = DatabaseHelper.USER_ID + " = ?";
        String[] whereArgs = {userId};

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.USER_PASSWORD, hashedPassword);

        try {
            int rowsUpdated = database.update(DatabaseHelper.TABLE_USER_MASTER, values, whereClause, whereArgs);
            return rowsUpdated > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating admin password", e);
            return false;
        }
    }

    /**
     * Check if admin exists by email/userId
     */
    public boolean adminExists(String userId) {
        if (database == null) return false;

        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USER_MASTER +
                " WHERE " + DatabaseHelper.USER_ID + " = ?";

        Cursor cursor = null;
        try {
            cursor = database.rawQuery(query, new String[]{userId});
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking admin existence", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Hash password using SHA-256
     */
    private String hash(String input) {
        if (TextUtils.isEmpty(input)) return "";

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available", e);
            return input; // Fallback to plain text (not recommended for production)
        }
    }

    /**
     * Close database connection
     */
    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * Admin User model class
     */
    public static class AdminUser {
        private int mastCode;
        private String userId;
        private String password;
        private String userName;

        // Constructors
        public AdminUser() {}

        public AdminUser(String userId, String password, String userName) {
            this.userId = userId;
            this.password = password;
            this.userName = userName;
        }

        // Getters and Setters
        public int getMastCode() { return mastCode; }
        public void setMastCode(int mastCode) { this.mastCode = mastCode; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
    }
}