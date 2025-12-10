package com.choicecrafter.students.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.students.R;
import com.choicecrafter.students.models.Conversation;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private final List<Conversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final Context context;
    private final Map<String, String> userAvatarMap = new HashMap<>();

    public ConversationAdapter(Context context, OnConversationClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setUserAvatars(Collection<? extends com.choicecrafter.students.models.User> users) {
        userAvatarMap.clear();
        if (users == null || users.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        addOrUpdateUserAvatars(users, false);
    }

    public void addOrUpdateUserAvatars(Collection<? extends com.choicecrafter.students.models.User> users) {
        addOrUpdateUserAvatars(users, true);
    }

    private void addOrUpdateUserAvatars(Collection<? extends com.choicecrafter.students.models.User> users, boolean notifyOnChange) {
        if (users == null) {
            return;
        }
        boolean changed = false;
        for (com.choicecrafter.students.models.User user : users) {
            if (user == null || TextUtils.isEmpty(user.getEmail())) {
                continue;
            }
            String avatarUrl = user.getAnonymousAvatar() != null ? user.getAnonymousAvatar().getImageUrl() : null;
            String existing = userAvatarMap.put(user.getEmail(), avatarUrl);
            if (!TextUtils.equals(existing, avatarUrl)) {
                changed = true;
            }
        }
        if (!notifyOnChange || changed) {
            notifyDataSetChanged();
        }
    }

    public void setConversations(List<Conversation> newConversations) {
        conversations.clear();
        conversations.addAll(newConversations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ShapeableImageView imagePrimary;
        ShapeableImageView imageSecondary;
        ShapeableImageView imageTertiary;
        TextView title;
        TextView lastMessage;
        TextView time;
        View unreadIndicator;
        Conversation conversation;
        final int singleAvatarSize;
        final int multiAvatarSize;
        final float twoAvatarOffset;
        final float threeAvatarHorizontalOffset;
        final float threeAvatarVerticalOffset;
        final MaterialCardView cardView;
        final int unreadBackgroundColor;
        final int defaultBackgroundColor;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imagePrimary = itemView.findViewById(R.id.conversationImage1);
            imageSecondary = itemView.findViewById(R.id.conversationImage2);
            imageTertiary = itemView.findViewById(R.id.conversationImage3);
            title = itemView.findViewById(R.id.conversationTitle);
            lastMessage = itemView.findViewById(R.id.conversationLastMessage);
            time = itemView.findViewById(R.id.conversationTime);
            unreadIndicator = itemView.findViewById(R.id.conversationUnread);
            itemView.setOnClickListener(this);
            singleAvatarSize = dpToPx(48);
            multiAvatarSize = dpToPx(36);
            twoAvatarOffset = dpToPx(12);
            threeAvatarHorizontalOffset = dpToPx(12);
            threeAvatarVerticalOffset = dpToPx(10);
            unreadBackgroundColor = ContextCompat.getColor(context, R.color.conversation_unread_background);
            defaultBackgroundColor = MaterialColors.getColor(cardView, com.google.android.material.R.attr.colorSurface);
        }

        void bind(Conversation conversation) {
            this.conversation = conversation;
            title.setText(conversation.getTitle());
            lastMessage.setText(conversation.getLastMessage());
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(conversation.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            time.setText(timeAgo);
            unreadIndicator.setVisibility(conversation.isUnread() ? View.VISIBLE : View.GONE);
            bindParticipantAvatars(conversation.getParticipants());
            title.setTypeface(null, conversation.isUnread() ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            cardView.setCardBackgroundColor(conversation.isUnread() ? unreadBackgroundColor : defaultBackgroundColor);
        }

        private void bindParticipantAvatars(List<String> participants) {
            List<String> uniqueParticipants = new ArrayList<>();
            if (participants != null) {
                LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
                for (String participant : participants) {
                    if (!TextUtils.isEmpty(participant)) {
                        uniqueIds.add(participant);
                    }
                }
                uniqueParticipants.addAll(uniqueIds);
            }

            if (uniqueParticipants.isEmpty()) {
                showPlaceholderAvatar();
                return;
            }

            if (uniqueParticipants.size() > 3) {
                Collections.shuffle(uniqueParticipants);
            }
            List<String> displayParticipants = uniqueParticipants.size() > 3
                    ? new ArrayList<>(uniqueParticipants.subList(0, 3))
                    : uniqueParticipants;

            adjustAvatarLayout(displayParticipants.size());
            ShapeableImageView[] avatarViews = {imagePrimary, imageSecondary, imageTertiary};
            for (int i = 0; i < avatarViews.length; i++) {
                ShapeableImageView avatarView = avatarViews[i];
                if (i < displayParticipants.size()) {
                    String participantId = displayParticipants.get(i);
                    String avatarUrl = userAvatarMap.get(participantId);
                    Object source = !TextUtils.isEmpty(avatarUrl) ? avatarUrl : R.drawable.profile;
                    avatarView.setVisibility(View.VISIBLE);
                    Glide.with(context)
                            .load(source)
                            .placeholder(R.drawable.ic_baseline_person_24)
                            .error(R.drawable.ic_baseline_person_24)
                            .into(avatarView);
                } else {
                    avatarView.setVisibility(View.GONE);
                }
            }
        }

        private void showPlaceholderAvatar() {
            adjustAvatarLayout(1);
            imagePrimary.setVisibility(View.VISIBLE);
            imageSecondary.setVisibility(View.GONE);
            imageTertiary.setVisibility(View.GONE);
            Glide.with(context)
                    .load(R.drawable.profile)
                    .placeholder(R.drawable.ic_baseline_person_24)
                    .into(imagePrimary);
        }

        private void adjustAvatarLayout(int avatarCount) {
            updateAvatarSize(imagePrimary, avatarCount <= 1 ? singleAvatarSize : multiAvatarSize);
            updateAvatarSize(imageSecondary, multiAvatarSize);
            updateAvatarSize(imageTertiary, multiAvatarSize);

            imagePrimary.setTranslationX(0f);
            imagePrimary.setTranslationY(0f);
            imageSecondary.setTranslationX(0f);
            imageSecondary.setTranslationY(0f);
            imageTertiary.setTranslationX(0f);
            imageTertiary.setTranslationY(0f);

            imagePrimary.bringToFront();
            imageSecondary.bringToFront();
            imageTertiary.bringToFront();

            if (avatarCount <= 1) {
                return;
            }

            if (avatarCount == 2) {
                float offset = twoAvatarOffset / 2f;
                imagePrimary.setTranslationX(-offset);
                imageSecondary.setTranslationX(offset);
                imageSecondary.bringToFront();
                imageTertiary.setVisibility(View.GONE);
            } else {
                imagePrimary.setTranslationX(-threeAvatarHorizontalOffset);
                imagePrimary.setTranslationY(-threeAvatarVerticalOffset);
                imageSecondary.setTranslationX(threeAvatarHorizontalOffset);
                imageSecondary.setTranslationY(-threeAvatarVerticalOffset);
                imageTertiary.setTranslationY(threeAvatarVerticalOffset);
                imageTertiary.bringToFront();
            }
        }

        private void updateAvatarSize(ShapeableImageView view, int size) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = size;
            params.height = size;
            view.setLayoutParams(params);
        }

        private int dpToPx(int dp) {
            float density = itemView.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }

        @Override
        public void onClick(View v) {
            if (listener != null && conversation != null) {
                listener.onConversationClick(conversation);
            }
        }
    }
}
