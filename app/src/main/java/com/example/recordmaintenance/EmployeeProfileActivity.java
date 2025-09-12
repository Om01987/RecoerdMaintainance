package com.example.recordmaintenance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployeeProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CHANGE_PASSWORD = 200;
    private static final int REQ_PICK_IMAGE = 501;
    private static final int REQ_CAPTURE_IMAGE = 502;
    private static final int REQ_PERMS = 1000;

    private MaterialToolbar toolbar;
    private TextView tvEmployeeName, tvEmployeeId, tvEmail,
            tvDesignation, tvDepartment, tvSalary, tvJoinedDate, tvPasswordStatus;
    private TextView tvAddressLine1, tvAddressLine2, tvCity, tvState, tvCountry;
    private MaterialButton btnChangePassword;
    private CircleImageView ivProfilePhoto;
    private View ivEditOverlay;

    private Uri pendingCameraUri;
    private EmployeeRepository repository;
    private AuthRepository authRepository;
    private Employee currentEmployee;
    private String employeeId; // This is the empId (e.g., MAN251001)
    private String currentUserUid; // Firebase UID
    private OpenAction pendingAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_profile);

        // Initialize repositories
        repository = new EmployeeRepository(this);
        authRepository = new AuthRepository(this);
        repository.open();

        // Get current user UID from Firebase Auth
        currentUserUid = authRepository.getCurrentUserUid();
        if (currentUserUid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get employee ID from intent (for backward compatibility)
        employeeId = getIntent().getStringExtra("employeeId");

        initializeViews();
        setupToolbar();
        loadEmployeeData();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvEmail = findViewById(R.id.tvEmail);
        tvDesignation = findViewById(R.id.tvDesignation);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSalary = findViewById(R.id.tvSalary);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvAddressLine1 = findViewById(R.id.tvAddressLine1);
        tvAddressLine2 = findViewById(R.id.tvAddressLine2);
        tvCity = findViewById(R.id.tvCity);
        tvState = findViewById(R.id.tvState);
        tvCountry = findViewById(R.id.tvCountry);
        tvPasswordStatus = findViewById(R.id.tvPasswordStatus);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        ivEditOverlay = findViewById(R.id.ivEditOverlay);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadEmployeeData() {
        // Load employee data using Firebase UID
        repository.getEmployeeByUid(currentUserUid, new EmployeeRepository.EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                runOnUiThread(() -> {
                    currentEmployee = employee;
                    displayEmployeeData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeProfileActivity.this, "Failed to load profile: " + error, Toast.LENGTH_LONG).show();
                    // Try using employeeId as fallback if provided
                    if (employeeId != null) {
                        loadByEmployeeId();
                    } else {
                        finish();
                    }
                });
            }
        });
    }

    private void loadByEmployeeId() {
        repository.getEmployeeByEmpId(employeeId, new EmployeeRepository.EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                runOnUiThread(() -> {
                    currentEmployee = employee;
                    displayEmployeeData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeProfileActivity.this, "Employee data not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayEmployeeData() {
        if (currentEmployee == null) return;

        tvEmployeeName.setText(currentEmployee.getEmpName());
        tvEmployeeId.setText(currentEmployee.getEmpId());
        tvEmail.setText(currentEmployee.getEmpEmail() != null
                ? currentEmployee.getEmpEmail() : "Not provided");
        tvDesignation.setText(currentEmployee.getDesignation() != null
                ? currentEmployee.getDesignation() : "Not assigned");
        tvDepartment.setText(currentEmployee.getDepartment() != null
                ? currentEmployee.getDepartment() : "Not assigned");
        tvSalary.setText("₹" + String.format("%.0f", currentEmployee.getSalary()));
        tvJoinedDate.setText(currentEmployee.getJoinedDate() != null
                ? currentEmployee.getJoinedDate() : "Not available");
        tvAddressLine1.setText(currentEmployee.getAddressLine1() != null
                ? currentEmployee.getAddressLine1() : "Not provided");
        tvAddressLine2.setText(currentEmployee.getAddressLine2() != null
                ? currentEmployee.getAddressLine2() : "Not provided");
        tvCity.setText(currentEmployee.getCity() != null
                ? currentEmployee.getCity() : "Not provided");
        tvState.setText(currentEmployee.getState() != null
                ? currentEmployee.getState() : "Not provided");
        tvCountry.setText(currentEmployee.getCountry() != null
                ? currentEmployee.getCountry() : "Not provided");
        tvPasswordStatus.setText(currentEmployee.isPasswordChanged()
                ? "Password has been changed" : "Using initial password");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentEmployee.getEmpName() + "'s Profile");
        }

        // Load profile photo
        String photoPath = currentEmployee.getProfilePhotoPath();
        if (photoPath != null && ImageUtils.isPhotoExists(photoPath)) {
            Picasso.get()
                    .load(new File(photoPath))
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(ivProfilePhoto);
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> {
            Intent i = new Intent(this, ChangePasswordActivity.class);
            i.putExtra("employeeId", currentEmployee != null ? currentEmployee.getEmpId() : employeeId);
            startActivityForResult(i, REQUEST_CHANGE_PASSWORD);
        });

        View.OnClickListener photoMenu = v -> {
            boolean hasPhoto = currentEmployee != null &&
                    currentEmployee.getProfilePhotoPath() != null &&
                    ImageUtils.isPhotoExists(currentEmployee.getProfilePhotoPath());

            PhotoSelectionDialogFragment dlg = PhotoSelectionDialogFragment.newInstance(hasPhoto);
            dlg.setPhotoSelectionListener(new PhotoSelectionDialogFragment.PhotoSelectionListener() {
                @Override public void onTakePhotoSelected() {
                    ensurePermissionsThen(EmployeeProfileActivity.this::openCamera);
                }
                @Override public void onChooseFromGallerySelected() {
                    ensurePermissionsThen(EmployeeProfileActivity.this::openGallery);
                }
                @Override public void onViewFullImageSelected() {
                    if (currentEmployee != null && currentEmployee.getProfilePhotoPath() != null) {
                        FullImageViewDialogFragment.newInstance(
                                currentEmployee.getProfilePhotoPath()
                        ).show(getSupportFragmentManager(), "FULL_IMAGE");
                    }
                }
                @Override public void onRemovePhotoSelected() {
                    removeProfilePhoto();
                }
            });
            dlg.show(getSupportFragmentManager(), "PHOTO_MENU");
        };

        ivProfilePhoto.setOnClickListener(photoMenu);
        ivEditOverlay.setOnClickListener(photoMenu);
    }

    private void removeProfilePhoto() {
        if (currentEmployee == null) return;

        String path = currentEmployee.getProfilePhotoPath();
        if (path != null) ImageUtils.deleteProfilePhoto(path);

        repository.updateEmployeeProfilePhoto(currentUserUid, null, new EmployeeRepository.UpdateCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    currentEmployee.setProfilePhotoPath(null);
                    ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
                    Toast.makeText(EmployeeProfileActivity.this, "Photo removed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeProfileActivity.this, "Failed to remove photo: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void ensurePermissionsThen(OpenAction action) {
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (perms.isEmpty()) {
            action.run();
        } else {
            ActivityCompat.requestPermissions(
                    this, perms.toArray(new String[0]), REQ_PERMS);
            pendingAction = action;
        }
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] grants) {
        if (rc == REQ_PERMS) {
            boolean ok = true;
            for (int g : grants) if (g != PackageManager.PERMISSION_GRANTED) ok = false;
            if (ok && pendingAction != null) {
                pendingAction.run();
                pendingAction = null;
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(rc, perms, grants);
        }
    }

    private interface OpenAction { void run(); }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_PICK_IMAGE);
    }

    private void openCamera() {
        try {
            File dir = ImageUtils.getProfilePhotosDirectory(this);
            String fn = ImageUtils.generatePhotoFileName(currentEmployee != null ?
                    currentEmployee.getEmpId() : "temp");
            File out = new File(dir, fn);
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", out);
            pendingCameraUri = uri;
            Intent c = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            c.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
            c.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(c, REQ_CAPTURE_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        if (req == REQ_PICK_IMAGE && data != null && data.getData() != null) {
            Uri uri = data.getData();
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            handleImage(uri);
        } else if (req == REQ_CAPTURE_IMAGE && pendingCameraUri != null) {
            handleImage(pendingCameraUri);
            pendingCameraUri = null;
        } else if (req == REQUEST_CHANGE_PASSWORD) {
            // Reload data to update password status
            loadEmployeeData();
            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleImage(Uri uri) {
        if (currentEmployee == null) return;

        String saved = ImageUtils.saveImageToInternalStorage(
                this, uri, currentEmployee.getEmpId());
        if (saved != null) {
            repository.updateEmployeeProfilePhoto(currentUserUid, saved, new EmployeeRepository.UpdateCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        currentEmployee.setProfilePhotoPath(saved);
                        Picasso.get()
                                .load(new File(saved))
                                .placeholder(R.drawable.ic_person_placeholder)
                                .into(ivProfilePhoto);
                        Toast.makeText(EmployeeProfileActivity.this, "Photo updated", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(EmployeeProfileActivity.this, "Failed to update photo: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        int id = it.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Logout", (d, w) -> {
                        authRepository.signOut();
                        repository.close();
                        Intent i = new Intent(this, LoginActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        } else if (id == R.id.action_refresh) {
            loadEmployeeData();
            Toast.makeText(this, "Profile refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export_profile_pdf) {
            exportProfileAsPdf();
            return true;
        }
        return super.onOptionsItemSelected(it);
    }

    private void exportProfileAsPdf() {
        if (currentEmployee == null) {
            Toast.makeText(this, "Employee data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            String job = currentEmployee.getEmpName() + " Profile - " +
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(new java.util.Date());

            ProfilePrintAdapter pa = new ProfilePrintAdapter(this, currentEmployee, job);
            PrintAttributes attrs = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("pdf", "PDF", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            PrintJob pj = pm.print(job, pa, attrs);
            if (pj != null) {
                Toast.makeText(this, "Select 'Save as PDF' in print dialog", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Print job failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "PDF export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (repository != null) repository.close();
        if (authRepository != null) authRepository.close();
        super.onDestroy();
    }
}