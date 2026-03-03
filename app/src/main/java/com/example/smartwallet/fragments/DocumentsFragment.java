package com.example.smartwallet.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.AddDocumentActivity;
import com.example.smartwallet.adapters.DocumentAdapter;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Document;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import androidx.activity.result.ActivityResultLauncher;

public class DocumentsFragment extends Fragment implements DocumentAdapter.OnDocumentClickListener {

    private RecyclerView rvDocuments;
    private DocumentAdapter adapter;
    private AppDatabase db;
    private String userId;
    private List<Document> allDocuments = new ArrayList<>();

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri uriContent = result.getUriContent();
                    if (uriContent != null) {
                        Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
                        intent.putExtra("scanned_file_uri", uriContent.toString());
                        startActivity(intent);
                    }
                } else {
                    Exception error = result.getError();
                    if (error != null) {
                        Toast.makeText(getContext(), "Scan cancelled or failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documents, container, false);

        rvDocuments = view.findViewById(R.id.rv_documents);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_document);
        FloatingActionButton fabScan = view.findViewById(R.id.fab_scan_document);
        EditText etSearch = view.findViewById(R.id.et_search_docs);

        db = AppDatabase.getInstance(getContext());
        userId = FirebaseAuth.getInstance().getUid();

        adapter = new DocumentAdapter(new ArrayList<>(), this);
        rvDocuments.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDocumentActivity.class)));

        fabScan.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            } else {
                startScanning();
            }
        });

        return view;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startScanning();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to scan documents", Toast.LENGTH_LONG).show();
                }
            });

    private void startScanning() {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.imageSourceIncludeGallery = true;
        cropImageOptions.imageSourceIncludeCamera = true;
        cropImageOptions.guidelines = CropImageView.Guidelines.ON;
        cropImage.launch(new CropImageContractOptions(null, cropImageOptions));
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            adapter.updateList(allDocuments);
        } else {
            List<Document> filtered = allDocuments.stream()
                    .filter(d -> d.getDocumentName().toLowerCase().contains(query.toLowerCase()) || 
                                (d.getDocumentNumber() != null && d.getDocumentNumber().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());
            adapter.updateList(filtered);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadDocuments();
        }
    }

    private void loadDocuments() {
        allDocuments = db.documentDao().getAllDocuments(userId);
        adapter.updateList(allDocuments);
    }

    @Override
    public void onDeleteClick(Document document) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete this document?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.documentDao().delete(document);
                    loadDocuments();
                    // Sync with Firebase
                    com.example.smartwallet.firebase.FirebaseSyncManager.getInstance(getContext()).downloadAllData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Document document) {
        Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
        intent.putExtra("document_id", document.getId());
        startActivity(intent);
    }

    @Override
    public void onOpenClick(Document document) {
        if (document.getFilePath() != null && !document.getFilePath().isEmpty()) {
            try {
                File file = new File(document.getFilePath());
                Uri uri;
                if (file.exists()) {
                    uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), 
                        requireContext().getPackageName() + ".provider", file);
                } else {
                    uri = Uri.parse(document.getFilePath());
                }

                String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(document.getFilePath());
                if (extension == null || extension.isEmpty()) {
                    int lastDot = document.getFilePath().lastIndexOf('.');
                    if (lastDot != -1) {
                        extension = document.getFilePath().substring(lastDot + 1);
                    }
                }
                
                String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                if (mimeType == null) {
                    mimeType = "*/*";
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                Intent chooser = Intent.createChooser(intent, "Open with");
                startActivity(chooser);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Cannot open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "No file associated with this document", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadClick(Document document) {
        if (document.getFilePath() != null && !document.getFilePath().isEmpty()) {
            try {
                Uri uri = Uri.parse(document.getFilePath());
                String fileName = document.getDocumentName();
                
                // For simplicity, we'll use a share intent which allows users to save to their device
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Download/Save Document"));
                
            } catch (Exception e) {
                Toast.makeText(getContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "No file to download", Toast.LENGTH_SHORT).show();
        }
    }
}
