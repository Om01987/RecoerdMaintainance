package com.example.recordmaintenance;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.Pattern;

public class EmployeeCodeGenerator {

    private static final String PREFS_NAME = "employee_code_prefs";
    private static final String KEY_LAST_CODE = "last_code_number";
    private static final String CODE_PREFIX = "MAN25";
    private static final int INITIAL_CODE = 1001;

    // Employee ID pattern validation
    private static final Pattern EMP_ID_PATTERN = Pattern.compile("^MAN25\\d{4}$");

    private final SharedPreferences prefs;

    public EmployeeCodeGenerator(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Generates next unique employee code following MAN25#### pattern
     */
    public String generateEmployeeCode() {
        int lastCode = prefs.getInt(KEY_LAST_CODE, INITIAL_CODE - 1);
        int nextCode = lastCode + 1;

        // Save the new code number
        prefs.edit().putInt(KEY_LAST_CODE, nextCode).apply();

        return String.format("%s%04d", CODE_PREFIX, nextCode);
    }

    /**
     * Validates if employee ID follows the correct pattern
     */
    public static boolean isValidEmployeeId(String empId) {
        if (empId == null || empId.trim().isEmpty()) {
            return false;
        }
        return EMP_ID_PATTERN.matcher(empId.trim()).matches();
    }

    /**
     * Generates initial password based on employee details
     * Pattern: first 2 letters of name + last 4 digits of emp code + 4 random chars
     */
    public static String generateInitialPassword(String empName, String empCode) {
        // Extract first 2 letters of name (lowercase)
        String namePrefix = empName.length() >= 2 ?
                empName.substring(0, 2).toLowerCase() : empName.toLowerCase();

        // Extract last 4 digits from employee code
        String codeDigits = empCode.length() >= 4 ?
                empCode.substring(empCode.length() - 4) : "0000";

        // Generate 4 random alphanumeric characters
        String randomSuffix = generateRandomString(4);

        return namePrefix + codeDigits + randomSuffix;
    }

    /**
     * Generates random alphanumeric string of specified length
     */
    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }

    /**
     * Hashes password using SHA-256
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
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
            return password; // Fallback to plain text (not recommended for production)
        }
    }

    /**
     * Validates password strength (optional)
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasUpper && hasLower && hasDigit;
    }
}
