package com.example.smartwallet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.models.Document;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<Document> documentList;
    private OnDocumentClickListener listener;

    public interface OnDocumentClickListener {
        void onDeleteClick(Document document);

        void onEditClick(Document document);

        void onOpenClick(Document document);

        void onDownloadClick(Document document);
    }

    public DocumentAdapter(List<Document> documentList, OnDocumentClickListener listener) {
        this.documentList = documentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documentList.get(position);
        holder.tvName.setText(document.getDocumentName());
        holder.tvCategory.setText(document.getCategory());
        if (document.getExpiryDate() != null && !document.getExpiryDate().isEmpty()) {
            holder.tvExpiry.setText("Expires: " + document.getExpiryDate());
            holder.tvExpiry.setVisibility(View.VISIBLE);
        } else {
            holder.tvExpiry.setVisibility(View.GONE);
        }

        holder.btnOpen.setOnClickListener(v -> listener.onOpenClick(document));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(document));
        holder.btnDownload.setOnClickListener(v -> listener.onDownloadClick(document));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(document));
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public void updateList(List<Document> newList) {
        this.documentList = newList;
        notifyDataSetChanged();
    }

    public static class DocumentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvExpiry;
        ImageButton btnDelete, btnEdit, btnOpen, btnDownload;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_doc_name);
            tvCategory = itemView.findViewById(R.id.tv_doc_category);
            tvExpiry = itemView.findViewById(R.id.tv_doc_expiry);
            btnOpen = itemView.findViewById(R.id.btn_open_doc);
            btnEdit = itemView.findViewById(R.id.btn_edit_doc);
            btnDownload = itemView.findViewById(R.id.btn_download_doc);
            btnDelete = itemView.findViewById(R.id.btn_delete_doc);
        }
    }
}
