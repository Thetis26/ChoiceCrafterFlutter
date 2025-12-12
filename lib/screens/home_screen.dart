import 'package:flutter/material.dart';

import '../sample_data.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'ChoiceCrafter',
          style: Theme.of(context).textTheme.headlineSmall,
        ),
        const SizedBox(height: 4),
        Text(
          'Stay in sync with the Android experience: courses, activities, and recommendations are mirrored here for iOS.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: 16),
        _StatusCard(),
        const SizedBox(height: 16),
        Text(
          'Courses',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        ...SampleData.courses.map((course) => _CourseCard(course: course)),
        const SizedBox(height: 16),
        Text(
          'Learning paths',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        ...SampleData.learningPaths
            .map((path) => _LearningPathCard(learningPathData: path)),
      ],
    );
  }
}

class _StatusCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'This week',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Icon(Icons.access_time, color: Colors.deepPurple),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Keep parity with Android: 3 activities waiting and one reflection due.',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text('Activities complete'),
                      SizedBox(height: 4),
                      LinearProgressIndicator(value: 0.4),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: const [
                    Text('2 of 5'),
                    Text('Mirror of Android tasks'),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _CourseCard extends StatelessWidget {
  const _CourseCard({required this.course});

  final CourseData course;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              course.title,
              style: Theme.of(context).textTheme.titleMedium,
            ),
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
                  onPressed: () {
                    Navigator.of(context).pushNamed(
                      '/courseActivities',
                      arguments: {
                        'courseId': course.id,
                        'highlightActivityId':
                            course.modules.first.activities.first.id,
                      },
                    );
                  },
                  child: const Text('View activities'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _LearningPathCard extends StatelessWidget {
  const _LearningPathCard({required this.learningPathData});

  final LearningPathData learningPathData;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              learningPathData.title,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 4),
            Text(learningPathData.description),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: learningPathData.topics
                  .map((topic) => Chip(label: Text(topic)))
                  .toList(),
            ),
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: () {
                  Navigator.of(context).pushNamed(
                    '/learningPath',
                    arguments: {'learningPathId': learningPathData.id},
                  );
                },
                child: const Text('Open path'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
