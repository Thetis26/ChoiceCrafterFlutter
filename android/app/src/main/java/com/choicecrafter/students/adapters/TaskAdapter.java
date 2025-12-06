package com.choicecrafter.studentapp.adapters;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.adapters.tasks.CodingChallengeViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.FillInTheBlankViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.InfoCardViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.MatchingPairViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.MultipleChoiceViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.OrderingTaskViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.RecommendationsCardHolder;
import com.choicecrafter.studentapp.adapters.tasks.SpotTheErrorViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.StatisticsViewHolder;
import com.choicecrafter.studentapp.adapters.tasks.TrueFalseViewHolder;
import com.choicecrafter.studentapp.models.Activity;
import com.choicecrafter.studentapp.models.Comment;
import com.choicecrafter.studentapp.models.tasks.CodingChallengeTask;
import com.choicecrafter.studentapp.models.tasks.FillInTheBlank;
import com.choicecrafter.studentapp.models.tasks.InfoCardTask;
import com.choicecrafter.studentapp.models.tasks.MatchingPairTask;
import com.choicecrafter.studentapp.models.ModuleProgress;
import com.choicecrafter.studentapp.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.studentapp.models.tasks.OrderingTask;
import com.choicecrafter.studentapp.models.Recommendation;
import com.choicecrafter.studentapp.models.tasks.SpotTheErrorTask;
import com.choicecrafter.studentapp.models.tasks.Task;
import com.choicecrafter.studentapp.models.TaskStats;
import com.choicecrafter.studentapp.models.tasks.TrueFalseTask;
import com.choicecrafter.studentapp.models.NudgePreferences;
import com.choicecrafter.studentapp.models.EnrollmentActivityProgress;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.repositories.ActivityRepository;
import com.choicecrafter.studentapp.repositories.CourseRepository;
import com.choicecrafter.studentapp.ui.activity.TaskSessionState;
import com.choicecrafter.studentapp.utils.ActivityScoreCalculator;
import com.choicecrafter.studentapp.utils.BadgePreferences;
import com.choicecrafter.studentapp.utils.TaskStatsKeyUtils;
import com.choicecrafter.studentapp.utils.TaskCelebrationManager;
import com.choicecrafter.studentapp.utils.LikePreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ActivityActionListener {
        void onReturnToActivities();
        void onRetryActivity();
        void onActivityCompleted();
    }

    private static final int VIEW_TYPE_MULTIPLE_CHOICE = 1;
    private static final int VIEW_TYPE_FILL_IN_THE_BLANK = 2;
    private static final int VIEW_TYPE_ORDERING = 3;
    private static final int VIEW_TYPE_MATCHING_PAIR = 4;
    private static final int VIEW_TYPE_INFO_CARD = 5;
    private static final int VIEW_TYPE_TRUE_FALSE = 6;
    private static final int VIEW_TYPE_SPOT_ERROR = 7;
    private static final int VIEW_TYPE_RECOMMENDATIONS = 8;
    private static final int VIEW_TYPE_STATISTICS = 9;
    private static final int VIEW_TYPE_DISCUSSION = 10;
    private static final int VIEW_TYPE_CODING_CHALLENGE = 11;

    private final List<Task> tasks;
    private final List<Recommendation> recommendations;
    private final EnrollmentActivityProgress activityProgress;
    private final ActivityRepository activityRepository = new ActivityRepository();
    private final CourseRepository courseRepository = new CourseRepository();
    private final Activity activityDetails;
    private final String courseId;
    private final CommentsAdapter commentsAdapter;
    private final ActivityActionListener actionListener;
    private final String currentUserIdentifier;
    private int currentTaskIndex = 0;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private final List<AtomicInteger> tasksAttempts = new ArrayList<>();
    private long taskStartTime;
    private boolean completionHandled = false;
    private NudgePreferences nudgePreferences;
    private final TaskSessionState taskSessionState;

    public TaskAdapter(List<Task> tasks,
                       List<Recommendation> recommendations,
                       EnrollmentActivityProgress activityProgress,
                       ProgressBar progressBar,
                       RecyclerView recyclerView,
                       Activity activityDetails,
                       String courseId,
                       ActivityActionListener actionListener,
                       User currentUser,
                       TaskSessionState taskSessionState) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        this.activityProgress = activityProgress;
        this.progressBar = progressBar;
        this.recyclerView = recyclerView;
        this.activityDetails = activityDetails;
        this.courseId = courseId;
        this.actionListener = actionListener;
        this.taskSessionState = taskSessionState != null ? taskSessionState : new TaskSessionState();
        this.currentUserIdentifier = resolveCurrentUserIdentifier(activityProgress, currentUser);
        ensureTaskStats();
        for (int i = 0; i < this.tasks.size(); i++) {
            tasksAttempts.add(new AtomicInteger(0));
        }
        this.taskStartTime = System.currentTimeMillis();
        List<Comment> commentList = activityDetails != null && activityDetails.getComments() != null
                ? activityDetails.getComments()
                : new ArrayList<>();
        if (activityDetails != null && activityDetails.getComments() == null) {
            activityDetails.setComments(commentList);
        }
        if (commentList != null) {
            commentList.sort((first, second) -> Long.compare(parseTimestamp(second), parseTimestamp(first)));
        }
        String progressUserId = activityProgress != null ? activityProgress.getUserId() : null;
        if (TextUtils.isEmpty(progressUserId) && currentUser != null) {
            progressUserId = currentUser.getEmail();
        }
        this.commentsAdapter = new CommentsAdapter(commentList,
                progressUserId,
                currentUser != null ? currentUser.getAnonymousAvatar() : null);
        this.currentTaskIndex = Math.min(calculateCompletedTaskCount(), this.tasks.size());
        if (this.progressBar != null) {
            this.progressBar.setProgress(this.currentTaskIndex);
        }
        this.completionHandled = isActivityCompleted();
    }

    private String resolveCurrentUserIdentifier(EnrollmentActivityProgress progress, User currentUser) {
        if (progress != null && !TextUtils.isEmpty(progress.getUserId())) {
            return progress.getUserId();
        }
        if (currentUser != null && !TextUtils.isEmpty(currentUser.getEmail())) {
            return currentUser.getEmail();
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            if (!TextUtils.isEmpty(firebaseUser.getEmail())) {
                return firebaseUser.getEmail();
            }
            if (!TextUtils.isEmpty(firebaseUser.getUid())) {
                return firebaseUser.getUid();
            }
        }
        return null;
    }

    public void updateNudgePreferences(NudgePreferences preferences) {
        boolean previousDiscussion = isDiscussionEnabled();
        boolean previousPrompt = isStatisticsPromptEnabled();
        this.nudgePreferences = preferences;
        boolean newDiscussion = isDiscussionEnabled();
        boolean newPrompt = isStatisticsPromptEnabled();

        if (previousDiscussion != newDiscussion) {
            notifyDataSetChanged();
            return;
        }

        if (previousPrompt != newPrompt) {
            notifyStatisticsCardChanged();
        }
    }

    private boolean isStatisticsPromptEnabled() {
        return nudgePreferences == null || nudgePreferences.isCompletedActivityPromptEnabled();
    }

    private boolean isDiscussionEnabled() {
        return nudgePreferences == null || nudgePreferences.isDiscussionForumEnabled();
    }

    private int getRecommendationsPosition() {
        return tasks.size();
    }

    private int getStatisticsPosition() {
        return getRecommendationsPosition() + 1;
    }

    private int getDiscussionPosition() {
        if (!isDiscussionEnabled()) {
            return -1;
        }
        return getStatisticsPosition() + 1;
    }

    private int getExtraItemCount() {
        int count = 2; // Recommendations + statistics cards
        if (isDiscussionEnabled()) {
            count++;
        }
        return count;
    }

    private void notifyStatisticsCardChanged() {
        int position = getStatisticsPosition();
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    private void notifyDiscussionCardChanged() {
        int position = getDiscussionPosition();
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    private void ensureTaskStats() {
        if (activityProgress != null && activityProgress.getTaskStats() == null) {
            activityProgress.setTaskStats(new HashMap<>());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < tasks.size()) {
            Task task = tasks.get(position);
            if (task instanceof MultipleChoiceQuestion) {
                return VIEW_TYPE_MULTIPLE_CHOICE;
            } else if (task instanceof FillInTheBlank) {
                return VIEW_TYPE_FILL_IN_THE_BLANK;
            } else if (task instanceof MatchingPairTask) {
                return VIEW_TYPE_MATCHING_PAIR;
            } else if (task instanceof OrderingTask) {
                return VIEW_TYPE_ORDERING;
            } else if (task instanceof SpotTheErrorTask) {
                return VIEW_TYPE_SPOT_ERROR;
            } else if (task instanceof InfoCardTask) {
                return VIEW_TYPE_INFO_CARD;
            } else if (task instanceof TrueFalseTask) {
                return VIEW_TYPE_TRUE_FALSE;
            } else if (task instanceof CodingChallengeTask) {
                return VIEW_TYPE_CODING_CHALLENGE;
            }
            return -1;
        }

        if (position == getRecommendationsPosition()) {
            return VIEW_TYPE_RECOMMENDATIONS;
        }
        if (position == getStatisticsPosition()) {
            return VIEW_TYPE_STATISTICS;
        }
        if (isDiscussionEnabled() && position == getDiscussionPosition()) {
            return VIEW_TYPE_DISCUSSION;
        }

        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_MULTIPLE_CHOICE) {
            View view = inflater.inflate(R.layout.multiple_choice_question_card, parent, false);
            return new MultipleChoiceViewHolder(view);
        } else if (viewType == VIEW_TYPE_FILL_IN_THE_BLANK) {
            View view = inflater.inflate(R.layout.fill_in_the_blank_card, parent, false);
            return new FillInTheBlankViewHolder(view);
        } else if (viewType == VIEW_TYPE_MATCHING_PAIR) {
            View view = inflater.inflate(R.layout.matching_pair_task_card, parent, false);
            return new MatchingPairViewHolder(view);
        } else if (viewType == VIEW_TYPE_ORDERING) {
            View view = inflater.inflate(R.layout.ordering_task_card, parent, false);
            return new OrderingTaskViewHolder(view);
        } else if (viewType == VIEW_TYPE_INFO_CARD) {
            View view = inflater.inflate(R.layout.info_card_task, parent, false);
            return new InfoCardViewHolder(view);
        } else if (viewType == VIEW_TYPE_TRUE_FALSE) {
            View view = inflater.inflate(R.layout.true_false_task_card, parent, false);
            return new TrueFalseViewHolder(view);
        } else if (viewType == VIEW_TYPE_SPOT_ERROR) {
            View view = inflater.inflate(R.layout.spot_the_error_task_card, parent, false);
            return new SpotTheErrorViewHolder(view);
        } else if (viewType == VIEW_TYPE_CODING_CHALLENGE) {
            View view = inflater.inflate(R.layout.coding_challenge_task_card, parent, false);
            return new CodingChallengeViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECOMMENDATIONS) {
            View view = inflater.inflate(R.layout.recommendations_card, parent, false);
            return new RecommendationsCardHolder(view);
        } else if (viewType == VIEW_TYPE_STATISTICS) {
            View view = inflater.inflate(R.layout.statistics_card_updated, parent, false);
            return new StatisticsViewHolder(view);
        } else if (viewType == VIEW_TYPE_DISCUSSION) {
            View view = inflater.inflate(R.layout.activity_discussion_card, parent, false);
            return new DiscussionViewHolder(view);
        }
        throw new IllegalArgumentException("Invalid view type");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < tasks.size()) {
            Task task = tasks.get(position);
            if (holder instanceof MultipleChoiceViewHolder multipleChoiceViewHolder) {
                multipleChoiceViewHolder.bind((MultipleChoiceQuestion) task);
                String taskKey = TaskStatsKeyUtils.buildKey(task);
                multipleChoiceViewHolder.setStateListener((selectedIndex, hintShown) ->
                        taskSessionState.updateMultipleChoiceState(taskKey, selectedIndex, hintShown));
                TaskSessionState.MultipleChoiceState state = taskSessionState.getMultipleChoiceState(taskKey);
                if (state != null) {
                    multipleChoiceViewHolder.restoreState(state.getSelectedIndex(), state.isHintShown());
                }
                holder.itemView.findViewById(R.id.hint_button)
                        .setOnClickListener(v -> multipleChoiceViewHolder.showHint());
            } else if (holder instanceof FillInTheBlankViewHolder fillInTheBlankViewHolder) {
                fillInTheBlankViewHolder.bind((FillInTheBlank) task);
                String taskKey = TaskStatsKeyUtils.buildKey(task);
                fillInTheBlankViewHolder.setStateListener((answers, hintShown) ->
                        taskSessionState.updateFillInTheBlankState(taskKey, answers, hintShown));
                TaskSessionState.FillInTheBlankState state = taskSessionState.getFillInTheBlankState(taskKey);
                if (state != null) {
                    fillInTheBlankViewHolder.restoreState(state.getAnswers(), state.isHintShown());
                }
                holder.itemView.findViewById(R.id.hint_button)
                        .setOnClickListener(v -> fillInTheBlankViewHolder.showHint());
            } else if (holder instanceof MatchingPairViewHolder matchingPairViewHolder) {
                matchingPairViewHolder.bind((MatchingPairTask) task);
                holder.itemView.findViewById(R.id.hint_button).setOnClickListener(v -> matchingPairViewHolder.showHint());
            } else if (holder instanceof OrderingTaskViewHolder orderingTaskViewHolder) {
                orderingTaskViewHolder.bind((OrderingTask) task);
                holder.itemView.findViewById(R.id.hint_button).setOnClickListener(v -> orderingTaskViewHolder.showHint());
            } else if (holder instanceof InfoCardViewHolder infoCardViewHolder) {
                infoCardViewHolder.bind((InfoCardTask) task);
            } else if (holder instanceof TrueFalseViewHolder trueFalseViewHolder) {
                trueFalseViewHolder.bind((TrueFalseTask) task);
                View hintButton = holder.itemView.findViewById(R.id.hint_button);
                if (hintButton != null) {
                    hintButton.setOnClickListener(v -> trueFalseViewHolder.showHint());
                }
            } else if (holder instanceof SpotTheErrorViewHolder spotTheErrorViewHolder) {
                spotTheErrorViewHolder.bind((SpotTheErrorTask) task);
                View hintButton = holder.itemView.findViewById(R.id.hint_button);
                if (hintButton != null) {
                    hintButton.setOnClickListener(v -> spotTheErrorViewHolder.showHint());
                }
            } else if (holder instanceof CodingChallengeViewHolder codingChallengeViewHolder) {
                codingChallengeViewHolder.bind((CodingChallengeTask) task);
                String taskKey = TaskStatsKeyUtils.buildKey(task);
                codingChallengeViewHolder.setStateListener((language, code, customInput, hintShown, lastOutput, lastStdErr, lastRunSuccessful) ->
                        taskSessionState.updateCodingChallengeState(taskKey, language, code, customInput, hintShown, lastOutput, lastStdErr, lastRunSuccessful));
                TaskSessionState.CodingChallengeState state = taskSessionState.getCodingChallengeState(taskKey);
                if (state != null) {
                    codingChallengeViewHolder.restoreState(state);
                }
                View hintButton = holder.itemView.findViewById(R.id.hint_button);
                if (hintButton != null) {
                    hintButton.setOnClickListener(v -> codingChallengeViewHolder.showHint());
                }
            }

            View checkAnswerButton = holder.itemView.findViewById(R.id.check_answer_button);
            if (checkAnswerButton != null) {
                checkAnswerButton.setOnClickListener(v -> {
                    if (currentTaskIndex >= tasks.size()) {
                        return;
                    }
                    if (tasksAttempts.isEmpty()) {
                        return;
                    }
                    int attemptIndex = Math.min(currentTaskIndex, tasksAttempts.size() - 1);
                    if (attemptIndex < 0) {
                        attemptIndex = 0;
                    }
                    tasksAttempts.get(attemptIndex).incrementAndGet();
                    if (holder instanceof FillInTheBlankViewHolder fillHolder) {
                        fillHolder.validateAnswers(isCorrect -> handleTaskResult(task, holder, isCorrect, isCorrect ? 1.0 : 0.0));
                    } else if (holder instanceof CodingChallengeViewHolder codingHolder) {
                        codingHolder.validateSolution((isCorrect, completionRatio) ->
                                handleTaskResult(task, holder, isCorrect, completionRatio));
                    } else {
                        boolean shouldAdvance = false;
                        double completionRatio = 0.0;
                        if (holder instanceof MultipleChoiceViewHolder) {
                            MultipleChoiceViewHolder mcHolder = (MultipleChoiceViewHolder) holder;
                            shouldAdvance = mcHolder.validateAnswer();
                            completionRatio = mcHolder.getLastCompletionRatio();
                        } else if (holder instanceof MatchingPairViewHolder) {
                            shouldAdvance = ((MatchingPairViewHolder) holder).validateAnswer();
                            completionRatio = shouldAdvance ? 1.0 : 0.0;
                        } else if (holder instanceof OrderingTaskViewHolder) {
                            shouldAdvance = ((OrderingTaskViewHolder) holder).validateOrder();
                            completionRatio = shouldAdvance ? 1.0 : 0.0;
                        } else if (holder instanceof InfoCardViewHolder) {
                            shouldAdvance = ((InfoCardViewHolder) holder).acknowledgeTask();
                            completionRatio = shouldAdvance ? 1.0 : 0.0;
                        } else if (holder instanceof TrueFalseViewHolder) {
                            shouldAdvance = ((TrueFalseViewHolder) holder).validateAnswer();
                            completionRatio = shouldAdvance ? 1.0 : 0.0;
                        } else if (holder instanceof SpotTheErrorViewHolder) {
                            shouldAdvance = ((SpotTheErrorViewHolder) holder).validateAnswer();
                            completionRatio = shouldAdvance ? 1.0 : 0.0;
                        }
                        handleTaskResult(task, holder, shouldAdvance, completionRatio);
                    }
                });
            }
        } else if (holder instanceof RecommendationsCardHolder) {
            Log.i("TaskAdapter", "Binding recommendations card. Recommendations: " + recommendations);
            ((RecommendationsCardHolder) holder).bind(recommendations, activityDetails);
            View button = holder.itemView.findViewById(R.id.see_statistics_button);
            if (button != null) {
                button.setOnClickListener(v -> {
                    int targetPosition = getStatisticsPosition();
                    if (targetPosition < 0) {
                        targetPosition = getDiscussionPosition();
                    }
                    if (targetPosition >= 0 && recyclerView != null) {
                        final int scrollTarget = targetPosition;
                        recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(scrollTarget), 700);
                    }
                });
            }
        } else if (holder instanceof StatisticsViewHolder statisticsViewHolder) {
            statisticsViewHolder.bind(activityProgress,
                    tasks,
                    isActivityCompleted(),
                    isDiscussionEnabled() ? v -> {
                        int discussionPosition = getDiscussionPosition();
                        if (discussionPosition >= 0 && recyclerView != null) {
                            final int scrollTarget = discussionPosition;
                            recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(scrollTarget), 700);
                        }
                    } : null,
                    actionListener != null ? v -> actionListener.onReturnToActivities() : null,
                    actionListener != null ? v -> actionListener.onRetryActivity() : null,
                    isStatisticsPromptEnabled(),
                    isDiscussionEnabled());
        } else if (holder instanceof DiscussionViewHolder discussionViewHolder) {
            discussionViewHolder.bind();
        }
    }

    private void handleTaskResult(Task task,
                                  RecyclerView.ViewHolder holder,
                                  boolean shouldAdvance,
                                  double completionRatio) {
        if (!shouldAdvance) {
            return;
        }
        if (shouldCelebrate(task, completionRatio)) {
            TaskCelebrationManager.getInstance(holder.itemView.getContext())
                    .celebrate(holder.itemView, task,
                            () -> onTaskValidationFinished(task, holder, true, completionRatio));
        } else {
            onTaskValidationFinished(task, holder, shouldAdvance, completionRatio);
        }
    }

    private boolean shouldCelebrate(Task task, double completionRatio) {
        if (task == null) {
            return false;
        }
        if (task instanceof InfoCardTask) {
            return false;
        }
        return completionRatio >= 0.999d;
    }

    private void onTaskValidationFinished(Task task, RecyclerView.ViewHolder holder, boolean shouldAdvance, double completionRatio) {
        if (!shouldAdvance) {
            return;
        }
        currentTaskIndex++;
        if (progressBar != null) {
            progressBar.setProgress(currentTaskIndex);
        }
        if (recyclerView != null) {
            int targetPosition = currentTaskIndex < tasks.size() ? currentTaskIndex : tasks.size();
            recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(targetPosition), 700);
        }
        if (!tasks.isEmpty()) {
            int target = Math.min(Math.max(currentTaskIndex - 1, 0), tasks.size() - 1);
            notifyItemChanged(target);
        }
        String attemptDateTime = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attemptDateTime = LocalDateTime.now().toString();
        }
        String timeSpent = calculateTimeSpent();
        int retriesIndex = Math.max(0, currentTaskIndex - 1);
        Integer retries = tasksAttempts.get(Math.min(retriesIndex, tasksAttempts.size() - 1)).get() - 1;
        Boolean hintsUsed = hasUsedHint(holder);
        boolean completed = shouldAdvance;
        double completionValue = completed ? 1.0 : 0.0;
        double scoreRatio = Math.max(0.0, Math.min(1.0, completionRatio));
        TaskStats taskStats = new TaskStats(attemptDateTime,
                timeSpent,
                retries,
                completed,
                hintsUsed,
                completionValue,
                scoreRatio);
        String taskKey = TaskStatsKeyUtils.buildKey(task);
        activityRepository.addTaskStats(activityProgress.getUserId(), activityProgress.getCourseId(), activityProgress.getActivityId(), taskKey, taskStats);
        taskSessionState.clearState(taskKey);

        Log.i("TaskAdapter", "Task stats added for task " + task.getTitle() + " -> position: " + currentTaskIndex + ", taskStats: " + taskStats);
        Log.i("TaskStats", "Adding for user " + activityProgress.getUserId() + " activity " + activityProgress.getActivityId() + " task " + task.getTitle() + " stats: " + taskStats);
        TaskStatsKeyUtils.putStats(activityProgress.getTaskStats(), task, taskStats);
        notifyStatisticsCardChanged();
        handleTaskCompletion();
    }

    private boolean hasUsedHint(RecyclerView.ViewHolder holder) {
        if (holder instanceof MultipleChoiceViewHolder multipleChoiceViewHolder) {
            return multipleChoiceViewHolder.hasUsedHint();
        } else if (holder instanceof FillInTheBlankViewHolder fillInTheBlankViewHolder) {
            return fillInTheBlankViewHolder.hasUsedHint();
        } else if (holder instanceof MatchingPairViewHolder matchingPairViewHolder) {
            return matchingPairViewHolder.hasUsedHint();
        } else if (holder instanceof OrderingTaskViewHolder orderingTaskViewHolder) {
            return orderingTaskViewHolder.hasUsedHint();
        } else if (holder instanceof TrueFalseViewHolder trueFalseViewHolder) {
            return trueFalseViewHolder.hasUsedHint();
        } else if (holder instanceof SpotTheErrorViewHolder spotTheErrorViewHolder) {
            return spotTheErrorViewHolder.hasUsedHint();
        } else if (holder instanceof CodingChallengeViewHolder codingChallengeViewHolder) {
            return codingChallengeViewHolder.hasUsedHint();
        }
        return false;
    }

    private void handleTaskCompletion() {
        if (activityProgress == null || activityProgress.getTaskStats() == null) {
            return;
        }
        if (isActivityCompleted() && !completionHandled) {
            completionHandled = true;
            int score = calculateEarnedXp();
            activityRepository.updateHighestScoreIfGreater(activityProgress.getUserId(), activityProgress.getCourseId(), activityProgress.getActivityId(), score);
            Integer currentHighest = activityProgress.getHighestScore();
            if (currentHighest == null || score > currentHighest) {
                activityProgress.setHighestScore(score);
            }
            notifyStatisticsCardChanged();
            notifyDiscussionCardChanged();
            if (actionListener != null) {
                actionListener.onActivityCompleted();
            }
        }
    }

    private int calculateEarnedXp() {
        Map<String, TaskStats> statsMap = activityProgress != null ? activityProgress.getTaskStats() : null;
        return ActivityScoreCalculator.calculateEarnedXp(tasks, statsMap);
    }

    private int calculateCompletedTaskCount() {
        if (activityProgress == null || activityProgress.getTaskStats() == null) {
            return 0;
        }
        int completed = 0;
        for (Task task : tasks) {
            TaskStats stats = TaskStatsKeyUtils.findStatsForTask(activityProgress.getTaskStats(), task);
            if (stats != null && stats.isCompleted()) {
                completed++;
            }
        }
        return completed;
    }

    private boolean isActivityCompleted() {
        if (activityProgress == null) {
            return false;
        }

        Integer highestScore = activityProgress.getHighestScore();
        if (highestScore != null) {
            return true;
        }

        if (tasks.isEmpty()) {
            return false;
        }

        Map<String, TaskStats> statsMap = activityProgress.getTaskStats();
        if (statsMap == null || statsMap.isEmpty()) {
            return false;
        }

        if (statsMap.size() < tasks.size()) {
            return false;
        }

        for (Task task : tasks) {
            TaskStats stats = TaskStatsKeyUtils.findStatsForTask(statsMap, task);
            if (stats == null || !stats.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private class DiscussionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView postLike;
        private final TextView postLikes;
        private final EditText commentEditText;
        private final Button postButton;
        private final RecyclerView commentsRecyclerView;
        private final View completionActionsContainer;
        private final Button returnButton;
        private final Button retryButton;

        DiscussionViewHolder(@NonNull View itemView) {
            super(itemView);
            postLike = itemView.findViewById(R.id.post_like);
            postLikes = itemView.findViewById(R.id.post_likes);
            commentEditText = itemView.findViewById(R.id.comment_edittext);
            postButton = itemView.findViewById(R.id.post);
            commentsRecyclerView = itemView.findViewById(R.id.comments_recycler_view);
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            commentsRecyclerView.setAdapter(commentsAdapter);
            commentsRecyclerView.setNestedScrollingEnabled(false);
            completionActionsContainer = itemView.findViewById(R.id.discussion_completion_actions);
            returnButton = itemView.findViewById(R.id.return_to_activities_from_discussion);
            retryButton = itemView.findViewById(R.id.retry_activity_from_discussion);
        }

        void bind() {
            final String activityIdentifier = getActivityIdentifier();
            boolean alreadyLiked = LikePreferences.hasLikedActivity(itemView.getContext(),
                    currentUserIdentifier,
                    activityIdentifier);
            postLike.setImageResource(alreadyLiked ? R.drawable.liked : R.drawable.like);
            updateLikesLabel();

            postLike.setOnClickListener(v -> {
                if (activityDetails != null && courseId != null && !courseId.isEmpty()) {
                    String identifier = getActivityIdentifier();
                    if (LikePreferences.hasLikedActivity(itemView.getContext(), currentUserIdentifier, identifier)) {
                        Toast.makeText(itemView.getContext(),
                                itemView.getContext().getString(R.string.reaction_already_applied_message),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    postLike.setEnabled(false);
                    courseRepository.addReaction(courseId, identifier, "like",
                            new CourseRepository.ReactionUpdateCallback() {
                                @Override
                                public void onSuccess(Map<String, Long> reactionCounts) {
                                    activityDetails.setReactions(reactionCounts);
                                    LikePreferences.markActivityLiked(itemView.getContext(),
                                            currentUserIdentifier,
                                            identifier);
                                    postLike.setImageResource(R.drawable.liked);
                                    postLike.setEnabled(true);
                                    updateLikesLabel();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    postLike.setEnabled(true);
                                    Toast.makeText(itemView.getContext(),
                                            itemView.getContext().getString(R.string.reaction_failed_message),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(itemView.getContext(),
                            itemView.getContext().getString(R.string.reaction_unavailable_message),
                            Toast.LENGTH_SHORT).show();
                }
            });

            postButton.setOnClickListener(v -> {
                String commentText = commentEditText.getText().toString().trim();
                if (!commentText.isEmpty()) {
                    String author = activityProgress != null ? activityProgress.getUserId() : null;
                    if (TextUtils.isEmpty(author) || "User".equalsIgnoreCase(author)) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getEmail())) {
                            author = firebaseUser.getEmail();
                        }
                    }
                    if (TextUtils.isEmpty(author)) {
                        author = "User";
                    }
                    Comment newComment = new Comment(author, commentText, String.valueOf(System.currentTimeMillis()));
                    if (activityDetails != null) {
                        String activityLocator = !TextUtils.isEmpty(activityDetails.getId())
                                ? activityDetails.getId()
                                : activityDetails.getTitle();
                        List<Comment> comments = activityDetails.getComments();
                        if (comments != null) {
                            comments.add(0, newComment);
                        }
                        if (courseId != null && !courseId.isEmpty()) {
                            courseRepository.addComment(courseId, activityLocator, newComment);
                        }
                        BadgePreferences.markCommentPosted(itemView.getContext(), author);
                        commentsAdapter.notifyItemInserted(0);
                        commentsRecyclerView.smoothScrollToPosition(0);
                    }
                    commentEditText.setText("");
                } else {
                    Toast.makeText(itemView.getContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });

            if (completionActionsContainer != null) {
                boolean completed = isActivityCompleted();
                completionActionsContainer.setVisibility(completed ? View.VISIBLE : View.GONE);
                if (completed && actionListener != null) {
                    if (returnButton != null) {
                        returnButton.setOnClickListener(v -> actionListener.onReturnToActivities());
                    }
                    if (retryButton != null) {
                        retryButton.setOnClickListener(v -> actionListener.onRetryActivity());
                    }
                } else {
                    if (returnButton != null) {
                        returnButton.setOnClickListener(null);
                    }
                    if (retryButton != null) {
                        retryButton.setOnClickListener(null);
                    }
                }
            }
        }

        private String getActivityIdentifier() {
            if (activityDetails == null) {
                return null;
            }
            if (!TextUtils.isEmpty(activityDetails.getId())) {
                return activityDetails.getId();
            }
            return activityDetails.getTitle();
        }

        private void updateLikesLabel() {
            long totalReactions = activityDetails != null ? activityDetails.getReactions() : 0;
            String reactionsLabel = itemView.getResources().getQuantityString(
                    R.plurals.reactions_count,
                    (int) Math.max(totalReactions, 0),
                    totalReactions);
            postLikes.setText(reactionsLabel);
        }
    }

    private static long parseTimestamp(@Nullable Comment comment) {
        if (comment == null || TextUtils.isEmpty(comment.getTimestamp())) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(comment.getTimestamp());
        } catch (NumberFormatException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private String calculateTimeSpent() {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - taskStartTime;
        taskStartTime = now;
        int totalSeconds = (int) (elapsedMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return tasks.size() + getExtraItemCount();
    }

    public int getStatisticsCardPosition() {
        return getStatisticsPosition();
    }

    public void resetForRetry() {
        if (activityProgress != null) {
            if (activityProgress.getTaskStats() != null) {
                activityProgress.getTaskStats().clear();
            }
            activityProgress.setHighestScore(null);
        }
        currentTaskIndex = 0;
        completionHandled = false;
        for (AtomicInteger attempts : tasksAttempts) {
            attempts.set(0);
        }
        taskStartTime = System.currentTimeMillis();
        taskSessionState.clear();
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        notifyDataSetChanged();
        if (recyclerView != null) {
            recyclerView.stopScroll();
            recyclerView.post(() -> recyclerView.scrollToPosition(0));
        }
    }

    public void refreshCompletionUi() {
        notifyStatisticsCardChanged();
        notifyDiscussionCardChanged();
    }

    public int getCurrentProgress() {
        return currentTaskIndex;
    }

    public int getMaxAccessiblePosition() {
        if (tasks.isEmpty()) {
            return Math.max(getItemCount() - 1, 0);
        }
        if (currentTaskIndex <= 0) {
            return 0;
        }
        if (currentTaskIndex >= tasks.size()) {
            return getItemCount() - 1;
        }
        return currentTaskIndex;
    }
}
