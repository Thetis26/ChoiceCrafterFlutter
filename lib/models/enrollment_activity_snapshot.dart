class EnrollmentActivitySnapshot {
  const EnrollmentActivitySnapshot({
    required this.activityId,
    required this.courseId,
    required this.userId,
    required this.taskStats,
    this.highestScore,
  });

  final String activityId;
  final String courseId;
  final String userId;
  final Map<String, TaskStats> taskStats;
  final int? highestScore;

  factory EnrollmentActivitySnapshot.fromMap(Map<String, dynamic> map) {
    final rawTaskStats = map['taskStats'];
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
    }
    final highestScore = map['highestScore'];
    return EnrollmentActivitySnapshot(
      activityId: (map['activityId'] as String?) ?? '',
      courseId: (map['courseId'] as String?) ?? '',
      userId: (map['userId'] as String?) ?? '',
      taskStats: parsedTaskStats,
      highestScore: highestScore is num ? highestScore.toInt() : null,
    );
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
}

class TaskStats {
  const TaskStats({
    this.attemptDateTime,
    this.timeSpent,
    this.retries,
    this.success,
    this.hintsUsed,
    this.completionRatio,
    this.scoreRatio,
  });

  final String? attemptDateTime;
  final String? timeSpent;
  final int? retries;
  final bool? success;
  final bool? hintsUsed;
  final double? completionRatio;
  final double? scoreRatio;

  factory TaskStats.fromMap(Map<String, dynamic> map) {
    return TaskStats(
      attemptDateTime: map['attemptDateTime'] as String?,
      timeSpent: map['timeSpent']?.toString(),
      retries: map['retries'] is num ? (map['retries'] as num).toInt() : null,
      success: map['success'] as bool?,
      hintsUsed: map['hintsUsed'] as bool?,
      completionRatio: _toDouble(map['completionRatio']),
      scoreRatio: _toDouble(map['scoreRatio']),
    );
  }

  DateTime? attemptDateTimeParsed() {
    if (attemptDateTime == null || attemptDateTime!.isEmpty) {
      return null;
    }
    return DateTime.tryParse(attemptDateTime!);
  }

  double resolveCompletionRatio() {
    if (completionRatio != null) {
      return completionRatio!.clamp(0.0, 1.0);
    }
    if (success == true) {
      return 1.0;
    }
    return 0.0;
  }

  double resolveScoreRatio() {
    if (scoreRatio != null) {
      return scoreRatio!.clamp(0.0, 1.0);
    }
    return resolveCompletionRatio();
  }

  bool isCompleted() {
    if (success == true) {
      return true;
    }
    final completion = resolveCompletionRatio();
    if (completion >= 1.0) {
      return true;
    }
    if (completion > 0.0) {
      if (scoreRatio == null) {
        return true;
      }
      if (scoreRatio! > 0.0) {
        return true;
      }
    }
    if (scoreRatio != null) {
      return scoreRatio! > 0.0;
    }
    return false;
  }
}

double? _toDouble(Object? value) {
  if (value is num) {
    return value.toDouble();
  }
  if (value is String) {
    return double.tryParse(value);
  }
  return null;
}
