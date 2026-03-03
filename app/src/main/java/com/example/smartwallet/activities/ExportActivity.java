package com.example.smartwallet.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ExportActivity extends AppCompatActivity {

    private AppDatabase db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        db = AppDatabase.getInstance(this);
        userId = FirebaseAuth.getInstance().getUid();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_export_csv).setOnClickListener(v -> exportToCSV());
        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportToPDF());
    }

    private void exportToCSV() {
        if (userId == null) return;
        
        String filename = "expenses_" + System.currentTimeMillis() + ".csv";
        File file = new File(getExternalFilesDir(null), filename);
        
        new Thread(() -> {
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                List<Expense> expenses = db.expenseDao().getAllExpenses(userId);
                writer.writeNext(new String[]{"Amount", "Category", "Payment Mode", "Date", "Notes"});
                for (Expense e : expenses) {
                    writer.writeNext(new String[]{
                            String.valueOf(e.getAmount()),
                            e.getCategory(),
                            e.getPaymentMode(),
                            e.getDate(),
                            e.getNotes() != null ? e.getNotes() : ""
                    });
                }
                runOnUiThread(() -> shareFile(file, "text/csv"));
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void exportToPDF() {
        if (userId == null) return;

        String filename = "expenses_" + System.currentTimeMillis() + ".pdf";
        File file = new File(getExternalFilesDir(null), filename);

        new Thread(() -> {
            try {
                com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
                document.add(new Paragraph("Smart Wallet Expense Report", titleFont));
                document.add(new Paragraph("Generated on: " + new Date().toString()));
                document.add(new Paragraph(" "));

                PdfPTable table = new PdfPTable(5);
                table.addCell("Amount");
                table.addCell("Category");
                table.addCell("Payment Mode");
                table.addCell("Date");
                table.addCell("Notes");

                List<Expense> expenses = db.expenseDao().getAllExpenses(userId);
                for (Expense e : expenses) {
                    table.addCell(String.valueOf(e.getAmount()));
                    table.addCell(e.getCategory());
                    table.addCell(e.getPaymentMode());
                    table.addCell(e.getDate());
                    table.addCell(e.getNotes() != null ? e.getNotes() : "");
                }
                document.add(table);
                document.close();
                runOnUiThread(() -> shareFile(file, "application/pdf"));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void shareFile(File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Exported File"));
    }
}
