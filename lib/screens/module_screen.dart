import 'package:flutter/material.dart';

import '../models/module.dart';
import '../repositories/course_repository.dart';

class ModuleScreen extends StatelessWidget {
  const ModuleScreen({
    super.key,
    required this.courseRepository,
  });

  final CourseRepository courseRepository;

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    Module? module;
    if (arguments is Map) {
      final moduleId = arguments['moduleId'] as String?;
      module = courseRepository.getModuleById(moduleId ?? '');
    }
    module ??= courseRepository.getAllCourses().first.modules.first;

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
                  'activity': activity,
                },
              ),
            ),
          );
        },
      ),
    );
  }
}
