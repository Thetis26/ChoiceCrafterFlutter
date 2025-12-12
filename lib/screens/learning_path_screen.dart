import 'package:flutter/material.dart';

import '../sample_data.dart';

class LearningPathScreen extends StatelessWidget {
  const LearningPathScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    LearningPathData learningPath = SampleData.learningPaths.first;
    if (arguments is Map) {
      final learningPathId = arguments['learningPathId'] as String?;
      learningPath = SampleData.learningPaths.firstWhere(
        (path) => path.id == learningPathId,
        orElse: () => SampleData.learningPaths.first,
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(learningPath.title),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              learningPath.description,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children:
                  learningPath.topics.map((topic) => Chip(label: Text(topic))).toList(),
            ),
            const SizedBox(height: 16),
            Card(
              child: ListTile(
                title: Text(learningPath.recommendation.title),
                subtitle: Text(learningPath.recommendation.summary),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).pushNamed(
                  '/recommendation',
                  arguments: {'recommendationId': learningPath.recommendation.id},
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
