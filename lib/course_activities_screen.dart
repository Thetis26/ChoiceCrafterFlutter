// lib/course_activities_screen.dart

import 'package:flutter/material.dart';

import 'models/course.dart';
import 'models/module.dart';
import 'models/user.dart';
import 'repositories/course_repository.dart';

class CourseActivitiesLoader extends StatelessWidget {
  const CourseActivitiesLoader({
    super.key,
    required this.courseRepository,
    required this.user,
    this.course,
    this.courseId,
    this.highlightActivityId,
  });

  final CourseRepository courseRepository;
  final User user;
  final Course? course;
  final String? courseId;
  final String? highlightActivityId;

  @override
  Widget build(BuildContext context) {
    final resolvedCourse = course;
    if (resolvedCourse != null) {
      return CourseActivitiesScreen(
        course: resolvedCourse,
        user: user,
        highlightActivityId: highlightActivityId,
      );
    }

    if (courseId == null) {
      return const Scaffold(
        body: Center(child: Text('No course data available.')),
      );
    }

    return FutureBuilder<Course?>(
      future: courseRepository.getCourseById(courseId!),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        if (snapshot.hasError) {
          return const Scaffold(body: Center(child: Text('Unable to load course.')));
        }
        final course = snapshot.data;
        if (course == null) {
          return const Scaffold(body: Center(child: Text('Course not found.')));
        }
        return CourseActivitiesScreen(
          course: course,
          user: user,
          highlightActivityId: highlightActivityId,
        );
      },
    );
  }
}

class CourseActivitiesScreen extends StatelessWidget {
  const CourseActivitiesScreen({
    super.key,
    required this.course,
    required this.user,
    this.highlightActivityId,
  });

  final Course course;
  final User user;
  final String? highlightActivityId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(course.title),
            Text(
              course.instructor,
              style: Theme.of(context)
                  .textTheme
                  .labelMedium
                  ?.copyWith(color: Colors.white70),
            ),
          ],
        ),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: course.modules.length,
        itemBuilder: (context, index) {
          final Module module = course.modules[index];
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ExpansionTile(
              title: Text(module.name),
              subtitle: Text(module.summary),
              children: module.activities.map((activity) {
                final isHighlighted = activity.id == highlightActivityId;
                return ListTile(
                  leading: Icon(
                    isHighlighted ? Icons.star : Icons.play_circle,
                    color: isHighlighted ? Colors.deepPurple : null,
                  ),
                  title: Text(activity.name),
                  subtitle: Text(activity.description),
                  trailing: Text('${activity.estimatedMinutes} min'),
                  onTap: () {
                    Navigator.of(context).pushNamed(
                      '/activity',
                      arguments: {
                        'activity': activity,
                        'courseId': course.id,
                        'user': user,
                      },
                    );
                  },
                );
              }).toList(),
            ),
          );
        },
      ),
    );
  }
}
