class UserActivity {
  const UserActivity({
    required this.id,
    required this.activityName,
    required this.courseTitle,
    required this.status,
    required this.progress,
    this.estimatedMinutes,
  });

  final String id;
  final String activityName;
  final String courseTitle;
  final String status;
  final double progress;
  final int? estimatedMinutes;
}
