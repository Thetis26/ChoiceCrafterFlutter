import 'activity.dart';
import 'module.dart';

class Course {
  const Course({
    required this.id,
    required this.title,
    required this.instructor,
    required this.summary,
    this.description,
    this.teacher,
    this.imageUrl,
    this.activities = const [],
    this.modules = const [],
  });

  final String id;
  final String title;
  final String instructor;
  final String summary;
  final String? description;
  final String? teacher;
  final String? imageUrl;
  final List<Activity> activities;
  final List<Module> modules;
}
