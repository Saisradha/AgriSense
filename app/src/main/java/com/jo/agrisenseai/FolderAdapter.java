package com.jo.agrisenseai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    public interface FolderListener {
        void onFolderClick(FolderEntity folder);
        void onFolderLongClick(View anchorView, FolderEntity folder);
    }

    private final List<FolderEntity> folders;
    private final FolderListener     listener;

    public FolderAdapter(List<FolderEntity> folders, FolderListener listener) {
        this.folders  = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderEntity folder = folders.get(position);
        holder.name.setText(folder.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFolderClick(folder);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onFolderLongClick(v, folder);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.folderName);
        }
    }
}
