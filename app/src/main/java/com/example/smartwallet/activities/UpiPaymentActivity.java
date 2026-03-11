package com.example.smartwallet.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.example.smartwallet.models.Expense;
import com.example.smartwallet.utils.CurrencyUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UpiPaymentActivity extends AppCompatActivity {

    private EditText etAmount, etUpiId, etNotes;
    private Spinner spinnerCategory;
    private AppDatabase db;
    private String userId;
    private String scannedMc, scannedTr, scannedTid, scannedPn;

    private final ActivityResultLauncher<Intent> upiPayLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK || result.getResultCode() == 11) {
                    if (result.getData() != null) {
                        String response = result.getData().getStringExtra("response");
                        if (response != null && response.toLowerCase().contains("success")) {
                            saveUpiExpense();
                        } else {
                            Toast.makeText(this, "Payment Not Completed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        saveUpiExpense();
                    }
                } else {
                    Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upi_payment);

        db = AppDatabase.getInstance(this);
        userId = FirebaseAuth.getInstance().getUid();

        etAmount = findViewById(R.id.et_amount);
        etUpiId = findViewById(R.id.et_upi_id);
        etNotes = findViewById(R.id.et_notes);
        spinnerCategory = findViewById(R.id.spinner_category);
        Button btnPay = findViewById(R.id.btn_pay);
        Button btnScan = findViewById(R.id.btn_scan_qr);

        String symbol = CurrencyUtils.getCurrencySymbol(this);
        ((TextInputLayout)etAmount.getParent().getParent()).setHint("Amount (" + symbol + ")");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        btnPay.setOnClickListener(v -> initiatePayment());
        btnScan.setOnClickListener(v -> scanQrCode());
    }

    private void scanQrCode() {
        com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions options = new com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build();

        com.google.mlkit.vision.codescanner.GmsBarcodeScanner scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && rawValue.startsWith("upi://pay")) {
                        handleUpiQr(rawValue);
                    } else {
                        Toast.makeText(this, "Invalid UPI QR Code", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Scanning failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleUpiQr(String qrData) {
        Uri uri = Uri.parse(qrData);
        String upiId = uri.getQueryParameter("pa");
        scannedPn = uri.getQueryParameter("pn");
        scannedMc = uri.getQueryParameter("mc");
        scannedTr = uri.getQueryParameter("tr");
        scannedTid = uri.getQueryParameter("tid");
        String amount = uri.getQueryParameter("am");
        String note = uri.getQueryParameter("tn");

        if (upiId != null) {
            etUpiId.setText(upiId);
        }
        if (amount != null) {
            etAmount.setText(amount);
        }
        if (note != null) {
            etNotes.setText(note);
        }
        
        Toast.makeText(this, "UPI Details Loaded", Toast.LENGTH_SHORT).show();
    }

    private void initiatePayment() {
        String amountStr = etAmount.getText().toString();
        String upiId = etUpiId.getText().toString();
        String notes = etNotes.getText().toString();

        if (amountStr.isEmpty() || upiId.isEmpty()) {
            Toast.makeText(this, "Amount and UPI ID are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedAmount = amountStr;
        try {
            double amt = Double.parseDouble(amountStr);
            formattedAmount = String.format(Locale.getDefault(), "%.2f", amt);
        } catch (Exception ignored) {}

        String tr = (scannedTr != null) ? scannedTr : "TR" + System.currentTimeMillis();
        String tid = (scannedTid != null) ? scannedTid : "TID" + System.currentTimeMillis();
        
        String currentUserName = "Smart Wallet User";
        if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null) {
            currentUserName = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0];
        }
        
        String pn = (scannedPn != null) ? scannedPn : currentUserName;
        String mc = (scannedMc != null) ? scannedMc : "";

        Uri.Builder builder = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", pn)
                .appendQueryParameter("tn", notes.isEmpty() ? "Payment" : notes)
                .appendQueryParameter("am", formattedAmount)
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tr", tr);

        if (!mc.isEmpty()) builder.appendQueryParameter("mc", mc);
        if (!tid.isEmpty()) builder.appendQueryParameter("tid", tid);

        Uri uri = builder.build();
        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        try {
            Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");
            upiPayLauncher.launch(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUpiExpense() {
        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) return;
        
        double amount = Double.parseDouble(amountStr);
        String category = spinnerCategory.getSelectedItem().toString();
        String notes = etNotes.getText().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        Expense expense = new Expense(userId, amount, category, "UPI", date, notes);
        new Thread(() -> {
            db.expenseDao().insert(expense);
            FirebaseSyncManager.getInstance(this).syncExpenses();
            runOnUiThread(() -> {
                Toast.makeText(this, "Payment Successful & Expense Added", Toast.LENGTH_LONG).show();
                finish();
            });
        }).start();
    }
}
