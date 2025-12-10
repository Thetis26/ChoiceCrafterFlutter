package com.choicecrafter.students.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.PersonalActivity;
import com.choicecrafter.students.utils.TimeAgoUtil;

import java.util.List;

public class PersonalActivityAdapter extends RecyclerView.Adapter<PersonalActivityAdapter.ActivityViewHolder> {

    private List<PersonalActivity> activityList;

    public PersonalActivityAdapter(List<PersonalActivity> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_card, parent, false);
        return new ActivityViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        PersonalActivity activity = activityList.get(position);
        holder.activityName.setText(activity.getActivityName());
        holder.activityDescription.setText(activity.getActivityDescription());
        holder.activityTime.setText(TimeAgoUtil.getTimeAgo(activity.getActivityTime()));
        holder.imageView.setImageResource(R.drawable.star_badge);
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView activityName, activityDescription, activityTime;
        ImageView imageView;

        public ActivityViewHolder(View itemView) {
            super(itemView);
            activityName = itemView.findViewById(R.id.comment_author);
            activityDescription = itemView.findViewById(R.id.comment_content);
            activityTime = itemView.findViewById(R.id.comment_date);
            imageView = itemView.findViewById(R.id.icon_colleague);
        }
    }
}
