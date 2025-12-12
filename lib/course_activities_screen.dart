// lib/course_activities_screen.dart

import 'package:flutter/material.dart';

import 'sample_data.dart';

class CourseActivitiesScreen extends StatefulWidget {
  const CourseActivitiesScreen({super.key});

  @override
  State<CourseActivitiesScreen> createState() => _CourseActivitiesScreenState();
}

class _CourseActivitiesScreenState extends State<CourseActivitiesScreen> {
  CourseData? course;
  String? highlightActivityId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is Map) {
      final courseId = arguments['courseId'] as String?;
      highlightActivityId = arguments['highlightActivityId'] as String?;
      course = SampleData.courses.firstWhere(
        (element) => element.id == courseId,
        orElse: () => SampleData.courses.first,
      );
    }
    course ??= SampleData.courses.first;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(course?.title ?? 'Course Activities'),
            if (course?.instructor != null)
              Text(
                course!.instructor,
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
        itemCount: course?.modules.length ?? 0,
        itemBuilder: (context, index) {
          final module = course!.modules[index];
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
                        'activity': {
                          'id': activity.id,
                          'name': activity.name,
                          'description': activity.description,
                          'type': activity.type,
                          'content': activity.content,
                        },
                        'courseId': course?.id,
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
