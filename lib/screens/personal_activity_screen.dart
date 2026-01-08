import 'package:flutter/material.dart';

import '../models/course.dart';
import '../models/enrollment_activity_snapshot.dart';
import '../models/user.dart';
import '../repositories/course_repository.dart';
import '../repositories/personal_statistics_repository.dart';

class PersonalActivityScreen extends StatefulWidget {
  const PersonalActivityScreen({
    super.key,
    required this.user,
    required this.courseRepository,
    required this.personalStatisticsRepository,
  });

  final User user;
  final CourseRepository courseRepository;
  final PersonalStatisticsRepository personalStatisticsRepository;

  @override
  State<PersonalActivityScreen> createState() => _PersonalActivityScreenState();
}

class _PersonalActivityScreenState extends State<PersonalActivityScreen> {
  late Future<_PersonalActivityData> _dataFuture;

  @override
  void initState() {
    super.initState();
    _dataFuture = _loadData();
  }

  Future<_PersonalActivityData> _loadData() async {
    final courses = await widget.courseRepository.getAllCourses();
    final userKey =
        widget.user.email.isNotEmpty ? widget.user.email : widget.user.id;
    final activitySnapshots = await widget.personalStatisticsRepository
        .fetchActivitySnapshotsForUser(userKey);
    return _PersonalActivityData.from(courses, activitySnapshots);
  }

  Future<void> _refresh() async {
    setState(() {
      _dataFuture = _loadData();
    });
    await _dataFuture;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<_PersonalActivityData>(
      future: _dataFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(
            child: Text(
              'Unable to load personal activity right now.',
              style: Theme.of(context).textTheme.bodyLarge,
            ),
          );
        }
        final data = snapshot.data ??
            _PersonalActivityData.empty();
        final activityCards =
            _buildActivityCards(data.activitySummaries, context);
        final streak = _computeStreak(data.allAttemptDates);
        final points = _computePoints(data.activitySummaries);
        final badges = _buildBadges(
          streak: streak,
          completedActivities:
              data.activitySummaries.where((summary) => summary.isCompleted).length,
          uniqueCourses: data.uniqueCourseCount,
          totalPoints: points,
        );

        return RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                'Statistics',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 12),
              _buildStatsHeader(context, streak, points),
              const SizedBox(height: 16),
              _buildMotivationCard(
                context,
                streak: streak,
                recentActivities: data.recentActivityCount,
                todayCompletions: data.todayCompletionCount,
              ),
              const SizedBox(height: 20),
              Text(
                'Activity badges',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 12),
              _buildBadgeRow(badges, context),
              const SizedBox(height: 20),
              Text(
                'Recent activity',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 12),
              if (activityCards.isEmpty)
                Text(
                  'Complete activities in your courses to see your progress here.',
                  style: Theme.of(context).textTheme.bodyMedium,
                )
              else
                ...activityCards,
            ],
          ),
        );
      },
    );
  }

  List<Widget> _buildActivityCards(
    List<_ActivitySummary> summaries,
    BuildContext context,
  ) {
    if (summaries.isEmpty) {
      return [];
    }
    final theme = Theme.of(context);
    return summaries.map((summary) {
      return Card(
        margin: const EdgeInsets.only(bottom: 12),
        child: ListTile(
          leading: CircleAvatar(
            backgroundColor:
                summary.isCompleted ? Colors.green.shade100 : Colors.blue.shade100,
            child: Icon(
              summary.isCompleted ? Icons.check_circle : Icons.play_arrow,
              color: summary.isCompleted ? Colors.green : Colors.blueAccent,
            ),
          ),
          title: Text(summary.activityName),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(summary.courseTitle, style: theme.textTheme.bodySmall),
              const SizedBox(height: 4),
              LinearProgressIndicator(value: summary.progress),
              const SizedBox(height: 4),
              Text(
                '${summary.status} · ${summary.lastAttemptLabel}',
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
          trailing: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '${summary.points}',
                style: theme.textTheme.titleMedium,
              ),
              Text(
                'pts',
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
        ),
      );
    }).toList();
  }
}

class _PersonalActivityData {
  const _PersonalActivityData({
    required this.activitySummaries,
    required this.allAttemptDates,
    required this.uniqueCourseCount,
    required this.recentActivityCount,
    required this.todayCompletionCount,
  });

  final List<_ActivitySummary> activitySummaries;
  final List<DateTime> allAttemptDates;
  final int uniqueCourseCount;
  final int recentActivityCount;
  final int todayCompletionCount;

  factory _PersonalActivityData.empty() => const _PersonalActivityData(
        activitySummaries: [],
        allAttemptDates: [],
        uniqueCourseCount: 0,
        recentActivityCount: 0,
        todayCompletionCount: 0,
      );

  factory _PersonalActivityData.from(
    List<Course> courses,
    List<EnrollmentActivitySnapshot> snapshots,
  ) {
    final activityNameById = <String, String>{};
    final courseTitleById = <String, String>{};
    for (final course in courses) {
      courseTitleById[course.id] = course.title;
      for (final module in course.modules) {
        for (final activity in module.activities) {
          activityNameById[activity.id] = activity.name;
        }
      }
    }

    final summaries = <_ActivitySummary>[];
    final attemptDates = <DateTime>[];
    final courseIds = <String>{};
    int recentActivityCount = 0;
    int todayCompletionCount = 0;
    final now = DateTime.now();

    for (final snapshot in snapshots) {
      final latestAttempt = snapshot.latestAttempt();
      if (latestAttempt != null) {
        attemptDates.add(latestAttempt);
        final difference = now.difference(latestAttempt);
        if (difference.inDays < 7) {
          recentActivityCount += 1;
        }
        if (difference.inHours < 24) {
          todayCompletionCount += 1;
        }
      }
      if (snapshot.courseId.isNotEmpty) {
        courseIds.add(snapshot.courseId);
      }
      final progress = snapshot.averageCompletionRatio();
      final scoreRatio = snapshot.averageScoreRatio();
      final isCompleted = progress >= 1.0 || scoreRatio >= 1.0;
      final points = snapshot.highestScore ??
          (scoreRatio > 0 ? (scoreRatio * 100).round() : (progress * 100).round());
      summaries.add(
        _ActivitySummary(
          activityId: snapshot.activityId,
          activityName: activityNameById[snapshot.activityId] ??
              'Activity ${snapshot.activityId}',
          courseTitle:
              courseTitleById[snapshot.courseId] ?? snapshot.courseId,
          status: isCompleted ? 'Completed' : 'In progress',
          progress: progress.clamp(0.0, 1.0),
          lastAttempt: latestAttempt,
          points: points,
        ),
      );
    }

    summaries.sort((a, b) => b._safeLastAttempt.compareTo(a._safeLastAttempt));

    return _PersonalActivityData(
      activitySummaries: summaries,
      allAttemptDates: attemptDates,
      uniqueCourseCount: courseIds.length,
      recentActivityCount: recentActivityCount,
      todayCompletionCount: todayCompletionCount,
    );
  }
}

class _ActivitySummary {
  const _ActivitySummary({
    required this.activityId,
    required this.activityName,
    required this.courseTitle,
    required this.status,
    required this.progress,
    required this.lastAttempt,
    required this.points,
  });

  final String activityId;
  final String activityName;
  final String courseTitle;
  final String status;
  final double progress;
  final DateTime? lastAttempt;
  final int points;

  bool get isCompleted => status == 'Completed';

  DateTime get _safeLastAttempt =>
      lastAttempt ?? DateTime.fromMillisecondsSinceEpoch(0);

  String get lastAttemptLabel {
    if (lastAttempt == null) {
      return 'Date unavailable';
    }
    final day = lastAttempt!.day.toString().padLeft(2, '0');
    final month = lastAttempt!.month.toString().padLeft(2, '0');
    return '$day/$month';
  }
}

class _ActivityBadge {
  const _ActivityBadge({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.earned,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final bool earned;
}

Widget _buildStatsHeader(BuildContext context, int streak, int points) {
  final theme = Theme.of(context);
  return Row(
    children: [
      Expanded(
        child: _StatChip(
          icon: Icons.local_fire_department,
          label: '$streak day streak',
          color: Colors.deepOrange,
          textStyle: theme.textTheme.titleMedium,
        ),
      ),
      const SizedBox(width: 12),
      Expanded(
        child: _StatChip(
          icon: Icons.emoji_events,
          label: '$points points',
          color: Colors.amber.shade700,
          textStyle: theme.textTheme.titleMedium,
        ),
      ),
    ],
  );
}

Widget _buildMotivationCard(
  BuildContext context, {
  required int streak,
  required int recentActivities,
  required int todayCompletions,
}) {
  final theme = Theme.of(context);
  final streakText = streak == 1 ? '1 day' : '$streak days';
  final todayText = todayCompletions == 1
      ? '1 activity today'
      : '$todayCompletions activities today';
  final recentText = recentActivities == 1
      ? '1 activity this week'
      : '$recentActivities activities this week';
  return Card(
    elevation: 2,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.emoji_events, color: Colors.orange, size: 36),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Nice! You have a $streakText streak. $todayText and $recentText. '
              'Keep building your momentum!',
              style: theme.textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    ),
  );
}

Widget _buildBadgeRow(List<_ActivityBadge> badges, BuildContext context) {
  return SizedBox(
    height: 120,
    child: ListView.separated(
      scrollDirection: Axis.horizontal,
      itemCount: badges.length,
      separatorBuilder: (_, __) => const SizedBox(width: 12),
      itemBuilder: (context, index) {
        final badge = badges[index];
        return Container(
          width: 180,
          decoration: BoxDecoration(
            color: badge.earned ? Colors.white : Colors.grey.shade100,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: badge.earned ? Colors.deepPurple : Colors.grey.shade300,
            ),
            boxShadow: [
              if (badge.earned)
                const BoxShadow(
                  color: Color(0x22000000),
                  blurRadius: 6,
                  offset: Offset(0, 2),
                ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                CircleAvatar(
                  backgroundColor: badge.earned
                      ? Colors.deepPurple.shade50
                      : Colors.grey.shade200,
                  child: Icon(
                    badge.icon,
                    color: badge.earned
                        ? Colors.deepPurple
                        : Colors.grey.shade500,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  badge.title,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                const SizedBox(height: 4),
                Text(
                  badge.subtitle,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        );
      },
    ),
  );
}

List<_ActivityBadge> _buildBadges({
  required int streak,
  required int completedActivities,
  required int uniqueCourses,
  required int totalPoints,
}) {
  return [
    _ActivityBadge(
      title: 'Streak Starter',
      subtitle: 'Maintain a 2-day streak.',
      icon: Icons.local_fire_department,
      earned: streak >= 2,
    ),
    _ActivityBadge(
      title: 'Course Explorer',
      subtitle: 'Complete activities in 2 courses.',
      icon: Icons.explore,
      earned: uniqueCourses >= 2,
    ),
    _ActivityBadge(
      title: 'Point Collector',
      subtitle: 'Earn 250 points.',
      icon: Icons.star,
      earned: totalPoints >= 250,
    ),
    _ActivityBadge(
      title: 'Activity Finisher',
      subtitle: 'Finish 3 activities.',
      icon: Icons.emoji_events,
      earned: completedActivities >= 3,
    ),
  ];
}

int _computeStreak(List<DateTime> attemptDates) {
  if (attemptDates.isEmpty) {
    return 0;
  }
  final normalized = attemptDates
      .map((date) => DateTime(date.year, date.month, date.day))
      .toSet();
  var streak = 0;
  var dayCursor = DateTime.now();
  while (true) {
    final dayKey = DateTime(dayCursor.year, dayCursor.month, dayCursor.day);
    if (!normalized.contains(dayKey)) {
      break;
    }
    streak += 1;
    dayCursor = dayCursor.subtract(const Duration(days: 1));
  }
  return streak;
}

int _computePoints(List<_ActivitySummary> summaries) {
  return summaries.fold<int>(0, (sum, summary) => sum + summary.points);
}

class _StatChip extends StatelessWidget {
  const _StatChip({
    required this.icon,
    required this.label,
    required this.color,
    this.textStyle,
  });

  final IconData icon;
  final String label;
  final Color color;
  final TextStyle? textStyle;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color(0x1F000000),
            blurRadius: 8,
            offset: Offset(0, 3),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: color),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              label,
              style: textStyle,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
