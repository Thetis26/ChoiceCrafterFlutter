import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class FillInTheBlankTaskCard extends StatefulWidget {
  const FillInTheBlankTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final FillInTheBlankTask task;
  final TaskTypeStyle style;

  @override
  State<FillInTheBlankTaskCard> createState() =>
      _FillInTheBlankTaskCardState();
}

class _FillInTheBlankTaskCardState extends State<FillInTheBlankTaskCard> {
  late final List<TextEditingController> _controllers;

  @override
  void initState() {
    super.initState();
    final blanks = widget.task.missingSegments.isNotEmpty
        ? widget.task.missingSegments
        : ['Answer', 'Answer'];
    _controllers =
        List.generate(blanks.length, (_) => TextEditingController());
  }

  @override
  void dispose() {
    for (final controller in _controllers) {
      controller.dispose();
    }
    super.dispose();
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _checkAnswer() {
    final correctAnswers = widget.task.missingSegments;
    if (correctAnswers.isEmpty) {
      _showSnack('No correct answers configured yet.');
      return;
    }
    final answers = _controllers
        .map((controller) => controller.text.trim())
        .toList();
    if (answers.any((answer) => answer.isEmpty)) {
      _showSnack('Fill in all blanks before checking.');
      return;
    }
    final isCorrect = answers.length == correctAnswers.length &&
        List.generate(answers.length, (index) {
          return answers[index].toLowerCase() ==
              correctAnswers[index].trim().toLowerCase();
        }).every((match) => match);
    _showSnack(isCorrect ? 'Great job! All blanks are correct.' : 'Not yet. Try again.');
  }

  void _showHint() {
    final hint = widget.task.explanation?.trim();
    _showSnack((hint != null && hint.isNotEmpty)
        ? hint
        : 'Check the context around each blank for clues.');
  }

  @override
  Widget build(BuildContext context) {
    final style = widget.style;
    final theme = Theme.of(context);
    final String helperText = (widget.task.explanation != null &&
            widget.task.explanation!.trim().isNotEmpty)
        ? widget.task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(widget.task, '+35 XP');
    final List<String> blanks = widget.task.missingSegments.isNotEmpty
        ? widget.task.missingSegments
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
              widget.task.title.isNotEmpty
                  ? widget.task.title
                  : 'Fill in the blanks',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.task.description.isNotEmpty
                  ? widget.task.description
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
                    widget.task.text.isNotEmpty
                        ? widget.task.text
                        : '____ <iostream>\nusing namespace ____;',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontFamily: 'Courier',
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 18),
                  ...List.generate(
                    blanks.length,
                    (index) => Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: buildBlankInputField(
                        controller: _controllers[index],
                        hintText: 'Blank ${index + 1}',
                      ),
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
