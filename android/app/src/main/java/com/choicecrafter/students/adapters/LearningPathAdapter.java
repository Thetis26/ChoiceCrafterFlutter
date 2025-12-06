package com.choicecrafter.studentapp.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.databinding.LearningPathItemBinding;

import java.util.List;

public class LearningPathAdapter extends RecyclerView.Adapter<LearningPathAdapter.LearningPathViewHolder> {

    private final List<String> modules;
    public LearningPathAdapter(List<String> modules) {
        this.modules = modules;
    }

    @NonNull
    @Override
    public LearningPathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        LearningPathItemBinding binding = LearningPathItemBinding.inflate(inflater, parent, false);
        return new LearningPathViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LearningPathViewHolder holder, int position) {
        String moduleName = modules.get(position);
        holder.binding.moduleNameTextView.setText(moduleName);
        holder.binding.getRoot().setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("moduleName", moduleName);
            Navigation.findNavController(v).navigate(R.id.moduleFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    static class LearningPathViewHolder extends RecyclerView.ViewHolder {
        private final LearningPathItemBinding binding;

        public LearningPathViewHolder(LearningPathItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
