package com.choicecrafter.students.adapters.tasks;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.students.R;
import com.choicecrafter.students.models.tasks.InfoCardTask;

public class InfoCardViewHolder extends RecyclerView.ViewHolder {

    private final TextView titleView;
    private final TextView descriptionView;
    private final TextView infoTextView;
    private final View mediaContainer;
    private final ImageView mediaImageView;
    private final View mediaOverlayView;
    private final Button interactiveButton;
    private final Button continueButton;

    public InfoCardViewHolder(@NonNull View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.info_card_title);
        descriptionView = itemView.findViewById(R.id.info_card_description);
        infoTextView = itemView.findViewById(R.id.info_card_text);
        mediaContainer = itemView.findViewById(R.id.info_card_media_container);
        mediaImageView = itemView.findViewById(R.id.info_card_image);
        mediaOverlayView = itemView.findViewById(R.id.info_card_media_overlay);
        interactiveButton = itemView.findViewById(R.id.interactive_button);
        continueButton = itemView.findViewById(R.id.check_answer_button);
    }

    public void bind(InfoCardTask task) {
        if (continueButton != null) {
            continueButton.setText(itemView.getContext().getString(R.string.info_card_continue));
        }

        titleView.setText(!TextUtils.isEmpty(task.getTitle())
                ? task.getTitle()
                : itemView.getContext().getString(R.string.info_card_default_title));
        if (descriptionView != null) {
            if (TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.GONE);
            } else {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            }
        }

        String contentType = task.getContentType() != null ? task.getContentType() : "";
        String contentText = task.getContentText();
        String mediaUrl = task.getMediaUrl();
        String interactiveUrl = task.getInteractiveUrl();

        infoTextView.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.GONE);
        if (interactiveButton != null) {
            interactiveButton.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(contentText)) {
            infoTextView.setVisibility(View.VISIBLE);
            infoTextView.setText(contentText);
        }

        boolean hasMedia = !TextUtils.isEmpty(mediaUrl);
        if (hasMedia) {
            mediaContainer.setVisibility(View.VISIBLE);
            Glide.with(mediaImageView.getContext())
                    .load(mediaUrl)
                    .placeholder(R.drawable.loading_background)
                    .error(R.drawable.bg_recommendation_thumbnail)
                    .into(mediaImageView);
        }

        if (mediaOverlayView != null) {
            boolean showOverlay = "video".equalsIgnoreCase(contentType)
                    || "interactive_video".equalsIgnoreCase(contentType)
                    || (!TextUtils.isEmpty(interactiveUrl) && TextUtils.isEmpty(contentType));
            mediaOverlayView.setVisibility(showOverlay ? View.VISIBLE : View.GONE);
        }

        if (!TextUtils.isEmpty(interactiveUrl) && interactiveButton != null) {
            interactiveButton.setVisibility(View.VISIBLE);
            String actionLabel = !TextUtils.isEmpty(task.getActionText())
                    ? task.getActionText()
                    : itemView.getContext().getString(R.string.info_card_action_explore);
            interactiveButton.setText(actionLabel);
            interactiveButton.setOnClickListener(v -> openInteractiveLink(interactiveUrl));
            mediaContainer.setOnClickListener(v -> openInteractiveLink(interactiveUrl));
        } else {
            mediaContainer.setOnClickListener(null);
        }
    }

    public boolean acknowledgeTask() {
        return true;
    }

    private void openInteractiveLink(String url) {
        Context context = itemView.getContext();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.info_card_unable_to_open_link, Toast.LENGTH_SHORT).show();
        }
    }
}
