import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class CodingChallengeTaskCard extends StatefulWidget {
  const CodingChallengeTaskCard({
    super.key,
    required this.task,
    required this.style,
    this.onAnswerChecked,
  });

  final CodingChallengeTask task;
  final TaskTypeStyle style;
  final ValueChanged<bool>? onAnswerChecked;

  @override
  State<CodingChallengeTaskCard> createState() =>
      _CodingChallengeTaskCardState();
}

class _CodingChallengeTaskCardState extends State<CodingChallengeTaskCard> {
  late String _selectedLanguage;
  late final TextEditingController _codeController;
  late final TextEditingController _inputController;

  @override
  void initState() {
    super.initState();
    _selectedLanguage = widget.task.defaultLanguage.isNotEmpty
        ? widget.task.defaultLanguage
        : _fallbackLanguage();
    _codeController = TextEditingController(
      text: _starterCodeForLanguage(_selectedLanguage),
    );
    _inputController = TextEditingController();
  }

  @override
  void dispose() {
    _codeController.dispose();
    _inputController.dispose();
    super.dispose();
  }

  String _starterCodeForLanguage(String language) {
    return widget.task.starterCodeByLanguage[language] ??
        widget.task.starterCodeByLanguage.values.firstOrNull ??
        '#include <iostream>\nusing namespace std;\n\nint main() {\n\n  return 0;\n}';
  }

  void _checkAnswer() {
    final solution = widget.task.solutionCodeByLanguage[_selectedLanguage] ??
        widget.task.solutionCodeByLanguage.values.firstOrNull;
    if (solution == null || solution.trim().isEmpty) {
      showTaskFeedback(
        context,
        message: 'No solution provided for this challenge yet.',
        isCorrect: false,
      );
      return;
    }
    final submitted = _codeController.text.trim();
    if (submitted.isEmpty) {
      showTaskFeedback(
        context,
        message: 'Add your code before checking.',
        isCorrect: false,
      );
      return;
    }

    final bool isCorrect = submitted == solution.trim();
    widget.onAnswerChecked?.call(isCorrect);
    showTaskFeedback(
      context,
      message: isCorrect ? 'Solution matches!' : 'Keep iterating on your code.',
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
              task.title.isNotEmpty ? task.title : 'Coding challenge',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.problemDescription.isNotEmpty
                  ? task.problemDescription
                  : task.description,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.blueGrey.shade600),
            ),
            if (task.expectedOutputDescription.isNotEmpty) ...[
              const SizedBox(height: 14),
              Text(
                'What we expect from the solution',
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: Colors.blueGrey.shade800,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                task.expectedOutputDescription,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: Colors.blueGrey.shade600),
              ),
            ],
            if (task.examples.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text(
                'Input/Output examples',
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: Colors.blueGrey.shade800,
                ),
              ),
              const SizedBox(height: 8),
              ...task.examples.map(_buildExampleCard),
            ],
            const SizedBox(height: 16),
            Text(
              'Choose language',
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade800,
              ),
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              isExpanded: true,
              value: _selectedLanguage,
              items: _availableLanguages()
                  .map(
                    (language) => DropdownMenuItem(
                      value: language,
                      child: Text(language),
                    ),
                  )
                  .toList(),
              onChanged: (value) {
                if (value == null) {
                  return;
                }
                setState(() {
                  _selectedLanguage = value;
                  _codeController.text = _starterCodeForLanguage(value);
                });
              },
              decoration: InputDecoration(
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: BorderSide(color: Colors.blueGrey.shade200),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Code editor',
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade800,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _codeController,
              maxLines: 10,
              style: theme.textTheme.bodyMedium?.copyWith(
                fontFamily: 'Courier',
                height: 1.4,
              ),
              decoration: InputDecoration(
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(18),
                  borderSide: BorderSide(color: Colors.blueGrey.shade200),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Input data to run (optional)',
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade800,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _inputController,
              maxLines: 3,
              decoration: InputDecoration(
                hintText: 'Enter the input you want to send when running.',
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(18),
                  borderSide: BorderSide(color: Colors.blueGrey.shade200),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Align(
              alignment: Alignment.centerLeft,
              child: ElevatedButton(
                onPressed: () {},
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF6E7BF2),
                  foregroundColor: Colors.white,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                  textStyle: const TextStyle(fontWeight: FontWeight.w600),
                ),
                child: const Text('Run code'),
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

  Widget _buildExampleCard(CodingChallengeExample example) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFE0E4FF)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Input',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            Text(example.input),
            const SizedBox(height: 8),
            const Text(
              'Output',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            Text(example.output),
            if (example.explanation != null &&
                example.explanation!.trim().isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                example.explanation!.trim(),
                style: const TextStyle(color: Color(0xFF6B7280)),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _fallbackLanguage() {
    if (widget.task.starterCodeByLanguage.keys.isNotEmpty) {
      return widget.task.starterCodeByLanguage.keys.first;
    }
    if (widget.task.solutionCodeByLanguage.keys.isNotEmpty) {
      return widget.task.solutionCodeByLanguage.keys.first;
    }
    return 'C++';
  }

  List<String> _availableLanguages() {
    final languages = <String>{
      ...widget.task.starterCodeByLanguage.keys,
      ...widget.task.solutionCodeByLanguage.keys,
      _selectedLanguage,
    };
    return languages.where((language) => language.trim().isNotEmpty).toList()
      ..sort();
  }
}

extension on Iterable<String> {
  String? get firstOrNull => isEmpty ? null : first;
}
