package com.example.smartwallet.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.app.DownloadManager;
import android.os.Environment;
import android.content.Context;
import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.AddDocumentActivity;
import com.example.smartwallet.adapters.DocumentAdapter;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.databinding.FragmentDocumentsBinding;
import com.example.smartwallet.models.Document;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import androidx.activity.result.ActivityResultLauncher;

public class DocumentsFragment extends Fragment implements DocumentAdapter.OnDocumentClickListener {

    private FragmentDocumentsBinding binding;
    private DocumentAdapter adapter;
    private AppDatabase db;
    private String userId;
    private List<Document> allDocuments = new ArrayList<>();

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    String filePath = result.getUriFilePath(requireContext(), true);
                    if (filePath != null) {
                        Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
                        intent.putExtra("scanned_file_path", filePath);
                        startActivity(intent);
                    } else if (result.getUriContent() != null) {
                        Uri uriContent = result.getUriContent();
                        Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
                        intent.putExtra("scanned_file_uri", uriContent.toString());
                        intent.setData(uriContent);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        binding = FragmentDocumentsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        db = AppDatabase.getInstance(getContext());
        userId = FirebaseAuth.getInstance().getUid();

        adapter = new DocumentAdapter(new ArrayList<>(), this);
        binding.rvDocuments.setAdapter(adapter);

        binding.etSearchDocs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.fabAddDocument.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDocumentActivity.class)));

        binding.fabScanDocument.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            } else {
                startScanning();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        Toast.makeText(getContext(), "Crop your document and tap 'ATTACH' at the top-right corner", Toast.LENGTH_LONG).show();
        
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.imageSourceIncludeGallery = true;
        cropImageOptions.imageSourceIncludeCamera = true;
        cropImageOptions.guidelines = CropImageView.Guidelines.ON;
        
        // UI Customization for high visibility
        cropImageOptions.activityTitle = "Step 2: Crop & Attach";
        cropImageOptions.cropMenuCropButtonTitle = "ATTACH";
        cropImageOptions.activityMenuIconColor = android.graphics.Color.WHITE;
        cropImageOptions.activityMenuTextColor = android.graphics.Color.WHITE;
        
        // High contrast colors
        cropImageOptions.toolbarColor = android.graphics.Color.parseColor("#212121");
        cropImageOptions.backgroundColor = android.graphics.Color.BLACK;
        cropImageOptions.guidelinesColor = android.graphics.Color.CYAN;
        cropImageOptions.borderCornerColor = android.graphics.Color.CYAN;
        
        cropImageOptions.allowRotation = true;
        cropImageOptions.allowCounterRotation = true;
        cropImageOptions.allowFlipping = true;
        cropImageOptions.showProgressBar = true;
        
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
                String path = document.getFilePath();
                Uri uri;
                String mimeType = null;

                // Improved Extension Extraction (handles query parameters in Firebase URLs)
                String cleanPath = path;
                if (path.contains("?")) {
                    cleanPath = path.substring(0, path.indexOf("?"));
                }
                String extension = "";
                int dotIndex = cleanPath.lastIndexOf('.');
                if (dotIndex != -1) {
                    extension = cleanPath.substring(dotIndex + 1).toLowerCase();
                }

                if (path.startsWith("http")) {
                    // Firebase URL
                    uri = Uri.parse(path);
                    if (!extension.isEmpty()) {
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    }
                } else if (path.startsWith("content://")) {
                    // Direct Content URI (e.g. from recent scans or other apps)
                    uri = Uri.parse(path);
                    mimeType = requireContext().getContentResolver().getType(uri);
                } else {
                    // Local File Path
                    File file = new File(path);
                    if (file.exists()) {
                        uri = FileProvider.getUriForFile(requireContext(),
                                requireContext().getPackageName() + ".fileprovider", file);
                        
                        if (!extension.isEmpty()) {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                        }
                    } else {
                        // Compatibility check: maybe it's a URI stored as a path
                        if (path.contains(":/")) {
                            uri = Uri.parse(path);
                            mimeType = requireContext().getContentResolver().getType(uri);
                        } else {
                            Toast.makeText(getContext(), "File not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                // Fallback for common types if MimeTypeMap fails
                if (mimeType == null || mimeType.isEmpty()) {
                    switch (extension) {
                        case "pdf":
                            mimeType = "application/pdf";
                            break;
                        case "jpg":
                        case "jpeg":
                        case "png":
                        case "webp":
                        case "gif":
                        case "bmp":
                            mimeType = "image/*";
                            break;
                        case "doc":
                        case "docx":
                            mimeType = "application/msword";
                            break;
                        case "xls":
                        case "xlsx":
                            mimeType = "application/vnd.ms-excel";
                            break;
                        case "ppt":
                        case "pptx":
                            mimeType = "application/vnd.ms-powerpoint";
                            break;
                        case "txt":
                            mimeType = "text/plain";
                            break;
                        case "zip":
                        case "rar":
                            mimeType = "application/zip";
                            break;
                        default:
                            mimeType = "*/*";
                            break;
                    }
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Use Intent.createChooser for better experience and compatibility
                startActivity(Intent.createChooser(intent, "Open with"));

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error opening document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getContext(), "No file associated with this document", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadClick(Document document) {
        String path = document.getFilePath();
        if (path != null && !path.isEmpty()) {
            try {
                if (path.startsWith("http")) {
                    // Start system download manager for network files (Firebase URLs)
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(path));
                    request.setTitle(document.getDocumentName());
                    request.setDescription("Downloading " + document.getDocumentName());
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    
                    // Improved filename construction
                    String fileName = document.getDocumentName();
                    String cleanPath = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
                    String extension = MimeTypeMap.getFileExtensionFromUrl(cleanPath);
                    
                    if (extension == null || extension.isEmpty()) {
                        int dotIndex = cleanPath.lastIndexOf('.');
                        if (dotIndex != -1) {
                            extension = cleanPath.substring(dotIndex + 1).toLowerCase();
                        }
                    }
                    
                    if (extension != null && !extension.isEmpty() && !fileName.toLowerCase().endsWith("." + extension)) {
                        fileName += "." + extension;
                    }

                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                    DownloadManager manager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    if (manager != null) {
                        manager.enqueue(request);
                        Toast.makeText(getContext(), "Download started. Check notifications.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // For local files, save directly to Downloads via MediaStore for direct download experience
                    saveLocalFileToDownloads(document);
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getContext(), "No file associated with this document", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLocalFileToDownloads(Document document) {
        String path = document.getFilePath();
        String displayName = document.getDocumentName();
        String mimeType = "application/octet-stream";
        
        // Find extension and mimeType
        String cleanPath = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
        String extension = "";
        int dotIndex = cleanPath.lastIndexOf('.');
        if (dotIndex != -1) {
            extension = cleanPath.substring(dotIndex + 1).toLowerCase();
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        
        if (extension != null && !extension.isEmpty() && !displayName.toLowerCase().endsWith("." + extension)) {
            displayName += "." + extension;
        }

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            }
            
            Uri externalUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            } else {
                // For older devices, we'd use a different approach, but this is fine for most modern devices
                externalUri = MediaStore.Files.getContentUri("external");
            }

            Uri downloadUri = requireContext().getContentResolver().insert(externalUri, values);
            
            if (downloadUri != null) {
                try (InputStream is = getFileStream(path);
                     OutputStream os = requireContext().getContentResolver().openOutputStream(downloadUri)) {
                    
                    if (is == null) {
                        Toast.makeText(getContext(), "File not found locally", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
                Toast.makeText(getContext(), "Document saved to Downloads folder", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to create destination file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Direct save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private InputStream getFileStream(String path) throws Exception {
        if (path.startsWith("content://")) {
            return requireContext().getContentResolver().openInputStream(Uri.parse(path));
        } else {
            File file = new File(path);
            if (file.exists()) {
                return new FileInputStream(file);
            } else {
                return null;
            }
        }
    }
}
