import 'module_progress.dart';

class CourseProgress {
  CourseProgress({
    required this.totalActivities,
    required this.activitiesStarted,
    required this.totalTasks,
    required this.attemptedTasks,
    required this.completedTasks,
    required this.completionPercentage,
    this.activitySnapshots = const [],
    this.moduleProgress = const {},
    this.earnedXp = 0,
    this.totalXp = 0,
  });

  final int totalActivities;
  final int activitiesStarted;
  final int totalTasks;
  final int attemptedTasks;
  final int completedTasks;
  final double completionPercentage;
  final List<Map<String, Object>> activitySnapshots;
  final Map<String, ModuleProgress> moduleProgress;
  final int earnedXp;
  final int totalXp;

  Map<String, Object> toMap() {
    final moduleProgressMap = <String, Object>{};
    for (final entry in moduleProgress.entries) {
      moduleProgressMap[entry.key] = {
        'completedTasks': entry.value.completedTasks,
        'totalTasks': entry.value.totalTasks,
        'completionPercentage': entry.value.completionPercentage,
      };
    }
    return {
      'totalActivities': totalActivities,
      'activitiesStarted': activitiesStarted,
      'totalTasks': totalTasks,
      'attemptedTasks': attemptedTasks,
      'completedTasks': completedTasks,
      'completionPercentage': completionPercentage,
      'activitySnapshots': List<Map<String, Object>>.from(activitySnapshots),
      'earnedXp': earnedXp,
      'totalXp': totalXp,
      'moduleProgress': moduleProgressMap,
    };
  }
}
