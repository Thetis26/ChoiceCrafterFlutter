package com.choicecrafter.students.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.databinding.ModuleCardBinding;
import com.choicecrafter.students.models.Module;
import com.choicecrafter.students.models.ModuleProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

    private final List<Module> modules;
    private final String userId;
    private final String courseId;
    private final boolean enableModuleNavigation;
    private final Map<String, ModuleProgress> moduleProgressMap;

    public ModuleAdapter(List<Module> modules, String userId, String courseId) {
        this(modules, userId, courseId, Collections.emptyMap(), true);
    }

    public ModuleAdapter(List<Module> modules,
                         String userId,
                         String courseId,
                         boolean enableModuleNavigation) {
        this(modules, userId, courseId, Collections.emptyMap(), enableModuleNavigation);
    }

    public ModuleAdapter(List<Module> modules,
                         String userId,
                         String courseId,
                         Map<String, ModuleProgress> moduleProgressMap,
                         boolean enableModuleNavigation) {
        this.modules = new ArrayList<>();
        if (modules != null) {
            this.modules.addAll(modules);
        }
        this.userId = userId;
        this.courseId = courseId;
        this.enableModuleNavigation = enableModuleNavigation;
        this.moduleProgressMap = new HashMap<>();
        if (moduleProgressMap != null) {
            this.moduleProgressMap.putAll(moduleProgressMap);
        }
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ModuleCardBinding binding = ModuleCardBinding.inflate(inflater, parent, false);
        return new ModuleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        Module module = modules.get(position);
        Context context = holder.itemView.getContext();
        holder.binding.moduleTitleTextView.setText(module.getTitle());
        holder.binding.moduleDescriptionTextView.setText(module.getDescription());

        int activityCount = module.getActivities() != null ? module.getActivities().size() : 0;
        holder.binding.moduleActivitiesTextView.setText(
                context.getString(R.string.module_activities_count, activityCount));

        int totalTasks = countModuleTasks(module);
        ModuleProgress moduleProgress = findModuleProgress(module, position);
        int completedTasks = 0;
        if (moduleProgress != null) {
            int moduleTotal = moduleProgress.getTotalTasks();
            if (moduleTotal > 0) {
                totalTasks = totalTasks > 0 ? totalTasks : moduleTotal;
            }
            completedTasks = Math.min(moduleProgress.getCompletedTasks(), totalTasks > 0 ? totalTasks : moduleProgress.getCompletedTasks());
        }
        int progress = computeProgressPercentage(module, totalTasks, completedTasks);
        holder.binding.moduleProgressIndicator.setProgress(progress);
        holder.binding.moduleProgressValueTextView.setText(
                context.getString(R.string.module_progress_percentage, progress));
        if (enableModuleNavigation) {
            holder.binding.getRoot().setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putParcelable("module", module);
                bundle.putString("userId", userId);
                bundle.putString("courseId", courseId);
                Navigation.findNavController(v).navigate(R.id.moduleFragment, bundle);
            });
        } else {
            holder.binding.getRoot().setOnClickListener(null);
            holder.binding.getRoot().setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        private final ModuleCardBinding binding;

        public ModuleViewHolder(ModuleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void updateModuleProgress(Map<String, ModuleProgress> progressMap) {
        moduleProgressMap.clear();
        if (progressMap != null) {
            moduleProgressMap.putAll(progressMap);
        }
        notifyDataSetChanged();
    }

    public void updateModules(List<Module> updatedModules) {
        modules.clear();
        if (updatedModules != null) {
            modules.addAll(updatedModules);
        }
        notifyDataSetChanged();
    }

    private int computeProgressPercentage(Module module, int totalTasks, int completedTasks) {
        if (!moduleProgressMap.isEmpty() && totalTasks > 0) {
            return Math.max(0, Math.min(100, Math.round((completedTasks * 100f) / totalTasks)));
        }
        return Math.max(0, Math.min(100, module.getCompletedPercentage()));
    }

    private ModuleProgress findModuleProgress(Module module, int position) {
        if (module == null || moduleProgressMap.isEmpty()) {
            return null;
        }
        String id = normalize(module.getId());
        if (!id.isEmpty() && moduleProgressMap.containsKey(id)) {
            return moduleProgressMap.get(id);
        }
        String title = normalize(module.getTitle());
        if (!title.isEmpty() && moduleProgressMap.containsKey(title)) {
            return moduleProgressMap.get(title);
        }
        String indexKey = "module_" + position;
        return moduleProgressMap.get(indexKey);
    }

    private int countModuleTasks(Module module) {
        if (module == null || module.getActivities() == null) {
            return 0;
        }
        int count = 0;
        for (com.choicecrafter.students.models.Activity activity : module.getActivities()) {
            if (activity != null && activity.getTasks() != null) {
                count += activity.getTasks().size();
            }
        }
        return count;
    }

    private String normalize(String value) {
        return value != null ? value.trim() : "";
    }
}
