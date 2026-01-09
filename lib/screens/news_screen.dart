import 'package:flutter/material.dart';

import '../models/course.dart';
import '../models/user.dart';
import '../repositories/course_repository.dart';

class NewsScreen extends StatelessWidget {
  const NewsScreen({
    super.key,
    required this.user,
    required this.courseRepository,
    required this.onCourseEnrolled,
  });

  final User user;
  final CourseRepository courseRepository;
  final void Function(Course course) onCourseEnrolled;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Course>>(
      future: courseRepository.getAvailableCourses(user),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return const Center(child: Text('Unable to load news right now.'));
        }
        final courses = snapshot.data ?? [];
        if (courses.isEmpty) {
          return const Center(child: Text('No course updates yet.'));
        }

        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: courses.length,
          itemBuilder: (context, index) {
            final course = courses[index];
            return Card(
              margin: const EdgeInsets.only(bottom: 12),
              child: ListTile(
                leading: const Icon(Icons.campaign),
                title: Text(course.title),
                subtitle: Text(course.summary),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => NewsCourseDetailScreen(
                        course: course,
                        user: user,
                        courseRepository: courseRepository,
                        onEnrolled: onCourseEnrolled,
                      ),
                    ),
                  );
                },
              ),
            );
          },
        );
      },
    );
  }
}

class NewsCourseDetailScreen extends StatelessWidget {
  const NewsCourseDetailScreen({
    super.key,
    required this.course,
    required this.user,
    required this.courseRepository,
    required this.onEnrolled,
  });

  final Course course;
  final User user;
  final CourseRepository courseRepository;
  final void Function(Course course) onEnrolled;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(course.title),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            course.summary,
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 16),
          ...course.modules.map(
            (module) => Card(
              margin: const EdgeInsets.only(bottom: 12),
              child: ExpansionTile(
                title: Text(module.name),
                subtitle: Text(module.summary),
                children: module.activities
                    .map(
                      (activity) => ListTile(
                        leading: const Icon(Icons.menu_book_outlined),
                        title: Text(activity.name),
                        subtitle: Text(activity.description),
                        trailing: Text('${activity.estimatedMinutes} min'),
                      ),
                    )
                    .toList(),
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.all(16),
        child: FilledButton.icon(
          onPressed: () async {
            await courseRepository.enrollInCourse(user, course);
            if (!context.mounted) {
              return;
            }
            Navigator.of(context).popUntil((route) => route.isFirst);
            onEnrolled(course);
          },
          icon: const Icon(Icons.check_circle_outline),
          label: const Text('Enroll'),
        ),
      ),
    );
  }
}
