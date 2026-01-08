import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class FillInTheBlankTaskCard extends StatelessWidget {
  const FillInTheBlankTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final FillInTheBlankTask task;
  final TaskTypeStyle style;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(task, '+35 XP');
    final List<String> blanks = task.missingSegments.isNotEmpty
        ? task.missingSegments
        : ['Answer', 'Answer'];

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFE3F2FC),
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
              task.title.isNotEmpty ? task.title : 'Fill in the blanks',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Complete the tale without breaking your combo.',
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
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    task.text.isNotEmpty
                        ? task.text
                        : '____ <iostream>\nusing namespace ____;',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontFamily: 'Courier',
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 18),
                  ...blanks
                      .map((_) => Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: buildBlankInput(context),
                          ))
                      .toList(),
                ],
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
