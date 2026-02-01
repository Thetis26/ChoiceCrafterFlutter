import 'task_stats.dart';

class EnrollmentActivityProgress {
  EnrollmentActivityProgress({
    required this.courseId,
    required this.activityId,
    required this.userId,
    Map<String, TaskStats>? taskStats,
    this.highestScore,
  }) : taskStats = taskStats ?? <String, TaskStats>{};

  String courseId;
  String activityId;
  String userId;
  Map<String, TaskStats> taskStats;
  int? highestScore;

  factory EnrollmentActivityProgress.empty() {
    return EnrollmentActivityProgress(
      courseId: '',
      activityId: '',
      userId: '',
    );
  }

  factory EnrollmentActivityProgress.fromMap(Map<String, dynamic>? map) {
    if (map == null) {
      return EnrollmentActivityProgress.empty();
    }
    final rawTaskStats = map['taskStats'];
    final parsedTaskStats = _parseTaskStats(rawTaskStats);
    final highestScore = map['highestScore'];
    return EnrollmentActivityProgress(
      activityId: (map['activityId'] as String?) ?? '',
      courseId: (map['courseId'] as String?) ?? '',
      userId: (map['userId'] as String?) ?? '',
      taskStats: parsedTaskStats,
      highestScore: highestScore is num ? highestScore.toInt() : null,
    );
  }

  Map<String, dynamic> toMap() {
    final taskStatsMap = <String, dynamic>{};
    for (final entry in taskStats.entries) {
      taskStatsMap[entry.key] = entry.value.toMap();
    }
    final map = <String, dynamic>{
      'courseId': courseId,
      'activityId': activityId,
      'userId': userId,
      'taskStats': taskStatsMap,
    };
    if (highestScore != null) {
      map['highestScore'] = highestScore;
    }
    return map;
  }

  DateTime? latestAttempt() {
    DateTime? latest;
    for (final stats in taskStats.values) {
      final attempt = stats.attemptDateTimeParsed();
      if (attempt == null) {
        continue;
      }
      if (latest == null || attempt.isAfter(latest)) {
        latest = attempt;
      }
    }
    return latest;
  }

  double averageCompletionRatio() {
    if (taskStats.isEmpty) {
      return 0;
    }
    final total = taskStats.values
        .map((stats) => stats.resolveCompletionRatio())
        .fold<double>(0, (sum, value) => sum + value);
    return total / taskStats.length;
  }

  double averageScoreRatio() {
    if (taskStats.isEmpty) {
      return 0;
    }
    final total = taskStats.values
        .map((stats) => stats.resolveScoreRatio())
        .fold<double>(0, (sum, value) => sum + value);
    return total / taskStats.length;
  }

  int completedTaskCount() {
    return taskStats.values.where((stats) => stats.isCompleted()).length;
  }

  static Map<String, TaskStats> _parseTaskStats(Object? rawTaskStats) {
    final parsedTaskStats = <String, TaskStats>{};
    if (rawTaskStats is Map) {
      for (final entry in rawTaskStats.entries) {
        final key = entry.key?.toString();
        if (key == null) {
          continue;
        }
        if (entry.value is Map) {
          parsedTaskStats[key] =
              TaskStats.fromMap(Map<String, dynamic>.from(entry.value as Map));
        }
      }
    } else if (rawTaskStats is List) {
      for (var index = 0; index < rawTaskStats.length; index += 1) {
        final entry = rawTaskStats[index];
        if (entry is Map) {
          final entryMap = Map<String, dynamic>.from(entry);
          final key = entryMap['taskId']?.toString() ??
              entryMap['id']?.toString() ??
              index.toString();
          parsedTaskStats[key] = TaskStats.fromMap(entryMap);
        }
      }
    }
    return parsedTaskStats;
  }
}
