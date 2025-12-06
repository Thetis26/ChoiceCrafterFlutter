package com.choicecrafter.studentapp.adapters.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.Activity;
import com.choicecrafter.studentapp.models.Recommendation;
import com.choicecrafter.studentapp.utils.AiHintService;
import com.choicecrafter.studentapp.utils.BadgePreferences;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecommendationsCardHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "RecommendationsCardHolder";
    private static final String PREFERENCES_NAME = "settings";
    private static final String KEY_APP_LANGUAGE = "app_lang";

    private final TextView recommendationTitle;
    private final LinearLayout recommendationsContainer;
    private final Button chatbotButton;
    private final AiHintService aiHintService;
    private final LayoutInflater layoutInflater;

    private String aiSuggestionsActivityKey;
    private List<String> aiSuggestionsCache;
    private String aiSuggestionsStatusMessage;
    private boolean aiSuggestionsLoading;
    private boolean aiSuggestionsAllowRetry;
    private boolean aiSuggestionsHadError;
    private int aiSuggestionsRequestToken;

    private final List<ChatMessage> chatbotConversation;
    private final StringBuilder chatbotConversationHistory;
    private boolean chatbotRequestInProgress;
    private int chatbotRequestToken;
    private String chatbotActivityKey;

    public RecommendationsCardHolder(@NonNull View itemView) {
        super(itemView);
        recommendationTitle = itemView.findViewById(R.id.recommendation_title);
        recommendationsContainer = itemView.findViewById(R.id.recommendations_container);
        chatbotButton = itemView.findViewById(R.id.chatbot_button);
        aiHintService = AiHintService.getInstance(itemView.getContext());
        layoutInflater = LayoutInflater.from(itemView.getContext());
        chatbotConversation = new ArrayList<>();
        chatbotConversationHistory = new StringBuilder();
    }

    public void bind(List<Recommendation> recommendations, Activity activityDetails) {
        recommendationsContainer.removeAllViews();

        if (activityDetails == null) {
            chatbotActivityKey = null;
            resetChatbotConversation();
        } else {
            String activityKey = buildActivityIdentifier(activityDetails);
            if (!TextUtils.equals(activityKey, chatbotActivityKey)) {
                chatbotActivityKey = activityKey;
                resetChatbotConversation();
            }
        }

        if (recommendations != null) {
            int itemSpacing = itemView.getResources().getDimensionPixelSize(R.dimen.recommendation_item_spacing);
            for (int index = 0; index < recommendations.size(); index++) {
                Recommendation recommendation = recommendations.get(index);
                Log.i(TAG, "Binding recommendation: " + recommendation.getUrl());
                View previewView = layoutInflater.inflate(R.layout.item_recommendation_preview, recommendationsContainer, false);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.bottomMargin = index == recommendations.size() - 1 ? 0 : itemSpacing;
                previewView.setLayoutParams(layoutParams);
                bindPreview(previewView, recommendation);
                previewView.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("url", recommendation.getUrl());
                    Navigation.findNavController(v).navigate(R.id.recommendationWebViewFragment, args);
                });
                recommendationsContainer.addView(previewView);
            }
        }

        bindAiSuggestionsSection(activityDetails, recommendations);

        if (chatbotButton != null) {
            if (activityDetails == null) {
                chatbotButton.setVisibility(View.GONE);
            } else {
                chatbotButton.setVisibility(View.VISIBLE);
                chatbotButton.setOnClickListener(v -> showChatbotDialog(activityDetails));
            }
        }
    }

    private void bindPreview(View previewView, Recommendation recommendation) {
        ImageView previewImage = previewView.findViewById(R.id.recommendation_preview_image);
        ImageView typeIcon = previewView.findViewById(R.id.recommendation_type_icon);
        TextView titleView = previewView.findViewById(R.id.recommendation_preview_title);
        TextView subtitleView = previewView.findViewById(R.id.recommendation_preview_subtitle);

        int typeIconRes = getTypeIconRes(recommendation);
        if (typeIconRes != 0) {
            typeIcon.setImageDrawable(ContextCompat.getDrawable(itemView.getContext(), typeIconRes));
        }

        titleView.setText(getPreviewTitle(recommendation));
        String subtitle = getPreviewSubtitle(recommendation);
        subtitleView.setText(subtitle);
        subtitleView.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);

        loadPreviewImage(previewImage, recommendation, typeIconRes);
    }

    private void loadPreviewImage(ImageView previewImage, Recommendation recommendation, int placeholderRes) {
        int fallbackRes = placeholderRes != 0 ? placeholderRes : R.drawable.ic_preview_web;
        String previewUrl = getPreviewImageUrl(recommendation);
        if (!TextUtils.isEmpty(previewUrl)) {
            Glide.with(previewImage.getContext())
                    .load(previewUrl)
                    .placeholder(ContextCompat.getDrawable(itemView.getContext(), fallbackRes))
                    .error(ContextCompat.getDrawable(itemView.getContext(), fallbackRes))
                    .centerCrop()
                    .into(previewImage);
        } else {
            previewImage.setImageDrawable(ContextCompat.getDrawable(itemView.getContext(), fallbackRes));
        }
    }

    private String getPreviewImageUrl(Recommendation recommendation) {
        if (isYoutube(recommendation)) {
            String videoId = extractYouTubeVideoId(recommendation.getUrl());
            if (!TextUtils.isEmpty(videoId)) {
                return String.format(Locale.US, "https://img.youtube.com/vi/%s/mqdefault.jpg", videoId);
            }
        }
        return null;
    }

    private String getPreviewTitle(Recommendation recommendation) {
        if (isYoutube(recommendation)) {
            return itemView.getContext().getString(R.string.recommendations_preview_video);
        } else if (isPdf(recommendation)) {
            return itemView.getContext().getString(R.string.recommendations_preview_pdf);
        } else {
            return itemView.getContext().getString(R.string.recommendations_preview_web);
        }
    }

    private String getPreviewSubtitle(Recommendation recommendation) {
        String url = recommendation.getUrl();
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        try {
            Uri uri = Uri.parse(url);
            if (!TextUtils.isEmpty(uri.getHost())) {
                return uri.getHost();
            }
            String lastSegment = uri.getLastPathSegment();
            return !TextUtils.isEmpty(lastSegment) ? lastSegment : url;
        } catch (Exception e) {
            return url;
        }
    }

    private String getRewardText(Recommendation recommendation) {
        if (isYoutube(recommendation)) {
            return itemView.getContext().getString(R.string.recommendations_reward_video);
        } else if (isPdf(recommendation)) {
            return itemView.getContext().getString(R.string.recommendations_reward_pdf);
        } else {
            return itemView.getContext().getString(R.string.recommendations_reward_web);
        }
    }

    private int getTypeIconRes(Recommendation recommendation) {
        if (isYoutube(recommendation)) {
            return R.drawable.ic_baseline_video_library_24;
        } else if (isPdf(recommendation)) {
            return R.drawable.ic_preview_pdf;
        } else {
            return R.drawable.ic_preview_web;
        }
    }

    private boolean isYoutube(Recommendation recommendation) {
        String type = recommendation.getType();
        String url = recommendation.getUrl();
        return (type != null && type.toLowerCase(Locale.US).contains("youtube"))
                || (url != null && (url.contains("youtube.com") || url.contains("youtu.be")));
    }

    private boolean isPdf(Recommendation recommendation) {
        String type = recommendation.getType();
        String url = recommendation.getUrl();
        return (type != null && type.toLowerCase(Locale.US).contains("pdf"))
                || (url != null && url.toLowerCase(Locale.US).contains(".pdf"));
    }

    private String extractYouTubeVideoId(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Uri uri = Uri.parse(url);
        if (uri == null) {
            return null;
        }
        if ("youtu.be".equalsIgnoreCase(uri.getHost())) {
            return uri.getLastPathSegment();
        }
        String videoId = uri.getQueryParameter("v");
        if (!TextUtils.isEmpty(videoId)) {
            return videoId;
        }
        List<String> segments = uri.getPathSegments();
        if (segments != null) {
            for (String segment : segments) {
                if (!TextUtils.isEmpty(segment) && segment.length() >= 10) {
                    return segment;
                }
            }
        }
        return null;
    }

    private void bindAiSuggestionsSection(Activity activityDetails, List<Recommendation> existingRecommendations) {
        if (activityDetails == null) {
            return;
        }

        View aiSectionView = layoutInflater.inflate(R.layout.layout_ai_suggestions, recommendationsContainer, false);
        CircularProgressIndicator progressIndicator = aiSectionView.findViewById(R.id.ai_suggestions_progress);
        TextView statusView = aiSectionView.findViewById(R.id.ai_suggestions_status);
        LinearLayout listView = aiSectionView.findViewById(R.id.ai_suggestions_list);

        recommendationsContainer.addView(aiSectionView);

        String activityKey = buildActivityIdentifier(activityDetails);
        boolean isSameActivity = TextUtils.equals(activityKey, aiSuggestionsActivityKey);

        if (!isSameActivity) {
            aiSuggestionsActivityKey = activityKey;
            aiSuggestionsCache = null;
            aiSuggestionsStatusMessage = null;
            aiSuggestionsAllowRetry = false;
            aiSuggestionsHadError = false;
            aiSuggestionsLoading = false;
        }

        if (aiSuggestionsCache != null && !aiSuggestionsCache.isEmpty()) {
            showAiSuggestionsList(listView, progressIndicator, statusView, aiSuggestionsCache);
        } else if (aiSuggestionsLoading) {
            showAiSuggestionsLoading(progressIndicator, statusView, listView);
        } else if (!TextUtils.isEmpty(aiSuggestionsStatusMessage)) {
            showAiSuggestionsStatus(statusView, progressIndicator, listView, aiSuggestionsStatusMessage,
                    aiSuggestionsHadError, aiSuggestionsAllowRetry, activityDetails, existingRecommendations);
        } else {
            startAiSuggestionsRequest(activityDetails, existingRecommendations, progressIndicator, statusView, listView);
        }
    }

    private void startAiSuggestionsRequest(Activity activityDetails,
                                           List<Recommendation> existingRecommendations,
                                           CircularProgressIndicator progressIndicator,
                                           TextView statusView,
                                           LinearLayout suggestionsList) {
        aiSuggestionsLoading = true;
        aiSuggestionsCache = null;
        aiSuggestionsStatusMessage = null;
        aiSuggestionsAllowRetry = false;
        aiSuggestionsHadError = false;

        showAiSuggestionsLoading(progressIndicator, statusView, suggestionsList);

        final int requestToken = ++aiSuggestionsRequestToken;
        String prompt = buildAiRecommendationsPrompt(activityDetails, existingRecommendations);

        aiHintService.requestHint(prompt, new AiHintService.HintCallback() {
            @Override
            public void onSuccess(String hint) {
                if (requestToken != aiSuggestionsRequestToken) {
                    return;
                }

                aiSuggestionsLoading = false;
                aiSuggestionsCache = extractAiSuggestions(hint);
                aiSuggestionsStatusMessage = null;
                aiSuggestionsAllowRetry = false;
                aiSuggestionsHadError = false;

                if (aiSuggestionsCache == null) {
                    aiSuggestionsCache = new ArrayList<>();
                }

                if (aiSuggestionsCache.size() > 3) {
                    aiSuggestionsCache = new ArrayList<>(aiSuggestionsCache.subList(0, 3));
                }

                if (aiSuggestionsCache.isEmpty()) {
                    String message = itemView.getContext().getString(R.string.recommendations_ai_empty);
                    aiSuggestionsCache = null;
                    aiSuggestionsStatusMessage = message;
                    showAiSuggestionsStatus(statusView, progressIndicator, suggestionsList, message,
                            false, false, activityDetails, existingRecommendations);
                } else {
                    showAiSuggestionsList(suggestionsList, progressIndicator, statusView, aiSuggestionsCache);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (requestToken != aiSuggestionsRequestToken) {
                    return;
                }

                aiSuggestionsLoading = false;
                aiSuggestionsCache = null;
                aiSuggestionsHadError = true;
                aiSuggestionsAllowRetry = true;

                String message = !TextUtils.isEmpty(errorMessage)
                        ? errorMessage
                        : itemView.getContext().getString(R.string.recommendations_ai_error);
                String formattedMessage = itemView.getContext().getString(
                        R.string.recommendations_ai_error_with_retry, message);
                aiSuggestionsStatusMessage = formattedMessage;

                showAiSuggestionsStatus(statusView, progressIndicator, suggestionsList, formattedMessage,
                        true, true, activityDetails, existingRecommendations);
            }
        });
    }

    private void showAiSuggestionsLoading(CircularProgressIndicator progressIndicator,
                                          TextView statusView,
                                          LinearLayout listView) {
        listView.setVisibility(View.GONE);
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
            progressIndicator.show();
        }
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(itemView.getContext().getString(R.string.recommendations_ai_loading));
        statusView.setTextColor(ContextCompat.getColor(statusView.getContext(), R.color.scorpion));
        statusView.setOnClickListener(null);
    }

    private void showAiSuggestionsStatus(TextView statusView,
                                         CircularProgressIndicator progressIndicator,
                                         LinearLayout listView,
                                         String message,
                                         boolean isError,
                                         boolean allowRetry,
                                         Activity activityDetails,
                                         List<Recommendation> existingRecommendations) {
        if (progressIndicator != null) {
            progressIndicator.hide();
            progressIndicator.setVisibility(View.GONE);
        }
        listView.setVisibility(View.GONE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(message);
        int colorRes = isError ? R.color.gamified_red : R.color.scorpion;
        statusView.setTextColor(ContextCompat.getColor(statusView.getContext(), colorRes));

        if (allowRetry) {
            statusView.setOnClickListener(v -> startAiSuggestionsRequest(activityDetails, existingRecommendations,
                    progressIndicator, statusView, listView));
        } else {
            statusView.setOnClickListener(null);
        }
    }

    private void showAiSuggestionsList(LinearLayout listView,
                                       CircularProgressIndicator progressIndicator,
                                       TextView statusView,
                                       List<String> suggestions) {
        if (progressIndicator != null) {
            progressIndicator.hide();
            progressIndicator.setVisibility(View.GONE);
        }
        statusView.setVisibility(View.GONE);
        statusView.setOnClickListener(null);
        listView.setVisibility(View.VISIBLE);
        renderAiSuggestions(listView, suggestions);
    }

    private void renderAiSuggestions(LinearLayout listView, List<String> suggestions) {
        listView.removeAllViews();
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }

        int spacing = itemView.getResources().getDimensionPixelSize(R.dimen.recommendation_item_spacing);
        for (int index = 0; index < suggestions.size(); index++) {
            String suggestion = suggestions.get(index);
            TextView suggestionView = new TextView(listView.getContext());
            TextViewCompat.setTextAppearance(suggestionView, R.style.TextAppearance_MaterialComponents_Body);
            suggestionView.setText(suggestion);
            suggestionView.setBackgroundResource(R.drawable.bg_ai_suggestion);
            suggestionView.setTextColor(ContextCompat.getColor(listView.getContext(), R.color.ai_suggestion_text));
            suggestionView.setLineSpacing(dpToPx(2), 1.0f);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (index > 0) {
                layoutParams.topMargin = spacing;
            }
            suggestionView.setLayoutParams(layoutParams);

            listView.addView(suggestionView);
        }
    }

    private List<String> extractAiSuggestions(String response) {
        List<String> suggestions = new ArrayList<>();
        if (TextUtils.isEmpty(response)) {
            return suggestions;
        }

        String[] lines = response.split("\n");
        for (String rawLine : lines) {
            if (TextUtils.isEmpty(rawLine)) {
                continue;
            }
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                line = line.substring(1).trim();
            } else {
                line = line.replaceFirst("^[0-9]+[\\).:-]?\\s*", "");
            }

            if (!TextUtils.isEmpty(line)) {
                suggestions.add(line.trim());
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add(response.trim());
        }

        return suggestions;
    }

    private String buildActivityIdentifier(Activity activityDetails) {
        if (activityDetails == null) {
            return "";
        }
        if (!TextUtils.isEmpty(activityDetails.getId())) {
            return activityDetails.getId();
        }
        String title = activityDetails.getTitle();
        String description = activityDetails.getDescription();
        return (title != null ? title : "") + '|' + (description != null ? description : "");
    }

    private String buildAiRecommendationsPrompt(Activity activityDetails, List<Recommendation> existingRecommendations) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a supportive study assistant generating fresh learning recommendations.");

        String preferredLanguage = getPreferredLanguageName();
        if (!TextUtils.isEmpty(preferredLanguage)) {
            builder.append(' ')
                    .append("Respond in ")
                    .append(preferredLanguage)
                    .append('.');
        }

        if (activityDetails != null) {
            if (!TextUtils.isEmpty(activityDetails.getTitle())) {
                builder.append(" Activity topic: \"")
                        .append(activityDetails.getTitle())
                        .append("\".");
            }
            if (!TextUtils.isEmpty(activityDetails.getDescription())) {
                builder.append(" Description: ")
                        .append(activityDetails.getDescription())
                        .append('.');
            }
        }

        if (existingRecommendations != null && !existingRecommendations.isEmpty()) {
            builder.append(" Already shared resources: ");
            int limit = Math.min(existingRecommendations.size(), 3);
            for (int index = 0; index < limit; index++) {
                Recommendation recommendation = existingRecommendations.get(index);
                if (recommendation == null) {
                    continue;
                }
                String descriptor = describeRecommendation(recommendation);
                if (!TextUtils.isEmpty(descriptor)) {
                    builder.append(descriptor);
                } else if (!TextUtils.isEmpty(recommendation.getUrl())) {
                    builder.append(recommendation.getUrl());
                }
                if (index < limit - 1) {
                    builder.append("; ");
                }
            }
            builder.append(".");
        }

        builder.append(" Suggest 3 additional study ideas that build on the activity and avoid repeating the existing resources.");
        builder.append(" Each suggestion should be practical, reference free or easily searchable materials, and stay under 25 words.");
        builder.append(" Respond with exactly 3 bullet points starting with '-' and nothing else.");

        return builder.toString();
    }

    private String getPreferredLanguageName() {
        Context context = itemView.getContext();
        String languageCode = getPreferredLanguageCode(context);
        if (TextUtils.isEmpty(languageCode)) {
            return "";
        }

        Locale locale = new Locale(languageCode);
        String displayLanguage = locale.getDisplayLanguage(Locale.ENGLISH);
        if (TextUtils.isEmpty(displayLanguage)) {
            displayLanguage = locale.getDisplayLanguage();
        }

        return displayLanguage != null ? displayLanguage.trim() : "";
    }

    private String getPreferredLanguageCode(Context context) {
        if (context == null) {
            return "";
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String saved = preferences.getString(KEY_APP_LANGUAGE, "");
        if (TextUtils.isEmpty(saved)) {
            return Locale.getDefault().getLanguage();
        }
        return saved;
    }

    private String describeRecommendation(Recommendation recommendation) {
        if (recommendation == null) {
            return "";
        }

        StringBuilder description = new StringBuilder();
        if (!TextUtils.isEmpty(recommendation.getType())) {
            description.append(recommendation.getType());
        }

        String subtitle = getPreviewSubtitle(recommendation);
        if (!TextUtils.isEmpty(subtitle)) {
            if (description.length() > 0) {
                description.append(" at ");
            }
            description.append(subtitle);
        } else if (!TextUtils.isEmpty(recommendation.getUrl())) {
            if (description.length() > 0) {
                description.append(" at ");
            }
            description.append(recommendation.getUrl());
        }

        return description.toString();
    }

    private void resetChatbotConversation() {
        chatbotConversation.clear();
        chatbotConversationHistory.setLength(0);
        chatbotRequestInProgress = false;
        chatbotRequestToken = 0;
    }

    private void showChatbotDialog(Activity activityDetails) {
        View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_chatbot, null);
        TextInputLayout questionInputLayout = dialogView.findViewById(R.id.chatbot_question_input_layout);
        EditText questionInput = dialogView.findViewById(R.id.chatbot_question_input);
        CircularProgressIndicator progressIndicator = dialogView.findViewById(R.id.chatbot_progress);
        LinearLayout conversationContainer = dialogView.findViewById(R.id.chatbot_conversation_container);
        NestedScrollView conversationScroll = dialogView.findViewById(R.id.chatbot_conversation_scroll);
        TextView emptyStateView = dialogView.findViewById(R.id.chatbot_empty_state);

        final AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                .setTitle(R.string.recommendations_chatbot_button)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        renderStoredChatMessages(conversationContainer, conversationScroll, emptyStateView);

        if (chatbotRequestInProgress) {
            progressIndicator.setVisibility(View.VISIBLE);
            progressIndicator.show();
            questionInputLayout.setEndIconVisible(false);
            questionInput.setEnabled(false);
        } else {
            progressIndicator.hide();
            progressIndicator.setVisibility(View.GONE);
            questionInputLayout.setEndIconVisible(true);
            questionInput.setEnabled(true);
        }

        View.OnClickListener sendQuestionListener = v -> {
            String question = questionInput.getText() != null ? questionInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(question)) {
                questionInput.setError(itemView.getContext().getString(R.string.recommendations_chatbot_question_error));
                return;
            }

            questionInput.setError(null);
            storeChatMessage(question, true);
            displayChatMessage(conversationContainer, conversationScroll, emptyStateView, question, true, true);

            progressIndicator.setVisibility(View.VISIBLE);
            progressIndicator.show();
            questionInputLayout.setEndIconVisible(false);
            questionInput.setEnabled(false);
            questionInput.setText("");

            chatbotRequestInProgress = true;
            final int requestToken = ++chatbotRequestToken;

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            String userEmail = firebaseUser != null ? firebaseUser.getEmail() : null;
            BadgePreferences.markChatbotDiscussion(itemView.getContext(), userEmail);

            final String questionForRequest = question;
            aiHintService.requestHint(buildPrompt(activityDetails,
                    chatbotConversationHistory.toString(), questionForRequest), new AiHintService.HintCallback() {
                @Override
                public void onSuccess(String hint) {
                    if (requestToken != chatbotRequestToken) {
                        return;
                    }

                    chatbotRequestInProgress = false;
                    String response = !TextUtils.isEmpty(hint)
                            ? hint
                            : itemView.getContext().getString(R.string.recommendations_chatbot_error);
                    storeChatMessage(response, false);
                    if (chatbotConversationHistory.length() > 0) {
                        chatbotConversationHistory.append("\n\n");
                    }
                    chatbotConversationHistory
                            .append("Learner: ")
                            .append(questionForRequest)
                            .append("\nAssistant: ")
                            .append(response);

                    if (dialog.isShowing()) {
                        progressIndicator.hide();
                        progressIndicator.setVisibility(View.GONE);
                        displayChatMessage(conversationContainer, conversationScroll, emptyStateView, response, false, true);
                        questionInputLayout.setEndIconVisible(true);
                        questionInput.setEnabled(true);
                        questionInput.requestFocus();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (requestToken != chatbotRequestToken) {
                        return;
                    }

                    chatbotRequestInProgress = false;
                    String fallback = !TextUtils.isEmpty(errorMessage)
                            ? errorMessage
                            : itemView.getContext().getString(R.string.recommendations_chatbot_error);
                    storeChatMessage(fallback, false);

                    if (dialog.isShowing()) {
                        progressIndicator.hide();
                        progressIndicator.setVisibility(View.GONE);
                        displayChatMessage(conversationContainer, conversationScroll, emptyStateView, fallback, false, true);
                        questionInputLayout.setEndIconVisible(true);
                        questionInput.setEnabled(true);
                        questionInput.requestFocus();
                    }
                }
            });
        };

        questionInputLayout.setEndIconOnClickListener(sendQuestionListener);

        dialog.setOnDismissListener(d -> {
            if (progressIndicator != null) {
                progressIndicator.hide();
                progressIndicator.setVisibility(View.GONE);
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void renderStoredChatMessages(LinearLayout container,
                                          NestedScrollView scrollView,
                                          TextView emptyStateView) {
        container.removeAllViews();
        if (chatbotConversation.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            return;
        }

        emptyStateView.setVisibility(View.GONE);
        for (ChatMessage message : chatbotConversation) {
            displayChatMessage(container, scrollView, emptyStateView, message.text, message.isUserMessage, false);
        }
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void storeChatMessage(String text, boolean isUserMessage) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        chatbotConversation.add(new ChatMessage(text, isUserMessage));
    }

    private void displayChatMessage(LinearLayout container,
                                    NestedScrollView scrollView,
                                    TextView emptyStateView,
                                    String text,
                                    boolean isUserMessage,
                                    boolean scrollToBottom) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        if (emptyStateView.getVisibility() == View.VISIBLE) {
            emptyStateView.setVisibility(View.GONE);
        }

        TextView messageView = new TextView(container.getContext());
        TextViewCompat.setTextAppearance(messageView, R.style.TextAppearance_MaterialComponents_Body);
        messageView.setText(text);
        messageView.setBackgroundResource(isUserMessage
                ? R.drawable.bg_chatbot_message_user
                : R.drawable.bg_chatbot_message_assistant);
        messageView.setTextColor(ContextCompat.getColor(container.getContext(),
                isUserMessage ? R.color.white : R.color.scorpion));
        messageView.setMaxWidth(dpToPx(260));
        messageView.setLineSpacing(dpToPx(2), 1.0f);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = isUserMessage ? Gravity.END : Gravity.START;
        layoutParams.topMargin = dpToPx(8);
        layoutParams.bottomMargin = dpToPx(4);
        layoutParams.leftMargin = isUserMessage ? dpToPx(48) : dpToPx(12);
        layoutParams.rightMargin = isUserMessage ? dpToPx(12) : dpToPx(48);
        messageView.setLayoutParams(layoutParams);

        container.addView(messageView);

        if (scrollToBottom) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private static final class ChatMessage {
        private final String text;
        private final boolean isUserMessage;

        private ChatMessage(String text, boolean isUserMessage) {
            this.text = text;
            this.isUserMessage = isUserMessage;
        }
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                itemView.getResources().getDisplayMetrics());
    }

    private String buildPrompt(Activity activityDetails, String conversationHistory, String question) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an encouraging study assistant helping a learner.");
        if (activityDetails != null) {
            if (!TextUtils.isEmpty(activityDetails.getTitle())) {
                promptBuilder.append(" The activity topic is \"")
                        .append(activityDetails.getTitle())
                        .append("\".");
            }
            if (!TextUtils.isEmpty(activityDetails.getDescription())) {
                promptBuilder.append(" Activity description: ")
                        .append(activityDetails.getDescription()).append('.');
            }
        }
        promptBuilder.append(" Provide actionable guidance, answer questions, or suggest new learning resources based on the request.");
        if (!TextUtils.isEmpty(conversationHistory)) {
            promptBuilder.append("\n\nConversation so far:\n")
                    .append(conversationHistory);
        }
        promptBuilder.append("\n\nLearner question: ").append(question);
        return promptBuilder.toString();
    }
}
