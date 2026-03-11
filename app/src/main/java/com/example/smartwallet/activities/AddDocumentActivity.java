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
import android.webkit.MimeTypeMap;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import android.graphics.Color;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

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
        Button btnScan = findViewById(R.id.btn_scan_file);
        Button btnSave = findViewById(R.id.btn_save_doc);
        
        db = AppDatabase.getInstance(this);

        // Populate Category Spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.doc_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        etExpiry.setOnClickListener(v -> showDatePicker());
        btnUpload.setOnClickListener(v -> pickFile());
        btnScan.setOnClickListener(v -> checkCameraPermission());
        
        tvFileStatus.setOnClickListener(v -> {
            if (!filePath.isEmpty()) {
                filePath = "";
                tvFileStatus.setText("No File Selected");
                Toast.makeText(this, "File Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        if (getIntent().hasExtra("scanned_file_path")) {
            String internalPath = getIntent().getStringExtra("scanned_file_path");
            if (internalPath != null) {
                filePath = internalPath;
                java.io.File f = new java.io.File(internalPath);
                tvFileStatus.setText("Scan Attached: " + f.getName());
            }
        } else if (getIntent().hasExtra("scanned_file_uri")) {
            Uri uri = Uri.parse(getIntent().getStringExtra("scanned_file_uri"));
            String fileName = "scan_" + System.currentTimeMillis() + ".jpg";
            String internalPath = com.example.smartwallet.utils.FileUtils.copyUriToInternalStorage(this, uri, fileName);
            if (internalPath != null) {
                filePath = internalPath;
                tvFileStatus.setText("Scan Attached: " + fileName);
            }
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

            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();
            if (doc.getCategory() != null) {
                int position = adapter.getPosition(doc.getCategory());
                if (position >= 0) {
                    spinnerCategory.setSelection(position);
                }
            }
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
                String extension = "";
                String mimeType = this.getContentResolver().getType(uri);
                if (mimeType != null) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                }
                
                String fileName = "doc_" + System.currentTimeMillis() + (extension != null && !extension.isEmpty() ? "." + extension : "");
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

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startScanning();
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startScanning();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan documents", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriFilePath(this, true) != null) {
                    String internalPath = result.getUriFilePath(this, true);
                    if (internalPath != null) {
                        filePath = internalPath;
                        java.io.File f = new java.io.File(internalPath);
                        tvFileStatus.setText("Scan Attached: " + f.getName());
                        Toast.makeText(this, "Document Attached Successfully", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getError() != null) {
                    Toast.makeText(this, "Scan failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    private void startScanning() {
        Toast.makeText(this, "Crop your document and tap 'ATTACH' at the top-right corner", Toast.LENGTH_LONG).show();
        
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.imageSourceIncludeGallery = true;
        cropImageOptions.imageSourceIncludeCamera = true;
        cropImageOptions.guidelines = CropImageView.Guidelines.ON;
        
        // UI Customization for high visibility - fixing "nothing visible" issue
        cropImageOptions.activityTitle = "Step 2: Crop & Attach";
        cropImageOptions.cropMenuCropButtonTitle = "ATTACH";
        cropImageOptions.activityMenuIconColor = Color.WHITE;
        cropImageOptions.activityMenuTextColor = Color.WHITE;
        
        // Use high contrast colors for the cropper screen
        cropImageOptions.toolbarColor = Color.parseColor("#212121");
        cropImageOptions.backgroundColor = Color.BLACK;
        cropImageOptions.guidelinesColor = Color.CYAN;
        cropImageOptions.borderCornerColor = Color.CYAN;
        
        cropImageOptions.allowRotation = true;
        cropImageOptions.allowCounterRotation = true;
        cropImageOptions.allowFlipping = true;
        
        cropImage.launch(new CropImageContractOptions(null, cropImageOptions));
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

        new Thread(() -> {
            if (docId == -1) {
                db.documentDao().insert(document);
                runOnUiThread(() -> Toast.makeText(this, "Document Saved Locally", Toast.LENGTH_SHORT).show());
            } else {
                document.setId(docId);
                db.documentDao().update(document);
                runOnUiThread(() -> Toast.makeText(this, "Document Updated Locally", Toast.LENGTH_SHORT).show());
            }

            // Trigger Sync
            FirebaseSyncManager.getInstance(this).syncDocuments();
            runOnUiThread(this::finish);
        }).start();
    }
}
