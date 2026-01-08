import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class SpotTheErrorTaskCard extends StatelessWidget {
  const SpotTheErrorTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final SpotTheErrorTask task;
  final TaskTypeStyle style;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(task, '+45 XP');

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFFBE7D5),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            buildTaskHeader(style, rewardText, background: Colors.white),
            const SizedBox(height: 18),
            Text(
              task.title.isNotEmpty ? task.title : 'Spot the error',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Choose the correct fix for the code snippet.',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.blueGrey.shade600),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Text(
                task.codeSnippet.isNotEmpty
                    ? task.codeSnippet
                    : 'include <iostream>\nint main {\n  cout << "Salut!"\n}',
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontFamily: 'Courier',
                  height: 1.4,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Column(
                children: (task.options.isNotEmpty
                        ? task.options
                        : [
                            'Add # to include, add () to main, and add return 0;',
                            'Remove the cout line and keep the rest unchanged.',
                            'Replace cin with scanf for input.',
                          ])
                    .map((option) => buildChoiceOption(option, style.color))
                    .toList(),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              helperText,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: Colors.blueGrey.shade500),
            ),
            const SizedBox(height: 16),
            buildTaskActions(context),
          ],
        ),
      ),
    );
  }
}
