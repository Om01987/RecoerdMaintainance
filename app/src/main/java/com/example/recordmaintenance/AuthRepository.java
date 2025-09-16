package com.example.recordmaintenance;

import android.content.Context;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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
     * Sign in user with Google ID token
     */
    public void signInWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(cred)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
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
                        callback.onError("Google authentication failed");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Get user role from database - Public method for LoginActivity
     */
    public void getUserRole(String uid, RoleCallback callback) {
        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        if (ds.exists()) {
                            String role = ds.child("role").getValue(String.class);
                            String empId = ds.child("empId").getValue(String.class);
                            if (role != null) callback.onRoleRetrieved(role, empId);
                            else callback.onError("User role not found");
                        } else callback.onError("User data not found in database");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, ResetCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess("Password reset email sent to " + email);
                    else callback.onError(task.getException()!=null?
                            task.getException().getMessage():"Failed to send reset email");
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
                        if (task.isSuccessful()) callback.onSuccess("Password updated successfully");
                        else callback.onError(task.getException()!=null?
                                task.getException().getMessage():"Failed to update password");
                    });
        } else callback.onError("User not authenticated");
    }

    /**
     * Re-authenticate user before password change
     */
    public void reauthenticateUser(String currentPassword, ReauthCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail()!=null) {
            AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(cred)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) callback.onSuccess();
                        else callback.onError(task.getException()!=null?
                                task.getException().getMessage():"Re-authentication failed");
                    });
        } else callback.onError("User not authenticated");
    }

    public void signOut() { mAuth.signOut(); }
    public FirebaseUser getCurrentUser() { return mAuth.getCurrentUser(); }
    public boolean isUserSignedIn() { return mAuth.getCurrentUser()!=null; }
    public String getCurrentUserUid() { return (mAuth.getCurrentUser()!=null)?mAuth.getCurrentUser().getUid():null; }

    public interface AuthCallback { void onSuccess(String role,String empId); void onError(String error);}
    public interface RoleCallback { void onRoleRetrieved(String role,String empId); void onError(String error);}
    public interface ResetCallback{ void onSuccess(String message); void onError(String error);}
    public interface UpdateCallback{ void onSuccess(String message); void onError(String error);}
    public interface ReauthCallback{ void onSuccess(); void onError(String error);}
    public void close(){}
}
