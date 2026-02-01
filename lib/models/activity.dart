import 'recommendation.dart';
import 'task.dart';

class Activity {
  const Activity({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.content,
    this.estimatedMinutes = 15,
    this.tasks = const [],
    this.recommendations = const [],
    this.reactions = const [],
    this.comments = const [],
  });

  final String id;
  final String name;
  final String description;
  final String type;
  final String content;
  final int estimatedMinutes;
  final List<Task> tasks;
  final List<Recommendation> recommendations;
  final List<ActivityReaction> reactions;
  final List<ActivityComment> comments;
}

class ActivityReaction {
  const ActivityReaction({
    required this.type,
    required this.count,
  });

  final String type;
  final int count;
}

class ActivityComment {
  const ActivityComment({
    required this.author,
    required this.message,
    required this.timestamp,
  });

  final String author;
  final String message;
  final DateTime timestamp;
}
