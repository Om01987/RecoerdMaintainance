package com.example.recordmaintenance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployeeProfileActivity extends AppCompatActivity {

    private OpenAction pendingAction;
    private static final int REQUEST_CHANGE_PASSWORD = 200;
    private static final int REQ_PICK_IMAGE = 501;
    private static final int REQ_CAPTURE_IMAGE = 502;
    private static final int REQ_PERMS = 1000;

    // UI Components
    private MaterialToolbar toolbar;
    private TextView tvEmployeeName, tvEmployeeId, tvEmail, tvDesignation,
            tvDepartment, tvSalary, tvJoinedDate, tvPasswordStatus;
    private TextView tvAddressLine1, tvAddressLine2, tvCity, tvState, tvCountry;
    private MaterialButton btnChangePassword;
    private CardView cvPersonalInfo, cvJobInfo, cvAddressInfo, cvSecurityInfo;

    // Profile photo
    private CircleImageView ivProfilePhoto;
    private View ivEditOverlay;
    private Uri pendingCameraUri = null;

    // Data Components
    private EmployeeRepository repository;
    private Employee currentEmployee;
    private String employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_profile);

        employeeId = getIntent().getStringExtra("employeeId");
        if (employeeId == null) {
            Toast.makeText(this, "Employee ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new EmployeeRepository(this);
        repository.open();
        initializeViews();
        setupToolbar();
        loadEmployeeData();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        // Personal Info
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvEmail = findViewById(R.id.tvEmail);
        // Job Info
        tvDesignation = findViewById(R.id.tvDesignation);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSalary = findViewById(R.id.tvSalary);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        // Address Info
        tvAddressLine1 = findViewById(R.id.tvAddressLine1);
        tvAddressLine2 = findViewById(R.id.tvAddressLine2);
        tvCity = findViewById(R.id.tvCity);
        tvState = findViewById(R.id.tvState);
        tvCountry = findViewById(R.id.tvCountry);
        // Security Info
        tvPasswordStatus = findViewById(R.id.tvPasswordStatus);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        // Card Views
        cvPersonalInfo = findViewById(R.id.cvPersonalInfo);
        cvJobInfo = findViewById(R.id.cvJobInfo);
        cvAddressInfo = findViewById(R.id.cvAddressInfo);
        cvSecurityInfo = findViewById(R.id.cvSecurityInfo);

        // Profile photo views
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
        currentEmployee = repository.getEmployeeByEmpId(employeeId);
        if (currentEmployee != null) {
            displayEmployeeData();
        } else {
            Toast.makeText(this, "Employee data not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayEmployeeData() {
        // Personal Info
        tvEmployeeName.setText(currentEmployee.getEmpName());
        tvEmployeeId.setText(currentEmployee.getEmpId());
        tvEmail.setText(currentEmployee.getEmpEmail() != null ? currentEmployee.getEmpEmail() : "Not provided");
        // Job Info
        tvDesignation.setText(currentEmployee.getDesignation() != null ? currentEmployee.getDesignation() : "Not assigned");
        tvDepartment.setText(currentEmployee.getDepartment() != null ? currentEmployee.getDepartment() : "Not assigned");
        tvSalary.setText("₹" + String.valueOf(currentEmployee.getSalary()));
        tvJoinedDate.setText(currentEmployee.getJoinedDate() != null ? currentEmployee.getJoinedDate() : "Not available");
        // Address Info
        tvAddressLine1.setText(currentEmployee.getAddressLine1() != null ? currentEmployee.getAddressLine1() : "Not provided");
        tvAddressLine2.setText(currentEmployee.getAddressLine2() != null ? currentEmployee.getAddressLine2() : "Not provided");
        tvCity.setText(currentEmployee.getCity() != null ? currentEmployee.getCity() : "Not provided");
        tvState.setText(currentEmployee.getState() != null ? currentEmployee.getState() : "Not provided");
        tvCountry.setText(currentEmployee.getCountry() != null ? currentEmployee.getCountry() : "Not provided");
        // Security Info
        tvPasswordStatus.setText(currentEmployee.isPasswordChanged() ?
                "Password has been changed" : "Using initial password");
        // Update toolbar title with employee name
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentEmployee.getEmpName() + "'s Profile");
        }

        // Profile Photo logic
        String photoPath = currentEmployee.getProfilePhotoPath();
        if (photoPath != null && !photoPath.trim().isEmpty() && ImageUtils.isPhotoExists(photoPath)) {
            com.squareup.picasso.Picasso.get()
                    .load(new File(photoPath))
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(ivProfilePhoto);
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            intent.putExtra("employeeId", employeeId);
            startActivityForResult(intent, REQUEST_CHANGE_PASSWORD);
        });

        // Profile photo change logic
        View.OnClickListener openPhotoMenu = v -> {
            boolean hasPhoto = currentEmployee.getProfilePhotoPath() != null
                    && !currentEmployee.getProfilePhotoPath().trim().isEmpty()
                    && ImageUtils.isPhotoExists(currentEmployee.getProfilePhotoPath());

            PhotoSelectionDialogFragment dlg = PhotoSelectionDialogFragment.newInstance(hasPhoto);
            dlg.setPhotoSelectionListener(new PhotoSelectionDialogFragment.PhotoSelectionListener() {
                @Override
                public void onTakePhotoSelected() {
                    ensurePermissionsThen(EmployeeProfileActivity.this::openCamera);
                }

                @Override
                public void onChooseFromGallerySelected() {
                    ensurePermissionsThen(EmployeeProfileActivity.this::openGallery);
                }

                @Override
                public void onViewFullImageSelected() {
                    showFullImage();
                }

                @Override
                public void onRemovePhotoSelected() {
                    removePhoto();
                }
            });
            dlg.show(getSupportFragmentManager(), "PHOTO_MENU");
        };

        ivProfilePhoto.setOnClickListener(openPhotoMenu);
        if (ivEditOverlay != null) ivEditOverlay.setOnClickListener(openPhotoMenu);
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
            ActivityCompat.requestPermissions(this,
                    perms.toArray(new String[0]), REQ_PERMS);
            pendingAction = action;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted && pendingAction != null) {
                pendingAction.run();
            } else {
                Toast.makeText(this, "Permissions required to access camera/gallery", Toast.LENGTH_LONG).show();
            }
        }
    }

    private interface OpenAction {
        void run();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_IMAGE);
    }

    private void openCamera() {
        try {
            File dir = ImageUtils.getProfilePhotosDirectory(this);
            String fileName = ImageUtils.generatePhotoFileName(employeeId);
            File out = new File(dir, fileName);
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", out);
            pendingCameraUri = uri;

            Intent camera = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
            camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(camera, REQ_CAPTURE_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullImage() {
        String path = currentEmployee.getProfilePhotoPath();
        FullImageViewDialogFragment.newInstance(path).show(getSupportFragmentManager(), "FULL_IMAGE");
    }

    private void removePhoto() {
        String path = currentEmployee.getProfilePhotoPath();
        if (path != null) ImageUtils.deleteProfilePhoto(path);
        if (repository.updateEmployeeProfilePhoto(employeeId, null)) {
            currentEmployee.setProfilePhotoPath(null);
            ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedImage(Uri uri) {
        // Save copy to internal storage and persist path
        String savedPath = ImageUtils.saveImageToInternalStorage(this, uri, employeeId);
        if (savedPath != null && repository.updateEmployeeProfilePhoto(employeeId, savedPath)) {
            currentEmployee.setProfilePhotoPath(savedPath);
            com.squareup.picasso.Picasso.get().load(new File(savedPath))
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(ivProfilePhoto);
            Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        } else if (id == R.id.action_refresh) {
            loadEmployeeData();
            Toast.makeText(this, "Profile refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    repository.close();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_lock_power_off)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == REQ_PICK_IMAGE && data != null && data.getData() != null) {
            try {
                // persistable permission for image URI use
                getContentResolver().takePersistableUriPermission(
                        data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignore) {}
            handleSelectedImage(data.getData());
        } else if (requestCode == REQ_CAPTURE_IMAGE && pendingCameraUri != null) {
            handleSelectedImage(pendingCameraUri);
            pendingCameraUri = null;
        } else if (requestCode == REQUEST_CHANGE_PASSWORD && resultCode == RESULT_OK) {
            loadEmployeeData();
            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.close();
        }
        super.onDestroy();
    }
}
