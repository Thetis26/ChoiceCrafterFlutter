class ModuleProgress {
  ModuleProgress({
    required this.completedTasks,
    required this.totalTasks,
  });

  final int completedTasks;
  final int totalTasks;

  double get completionPercentage {
    if (totalTasks == 0) {
      return 0;
    }
    return (completedTasks * 100) / totalTasks;
  }

  bool get isCompleted => totalTasks > 0 && completedTasks >= totalTasks;
}
