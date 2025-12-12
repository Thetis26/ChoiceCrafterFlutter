// lib/activity_screen.dart

import 'package:flutter/material.dart';

import 'models/activity.dart';

class ActivityScreen extends StatelessWidget {
  const ActivityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is! Map || arguments['activity'] is! Activity) {
      return const Scaffold(body: Center(child: Text('No activity data provided.')));
    }

    final Activity activity = arguments['activity'] as Activity;
    final String? courseId = arguments['courseId'] as String?;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(activity.name),
            if (courseId != null)
              Text(
                'Course: $courseId',
                style: Theme.of(context)
                    .textTheme
                    .labelMedium
                    ?.copyWith(color: Colors.white70),
              ),
          ],
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Chip(label: Text(activity.type)),
                if (courseId != null) ...[
                  const SizedBox(width: 8),
                  Chip(label: Text('Course $courseId')),
                ],
              ],
            ),
            const SizedBox(height: 12),
            Text(activity.description, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 16),
            Text(activity.content),
            const Spacer(),
            Align(
              alignment: Alignment.centerRight,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.of(context).pushNamed(
                    '/courseActivities',
                    arguments: {
                      'courseId': courseId,
                      'highlightActivityId': activity.id,
                    },
                  );
                },
                icon: const Icon(Icons.list),
                label: const Text('Back to activities'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
