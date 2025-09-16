package com.example.recordmaintenance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IdCardPrintAdapter extends PrintDocumentAdapter {

    private Context context;
    private Employee employee;
    private PdfDocument pdfDocument;

    public IdCardPrintAdapter(Context context, Employee employee) {
        this.context = context;
        this.employee = employee;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback,
                         Bundle extras) {

        // Create PDF document
        pdfDocument = new PdfDocument();

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("ID_Card_" + employee.getEmpId() + ".pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();

        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {

        // Create a page
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();

        // Draw ID card on the page (centered)
        float cardWidth = 400;
        float cardHeight = 250;
        float startX = (pageInfo.getPageWidth() - cardWidth) / 2;
        float startY = (pageInfo.getPageHeight() - cardHeight) / 2;

        drawIdCard(canvas, startX, startY, cardWidth, cardHeight);

        pdfDocument.finishPage(page);

        try {
            pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    private void drawIdCard(Canvas canvas, float x, float y, float width, float height) {
        // Background gradient effect
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRect(x, y, x + width, y + height, backgroundPaint);

        // Add subtle gradient effect
        Paint gradientPaint = new Paint();
        gradientPaint.setColor(Color.parseColor("#1976D2"));
        canvas.drawRect(x, y + height * 0.6f, x + width, y + height, gradientPaint);

        // Company header
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(24);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
        headerPaint.setAntiAlias(true);
        canvas.drawText("MatraSoftech", x + 20, y + 40, headerPaint);

        // ID Badge
        Paint badgePaint = new Paint();
        badgePaint.setColor(Color.parseColor("#40FFFFFF"));
        canvas.drawRect(x + width - 120, y + 15, x + width - 20, y + 50, badgePaint);

        Paint badgeTextPaint = new Paint();
        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(14);
        badgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        badgeTextPaint.setAntiAlias(true);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ID CARD", x + width - 70, y + 35, badgeTextPaint);

        // Photo placeholder circle
        Paint photoPaint = new Paint();
        photoPaint.setColor(Color.WHITE);
        photoPaint.setAntiAlias(true);
        canvas.drawCircle(x + 80, y + 120, 40, photoPaint);

        // Try to load and draw actual photo
        String photoPath = employee.getProfilePhotoPath();
        if (photoPath != null && ImageUtils.isPhotoExists(photoPath)) {
            try {
                Bitmap photoBitmap = ImageUtils.loadBitmapFromPath(photoPath);
                if (photoBitmap != null) {
                    // Create circular photo
                    Bitmap scaledPhoto = Bitmap.createScaledBitmap(photoBitmap, 80, 80, true);
                    // Draw photo in circle (simplified approach)
                    canvas.save();
                    canvas.clipRect(x + 40, y + 80, x + 120, y + 160);
                    canvas.drawBitmap(scaledPhoto, x + 40, y + 80, null);
                    canvas.restore();
                }
            } catch (Exception e) {
                // Use placeholder if photo loading fails
            }
        }

        // Photo border
        Paint photoBorderPaint = new Paint();
        photoBorderPaint.setColor(Color.WHITE);
        photoBorderPaint.setStyle(Paint.Style.STROKE);
        photoBorderPaint.setStrokeWidth(3);
        photoBorderPaint.setAntiAlias(true);
        canvas.drawCircle(x + 80, y + 120, 40, photoBorderPaint);

        // Employee name
        Paint namePaint = new Paint();
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(20);
        namePaint.setTypeface(Typeface.DEFAULT_BOLD);
        namePaint.setAntiAlias(true);
        canvas.drawText(employee.getEmpName(), x + 140, y + 90, namePaint);

        // Employee ID
        Paint detailsPaint = new Paint();
        detailsPaint.setColor(Color.parseColor("#E0FFFFFF"));
        detailsPaint.setTextSize(16);
        detailsPaint.setAntiAlias(true);
        canvas.drawText("ID: " + employee.getEmpId(), x + 140, y + 115, detailsPaint);

        // Designation
        String designation = employee.getDesignation() != null ? employee.getDesignation() : "Employee";
        canvas.drawText(designation, x + 140, y + 135, detailsPaint);

        // Department
        String department = (employee.getDepartment() != null ? employee.getDepartment() : "General") + " Dept";
        canvas.drawText(department, x + 140, y + 155, detailsPaint);

        // Footer section
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#B0FFFFFF"));
        footerPaint.setTextSize(12);
        footerPaint.setAntiAlias(true);
        canvas.drawText("Valid until further notice", x + 20, y + height - 20, footerPaint);

        // Join year
        String joinYear = "2023";
        if (employee.getJoinedDate() != null && employee.getJoinedDate().length() >= 4) {
            joinYear = employee.getJoinedDate().substring(0, 4);
        }

        Paint joinYearPaint = new Paint();
        joinYearPaint.setColor(Color.parseColor("#B0FFFFFF"));
        joinYearPaint.setTextSize(12);
        joinYearPaint.setAntiAlias(true);
        joinYearPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Since " + joinYear, x + width - 20, y + height - 20, joinYearPaint);

        // Add current date
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        Paint datePaint = new Paint();
        datePaint.setColor(Color.parseColor("#B0FFFFFF"));
        datePaint.setTextSize(10);
        datePaint.setAntiAlias(true);
        datePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Generated: " + currentDate, x + width/2, y + height - 5, datePaint);
    }

    @Override
    public void onFinish() {
        super.onFinish();
        if (pdfDocument != null) {
            pdfDocument.close();
            pdfDocument = null;
        }
    }
}