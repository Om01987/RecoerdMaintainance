package com.example.recordmaintenance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * Enhanced full image viewer with zoomable PhotoView
 * Supports pinch-to-zoom, double-tap zoom, and pan gestures
 */
public class FullImageViewDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE_PATH = "image_path";
    private String imagePath;
    private PhotoView photoView;

    public static FullImageViewDialogFragment newInstance(String imagePath) {
        FullImageViewDialogFragment fragment = new FullImageViewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }

        // Make dialog fullscreen for better image viewing
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Create PhotoView programmatically for maximum compatibility
        photoView = new PhotoView(requireContext());

        // Configure PhotoView settings
        photoView.setScaleType(PhotoView.ScaleType.CENTER_INSIDE);
        photoView.setMaximumScale(5.0f); // Allow 5x zoom
        photoView.setMediumScale(2.5f); // Medium zoom level
        photoView.setMinimumScale(1.0f); // Minimum scale to fit screen

        // Enable zoom controls
        photoView.setZoomable(true);

        // Load image
        loadImageIntoPhotoView();

        // Add click listener to close dialog
        photoView.setOnClickListener(v -> dismiss());

        return photoView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Make dialog fullscreen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        return dialog;
    }

    private void loadImageIntoPhotoView() {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                // Load image with Picasso for reliable loading
                Picasso.get()
                        .load(imageFile)
                        .fit() // Fit to PhotoView bounds
                        .centerInside() // Center and scale to fit
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .into(photoView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                // Image loaded successfully, PhotoView handles zooming
                                photoView.setScale(1.0f, true); // Reset to fit scale
                            }

                            @Override
                            public void onError(Exception e) {
                                // Handle error - PhotoView will show placeholder/error image
                                if (getContext() != null) {
                                    android.widget.Toast.makeText(getContext(),
                                            "Failed to load image",
                                            android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                // File doesn't exist, show placeholder
                photoView.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            // No image path provided, show placeholder
            photoView.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Make dialog take full screen
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }
}
