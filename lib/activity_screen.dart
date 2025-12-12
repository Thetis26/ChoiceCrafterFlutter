// lib/activity_screen.dart

import 'package:flutter/material.dart';

class ActivityScreen extends StatefulWidget {
  const ActivityScreen({super.key});

  @override
  State<ActivityScreen> createState() => _ActivityScreenState();
}

class _ActivityScreenState extends State<ActivityScreen> {
  Map<String, dynamic>? activityData;
  String? courseId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is Map) {
      setState(() {
        activityData = Map<String, dynamic>.from(arguments['activity'] as Map);
        courseId = arguments['courseId'] as String?;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final String activityName = activityData?['name'] ?? 'Unknown Activity';
    final String description = activityData?['description'] ?? '';
    final String type = activityData?['type'] ?? 'activity';
    final String content = activityData?['content'] ?? '';

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(activityName),
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
                Chip(label: Text(type)),
                if (courseId != null) ...[
                  const SizedBox(width: 8),
                  Chip(label: Text('Course $courseId')),
                ],
              ],
            ),
            const SizedBox(height: 12),
            Text(description, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 16),
            Text(content),
            const Spacer(),
            Align(
              alignment: Alignment.centerRight,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.of(context).pushNamed(
                    '/courseActivities',
                    arguments: {
                      'courseId': courseId,
                      'highlightActivityId': activityData?['id'],
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
