import 'package:flutter/material.dart';

import '../models/activity.dart';
import '../repositories/course_repository.dart';

class ColleaguesActivityScreen extends StatelessWidget {
  const ColleaguesActivityScreen({
    super.key,
    required this.courseRepository,
  });

  final CourseRepository courseRepository;

  @override
  Widget build(BuildContext context) {
    final activities = courseRepository.getAllActivities();
    final courseId = courseRepository.getAllCourses().first.id;
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
  }
}
