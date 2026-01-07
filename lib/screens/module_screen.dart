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
    String? moduleId;
    if (arguments is Map) {
      moduleId = arguments['moduleId'] as String?;
    }

    final Future<Module?> moduleFuture = moduleId == null
        ? courseRepository.getAllCourses().then((courses) {
            if (courses.isEmpty) {
              return null;
            }
            final modules = courses.first.modules;
            return modules.isNotEmpty ? modules.first : null;
          })
        : courseRepository.getModuleById(moduleId);

    return FutureBuilder<Module?>(
      future: moduleFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        if (snapshot.hasError) {
          return const Scaffold(
            body: Center(child: Text('Unable to load module details.')),
          );
        }
        final module = snapshot.data;
        if (module == null) {
          return const Scaffold(
            body: Center(child: Text('No module data available.')),
          );
        }

        return Scaffold(
          appBar: AppBar(
            title: Text(module.name),
          ),
          body: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: module.activities.length,
            itemBuilder: (context, index) {
              final activity = module.activities[index];
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
      },
    );
  }
}
