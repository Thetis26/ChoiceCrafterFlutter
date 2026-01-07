import 'package:flutter/material.dart';

import '../models/activity.dart';
import '../models/course.dart';
import '../repositories/course_repository.dart';

class ColleaguesActivityScreen extends StatelessWidget {
  const ColleaguesActivityScreen({
    super.key,
    required this.courseRepository,
  });

  final CourseRepository courseRepository;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Course>>(
      future: courseRepository.getAllCourses(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return const Center(child: Text('Unable to load activity feed.'));
        }
        final courses = snapshot.data ?? [];
        if (courses.isEmpty) {
          return const Center(child: Text('No activities yet.'));
        }

        final activities = courses
            .expand((course) => course.modules)
            .expand((module) => module.activities)
            .toList();
        final courseId = courses.first.id;

        if (activities.isEmpty) {
          return const Center(child: Text('No activities yet.'));
        }

        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: activities.length,
          itemBuilder: (context, index) {
            final Activity activity = activities[index];
            return Card(
              margin: const EdgeInsets.only(bottom: 12),
              child: ListTile(
                title: Text('${activity.name} (${activity.type})'),
                subtitle: Text(
                  'Colleagues are working on this now. ${activity.estimatedMinutes} min left.',
                ),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).pushNamed(
                  '/activity',
                  arguments: {
                    'activity': activity,
                    'courseId': courseId,
                  },
                ),
              ),
            );
          },
        );
      },
    );
  }
}
