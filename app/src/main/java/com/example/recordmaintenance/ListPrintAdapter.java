package com.example.recordmaintenance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * PrintDocumentAdapter for exporting employee list to PDF
 * Handles pagination and proper formatting for professional output
 */
public class ListPrintAdapter extends PrintDocumentAdapter {
    private static final String TAG = "ListPrintAdapter";

    private Context context;
    private List<Employee> employees;
    private String jobTitle;
    private PrintedPdfDocument pdfDocument;
    private int totalPages = 1;

    // Page layout constants (in points, 72 points = 1 inch)
    private static final int PAGE_MARGIN = 50;
    private static final int HEADER_HEIGHT = 80;
    private static final int ROW_HEIGHT = 25;
    private static final int ROWS_PER_PAGE = 25; // Adjust based on page size

    public ListPrintAdapter(Context context, List<Employee> employees, String jobTitle) {
        this.context = context;
        this.employees = employees;
        this.jobTitle = jobTitle;
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
            // Create PDF document with the print attributes
            pdfDocument = new PrintedPdfDocument(context, newAttributes);

            // Calculate total pages needed
            totalPages = (int) Math.ceil((double) employees.size() / ROWS_PER_PAGE);
            if (totalPages == 0) totalPages = 1; // At least one page for headers

            // Create document info
            PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(jobTitle)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalPages);

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
            // Draw all pages
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    return;
                }

                drawPage(pageIndex);
            }

            // Write PDF to file
            try (FileOutputStream fos = new FileOutputStream(destination.getFileDescriptor())) {
                pdfDocument.writeTo(fos);
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }

        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF", e);
            callback.onWriteFailed("Write failed: " + e.getMessage());
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    private void drawPage(int pageIndex) {
        PdfDocument.Page page = pdfDocument.startPage(pageIndex);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int pageWidth = canvas.getWidth();
        int pageHeight = canvas.getHeight();
        int currentY = PAGE_MARGIN;

        // Draw header
        currentY = drawHeader(canvas, paint, pageWidth, currentY, pageIndex);

        // Draw table header
        currentY = drawTableHeader(canvas, paint, pageWidth, currentY);

        // Draw employee data for this page
        int startIndex = pageIndex * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, employees.size());

        for (int i = startIndex; i < endIndex; i++) {
            Employee emp = employees.get(i);
            currentY = drawEmployeeRow(canvas, paint, pageWidth, currentY, emp, i % 2 == 0);
        }

        // Draw footer
        drawFooter(canvas, paint, pageWidth, pageHeight, pageIndex + 1, totalPages);

        pdfDocument.finishPage(page);
    }

    private int drawHeader(Canvas canvas, Paint paint, int pageWidth, int startY, int pageIndex) {
        // Title
        paint.setTextSize(20);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);

        String title = jobTitle != null ? jobTitle : "Employee Report";
        float titleWidth = paint.measureText(title);
        canvas.drawText(title, (pageWidth - titleWidth) / 2, startY + 20, paint);

        // Subtitle with date and page
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.GRAY);

        String subtitle = "Generated on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",
                java.util.Locale.getDefault()).format(new java.util.Date());
        if (totalPages > 1) {
            subtitle += " • Page " + (pageIndex + 1) + " of " + totalPages;
        }

        float subtitleWidth = paint.measureText(subtitle);
        canvas.drawText(subtitle, (pageWidth - subtitleWidth) / 2, startY + 40, paint);

        return startY + HEADER_HEIGHT;
    }

    private int drawTableHeader(Canvas canvas, Paint paint, int pageWidth, int startY) {
        paint.setColor(Color.rgb(64, 81, 181)); // Material primary color
        paint.setStyle(Paint.Style.FILL);

        // Draw header background
        canvas.drawRect(PAGE_MARGIN, startY, pageWidth - PAGE_MARGIN, startY + ROW_HEIGHT, paint);

        // Draw header text
        paint.setColor(Color.WHITE);
        paint.setTextSize(11);
        paint.setFakeBoldText(true);

        int colWidth = (pageWidth - 2 * PAGE_MARGIN) / 6; // 6 columns
        int currentX = PAGE_MARGIN + 5;
        int textY = startY + ROW_HEIGHT - 8;

        canvas.drawText("Name", currentX, textY, paint);
        currentX += colWidth;
        canvas.drawText("ID", currentX, textY, paint);
        currentX += colWidth;
        canvas.drawText("Department", currentX, textY, paint);
        currentX += colWidth;
        canvas.drawText("Designation", currentX, textY, paint);
        currentX += colWidth;
        canvas.drawText("Salary", currentX, textY, paint);
        currentX += colWidth;
        canvas.drawText("Joined", currentX, textY, paint);

        paint.setStyle(Paint.Style.STROKE);
        return startY + ROW_HEIGHT;
    }

    private int drawEmployeeRow(Canvas canvas, Paint paint, int pageWidth, int startY,
                                Employee emp, boolean isEvenRow) {

        // Alternate row background
        if (isEvenRow) {
            paint.setColor(Color.rgb(248, 249, 250));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(PAGE_MARGIN, startY, pageWidth - PAGE_MARGIN, startY + ROW_HEIGHT, paint);
        }

        // Draw text
        paint.setColor(Color.BLACK);
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        paint.setStyle(Paint.Style.FILL);

        int colWidth = (pageWidth - 2 * PAGE_MARGIN) / 6;
        int currentX = PAGE_MARGIN + 5;
        int textY = startY + ROW_HEIGHT - 8;

        // Helper method to truncate text if too long
        String name = truncateText(paint, emp.getEmpName() != null ? emp.getEmpName() : "N/A", colWidth - 10);
        canvas.drawText(name, currentX, textY, paint);

        currentX += colWidth;
        String id = truncateText(paint, emp.getEmpId() != null ? emp.getEmpId() : "N/A", colWidth - 10);
        canvas.drawText(id, currentX, textY, paint);

        currentX += colWidth;
        String dept = truncateText(paint, emp.getDepartment() != null ? emp.getDepartment() : "N/A", colWidth - 10);
        canvas.drawText(dept, currentX, textY, paint);

        currentX += colWidth;
        String desig = truncateText(paint, emp.getDesignation() != null ? emp.getDesignation() : "N/A", colWidth - 10);
        canvas.drawText(desig, currentX, textY, paint);

        currentX += colWidth;
        String salary = "₹" + String.format("%.0f", emp.getSalary());
        canvas.drawText(salary, currentX, textY, paint);

        currentX += colWidth;
        String joined = truncateText(paint, emp.getJoinedDate() != null ? emp.getJoinedDate() : "N/A", colWidth - 10);
        canvas.drawText(joined, currentX, textY, paint);

        // Draw row border
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRect(PAGE_MARGIN, startY, pageWidth - PAGE_MARGIN, startY + ROW_HEIGHT, paint);

        return startY + ROW_HEIGHT;
    }

    private void drawFooter(Canvas canvas, Paint paint, int pageWidth, int pageHeight,
                            int currentPage, int totalPages) {
        paint.setColor(Color.GRAY);
        paint.setTextSize(9);
        paint.setFakeBoldText(false);

        String footerText = "Employee Management System • " + employees.size() + " records";
        if (totalPages > 1) {
            footerText += " • Page " + currentPage + " of " + totalPages;
        }

        float footerWidth = paint.measureText(footerText);
        canvas.drawText(footerText, (pageWidth - footerWidth) / 2, pageHeight - 20, paint);
    }

    private String truncateText(Paint paint, String text, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);

        int end = text.length();
        while (end > 0 && paint.measureText(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }

        return text.substring(0, end) + ellipsis;
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
