import 'package:flutter/material.dart';

import '../../../models/task.dart';

class TaskTypeStyle {
  const TaskTypeStyle({
    required this.label,
    required this.icon,
    required this.color,
  });

  final String label;
  final IconData icon;
  final Color color;
}

TaskTypeStyle taskTypeStyle(Task task, BuildContext context) {
  if (task is MultipleChoiceTask) {
    return const TaskTypeStyle(
      label: 'Multiple Choice',
      icon: Icons.local_fire_department,
      color: Colors.indigo,
    );
  }
  if (task is FillInTheBlankTask) {
    return const TaskTypeStyle(
      label: 'Fill in the Blank',
      icon: Icons.lightbulb_outline,
      color: Colors.blue,
    );
  }
  if (task is MatchingPairTask) {
    return const TaskTypeStyle(
      label: 'Matching Pairs',
      icon: Icons.compare_arrows,
      color: Colors.green,
    );
  }
  if (task is OrderingTask) {
    return const TaskTypeStyle(
      label: 'Ordering',
      icon: Icons.checklist,
      color: Colors.indigo,
    );
  }
  if (task is TrueFalseTask) {
    return const TaskTypeStyle(
      label: 'True / False',
      icon: Icons.rule,
      color: Colors.blueGrey,
    );
  }
  if (task is SpotTheErrorTask) {
    return const TaskTypeStyle(
      label: 'Spot the Error',
      icon: Icons.smart_display,
      color: Colors.deepOrange,
    );
  }
  if (task is CodingChallengeTask) {
    return const TaskTypeStyle(
      label: 'Coding Challenge',
      icon: Icons.code,
      color: Colors.teal,
    );
  }
  if (task is InfoCardTask) {
    return const TaskTypeStyle(
      label: 'Info Card',
      icon: Icons.info_outline,
      color: Colors.blue,
    );
  }
  return TaskTypeStyle(
    label: task.type,
    icon: Icons.assignment,
    color: Theme.of(context).colorScheme.primary,
  );
}
