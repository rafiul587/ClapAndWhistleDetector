package com.example.clapandwhistledetector.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clapandwhistledetector.model.FileModel;
import com.example.clapandwhistledetector.databinding.ItemFileBinding;

import java.util.List;


 public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileHolder> {

    public interface ClickListener {
        void onClick(int adapterPosition);
    }

    ClickListener clickListener;
    private List<FileModel> fileList;

    public FileListAdapter(ClickListener clickListener, List<FileModel> fileList) {
        this.clickListener = clickListener;
        this.fileList = fileList;
    }

    ItemFileBinding binding;

    @NonNull
    @Override
    public FileHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        binding = ItemFileBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);

        return new FileHolder(binding);
    }

    @Override
    public void onBindViewHolder(FileHolder fileHolder, int i) {
        Log.d("TAG", "onBindViewHolder: ");
        fileHolder.fileName.setText(fileList.get(i).getName());
        fileHolder.radioButton.setChecked(fileList.get(i).isActive());
        if(!fileHolder.radioButton.isChecked()){
            fileHolder.radioButton.setAlpha(.5f);
        }
    }

    public class FileHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView fileName;
        RadioButton radioButton;

        public FileHolder(@NonNull ItemFileBinding itemView) {
            super(itemView.getRoot());
            fileName = itemView.fileName;
            radioButton = itemView.radioButton;
            itemView.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(getAdapterPosition());
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }
}

