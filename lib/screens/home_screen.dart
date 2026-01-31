import 'package:flutter/material.dart';

import '../models/course.dart';
import '../models/enrollment_activity_progress.dart';
import '../models/user.dart';
import '../repositories/course_repository.dart';
import '../repositories/personal_statistics_repository.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.user,
    required this.courseRepository,
    required this.onCourseSelected,
    this.highlightCourseId,
  });

  final User user;
  final CourseRepository courseRepository;
  final void Function(Course course) onCourseSelected;
  final String? highlightCourseId;

  @override
  Widget build(BuildContext context) {
    debugPrint('HomeScreen.build - user: ${user.fullName}');
    return FutureBuilder<_HomeScreenData>(
      future: _loadData(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          debugPrint('HomeScreen: loading courses for ${user.fullName}...');
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return const Center(child: Text('Unable to load courses right now.'));
        }

        final data = snapshot.data ?? _HomeScreenData.empty();
        final courses = data.courses;
        debugPrint('HomeScreen: loaded ${courses.length} courses for ${user.fullName}');
        if (courses.isEmpty) {
          return const Center(child: Text('No enrolled courses yet.'));
        }

        final activityProgressById =
            _buildActivityProgressLookup(data.activitySnapshots);

        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Welcome, ${user.fullName}', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 4),
            Text(
              'Your enrolled courses mirror the Android experience so testing matches across platforms.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            ...courses.map(
              (course) {
                final isHighlighted = course.id == highlightCourseId;
                final courseProgress =
                    _courseProgress(course, activityProgressById);
                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  color: isHighlighted
                      ? Theme.of(context).colorScheme.primaryContainer
                      : null,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          course.title,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: 4),
                        Text(course.summary),
                        const SizedBox(height: 8),
                        _ProgressRow(progress: courseProgress),
                        const SizedBox(height: 12),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                const Icon(Icons.person, size: 16),
                                const SizedBox(width: 4),
                                Text(course.instructor),
                              ],
                            ),
                            TextButton(
                              onPressed: () => onCourseSelected(course),
                              child: const Text('Open'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ],
        );
      },
    );
  }

  Future<_HomeScreenData> _loadData() async {
    final courses = await courseRepository.getEnrolledCourses(user);
    final userKey = user.email.isNotEmpty ? user.email : user.id;
    final activitySnapshots = await PersonalStatisticsRepository()
        .fetchActivitySnapshotsForUser(userKey);
    return _HomeScreenData(
      courses: courses,
      activitySnapshots: activitySnapshots,
    );
  }

  Map<String, double> _buildActivityProgressLookup(
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

  double _courseProgress(Course course, Map<String, double> progressByActivity) {
    final activities =
        course.modules.expand((module) => module.activities).toList();
    if (activities.isEmpty) {
      return 0.0;
    }
    final total = activities
        .map((activity) => progressByActivity[activity.id] ?? 0.0)
        .fold<double>(0.0, (sum, value) => sum + value);
    return (total / activities.length).clamp(0.0, 1.0);
  }
}

class _HomeScreenData {
  const _HomeScreenData({
    required this.courses,
    required this.activitySnapshots,
  });

  final List<Course> courses;
  final List<EnrollmentActivityProgress> activitySnapshots;

  factory _HomeScreenData.empty() => const _HomeScreenData(
        courses: [],
        activitySnapshots: [],
      );
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
