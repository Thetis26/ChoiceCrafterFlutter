import 'package:flutter/material.dart';

import '../sample_data.dart';

class ModuleScreen extends StatelessWidget {
  const ModuleScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    ModuleData? module;
    if (arguments is Map) {
      final moduleId = arguments['moduleId'] as String?;
      module = SampleData.courses
          .expand((course) => course.modules)
          .firstWhere((item) => item.id == moduleId, orElse: () => SampleData.courses.first.modules.first);
    }
    module ??= SampleData.courses.first.modules.first;

    return Scaffold(
      appBar: AppBar(
        title: Text(module.name),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: module.activities.length,
        itemBuilder: (context, index) {
          final activity = module!.activities[index];
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ListTile(
              title: Text(activity.name),
              subtitle: Text(activity.description),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.of(context).pushNamed(
                '/activity',
                arguments: {
                  'activity': {
                    'id': activity.id,
                    'name': activity.name,
                    'description': activity.description,
                    'type': activity.type,
                    'content': activity.content,
                  },
                },
              ),
            ),
          );
        },
      ),
    );
  }
}
