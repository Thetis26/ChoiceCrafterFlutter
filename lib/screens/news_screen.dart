import 'package:flutter/material.dart';

import '../models/course.dart';
import '../repositories/course_repository.dart';

class NewsScreen extends StatelessWidget {
  const NewsScreen({
    super.key,
    required this.courseRepository,
    required this.onCourseSelected,
  });

  final CourseRepository courseRepository;
  final void Function(Course course) onCourseSelected;

  @override
  Widget build(BuildContext context) {
    final courses = courseRepository.getAllCourses();
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: courses.length,
      itemBuilder: (context, index) {
        final course = courses[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            leading: const Icon(Icons.campaign),
            title: Text(course.title),
            subtitle: Text(course.summary),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => onCourseSelected(course),
          ),
        );
      },
    );
  }
}
