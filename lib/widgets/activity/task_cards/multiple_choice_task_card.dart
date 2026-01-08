import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class MultipleChoiceTaskCard extends StatefulWidget {
  const MultipleChoiceTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final MultipleChoiceTask task;
  final TaskTypeStyle style;

  @override
  State<MultipleChoiceTaskCard> createState() =>
      _MultipleChoiceTaskCardState();
}

class _MultipleChoiceTaskCardState extends State<MultipleChoiceTaskCard> {
  int? _selectedIndex;

  void _showSnack(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _checkAnswer() {
    if (_selectedIndex == null) {
      _showSnack('Select an answer before checking.');
      return;
    }
    final correctIndex = widget.task.correctAnswer;
    if (correctIndex == null) {
      _showSnack('No correct answer configured yet.');
      return;
    }
    final isCorrect = _selectedIndex == correctIndex;
    _showSnack(isCorrect ? 'Correct! Nice work.' : 'Not quite. Try again.');
  }

  void _showHint() {
    final hint = widget.task.explanation?.trim();
    _showSnack((hint != null && hint.isNotEmpty)
        ? hint
        : 'Review the question and eliminate the obvious distractors.');
  }

  @override
  Widget build(BuildContext context) {
    final style = widget.style;
    final theme = Theme.of(context);
    final String helperText = (widget.task.explanation != null &&
            widget.task.explanation!.trim().isNotEmpty)
        ? widget.task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(widget.task, '+40 XP');
    final options = widget.task.options.isNotEmpty
        ? widget.task.options
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
            const SizedBox(height: 18),
            Text(
              widget.task.question.isNotEmpty
                  ? widget.task.question
                  : widget.task.title,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.task.description.isNotEmpty
                  ? widget.task.description
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
                  return buildChoiceOption(
                    option,
                    style.color,
                    selected: _selectedIndex == index,
                    onTap: () => setState(() => _selectedIndex = index),
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
              onHint: _showHint,
              onCheck: _checkAnswer,
            ),
          ],
        ),
      ),
    );
  }
}
