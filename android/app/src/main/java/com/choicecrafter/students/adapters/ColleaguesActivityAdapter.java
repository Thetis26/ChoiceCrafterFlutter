package com.choicecrafter.students.adapters;

import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.ColleagueActivity;
import com.choicecrafter.students.utils.TimeAgoUtil;

import java.util.List;

public class ColleaguesActivityAdapter extends RecyclerView.Adapter<ColleaguesActivityAdapter.ActivityViewHolder> {

    private List<ColleagueActivity> activityList;

    public ColleaguesActivityAdapter(List<ColleagueActivity> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.colleague_activity_card, parent, false);
        return new ActivityViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ColleagueActivity activity = activityList.get(position);
        holder.activityName.setText(activity.getActivityName());
        holder.activityDescription.setText(activity.getActivityDescription());
        holder.timestamp.setText(TimeAgoUtil.getTimeAgo(activity.getTimestamp()));
        String anonymousName = activity.getAnonymousName();
        if (anonymousName == null || anonymousName.trim().isEmpty()) {
            anonymousName = holder.colleagueName.getContext().getString(R.string.colleagues_activity_anonymous_name_fallback);
        }
        holder.colleagueName.setText(anonymousName);
        Log.i("ColleaguesActivityAdapter", "Loading avatar image URL: " + activity.getImageUrl());
        try {
            Glide.with(holder.colleagueImage.getContext())
                    .load(activity.getImageUrl())
                    .placeholder(R.drawable.avatar_andrei)
                    .error(R.drawable.avatar1)
                    .into(holder.colleagueImage);
        } catch (Exception e) {
            Log.e("ColleaguesActivityAdapter", "Error loading image with Glide", e);
            holder.colleagueName.setText(holder.colleagueName.getContext().getString(R.string.colleagues_activity_anonymous_name_fallback));
            holder.colleagueImage.setImageResource(R.drawable.avatar1);
        }
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView colleagueName, activityName, activityDescription, timestamp;
        ImageView colleagueImage;

        public ActivityViewHolder(View itemView) {
            super(itemView);
            colleagueName = itemView.findViewById(R.id.colleagueName);
            activityName = itemView.findViewById(R.id.activityName);
            activityDescription = itemView.findViewById(R.id.activityDescription);
            timestamp = itemView.findViewById(R.id.timestamp);
            colleagueImage = itemView.findViewById(R.id.colleagueImage);
        }
    }
}
