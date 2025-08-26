package com.example.recordmaintenance;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class PhotoSelectionDialogFragment extends DialogFragment {

    private PhotoSelectionListener listener;
    private boolean hasExistingPhoto = false;

    public interface PhotoSelectionListener {
        void onTakePhotoSelected();
        void onChooseFromGallerySelected();
        void onViewFullImageSelected();
        void onRemovePhotoSelected();
    }

    public static PhotoSelectionDialogFragment newInstance(boolean hasExistingPhoto) {
        PhotoSelectionDialogFragment fragment = new PhotoSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("hasExistingPhoto", hasExistingPhoto);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            hasExistingPhoto = getArguments().getBoolean("hasExistingPhoto", false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Profile Photo Options");

        String[] options;
        if (hasExistingPhoto) {
            options = new String[]{
                    "📷 Take Photo",
                    "🖼️ Choose from Gallery",
                    "👁️ View Full Image",
                    "🗑️ Remove Photo"
            };
        } else {
            options = new String[]{
                    "📷 Take Photo",
                    "🖼️ Choose from Gallery"
            };
        }

        builder.setItems(options, (dialog, which) -> {
            if (listener != null) {
                if (hasExistingPhoto) {
                    switch (which) {
                        case 0:
                            listener.onTakePhotoSelected();
                            break;
                        case 1:
                            listener.onChooseFromGallerySelected();
                            break;
                        case 2:
                            listener.onViewFullImageSelected();
                            break;
                        case 3:
                            listener.onRemovePhotoSelected();
                            break;
                    }
                } else {
                    switch (which) {
                        case 0:
                            listener.onTakePhotoSelected();
                            break;
                        case 1:
                            listener.onChooseFromGallerySelected();
                            break;
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", null);

        return builder.create();
    }

    public void setPhotoSelectionListener(PhotoSelectionListener listener) {
        this.listener = listener;
    }
}