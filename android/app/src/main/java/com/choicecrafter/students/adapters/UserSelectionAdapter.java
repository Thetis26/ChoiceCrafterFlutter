package com.choicecrafter.studentapp.adapters;

import android.content.Context;
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

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final List<User> users = new ArrayList<>();
    private final Context context;
    private final OnUserClickListener listener;

    public UserSelectionAdapter(Context context, OnUserClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setUsers(List<User> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_selection, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ShapeableImageView avatar;
        private final TextView name;
        private final TextView email;
        private User user;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.userAvatar);
            name = itemView.findViewById(R.id.userName);
            email = itemView.findViewById(R.id.userEmail);
            itemView.setOnClickListener(this);
        }

        void bind(User user) {
            this.user = user;
            name.setText(user.getName());
            email.setText(user.getEmail());
            if (user.getAnonymousAvatar() != null) {
                Glide.with(context)
                        .load(user.getAnonymousAvatar().getImageUrl())
                        .placeholder(R.drawable.ic_baseline_person_24)
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.ic_baseline_person_24);
            }
        }

        @Override
        public void onClick(View v) {
            if (listener != null && user != null) {
                listener.onUserClick(user);
            }
        }
    }
}
