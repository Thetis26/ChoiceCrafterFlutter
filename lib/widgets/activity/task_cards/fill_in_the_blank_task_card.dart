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
        List<TextEditingController>.generate(blanks.length, (_) => TextEditingController());
  }

  @override
  void dispose() {
    for (final controller in _controllers) {
      controller.dispose();
    }
    super.dispose();
  }

  void _checkAnswer() {
    final missingSegments = widget.task.missingSegments;
    if (missingSegments.isEmpty) {
      showTaskFeedback(
        context,
        message: 'No correct answers provided for this task yet.',
        isCorrect: false,
      );
      return;
    }

    final responses = _controllers
        .map((controller) => controller.text.trim())
        .toList();
    if (responses.any((entry) => entry.isEmpty)) {
      showTaskFeedback(
        context,
        message: 'Fill in all blanks before checking.',
        isCorrect: false,
      );
      return;
    }

    final normalizedResponses =
        responses.map((entry) => entry.toLowerCase()).toList();
    final normalizedAnswers =
        missingSegments.map((entry) => entry.trim().toLowerCase()).toList();
    final bool isCorrect =
        normalizedResponses.length == normalizedAnswers.length &&
            List.generate(normalizedResponses.length,
                    (index) => normalizedResponses[index] == normalizedAnswers[index])
                .every((match) => match);

    showTaskFeedback(
      context,
      message: isCorrect ? 'Nice! All blanks are correct.' : 'Some blanks are off. Try again!',
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
    final String rewardText = taskRewardText(task, '+35 XP');

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
                  ..._controllers.asMap().entries.map((entry) {
                    final index = entry.key;
                    final controller = entry.value;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: TextField(
                        controller: controller,
                        decoration: InputDecoration(
                          hintText: 'Blank ${index + 1}',
                          filled: true,
                          fillColor: Colors.white,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(16),
                            borderSide: BorderSide(
                              color: Colors.blueGrey.shade200,
                              width: 1.4,
                            ),
                          ),
                          enabledBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(16),
                            borderSide: BorderSide(
                              color: Colors.blueGrey.shade200,
                              width: 1.4,
                            ),
                          ),
                        ),
                      ),
                    );
                  }),
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
