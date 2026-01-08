import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class CodingChallengeTaskCard extends StatelessWidget {
  const CodingChallengeTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final CodingChallengeTask task;
  final TaskTypeStyle style;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(task, '+40 XP');
    final String language =
        task.defaultLanguage.isNotEmpty ? task.defaultLanguage : _fallbackLanguage();
    final String starterCode = task.starterCodeByLanguage[language] ??
        task.starterCodeByLanguage.values.firstOrNull ??
        '#include <iostream>\nusing namespace std;\n\nint main() {\n\n  return 0;\n}';

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
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.blueGrey.shade200),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      language,
                      style: theme.textTheme.bodyMedium,
                    ),
                  ),
                  const Icon(Icons.expand_more),
                ],
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
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Text(
                starterCode,
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontFamily: 'Courier',
                  height: 1.4,
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
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: Colors.blueGrey.shade200),
              ),
              child: Text(
                'Enter the input you want to send when running.',
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: Colors.blueGrey.shade500),
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
            Row(
              children: [
                Expanded(child: _buildActionButton('Hint')),
                const SizedBox(width: 12),
                Expanded(child: _buildActionButton('Show solution')),
                const SizedBox(width: 12),
                Expanded(child: _buildActionButton('Check Answer')),
              ],
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

  Widget _buildActionButton(String label) {
    return ElevatedButton(
      onPressed: () {},
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF6E7BF2),
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 14),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
        ),
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
      ),
      child: Text(label, textAlign: TextAlign.center),
    );
  }

  String _fallbackLanguage() {
    if (task.starterCodeByLanguage.keys.isNotEmpty) {
      return task.starterCodeByLanguage.keys.first;
    }
    if (task.solutionCodeByLanguage.keys.isNotEmpty) {
      return task.solutionCodeByLanguage.keys.first;
    }
    return 'C++';
  }
}

extension on Iterable<String> {
  String? get firstOrNull => isEmpty ? null : first;
}
