package com.example.recordmaintenance;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static final String PROFILE_PHOTOS_DIR = "profile_photos";

    /**
     * Create directory for storing profile photos
     */
    public static File getProfilePhotosDirectory(Context context) {
        File profileDir = new File(context.getFilesDir(), PROFILE_PHOTOS_DIR);
        if (!profileDir.exists()) {
            profileDir.mkdirs();
        }
        return profileDir;
    }

    /**
     * Generate unique filename for profile photo
     */
    public static String generatePhotoFileName(String empId) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return empId + "_" + timeStamp + ".jpg";
    }

    /**
     * Save image from URI to internal storage
     */
    public static String saveImageToInternalStorage(Context context, Uri imageUri, String empId) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            File profileDir = getProfilePhotosDirectory(context);
            String fileName = generatePhotoFileName(empId);
            File destinationFile = new File(profileDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "Image saved to: " + destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    /**
     * Delete old profile photo
     */
    public static boolean deleteProfilePhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return true;
        }

        try {
            File file = new File(photoPath);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "Old photo deleted: " + deleted);
                return deleted;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting photo", e);
        }
        return false;
    }

    /**
     * Check if photo file exists
     */
    public static boolean isPhotoExists(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return false;
        }
        return new File(photoPath).exists();
    }

    /**
     * Get file size in KB
     */
    public static long getFileSizeKB(String filePath) {
        File file = new File(filePath);
        return file.length() / 1024;
    }

    /**
     * Load bitmap from file path with memory optimization
     */
    public static Bitmap loadBitmapFromPath(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Scale down to reduce memory usage
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap from path: " + path, e);
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory loading bitmap: " + path, e);
            // Try with higher sample size
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeFile(path, options);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to load bitmap even with higher sample size", ex);
                return null;
            }
        }
    }

    /**
     * Clean up old unused photos (optional utility method)
     */
    public static void cleanupUnusedPhotos(Context context) {
        // This method can be called periodically to clean up orphaned photo files
        File profileDir = getProfilePhotosDirectory(context);
        File[] files = profileDir.listFiles();

        if (files != null) {
            // You can implement logic to check which photos are no longer referenced
            // in the database and delete them
            Log.d(TAG, "Found " + files.length + " profile photos");
        }
    }
}