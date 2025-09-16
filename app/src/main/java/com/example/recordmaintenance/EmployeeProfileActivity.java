package com.example.recordmaintenance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployeeProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CHANGE_PASSWORD = 200;
    private static final int REQ_PICK_IMAGE = 501;
    private static final int REQ_CAPTURE_IMAGE = 502;
    private static final int REQ_PERMS = 1000;
    private static final int REQ_STORAGE_PERMS = 1001;

    private MaterialToolbar toolbar;
    private TextView tvEmployeeName, tvEmployeeId, tvEmail,
            tvDesignation, tvDepartment, tvSalary, tvJoinedDate, tvPasswordStatus;
    private TextView tvAddressLine1, tvAddressLine2, tvCity, tvState, tvCountry;
    private MaterialButton btnChangePassword, btnDownloadIdCard, btnPrintIdCard;
    private CircleImageView ivProfilePhoto, ivIdCardPhoto;
    private TextView tvIdCardName, tvIdCardId, tvIdCardDesignation, tvIdCardDepartment, tvIdCardJoinDate;
    private View ivEditOverlay;
    private CardView cvIdCardPreview;

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

        // ID Card views
        btnDownloadIdCard = findViewById(R.id.btnDownloadIdCard);
        btnPrintIdCard = findViewById(R.id.btnPrintIdCard);
        ivIdCardPhoto = findViewById(R.id.ivIdCardPhoto);
        tvIdCardName = findViewById(R.id.tvIdCardName);
        tvIdCardId = findViewById(R.id.tvIdCardId);
        tvIdCardDesignation = findViewById(R.id.tvIdCardDesignation);
        tvIdCardDepartment = findViewById(R.id.tvIdCardDepartment);
        tvIdCardJoinDate = findViewById(R.id.tvIdCardJoinDate);
        cvIdCardPreview = findViewById(R.id.cvIdCardPreview);
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
                    updateIdCardData();
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
                    updateIdCardData();
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

    private void updateIdCardData() {
        if (currentEmployee == null) return;

        // Update ID card information
        tvIdCardName.setText(currentEmployee.getEmpName());
        tvIdCardId.setText(currentEmployee.getEmpId());
        tvIdCardDesignation.setText(currentEmployee.getDesignation() != null
                ? currentEmployee.getDesignation() : "Employee");
        tvIdCardDepartment.setText((currentEmployee.getDepartment() != null
                ? currentEmployee.getDepartment() : "General") + " Department");

        // Format join date for ID card
        if (currentEmployee.getJoinedDate() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                Date date = inputFormat.parse(currentEmployee.getJoinedDate());
                tvIdCardJoinDate.setText("Since " + outputFormat.format(date));
            } catch (Exception e) {
                tvIdCardJoinDate.setText("Since " + currentEmployee.getJoinedDate().substring(0, 4));
            }
        } else {
            tvIdCardJoinDate.setText("Since 2023");
        }

        // Load profile photo for ID card
        String photoPath = currentEmployee.getProfilePhotoPath();
        if (photoPath != null && ImageUtils.isPhotoExists(photoPath)) {
            Picasso.get()
                    .load(new File(photoPath))
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(ivIdCardPhoto);
        } else {
            ivIdCardPhoto.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> {
            Intent i = new Intent(this, ChangePasswordActivity.class);
            i.putExtra("employeeId", currentEmployee != null ? currentEmployee.getEmpId() : employeeId);
            startActivityForResult(i, REQUEST_CHANGE_PASSWORD);
        });

        // ID Card action buttons
        btnDownloadIdCard.setOnClickListener(v -> downloadIdCard());
        btnPrintIdCard.setOnClickListener(v -> printIdCard());

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

    private void downloadIdCard() {
        if (currentEmployee == null) {
            Toast.makeText(this, "Employee data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check storage permission for Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE_PERMS);
                return;
            }
        }

        generateAndSaveIdCard();
    }

    private void generateAndSaveIdCard() {
        try {
            // Create bitmap from ID card view
            Bitmap idCardBitmap = createIdCardBitmap();

            // Save to Downloads folder
            String fileName = "ID_Card_" + currentEmployee.getEmpId() + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            idCardBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Toast.makeText(this, "ID Card saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

            // Show option to share
            showShareDialog(file);

        } catch (IOException e) {
            Toast.makeText(this, "Failed to save ID card: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createIdCardBitmap() {
        // Create a high-quality bitmap of the ID card
        int width = 800;
        int height = 500;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Create gradient background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#2196F3")); // Material Blue
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Add company header
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(40);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
        headerPaint.setAntiAlias(true);
        canvas.drawText("MatraSoftech", 40, 60, headerPaint);

        Paint idBadgePaint = new Paint();
        idBadgePaint.setColor(Color.parseColor("#40FFFFFF"));
        canvas.drawRect(width - 200, 20, width - 40, 70, idBadgePaint);

        Paint idTextPaint = new Paint();
        idTextPaint.setColor(Color.WHITE);
        idTextPaint.setTextSize(24);
        idTextPaint.setAntiAlias(true);
        canvas.drawText("ID CARD", width - 180, 50, idTextPaint);

        // Add employee photo circle (placeholder)
        Paint photoPaint = new Paint();
        photoPaint.setColor(Color.WHITE);
        photoPaint.setAntiAlias(true);
        canvas.drawCircle(120, 200, 60, photoPaint);

        // Try to load actual photo
        String photoPath = currentEmployee.getProfilePhotoPath();
        if (photoPath != null && ImageUtils.isPhotoExists(photoPath)) {
            try {
                Bitmap photoBitmap = ImageUtils.loadBitmapFromPath(photoPath);
                if (photoBitmap != null) {
                    // Scale and draw the photo
                    Bitmap scaledPhoto = Bitmap.createScaledBitmap(photoBitmap, 120, 120, true);
                    canvas.drawBitmap(scaledPhoto, 60, 140, null);
                }
            } catch (Exception e) {
                // Use placeholder if photo loading fails
            }
        }

        // Add employee details
        Paint namePaint = new Paint();
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(32);
        namePaint.setTypeface(Typeface.DEFAULT_BOLD);
        namePaint.setAntiAlias(true);
        canvas.drawText(currentEmployee.getEmpName(), 220, 160, namePaint);

        Paint detailsPaint = new Paint();
        detailsPaint.setColor(Color.parseColor("#E0FFFFFF"));
        detailsPaint.setTextSize(24);
        detailsPaint.setAntiAlias(true);
        canvas.drawText(currentEmployee.getEmpId(), 220, 190, detailsPaint);
        canvas.drawText(currentEmployee.getDesignation() != null ?
                currentEmployee.getDesignation() : "Employee", 220, 220, detailsPaint);
        canvas.drawText((currentEmployee.getDepartment() != null ?
                currentEmployee.getDepartment() : "General") + " Department", 220, 250, detailsPaint);

        // Add footer
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#B0FFFFFF"));
        footerPaint.setTextSize(18);
        footerPaint.setAntiAlias(true);
        canvas.drawText("Valid until further notice", 40, height - 40, footerPaint);

        String joinYear = "2023";
        if (currentEmployee.getJoinedDate() != null) {
            joinYear = currentEmployee.getJoinedDate().substring(0, 4);
        }
        canvas.drawText("Since " + joinYear, width - 150, height - 40, footerPaint);

        return bitmap;
    }

    private void showShareDialog(File file) {
        new AlertDialog.Builder(this)
                .setTitle("ID Card Saved")
                .setMessage("ID Card has been saved to Downloads. Would you like to share it?")
                .setPositiveButton("Share", (dialog, which) -> shareIdCard(file))
                .setNegativeButton("Close", null)
                .show();
    }

    private void shareIdCard(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Employee ID Card - " + currentEmployee.getEmpName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share ID Card"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share ID card", Toast.LENGTH_SHORT).show();
        }
    }

    private void printIdCard() {
        if (currentEmployee == null) {
            Toast.makeText(this, "Employee data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            String jobName = "ID_Card_" + currentEmployee.getEmpName();

            PrintDocumentAdapter adapter = new IdCardPrintAdapter(this, currentEmployee);
            PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("pdf", "PDF", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            PrintJob printJob = printManager.print(jobName, adapter, attributes);
            if (printJob != null) {
                Toast.makeText(this, "Select 'Save as PDF' or print to printer", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Print job failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Print failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                    ivIdCardPhoto.setImageResource(R.drawable.ic_person_placeholder);
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
        } else if (rc == REQ_STORAGE_PERMS) {
            boolean ok = true;
            for (int g : grants) if (g != PackageManager.PERMISSION_GRANTED) ok = false;
            if (ok) {
                generateAndSaveIdCard();
            } else {
                Toast.makeText(this, "Storage permission required to save ID card", Toast.LENGTH_LONG).show();
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
                        // Also update ID card photo
                        Picasso.get()
                                .load(new File(saved))
                                .placeholder(R.drawable.ic_person_placeholder)
                                .into(ivIdCardPhoto);
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
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new Date());

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