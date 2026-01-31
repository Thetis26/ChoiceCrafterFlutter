// lib/course_activities_screen.dart

import 'package:flutter/material.dart';

import 'models/course.dart';
import 'models/module.dart';
import 'models/enrollment_activity_progress.dart';
import 'models/user.dart';
import 'repositories/course_repository.dart';
import 'repositories/personal_statistics_repository.dart';

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
      return _buildCourseScreen(context, resolvedCourse);
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
        return _buildCourseScreen(context, course);
      },
    );
  }

  Widget _buildCourseScreen(BuildContext context, Course course) {
    final userKey = user.email.isNotEmpty ? user.email : user.id;
    return FutureBuilder<List<EnrollmentActivityProgress>>(
      future:
          PersonalStatisticsRepository().fetchActivitySnapshotsForUser(userKey),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        if (snapshot.hasError) {
          return const Scaffold(
            body: Center(child: Text('Unable to load progress data.')),
          );
        }
        final progressByActivity =
            _buildProgressLookup(snapshot.data ?? const []);
        return CourseActivitiesScreen(
          course: course,
          user: user,
          highlightActivityId: highlightActivityId,
          progressByActivity: progressByActivity,
        );
      },
    );
  }

  Map<String, double> _buildProgressLookup(
    List<EnrollmentActivityProgress> snapshots,
  ) {
    final lookup = <String, double>{};
    for (final snapshot in snapshots) {
      if (snapshot.activityId.isEmpty) {
        continue;
      }
      final progress = snapshot.averageCompletionRatio().clamp(0.0, 1.0);
      final existing = lookup[snapshot.activityId];
      if (existing == null || progress > existing) {
        lookup[snapshot.activityId] = progress;
      }
    }
    return lookup;
  }
}

class CourseActivitiesScreen extends StatelessWidget {
  const CourseActivitiesScreen({
    super.key,
    required this.course,
    required this.user,
    this.highlightActivityId,
    this.progressByActivity = const {},
  });

  final Course course;
  final User user;
  final String? highlightActivityId;
  final Map<String, double> progressByActivity;

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
          final moduleProgress = _moduleProgress(module);
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ExpansionTile(
              title: Text(module.name),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(module.summary),
                  const SizedBox(height: 8),
                  _ProgressRow(progress: moduleProgress),
                ],
              ),
              children: module.activities.map((activity) {
                final isHighlighted = activity.id == highlightActivityId;
                final activityProgress =
                    progressByActivity[activity.id] ?? 0.0;
                return ListTile(
                  leading: Icon(
                    isHighlighted ? Icons.star : Icons.play_circle,
                    color: isHighlighted ? Colors.deepPurple : null,
                  ),
                  title: Text(activity.name),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(activity.description),
                      const SizedBox(height: 6),
                      _ProgressRow(progress: activityProgress),
                    ],
                  ),
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

  double _moduleProgress(Module module) {
    if (module.activities.isEmpty) {
      return 0.0;
    }
    final total = module.activities
        .map((activity) => progressByActivity[activity.id] ?? 0.0)
        .fold<double>(0.0, (sum, value) => sum + value);
    return (total / module.activities.length).clamp(0.0, 1.0);
  }
}

class _ProgressRow extends StatelessWidget {
  const _ProgressRow({required this.progress});

  final double progress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final percentage = (progress * 100).round();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Progress', style: theme.textTheme.bodySmall),
            Text('$percentage%', style: theme.textTheme.bodySmall),
          ],
        ),
        const SizedBox(height: 6),
        LinearProgressIndicator(value: progress),
      ],
    );
  }
}
