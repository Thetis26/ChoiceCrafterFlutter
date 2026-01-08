import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class TrueFalseTaskCard extends StatefulWidget {
  const TrueFalseTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final TrueFalseTask task;
  final TaskTypeStyle style;

  @override
  State<TrueFalseTaskCard> createState() => _TrueFalseTaskCardState();
}

class _TrueFalseTaskCardState extends State<TrueFalseTaskCard> {
  bool? _selectedValue;

  void _checkAnswer() {
    if (widget.task.correctAnswer == null) {
      showTaskFeedback(
        context,
        message: 'No correct answer provided for this task yet.',
        isCorrect: false,
      );
      return;
    }
    if (_selectedValue == null) {
      showTaskFeedback(
        context,
        message: 'Choose true or false before checking.',
        isCorrect: false,
      );
      return;
    }
    final bool isCorrect = _selectedValue == widget.task.correctAnswer;
    showTaskFeedback(
      context,
      message: isCorrect ? 'Correct!' : 'That is not correct. Try again.',
      isCorrect: isCorrect,
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final task = widget.task;
    final style = widget.style;
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String titleText =
        task.statement.isNotEmpty ? task.statement : task.title;
    final String subtitleText = task.description.isNotEmpty
        ? task.description
        : 'Decide if the statement below is correct.';
    final String rewardText = task.status.isNotEmpty ? task.status : '+25 XP';

    Widget buildOption(String label, bool value) {
      final bool isSelected = _selectedValue == value;
      return InkWell(
        onTap: () => setState(() => _selectedValue = value),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Icon(
                isSelected
                    ? Icons.radio_button_checked
                    : Icons.radio_button_unchecked,
                color: style.color,
              ),
              const SizedBox(width: 12),
              Text(label, style: const TextStyle(fontSize: 16)),
            ],
          ),
        ),
      );
    }

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFE9EDFF),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(style.icon, color: style.color, size: 20),
                ),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      style.label.toUpperCase(),
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: Colors.blueGrey.shade700,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.6,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      rewardText,
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: Colors.blueGrey.shade400),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 18),
            Text(
              titleText,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              subtitleText,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.blueGrey.shade600),
            ),
            const SizedBox(height: 16),
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                children: [
                  buildOption('True', true),
                  const Divider(height: 1),
                  buildOption('False', false),
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
            buildTaskActions(context, onCheckAnswer: _checkAnswer),
          ],
        ),
      ),
    );
  }
}
