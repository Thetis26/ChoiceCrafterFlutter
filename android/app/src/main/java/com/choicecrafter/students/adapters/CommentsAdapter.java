package com.choicecrafter.studentapp.adapters;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.Comment;
import com.choicecrafter.studentapp.utils.Avatar;
import com.choicecrafter.studentapp.utils.TimeAgoUtil;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private static final List<Integer> FALLBACK_AVATARS = Arrays.asList(
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar_andrei
    );

    private final List<Comment> comments;
    @Nullable
    private final String currentUserIdNormalized;
    @Nullable
    private final Avatar currentUserAvatar;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Map<String, String> displayNameCache = new HashMap<>();
    private final Map<String, Avatar> avatarCache = new HashMap<>();
    private final Set<String> loadingUsers = new HashSet<>();

    public CommentsAdapter(List<Comment> comments,
                           @Nullable String currentUserId,
                           @Nullable Avatar currentUserAvatar) {
        this.comments = comments;
        this.currentUserAvatar = currentUserAvatar;
        this.currentUserIdNormalized = normalize(currentUserId);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_card, parent, false);
        return new CommentViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.text.setText(comment.getText());
        holder.timestamp.setText(formatTimestamp(comment.getTimestamp()));

        String userId = comment.getUserId();
        if (isCurrentUser(userId)) {
            holder.user.setText(holder.itemView.getContext().getString(R.string.comment_author_you));
            loadAvatarIntoView(holder, currentUserAvatar, position);
            return;
        }

        String normalizedUserId = normalize(userId);
        String cachedName = !TextUtils.isEmpty(normalizedUserId) ? displayNameCache.get(normalizedUserId) : null;
        if (!TextUtils.isEmpty(cachedName)) {
            holder.user.setText(cachedName);
        } else {
            holder.user.setText(holder.itemView.getContext().getString(R.string.comment_author_anonymous));
            if (!TextUtils.isEmpty(userId)) {
                fetchUserDetails(userId, holder.itemView.getContext());
            }
        }

        Avatar cachedAvatar = !TextUtils.isEmpty(normalizedUserId) ? avatarCache.get(normalizedUserId) : null;
        if (cachedAvatar != null && !TextUtils.isEmpty(cachedAvatar.getImageUrl())) {
            loadAvatarIntoView(holder, cachedAvatar, position);
        } else {
            loadAvatarIntoView(holder, null, position);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private void fetchUserDetails(@NonNull String userId, Context context) {
        String normalizedUserId = normalize(userId);
        if (TextUtils.isEmpty(normalizedUserId)) {
            return;
        }
        if (displayNameCache.containsKey(normalizedUserId) || loadingUsers.contains(normalizedUserId)) {
            return;
        }
        loadingUsers.add(normalizedUserId);
        firestore.collection("users")
                .whereEqualTo("email", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String resolvedName = context.getString(R.string.comment_author_anonymous);
                    Avatar resolvedAvatar = null;
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Object avatarObj = document.get("anonymousAvatar");
                        String avatarName = null;
                        String avatarImageUrl = null;
                        if (avatarObj instanceof Map<?, ?> rawMap) {
                            Object nameObj = rawMap.get("name");
                            Object imageObj = rawMap.get("imageUrl");
                            if (nameObj != null) {
                                avatarName = String.valueOf(nameObj);
                            }
                            if (imageObj != null) {
                                avatarImageUrl = String.valueOf(imageObj);
                            }
                        }
                        if (TextUtils.isEmpty(avatarName)) {
                            avatarName = document.getString("name");
                        }
                        if (!TextUtils.isEmpty(avatarName)) {
                            resolvedName = avatarName;
                        }
                        resolvedAvatar = new Avatar(avatarName, avatarImageUrl);
                    }
                    displayNameCache.put(normalizedUserId, resolvedName);
                    if (resolvedAvatar != null) {
                        avatarCache.put(normalizedUserId, resolvedAvatar);
                    }
                    loadingUsers.remove(normalizedUserId);
                    notifyUserUpdated(userId);
                })
                .addOnFailureListener(e -> {
                    displayNameCache.put(normalizedUserId, context.getString(R.string.comment_author_anonymous));
                    avatarCache.remove(normalizedUserId);
                    loadingUsers.remove(normalizedUserId);
                    notifyUserUpdated(userId);
                });
    }

    private void notifyUserUpdated(String userId) {
        String normalized = normalize(userId);
        if (TextUtils.isEmpty(normalized)) {
            return;
        }
        for (int i = 0; i < comments.size(); i++) {
            if (normalized.equals(normalize(comments.get(i).getUserId()))) {
                notifyItemChanged(i);
            }
        }
    }

    private boolean isCurrentUser(@Nullable String userId) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(currentUserIdNormalized)) {
            return false;
        }
        return currentUserIdNormalized.equals(normalize(userId));
    }

    @Nullable
    private String formatTimestamp(@Nullable String rawTimestamp) {
        if (TextUtils.isEmpty(rawTimestamp)) {
            return null;
        }
        try {
            return TimeAgoUtil.getTimeAgo(Long.parseLong(rawTimestamp));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void loadAvatarIntoView(@NonNull CommentViewHolder holder, @Nullable Avatar avatar, int fallbackIndex) {
        int adapterPosition = holder.getBindingAdapterPosition();
        if (adapterPosition != RecyclerView.NO_POSITION) {
            fallbackIndex = adapterPosition;
        }
        int fallbackRes = FALLBACK_AVATARS.get(Math.floorMod(fallbackIndex, FALLBACK_AVATARS.size()));
        if (avatar != null && !TextUtils.isEmpty(avatar.getImageUrl())) {
            Glide.with(holder.image.getContext())
                    .load(avatar.getImageUrl())
                    .placeholder(fallbackRes)
                    .error(fallbackRes)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(fallbackRes);
        }
    }

    @Nullable
    private String normalize(@Nullable String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        return userId.trim().toLowerCase(Locale.ROOT);
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        final TextView user;
        final TextView text;
        final TextView timestamp;
        final ShapeableImageView image;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            user = itemView.findViewById(R.id.comment_author);
            text = itemView.findViewById(R.id.comment_content);
            timestamp = itemView.findViewById(R.id.comment_date);
            image = itemView.findViewById(R.id.icon_colleague);
        }
    }
}
