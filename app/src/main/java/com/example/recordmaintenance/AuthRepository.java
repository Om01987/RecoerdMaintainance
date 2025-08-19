package com.example.recordmaintenance;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthRepository {

    private static final String PREFS = "auth_prefs";
    private static final String KEY_ADMIN_EMAIL = "admin_email";
    private static final String KEY_ADMIN_HASH = "admin_hash";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
    private static final String DEFAULT_ADMIN_PASS = "Admin@123";

    private final SharedPreferences prefs;

    public AuthRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureDefaultAdmin();
    }

    private void ensureDefaultAdmin() {
        if (TextUtils.isEmpty(prefs.getString(KEY_ADMIN_EMAIL, ""))) {
            prefs.edit()
                    .putString(KEY_ADMIN_EMAIL, DEFAULT_ADMIN_EMAIL)
                    .putString(KEY_ADMIN_HASH, hash(DEFAULT_ADMIN_PASS))
                    .apply();
        }
    }

    public boolean verifyAdmin(String email, String password) {
        String storedEmail = prefs.getString(KEY_ADMIN_EMAIL, "");
        String storedHash = prefs.getString(KEY_ADMIN_HASH, "");
        return storedEmail.equalsIgnoreCase(email) && storedHash.equals(hash(password));
    }

    public void updateAdminCredentials(String email, String newPassword) {
        prefs.edit()
                .putString(KEY_ADMIN_EMAIL, email)
                .putString(KEY_ADMIN_HASH, hash(newPassword))
                .apply();
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
