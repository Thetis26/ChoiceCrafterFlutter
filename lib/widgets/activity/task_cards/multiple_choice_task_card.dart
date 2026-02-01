import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class MultipleChoiceTaskCard extends StatefulWidget {
  const MultipleChoiceTaskCard({
    super.key,
    required this.task,
    required this.style,
    this.onAnswerChecked,
  });

  final MultipleChoiceTask task;
  final TaskTypeStyle style;
  final ValueChanged<bool>? onAnswerChecked;

  @override
  State<MultipleChoiceTaskCard> createState() =>
      _MultipleChoiceTaskCardState();
}

class _MultipleChoiceTaskCardState extends State<MultipleChoiceTaskCard> {
  int? _selectedIndex;

  void _checkAnswer() {
    final correctAnswer = widget.task.correctAnswer;
    if (correctAnswer == null) {
      showTaskFeedback(
        context,
        message: 'No correct answer provided for this task yet.',
        isCorrect: false,
      );
      return;
    }
    if (_selectedIndex == null) {
      showTaskFeedback(
        context,
        message: 'Select an answer before checking.',
        isCorrect: false,
      );
      return;
    }
    final bool isCorrect = _selectedIndex == correctAnswer;
    widget.onAnswerChecked?.call(isCorrect);
    showTaskFeedback(
      context,
      message: isCorrect ? 'Correct! Great job.' : 'Not quite. Try again!',
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
    final String rewardText = taskRewardText(task, '+40 XP');
    final List<String> options = task.options.isNotEmpty
        ? task.options
        : ['Option A', 'Option B', 'Option C'];

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFE7E9FF),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            buildTaskHeader(style, rewardText, background: Colors.white),
            const SizedBox(height: 16),
            buildAiHelperBanner(context, task),
            const SizedBox(height: 16),
            Text(
              task.question.isNotEmpty ? task.question : task.title,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Choose the correct answer from the list.',
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
                children: List.generate(options.length, (index) {
                  final option = options[index];
                  final bool isSelected = _selectedIndex == index;
                  return InkWell(
                    onTap: () => setState(() => _selectedIndex = index),
                    borderRadius: BorderRadius.circular(12),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Row(
                        children: [
                          Icon(
                            isSelected
                                ? Icons.radio_button_checked
                                : Icons.radio_button_unchecked,
                            color: style.color,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              option,
                              style: const TextStyle(fontSize: 16),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              helperText,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: Colors.blueGrey.shade500),
            ),
            const SizedBox(height: 16),
            buildTaskActions(
              context,
              onCheckAnswer: _checkAnswer,
              showHintButton: false,
            ),
          ],
        ),
      ),
    );
  }
}
