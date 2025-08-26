package com.example.recordmaintenance;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.squareup.picasso.Picasso;
import java.io.File;

public class FullImageViewDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE_PATH = "image_path";
    private String imagePath;

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
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Create ImageView for full image
        ImageView imageView = new ImageView(getActivity());
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setAdjustViewBounds(true);

        // Load image using Picasso
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Picasso.get()
                        .load(imageFile)
                        .fit()
                        .centerInside()
                        .into(imageView);
            } else {
                // Show placeholder if file doesn't exist
                imageView.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_person_placeholder);
        }

        builder.setView(imageView);
        builder.setTitle("Profile Photo");
        builder.setPositiveButton("Close", (dialog, which) -> dismiss());

        return builder.create();
    }
}