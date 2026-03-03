package com.example.smartwallet.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.example.smartwallet.models.Document;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddDocumentActivity extends AppCompatActivity {

    private EditText etName, etNumber, etExpiry, etNotes;
    private Spinner spinnerCategory;
    private TextView tvFileStatus;
    private AppDatabase db;
    private String filePath = "";
    private int docId = -1;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_doc_name);
        etNumber = findViewById(R.id.et_doc_number);
        etExpiry = findViewById(R.id.et_expiry_date);
        etNotes = findViewById(R.id.et_doc_notes);
        spinnerCategory = findViewById(R.id.spinner_doc_category);
        tvFileStatus = findViewById(R.id.tv_file_status);
        Button btnUpload = findViewById(R.id.btn_upload_file);
        Button btnSave = findViewById(R.id.btn_save_doc);
        Button btnClear = new Button(this); // Just for logic if I don't want to edit layout heavily
        // Better: check if layout has a clear button or just allow clicking upload again
        
        db = AppDatabase.getInstance(this);

        etExpiry.setOnClickListener(v -> showDatePicker());
        btnUpload.setOnClickListener(v -> pickFile());
        
        tvFileStatus.setOnClickListener(v -> {
            if (!filePath.isEmpty()) {
                filePath = "";
                tvFileStatus.setText("No File Selected");
                Toast.makeText(this, "File Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        if (getIntent().hasExtra("scanned_file_uri")) {
            filePath = getIntent().getStringExtra("scanned_file_uri");
            tvFileStatus.setText("Scan Attached: " + filePath);
        }

        if (getIntent().hasExtra("document_id")) {
            docId = getIntent().getIntExtra("document_id", -1);
            loadDocumentData();
            btnSave.setText("Update Document");
        }

        btnSave.setOnClickListener(v -> saveDocument());
    }

    private void loadDocumentData() {
        Document doc = db.documentDao().getDocumentById(userId, docId);
        if (doc != null) {
            etName.setText(doc.getDocumentName());
            etNumber.setText(doc.getDocumentNumber());
            etExpiry.setText(doc.getExpiryDate());
            etNotes.setText(doc.getNotes());
            filePath = doc.getFilePath();
            if (filePath != null && !filePath.isEmpty()) {
                tvFileStatus.setText("File: " + filePath);
            }

            ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
            spinnerCategory.setSelection(adapter.getPosition(doc.getCategory()));
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            etExpiry.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Document"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String fileName = "doc_" + System.currentTimeMillis();
                String internalPath = com.example.smartwallet.utils.FileUtils.copyUriToInternalStorage(this, uri, fileName);
                if (internalPath != null) {
                    filePath = internalPath;
                    tvFileStatus.setText("File Selected: " + fileName);
                } else {
                    Toast.makeText(this, "Failed to copy file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveDocument() {
        String name = etName.getText().toString();
        String number = etNumber.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String expiry = etExpiry.getText().toString();
        String notes = etNotes.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter document name", Toast.LENGTH_SHORT).show();
            return;
        }

        Document document = new Document(userId, name, number, category, expiry, filePath, notes);

        if (docId == -1) {
            db.documentDao().insert(document);
            Toast.makeText(this, "Document Saved Locally", Toast.LENGTH_SHORT).show();
        } else {
            document.setId(docId);
            db.documentDao().update(document);
            Toast.makeText(this, "Document Updated Locally", Toast.LENGTH_SHORT).show();
        }

        // Trigger Sync
        FirebaseSyncManager.getInstance(this).syncDocuments();

        finish();
    }
}
