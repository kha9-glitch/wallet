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

public class DocumentsFragment extends Fragment implements DocumentAdapter.OnDocumentClickListener {

    private RecyclerView rvDocuments;
    private DocumentAdapter adapter;
    private AppDatabase db;
    private String userId;
    private List<Document> allDocuments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documents, container, false);

        rvDocuments = view.findViewById(R.id.rv_documents);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_document);
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

        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDocumentActivity.class)));

        return view;
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
        db.documentDao().delete(document);
        loadDocuments();
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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(document.getFilePath()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Cannot open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "No file associated with this document", Toast.LENGTH_SHORT).show();
        }
    }
}
