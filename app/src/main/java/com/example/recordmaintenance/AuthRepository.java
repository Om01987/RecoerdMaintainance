package com.example.recordmaintenance;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import androidx.annotation.NonNull;

/**
 * Firebase-based Authentication Repository
 * Replaces SQLite-based authentication with Firebase Auth + Realtime Database
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final Context context;

    public AuthRepository(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Sign in user with email and password
     */
    public void signIn(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Get user role from database
                            getUserRole(user.getUid(), new RoleCallback() {
                                @Override
                                public void onRoleRetrieved(String role, String empId) {
                                    callback.onSuccess(role, empId);
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onError("Failed to get user role: " + error);
                                }
                            });
                        } else {
                            callback.onError("Authentication failed");
                        }
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Get user role from database - Public method for LoginActivity
     */
    public void getUserRole(String uid, RoleCallback callback) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String role = dataSnapshot.child("role").getValue(String.class);
                    String empId = dataSnapshot.child("empId").getValue(String.class);
                    if (role != null) {
                        callback.onRoleRetrieved(role, empId);
                    } else {
                        callback.onError("User role not found");
                    }
                } else {
                    callback.onError("User data not found in database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, ResetCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Password reset email sent to " + email);
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to send reset email";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Update user password (requires recent authentication)
     */
    public void updatePassword(String newPassword, UpdateCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess("Password updated successfully");
                        } else {
                            String errorMsg = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to update password";
                            callback.onError(errorMsg);
                        }
                    });
        } else {
            callback.onError("User not authenticated");
        }
    }

    /**
     * Re-authenticate user before password change
     */
    public void reauthenticateUser(String currentPassword, ReauthCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            com.google.firebase.auth.AuthCredential credential =
                    com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            String errorMsg = task.getException() != null ?
                                    task.getException().getMessage() : "Re-authentication failed";
                            callback.onError(errorMsg);
                        }
                    });
        } else {
            callback.onError("User not authenticated");
        }
    }

    /**
     * Sign out current user
     */
    public void signOut() {
        mAuth.signOut();
    }

    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Get current user UID
     */
    public String getCurrentUserUid() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Callback interfaces
    public interface AuthCallback {
        void onSuccess(String role, String empId);
        void onError(String error);
    }

    public interface RoleCallback {
        void onRoleRetrieved(String role, String empId);
        void onError(String error);
    }

    public interface ResetCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface ReauthCallback {
        void onSuccess();
        void onError(String error);
    }

    // Cleanup method (no SQLite to close)
    public void close() {
        // No resources to clean up for Firebase
    }
}