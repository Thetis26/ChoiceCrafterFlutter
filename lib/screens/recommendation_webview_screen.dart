import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../sample_data.dart';

class RecommendationWebViewScreen extends StatelessWidget {
  const RecommendationWebViewScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    RecommendationData recommendation = SampleData.learningPaths.first.recommendation;
    if (arguments is Map) {
      final recommendationId = arguments['recommendationId'] as String?;
      recommendation = SampleData.learningPaths
          .map((path) => path.recommendation)
          .firstWhere(
            (rec) => rec.id == recommendationId,
            orElse: () => SampleData.learningPaths.first.recommendation,
          );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(recommendation.title),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              recommendation.summary,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 12),
            const Text('Open the link in your browser to mirror the Android web view.'),
            const SizedBox(height: 8),
            SelectableText(recommendation.url),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: recommendation.url));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Link copied to clipboard.')),
                );
              },
              icon: const Icon(Icons.copy),
              label: const Text('Copy link'),
            ),
          ],
        ),
      ),
    );
  }
}
