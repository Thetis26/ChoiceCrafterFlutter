package com.choicecrafter.studentapp.ui.statistics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.badges.BadgeDefinition;
import com.choicecrafter.studentapp.models.badges.BadgeStatus;

import java.util.ArrayList;
import java.util.List;

public class BadgeDetailAdapter extends RecyclerView.Adapter<BadgeDetailAdapter.BadgeViewHolder> {

    private final List<BadgeStatus> badges = new ArrayList<>();

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_detail, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        BadgeStatus status = badges.get(position);
        BadgeDefinition definition = status.getDefinition();
        holder.title.setText(definition.getTitle());
        holder.description.setText(definition.getDescription());
        holder.points.setText(holder.itemView.getContext()
                .getString(R.string.badge_points_detail_format, definition.getPoints()));

        if (status.isEarned()) {
            holder.icon.setImageResource(R.drawable.trophy_award);
            holder.status.setText(R.string.badge_status_unlocked);
            holder.status.setBackgroundResource(R.drawable.bg_badge_status_unlocked);
            holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        } else {
            holder.icon.setImageResource(R.drawable.trophy);
            holder.status.setText(R.string.badge_status_locked);
            holder.status.setBackgroundResource(R.drawable.bg_badge_status_locked);
            holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.scorpion));
        }
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    public void submitList(List<BadgeStatus> statuses) {
        badges.clear();
        if (statuses != null) {
            badges.addAll(statuses);
        }
        notifyDataSetChanged();
    }

    static final class BadgeViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView status;
        final TextView points;
        final TextView description;

        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.badgeDetailIcon);
            title = itemView.findViewById(R.id.badgeDetailTitle);
            status = itemView.findViewById(R.id.badgeDetailStatus);
            points = itemView.findViewById(R.id.badgeDetailPoints);
            description = itemView.findViewById(R.id.badgeDetailDescription);
        }
    }
}
