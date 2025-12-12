import 'package:flutter/material.dart';

import '../sample_data.dart';

class ColleaguesActivityScreen extends StatelessWidget {
  const ColleaguesActivityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: SampleData.activities.length,
      itemBuilder: (context, index) {
        final activity = SampleData.activities[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            title: Text('${activity.name} (${activity.type})'),
            subtitle: Text(
              'Colleagues are working on this now. ${activity.estimatedMinutes} min left.',
            ),
            trailing: const Icon(Icons.chevron_right),
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
                  'courseId': SampleData.courses.first.id,
                },
              );
            },
          ),
        );
      },
    );
  }
}
