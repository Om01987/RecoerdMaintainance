package com.example.recordmaintenance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * PrintDocumentAdapter for exporting employee profile to PDF
 * Creates a clean, professional layout of employee information
 */
public class ProfilePrintAdapter extends PrintDocumentAdapter {
    private static final String TAG = "ProfilePrintAdapter";

    private Context context;
    private Employee employee;
    private String jobTitle;
    private PrintedPdfDocument pdfDocument;

    // Page layout constants
    private static final int PAGE_MARGIN = 60;
    private static final int SECTION_SPACING = 40;
    private static final int LINE_HEIGHT = 20;
    private static final int PHOTO_SIZE = 120;

    public ProfilePrintAdapter(Context context, Employee employee, String jobTitle) {
        this.context = context;
        this.employee = employee;
        this.jobTitle = jobTitle != null ? jobTitle : employee.getEmpName() + "'s Profile";
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback,
                         Bundle extras) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        try {
            pdfDocument = new PrintedPdfDocument(context, newAttributes);

            PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(jobTitle)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1); // Profile fits on one page

            PrintDocumentInfo info = builder.build();
            callback.onLayoutFinished(info, !newAttributes.equals(oldAttributes));

        } catch (Exception e) {
            Log.e(TAG, "Error in onLayout", e);
            callback.onLayoutFailed("Layout failed: " + e.getMessage());
        }
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {

        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            return;
        }

        try {
            drawProfilePage();

            try (FileOutputStream fos = new FileOutputStream(destination.getFileDescriptor())) {
                pdfDocument.writeTo(fos);
                callback.onWriteFinished(new PageRange[]{new PageRange(0, 0)});
            }

        } catch (IOException e) {
            Log.e(TAG, "Error writing profile PDF", e);
            callback.onWriteFailed("Write failed: " + e.getMessage());
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    private void drawProfilePage() {
        PdfDocument.Page page = pdfDocument.startPage(0);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int pageWidth = canvas.getWidth();
        int currentY = PAGE_MARGIN;

        // Draw header with employee name
        currentY = drawProfileHeader(canvas, paint, pageWidth, currentY);

        // Draw profile photo if available
        currentY = drawProfilePhoto(canvas, paint, pageWidth, currentY);

        // Draw personal information section
        currentY = drawPersonalInfo(canvas, paint, pageWidth, currentY);

        // Draw job information section
        currentY = drawJobInfo(canvas, paint, pageWidth, currentY);

        // Draw address information section
        currentY = drawAddressInfo(canvas, paint, pageWidth, currentY);

        // Draw security information section
        currentY = drawSecurityInfo(canvas, paint, pageWidth, currentY);

        // Draw footer
        drawProfileFooter(canvas, paint, pageWidth, canvas.getHeight());

        pdfDocument.finishPage(page);
    }

    private int drawProfileHeader(Canvas canvas, Paint paint, int pageWidth, int startY) {
        // Company header
        paint.setColor(Color.rgb(64, 81, 181));
        paint.setTextSize(24);
        paint.setFakeBoldText(true);

        String companyName = "Employee Management System";
        float companyWidth = paint.measureText(companyName);
        canvas.drawText(companyName, (pageWidth - companyWidth) / 2, startY + 30, paint);

        // Employee name
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);

        String empName = employee.getEmpName() != null ? employee.getEmpName() : "Employee Profile";
        float nameWidth = paint.measureText(empName);
        canvas.drawText(empName, (pageWidth - nameWidth) / 2, startY + 60, paint);

        // Employee ID
        paint.setTextSize(14);
        paint.setColor(Color.GRAY);
        paint.setFakeBoldText(false);

        String empId = "Employee ID: " + (employee.getEmpId() != null ? employee.getEmpId() : "N/A");
        float idWidth = paint.measureText(empId);
        canvas.drawText(empId, (pageWidth - idWidth) / 2, startY + 80, paint);

        // Draw separator line
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(2);
        canvas.drawLine(PAGE_MARGIN, startY + 100, pageWidth - PAGE_MARGIN, startY + 100, paint);

        return startY + 120;
    }

    private int drawProfilePhoto(Canvas canvas, Paint paint, int pageWidth, int startY) {
        String photoPath = employee.getProfilePhotoPath();
        if (photoPath != null && ImageUtils.isPhotoExists(photoPath)) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                if (bitmap != null) {
                    // Scale and center the photo
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, PHOTO_SIZE, PHOTO_SIZE, true);

                    int photoX = (pageWidth - PHOTO_SIZE) / 2;
                    canvas.drawBitmap(scaledBitmap, photoX, startY, paint);

                    // Draw border around photo
                    paint.setColor(Color.LTGRAY);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    canvas.drawRect(photoX, startY, photoX + PHOTO_SIZE, startY + PHOTO_SIZE, paint);
                    paint.setStyle(Paint.Style.FILL);

                    scaledBitmap.recycle();
                    bitmap.recycle();

                    return startY + PHOTO_SIZE + SECTION_SPACING;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile photo", e);
            }
        }

        return startY; // No photo, don't add extra space
    }

    private int drawPersonalInfo(Canvas canvas, Paint paint, int pageWidth, int startY) {
        currentY = drawSectionHeader(canvas, paint, "👤 Personal Information", startY);

        currentY = drawInfoField(canvas, paint, "Full Name:",
                employee.getEmpName() != null ? employee.getEmpName() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "Employee ID:",
                employee.getEmpId() != null ? employee.getEmpId() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "Email Address:",
                employee.getEmpEmail() != null ? employee.getEmpEmail() : "N/A", currentY);

        return currentY + SECTION_SPACING;
    }

    private int drawJobInfo(Canvas canvas, Paint paint, int pageWidth, int startY) {
        currentY = drawSectionHeader(canvas, paint, "💼 Job Information", startY);

        currentY = drawInfoField(canvas, paint, "Designation:",
                employee.getDesignation() != null ? employee.getDesignation() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "Department:",
                employee.getDepartment() != null ? employee.getDepartment() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "Salary:",
                "₹" + String.format("%.0f", employee.getSalary()), currentY);

        currentY = drawInfoField(canvas, paint, "Joined Date:",
                employee.getJoinedDate() != null ? employee.getJoinedDate() : "N/A", currentY);

        return currentY + SECTION_SPACING;
    }

    private int drawAddressInfo(Canvas canvas, Paint paint, int pageWidth, int startY) {
        currentY = drawSectionHeader(canvas, paint, "📍 Address Information", startY);

        currentY = drawInfoField(canvas, paint, "Address Line 1:",
                employee.getAddressLine1() != null ? employee.getAddressLine1() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "Address Line 2:",
                employee.getAddressLine2() != null ? employee.getAddressLine2() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "City:",
                employee.getCity() != null ? employee.getCity() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "State:",
                employee.getState() != null ? employee.getState() : "N/A", currentY);

        currentY = drawInfoField(canvas, paint, "Country:",
                employee.getCountry() != null ? employee.getCountry() : "N/A", currentY);

        return currentY + SECTION_SPACING;
    }

    private int drawSecurityInfo(Canvas canvas, Paint paint, int pageWidth, int startY) {
        currentY = drawSectionHeader(canvas, paint, "🔐 Security Information", startY);

        String passwordStatus = employee.isPasswordChanged() ?
                "Password has been changed by employee" :
                "Using initial system-generated password";

        currentY = drawInfoField(canvas, paint, "Password Status:", passwordStatus, currentY);

        return currentY + SECTION_SPACING;
    }

    private int drawSectionHeader(Canvas canvas, Paint paint, String title, int startY) {
        paint.setColor(Color.rgb(64, 81, 181));
        paint.setTextSize(16);
        paint.setFakeBoldText(true);

        canvas.drawText(title, PAGE_MARGIN, startY + LINE_HEIGHT, paint);

        // Draw underline
        paint.setStrokeWidth(2);
        float titleWidth = paint.measureText(title);
        canvas.drawLine(PAGE_MARGIN, startY + LINE_HEIGHT + 3,
                PAGE_MARGIN + titleWidth, startY + LINE_HEIGHT + 3, paint);

        return startY + LINE_HEIGHT + 15;
    }

    private int drawInfoField(Canvas canvas, Paint paint, String label, String value, int startY) {
        // Draw label
        paint.setColor(Color.GRAY);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);

        canvas.drawText(label, PAGE_MARGIN + 20, startY + LINE_HEIGHT, paint);

        // Draw value
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);

        float labelWidth = paint.measureText(label + "  ");
        canvas.drawText(value, PAGE_MARGIN + 20 + labelWidth, startY + LINE_HEIGHT, paint);

        return startY + LINE_HEIGHT + 5;
    }

    private void drawProfileFooter(Canvas canvas, Paint paint, int pageWidth, int pageHeight) {
        paint.setColor(Color.GRAY);
        paint.setTextSize(10);
        paint.setFakeBoldText(false);

        String footerText = "Generated on " +
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date()) +
                " • Employee Management System";

        float footerWidth = paint.measureText(footerText);
        canvas.drawText(footerText, (pageWidth - footerWidth) / 2, pageHeight - 30, paint);
    }

    @Override
    public void onFinish() {
        super.onFinish();
        if (pdfDocument != null) {
            pdfDocument.close();
            pdfDocument = null;
        }
    }

    // Keep class-level currentY for section drawing
    private int currentY = 0;
}
