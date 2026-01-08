import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class SpotTheErrorTaskCard extends StatefulWidget {
  const SpotTheErrorTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final SpotTheErrorTask task;
  final TaskTypeStyle style;

  @override
  State<SpotTheErrorTaskCard> createState() => _SpotTheErrorTaskCardState();
}

class _SpotTheErrorTaskCardState extends State<SpotTheErrorTaskCard> {
  int? _selectedIndex;

  void _showSnack(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _checkAnswer() {
    if (_selectedIndex == null) {
      _showSnack('Select an option before checking.');
      return;
    }
    final correctIndex = widget.task.correctOptionIndex;
    if (correctIndex == null) {
      _showSnack('No correct option configured yet.');
      return;
    }
    _showSnack(_selectedIndex == correctIndex
        ? 'Correct! You spotted it.'
        : 'That is not the right fix. Try again.');
  }

  void _showHint() {
    final hint = widget.task.explanation?.trim();
    _showSnack((hint != null && hint.isNotEmpty)
        ? hint
        : 'Look for syntax and missing punctuation in the snippet.');
  }

  @override
  Widget build(BuildContext context) {
    final style = widget.style;
    final theme = Theme.of(context);
    final String helperText = (widget.task.explanation != null &&
            widget.task.explanation!.trim().isNotEmpty)
        ? widget.task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(widget.task, '+45 XP');
    final options = widget.task.options.isNotEmpty
        ? widget.task.options
        : [
            'Add # to include, add () to main, and add return 0;',
            'Remove the cout line and keep the rest unchanged.',
            'Replace cin with scanf for input.',
          ];

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
              widget.task.title.isNotEmpty ? widget.task.title : 'Spot the error',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.task.description.isNotEmpty
                  ? widget.task.description
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
                widget.task.codeSnippet.isNotEmpty
                    ? widget.task.codeSnippet
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
