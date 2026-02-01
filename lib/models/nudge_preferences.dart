class NudgePreferences {
  NudgePreferences({
    this.userEmail,
    this.personalStatisticsPromptEnabled,
    this.completedActivityPromptEnabled,
    this.colleaguesPromptEnabled,
    this.activityStartedNotificationsEnabled,
    this.reminderNotificationsEnabled,
    this.colleaguesActivityPageEnabled,
    this.discussionForumEnabled,
  });

  static const String collectionName = 'NUDGE_PREFERENCES';

  final String? userEmail;
  final bool? personalStatisticsPromptEnabled;
  final bool? completedActivityPromptEnabled;
  final bool? colleaguesPromptEnabled;
  final bool? activityStartedNotificationsEnabled;
  final bool? reminderNotificationsEnabled;
  final bool? colleaguesActivityPageEnabled;
  final bool? discussionForumEnabled;

  factory NudgePreferences.createDefault(String? userEmail) {
    return NudgePreferences(
      userEmail: userEmail,
      personalStatisticsPromptEnabled: true,
      completedActivityPromptEnabled: true,
      colleaguesPromptEnabled: true,
      activityStartedNotificationsEnabled: true,
      reminderNotificationsEnabled: true,
      colleaguesActivityPageEnabled: true,
      discussionForumEnabled: true,
    );
  }

  bool get isPersonalStatisticsPromptEnabled =>
      personalStatisticsPromptEnabled ?? true;
  bool get isCompletedActivityPromptEnabled =>
      completedActivityPromptEnabled ?? true;
  bool get isColleaguesPromptEnabled => colleaguesPromptEnabled ?? true;
  bool get isActivityStartedNotificationsEnabled =>
      activityStartedNotificationsEnabled ?? true;
  bool get isReminderNotificationsEnabled => reminderNotificationsEnabled ?? true;
  bool get isColleaguesActivityPageEnabled =>
      colleaguesActivityPageEnabled ?? true;
  bool get isDiscussionForumEnabled => discussionForumEnabled ?? true;
}
