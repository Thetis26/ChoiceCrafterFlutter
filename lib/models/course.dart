import 'module.dart';

class Course {
  const Course({
    required this.id,
    required this.title,
    required this.instructor,
    required this.summary,
    required this.modules,
  });

  final String id;
  final String title;
  final String instructor;
  final String summary;
  final List<Module> modules;
}
