import 'package:flutter/material.dart';

import '../models/course.dart';
import '../models/user.dart';
import '../repositories/course_repository.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.user,
    required this.courseRepository,
    required this.onCourseSelected,
  });

  final User user;
  final CourseRepository courseRepository;
  final void Function(Course course) onCourseSelected;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Course>>(
      future: courseRepository.getEnrolledCourses(user),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return const Center(child: Text('Unable to load courses right now.'));
        }

        final courses = snapshot.data ?? [];
        if (courses.isEmpty) {
          return const Center(child: Text('No enrolled courses yet.'));
        }

        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Welcome, ${user.fullName}', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 4),
            Text(
              'Your enrolled courses mirror the Android experience so testing matches across platforms.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            ...courses.map(
              (course) => Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(course.title, style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 4),
                      Text(course.summary),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.person, size: 16),
                              const SizedBox(width: 4),
                              Text(course.instructor),
                            ],
                          ),
                          TextButton(
                            onPressed: () => onCourseSelected(course),
                            child: const Text('Open'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
