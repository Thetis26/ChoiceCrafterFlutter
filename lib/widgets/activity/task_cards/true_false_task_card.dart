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
  bool? _selectedAnswer;

  void _showSnack(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _checkAnswer() {
    if (_selectedAnswer == null) {
      _showSnack('Pick true or false before checking.');
      return;
    }
    final correctAnswer = widget.task.correctAnswer;
    if (correctAnswer == null) {
      _showSnack('No correct answer configured yet.');
      return;
    }
    _showSnack(_selectedAnswer == correctAnswer
        ? 'Correct! Well done.'
        : 'That is not correct. Try again.');
  }

  void _showHint() {
    final hint = widget.task.explanation?.trim();
    _showSnack(
      (hint != null && hint.isNotEmpty)
          ? hint
          : 'Look for clues in the statement.',
    );
  }

  @override
  Widget build(BuildContext context) {
    final style = widget.style;
    final theme = Theme.of(context);
    final String helperText = (widget.task.explanation != null &&
            widget.task.explanation!.trim().isNotEmpty)
        ? widget.task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String titleText =
        widget.task.statement.isNotEmpty ? widget.task.statement : widget.task.title;
    final String subtitleText = widget.task.description.isNotEmpty
        ? widget.task.description
        : 'Decide if the statement below is correct.';
    final String rewardText =
        widget.task.status.isNotEmpty ? widget.task.status : '+25 XP';

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
                  buildTrueFalseOption(
                    'True',
                    style.color,
                    selected: _selectedAnswer == true,
                    onTap: () => setState(() => _selectedAnswer = true),
                  ),
                  const Divider(height: 1),
                  buildTrueFalseOption(
                    'False',
                    style.color,
                    selected: _selectedAnswer == false,
                    onTap: () => setState(() => _selectedAnswer = false),
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
