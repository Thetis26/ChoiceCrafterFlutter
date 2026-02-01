import 'notification_type.dart';

class Notification {
  Notification({
    required this.userId,
    required this.type,
    required this.courseId,
    required this.activityId,
    required this.relatedUserId,
    required this.timestamp,
    required this.details,
    this.id,
  });

  final String? id;
  final String userId;
  final NotificationType type;
  final String courseId;
  final String activityId;
  final String relatedUserId;
  final String timestamp;
  final String details;
}
