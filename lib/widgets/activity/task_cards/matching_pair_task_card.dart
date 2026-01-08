import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class MatchingPairTaskCard extends StatelessWidget {
  const MatchingPairTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final MatchingPairTask task;
  final TaskTypeStyle style;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(task, '+45 XP');
    final List<String> leftItems =
        task.leftItems.isNotEmpty ? task.leftItems : ['Spring', 'React'];
    final List<String> rightItems =
        task.rightItems.isNotEmpty ? task.rightItems : ['Frontend', 'Backend'];

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
              task.title.isNotEmpty ? task.title : 'Match the pairs',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Match partners to keep the synergy meter full.',
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
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      children: leftItems
                          .map((item) => _buildMatchPill(item))
                          .toList(),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      children: rightItems
                          .map((item) => _buildMatchPill(item))
                          .toList(),
                    ),
                  ),
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

  Widget _buildMatchPill(String label) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: const Color(0xFFFFE6D1),
          borderRadius: BorderRadius.circular(18),
        ),
        child: Text(
          label,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontWeight: FontWeight.w600,
            color: Color(0xFF4A4A4A),
          ),
        ),
      ),
    );
  }
}
