import 'activity.dart';

class Module {
  const Module({
    required this.id,
    required this.name,
    required this.summary,
    required this.activities,
    this.title,
    this.description,
    this.completedPercentage = 0,
    this.courseId,
  });

  final String id;
  final String name;
  final String summary;
  final List<Activity> activities;
  final String? title;
  final String? description;
  final int completedPercentage;
  final String? courseId;
}
