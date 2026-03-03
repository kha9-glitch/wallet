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
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UpiPaymentActivity extends AppCompatActivity {

    private EditText etAmount, etUpiId, etNotes;
    private Spinner spinnerCategory;
    private AppDatabase db;
    private String userId;

    private final ActivityResultLauncher<Intent> upiPayLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handling UPI result
                // Note: UPI apps behave differently. Most return data in intent.
                if (result.getResultCode() == RESULT_OK || result.getResultCode() == 11) {
                    if (result.getData() != null) {
                        String response = result.getData().getStringExtra("response");
                        if (response != null && response.toLowerCase().contains("success")) {
                            saveUpiExpense();
                        } else {
                            Toast.makeText(this, "Payment Not Completed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Some apps don't return data but result is OK
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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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
        String amount = etAmount.getText().toString();
        String upiId = etUpiId.getText().toString();
        String notes = etNotes.getText().toString();

        if (amount.isEmpty() || upiId.isEmpty()) {
            Toast.makeText(this, "Amount and UPI ID are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", "Smart Wallet User")
                .appendQueryParameter("tn", notes)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build();

        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");
        if (null != chooser.resolveActivity(getPackageManager())) {
            upiPayLauncher.launch(chooser);
        } else {
            Toast.makeText(this, "No UPI app found, please install one to continue", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUpiExpense() {
        double amount = Double.parseDouble(etAmount.getText().toString());
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
