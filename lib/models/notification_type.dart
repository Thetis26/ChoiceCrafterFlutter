enum NotificationType {
  activityStarted('Activity Started'),
  colleagueActivityStarted('Colleague Activity Started'),
  pointsThresholdReached('Points Milestone'),
  commentAdded('Comment Added'),
  chatMessage('Chat Message'),
  reminder('Reminder');

  const NotificationType(this.displayName);

  final String displayName;
}
