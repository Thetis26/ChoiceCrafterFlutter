package com.choicecrafter.students.ui.news;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.choicecrafter.students.databinding.FragmentLearningPathDetailBinding;
import com.choicecrafter.students.R;
import com.choicecrafter.students.adapters.ModuleAdapter;
import com.choicecrafter.students.models.Course;
import com.choicecrafter.students.models.Module;


public class LearningPathDetailFragment extends Fragment {

    private FragmentLearningPathDetailBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLearningPathDetailBinding.inflate(inflater, container, false);
        //requireActivity().findViewById(R.id.app_bar_main).setVisibility(View.GONE);

        if (getArguments() != null) {
            Course learningPath = getArguments().getParcelable("course");
            String userId = getArguments().getString("userId");
            if (learningPath != null) {
                if (!TextUtils.isEmpty(learningPath.getTitle())) {
                    binding.learningPathTitleTextView.setText(learningPath.getTitle());
                }
                if (!TextUtils.isEmpty(learningPath.getDescription())) {
                    binding.learningPathDescriptionTextView.setText(learningPath.getDescription());
                    binding.learningPathDescriptionTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.learningPathDescriptionTextView.setVisibility(View.GONE);
                }

                int moduleCount = 0;
                int totalActivityCount = 0;
                if (learningPath.getModules() != null && !learningPath.getModules().isEmpty()) {
                    moduleCount = learningPath.getModules().size();
                    for (Module module : learningPath.getModules()) {
                        if (module != null && module.getActivities() != null) {
                            totalActivityCount += module.getActivities().size();
                        }
                    }
                } else if (learningPath.getActivities() != null) {
                    totalActivityCount = learningPath.getActivities().size();
                }

                boolean hasStructureInformation = moduleCount > 0 || totalActivityCount > 0;
                binding.learningPathStructureHeaderTextView.setVisibility(
                        hasStructureInformation ? View.VISIBLE : View.GONE);

                if (moduleCount > 0) {
                    binding.learningPathModuleSummaryTextView.setText(
                            getString(R.string.learning_path_module_summary_format, moduleCount));
                    binding.learningPathModuleSummaryTextView.setVisibility(View.VISIBLE);

                    binding.modulesRecyclerView.setVisibility(View.VISIBLE);
                    binding.modulesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.modulesRecyclerView.setAdapter(
                            new ModuleAdapter(learningPath.getModules(), userId, learningPath.getId(), null, false));
                } else {
                    binding.learningPathModuleSummaryTextView.setVisibility(View.GONE);
                    binding.modulesRecyclerView.setVisibility(View.GONE);
                }

                if (totalActivityCount > 0) {
                    binding.learningPathActivitySummaryTextView.setText(
                            getString(R.string.learning_path_activity_summary_format, totalActivityCount));
                    binding.learningPathActivitySummaryTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.learningPathActivitySummaryTextView.setVisibility(View.GONE);
                }

                binding.learningPathNoStructureTextView.setVisibility(
                        hasStructureInformation ? View.GONE : View.VISIBLE);
            }
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
