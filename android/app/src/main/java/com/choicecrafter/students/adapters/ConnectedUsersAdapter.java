package com.choicecrafter.studentapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ConnectedUsersAdapter extends RecyclerView.Adapter<ConnectedUsersAdapter.UserViewHolder> {

    private final List<User> users = new ArrayList<>();
    private final Context context;

    public ConnectedUsersAdapter(Context context) {
        this.context = context;
    }

    public void setUsers(List<User> newUsers) {
        Log.i("ConnectedUsersAdapter", "setUsers called with newUsers size: " + newUsers.size());
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_connected_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.getName());
        Glide.with(context)
                .load(user.getAnonymousAvatar().getImageUrl())
                .placeholder(R.drawable.avatar1)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView image;
        TextView name;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.userImage);
            name = itemView.findViewById(R.id.userName);
        }
    }
}
