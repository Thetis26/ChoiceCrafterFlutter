package com.choicecrafter.studentapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.EnrollmentActivityProgress;

import java.util.List;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder> {

    private final List<EnrollmentActivityProgress> userActivities;
    private final OnStatisticsActionListener actionListener;

    public interface OnStatisticsActionListener {
        void onDiscussionClick(EnrollmentActivityProgress userActivity);
    }

    public StatisticsAdapter(List<EnrollmentActivityProgress> userActivities, OnStatisticsActionListener actionListener) {
        this.userActivities = userActivities;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public StatisticsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.statistics_card_updated, parent, false);
        return new StatisticsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatisticsViewHolder holder, int position) {
        EnrollmentActivityProgress userActivity = userActivities.get(position);
        holder.bind(userActivity);
    }

    @Override
    public int getItemCount() {
        return userActivities.size();
    }

    class StatisticsViewHolder extends RecyclerView.ViewHolder {
        private final Button toDiscussionsButton;

        public StatisticsViewHolder(@NonNull View itemView) {
            super(itemView);
            toDiscussionsButton = itemView.findViewById(R.id.to_discussion);
        }

        public void bind(EnrollmentActivityProgress userActivity) {
            if (actionListener == null) {
                return;
            }

            toDiscussionsButton.setOnClickListener(v -> actionListener.onDiscussionClick(userActivity));
        }
    }
}