import 'package:flutter/material.dart';

class StatisticsScreen extends StatelessWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final metrics = [
      ('Course completion', 0.62),
      ('Engagement parity', 0.85),
      ('Weekly streak', 0.4),
      ('Inbox cleared', 0.9),
    ];

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Statistics',
          style: Theme.of(context).textTheme.headlineSmall,
        ),
        const SizedBox(height: 8),
        Text(
          'These mirrors the Android dashboard with Flutter widgets.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: 16),
        ...metrics.map(
          (metric) => Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(metric.$1, style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  LinearProgressIndicator(value: metric.$2),
                  const SizedBox(height: 4),
                  Text('${(metric.$2 * 100).round()}% complete'),
                ],
              ),
            ),
          ),
        ),
        Card(
          child: ListTile(
            leading: const Icon(Icons.emoji_events),
            title: const Text('Badges and milestones'),
            subtitle: const Text('Tap to open the badges info view.'),
            onTap: () => Navigator.of(context).pushNamed('/learningPath', arguments: {
              'learningPathId': 'lp-2',
            }),
          ),
        ),
      ],
    );
  }
}
