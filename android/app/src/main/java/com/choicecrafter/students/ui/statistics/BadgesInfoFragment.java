package com.choicecrafter.students.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.MainViewModel;
import com.choicecrafter.students.R;
import com.choicecrafter.students.badges.BadgeEvaluator;
import com.choicecrafter.students.models.badges.BadgeStatus;

import java.util.List;

public class BadgesInfoFragment extends Fragment {

    private BadgeDetailAdapter badgeDetailAdapter;
    private TextView emptyStateView;
    private MainViewModel mainViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges_info, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.badgesDetailRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        badgeDetailAdapter = new BadgeDetailAdapter();
        recyclerView.setAdapter(badgeDetailAdapter);
        emptyStateView = view.findViewById(R.id.badgesDetailEmptyState);

        if (mainViewModel != null) {
            mainViewModel.getBadgeStatuses().observe(getViewLifecycleOwner(), this::renderBadgeStatuses);
        } else {
            renderBadgeStatuses(null);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_badges);
    }

    private void renderBadgeStatuses(@Nullable List<BadgeStatus> statuses) {
        List<BadgeStatus> data = statuses;
        if (data == null || data.isEmpty()) {
            data = BadgeEvaluator.buildDefaultStatuses();
            if (emptyStateView != null) {
                emptyStateView.setVisibility(View.VISIBLE);
            }
        } else if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
        if (badgeDetailAdapter != null) {
            badgeDetailAdapter.submitList(data);
        }
    }
}
