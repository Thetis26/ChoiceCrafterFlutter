// lib/course_activities_screen.dart

import 'package:flutter/material.dart';

import 'models/course.dart';
import 'models/module.dart';

class CourseActivitiesScreen extends StatelessWidget {
  const CourseActivitiesScreen({
    super.key,
    required this.course,
    this.highlightActivityId,
  });

  final Course course;
  final String? highlightActivityId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(course.title),
            Text(
              course.instructor,
              style: Theme.of(context)
                  .textTheme
                  .labelMedium
                  ?.copyWith(color: Colors.white70),
            ),
          ],
        ),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: course.modules.length,
        itemBuilder: (context, index) {
          final Module module = course.modules[index];
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ExpansionTile(
              title: Text(module.name),
              subtitle: Text(module.summary),
              children: module.activities.map((activity) {
                final isHighlighted = activity.id == highlightActivityId;
                return ListTile(
                  leading: Icon(
                    isHighlighted ? Icons.star : Icons.play_circle,
                    color: isHighlighted ? Colors.deepPurple : null,
                  ),
                  title: Text(activity.name),
                  subtitle: Text(activity.description),
                  trailing: Text('${activity.estimatedMinutes} min'),
                  onTap: () {
                    Navigator.of(context).pushNamed(
                      '/activity',
                      arguments: {
                        'activity': activity,
                        'courseId': course.id,
                      },
                    );
                  },
                );
              }).toList(),
            ),
          );
        },
      ),
    );
  }
}
