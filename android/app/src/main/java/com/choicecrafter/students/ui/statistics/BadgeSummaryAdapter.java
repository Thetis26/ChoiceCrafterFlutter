package com.choicecrafter.studentapp.ui.statistics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.badges.BadgeDefinition;
import com.choicecrafter.studentapp.models.badges.BadgeStatus;

import java.util.ArrayList;
import java.util.List;

class BadgeSummaryAdapter extends RecyclerView.Adapter<BadgeSummaryAdapter.BadgeViewHolder> {

    private final List<BadgeStatus> earnedBadges = new ArrayList<>();

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_summary, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        BadgeStatus status = earnedBadges.get(position);
        BadgeDefinition definition = status.getDefinition();
        holder.badgeTitle.setText(definition.getTitle());
        holder.badgePoints.setText(holder.itemView.getContext()
                .getString(R.string.badge_points_summary_format, definition.getPoints()));
        holder.badgeIcon.setImageResource(R.drawable.trophy_award);
    }

    @Override
    public int getItemCount() {
        return earnedBadges.size();
    }

    void submitList(List<BadgeStatus> statuses) {
        earnedBadges.clear();
        if (statuses != null) {
            for (BadgeStatus status : statuses) {
                if (status != null && status.isEarned()) {
                    earnedBadges.add(status);
                }
            }
        }
        notifyDataSetChanged();
    }

    static final class BadgeViewHolder extends RecyclerView.ViewHolder {
        final TextView badgeTitle;
        final TextView badgePoints;
        final ImageView badgeIcon;

        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeTitle = itemView.findViewById(R.id.badgeTitle);
            badgePoints = itemView.findViewById(R.id.badgePoints);
            badgeIcon = itemView.findViewById(R.id.badgeIcon);
        }
    }
}
