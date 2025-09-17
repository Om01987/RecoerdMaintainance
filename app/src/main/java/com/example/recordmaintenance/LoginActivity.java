package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvRoleHeader, tvForgot;
    private MaterialButton btnLogin;
    private SignInButton btnGoogle;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private GoogleSignInClient googleClient;
    private final boolean isAdminMode = BuildConfig.IS_ADMIN;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleGoogleSignIn(result.getData());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvRoleHeader = findViewById(R.id.tvRoleHeader);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvForgot = findViewById(R.id.tvForgot);
        progress = findViewById(R.id.progress);

        authRepository = new AuthRepository(this);
        setupGoogleSignIn();

        if (authRepository.isUserSignedIn()) {
            showLoading(true);
            getCurrentUserRoleAndRedirect();
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogle.setOnClickListener(v -> {
            // Sign out from Google first to force account selection
            googleClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = googleClient.getSignInIntent();
                googleLauncher.launch(signInIntent);
            });
        });
        tvForgot.setOnClickListener(v -> startActivity(
                new Intent(this, ForgotPasswordActivity.class)));

        updateUIForRole();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);
    }

    private void handleGoogleSignIn(@Nullable Intent data) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount acc = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(acc.getIdToken());
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed", e);
            showError("Google sign-in failed");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading(true);
        authRepository.signInWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String role, String empId) {
                showLoading(false);
                redirectBasedOnRole(role, empId);
            }
            @Override
            public void onError(String error) {
                showLoading(false);
                showError("Not a registered email");
            }
        });
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            valid = false;
        }

        if (TextUtils.isEmpty(pass)) {
            tilPassword.setError("Password required");
            valid = false;
        }

        if (!valid) return;

        showLoading(true);
        authRepository.signIn(email, pass, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String role, String empId) {
                showLoading(false);
                redirectBasedOnRole(role, empId);
            }
            @Override
            public void onError(String error) {
                showLoading(false);
                showError("Login failed: " + error);
            }
        });
    }

    private void getCurrentUserRoleAndRedirect() {
        String uid = authRepository.getCurrentUserUid();
        if (uid != null) {
            authRepository.getUserRole(uid, new AuthRepository.RoleCallback() {
                @Override
                public void onRoleRetrieved(String role, String empId) {
                    showLoading(false);
                    redirectBasedOnRole(role, empId);
                }
                @Override
                public void onError(String error) {
                    showLoading(false);
                    authRepository.signOut();
                    showError("Please sign in again");
                }
            });
        } else {
            showLoading(false);
        }
    }

    private void redirectBasedOnRole(String role, String empId) {
        boolean unauthorized = isAdminMode
                ? !"admin".equals(role)
                : !"employee".equals(role);
        if (unauthorized) {
            authRepository.signOut();
            showError("Access denied");
            return;
        }

        Intent i = new Intent(this,
                isAdminMode ? MainActivity.class : EmployeeProfileActivity.class);
        i.putExtra("employeeId", empId);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void updateUIForRole() {
        tvRoleHeader.setText(isAdminMode
                ? "🔐 Admin Login" : "👤 Employee Login");
        tilEmail.setHint(isAdminMode ? "Admin Email" : "Employee Email");
        btnGoogle.setVisibility(isAdminMode ? View.GONE : View.VISIBLE);
        tvForgot.setVisibility(isAdminMode ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!loading);
        }
        if (btnGoogle != null) {
            btnGoogle.setEnabled(!loading);
        }
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Log.e(TAG, msg);
    }
}
