import 'package:flutter/material.dart';

import '../../../models/task.dart';
import '../../../services/open_ai_hints_service.dart';
import 'task_type_style.dart';

String taskRewardText(Task task, String fallback) {
  return task.status.isNotEmpty ? task.status : fallback;
}

Widget buildTaskHeader(
  TaskTypeStyle style,
  String rewardText, {
  required Color background,
}) {
  return Row(
    children: [
      Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: background,
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
            style: TextStyle(
              color: Colors.blueGrey.shade700,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.6,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            rewardText,
            style: TextStyle(color: Colors.blueGrey.shade400),
          ),
        ],
      ),
    ],
  );
}

void showTaskFeedback(
  BuildContext context, {
  required String message,
  required bool isCorrect,
}) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(message),
      backgroundColor: isCorrect ? const Color(0xFF16A34A) : const Color(0xFFDC2626),
    ),
  );
}

Widget buildTaskActions(
  BuildContext context, {
  VoidCallback? onHint,
  VoidCallback? onCheckAnswer,
}) {
  final Color buttonColor = const Color(0xFF6E7BF2);
  return Row(
    children: [
      Expanded(
        child: ElevatedButton(
          onPressed: onHint,
          style: ElevatedButton.styleFrom(
            backgroundColor: buttonColor,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            textStyle: const TextStyle(fontWeight: FontWeight.w600),
          ),
          child: const Text('Hint'),
        ),
      ),
      const SizedBox(width: 16),
      Expanded(
        child: ElevatedButton(
          onPressed: onCheckAnswer,
          style: ElevatedButton.styleFrom(
            backgroundColor: buttonColor,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            textStyle: const TextStyle(fontWeight: FontWeight.w600),
          ),
          child: const Text('Check Answer'),
        ),
      ),
    ],
  );
}

Future<void> showTaskHintDialog(
  BuildContext context, {
  required Task task,
}) async {
  await showDialog(
    context: context,
    builder: (context) => _TaskHintDialog(task: task),
  );
}

Future<void> showTaskFinalAnswerDialog(
  BuildContext context, {
  required Task task,
  String? preferredLanguage,
}) async {
  await showDialog(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Final answer'),
      content: SingleChildScrollView(
        child: SelectableText(
          taskFinalAnswer(task, preferredLanguage: preferredLanguage),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Close'),
        ),
      ],
    ),
  );
}

String taskFinalAnswer(Task task, {String? preferredLanguage}) {
  if (task is MultipleChoiceTask) {
    final index = task.correctAnswer;
    if (index == null || index < 0 || index >= task.options.length) {
      return 'No final answer provided for this task yet.';
    }
    return task.options[index];
  }
  if (task is FillInTheBlankTask) {
    if (task.missingSegments.isEmpty) {
      return 'No final answer provided for this task yet.';
    }
    return task.missingSegments.join(', ');
  }
  if (task is MatchingPairTask) {
    if (task.correctMatches.isEmpty) {
      return 'No final answer provided for this task yet.';
    }
    final entries = task.correctMatches.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));
    return entries.map((entry) => '${entry.key} → ${entry.value}').join('\n');
  }
  if (task is OrderingTask) {
    final items = task.items;
    if (items.isEmpty) {
      return 'No final answer provided for this task yet.';
    }
    final order = task.correctOrder;
    if (order.isEmpty) {
      return items.join(' → ');
    }
    final orderedItems = <String>[];
    for (final index in order) {
      if (index >= 0 && index < items.length) {
        orderedItems.add(items[index]);
      }
    }
    return orderedItems.isEmpty ? items.join(' → ') : orderedItems.join(' → ');
  }
  if (task is TrueFalseTask) {
    final answer = task.correctAnswer;
    if (answer == null) {
      return 'No final answer provided for this task yet.';
    }
    return answer ? 'True' : 'False';
  }
  if (task is SpotTheErrorTask) {
    final index = task.correctOptionIndex;
    if (index == null || index < 0 || index >= task.options.length) {
      return 'No final answer provided for this task yet.';
    }
    return task.options[index];
  }
  if (task is CodingChallengeTask) {
    final normalizedPreferred = preferredLanguage?.trim();
    final language = (normalizedPreferred != null &&
            task.solutionCodeByLanguage.containsKey(normalizedPreferred))
        ? normalizedPreferred
        : (task.defaultLanguage.isNotEmpty &&
                task.solutionCodeByLanguage.containsKey(task.defaultLanguage)
            ? task.defaultLanguage
            : task.solutionCodeByLanguage.keys.firstOrNull);
    final solution = language != null
        ? task.solutionCodeByLanguage[language]
        : task.solutionCodeByLanguage.values.firstOrNull;
    if (solution == null || solution.trim().isEmpty) {
      return 'No final answer provided for this task yet.';
    }
    final languageLabel = language != null ? 'Language: $language\n' : '';
    return '$languageLabel$solution';
  }
  return 'No final answer provided for this task yet.';
}

Widget buildTrueFalseOption(String label, Color accentColor) {
  return Padding(
    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    child: Row(
      children: [
        Icon(Icons.radio_button_unchecked, color: accentColor),
        const SizedBox(width: 12),
        Text(label, style: const TextStyle(fontSize: 16)),
      ],
    ),
  );
}

class _TaskHintDialog extends StatefulWidget {
  const _TaskHintDialog({required this.task});

  final Task task;

  @override
  State<_TaskHintDialog> createState() => _TaskHintDialogState();
}

class _TaskHintDialogState extends State<_TaskHintDialog> {
  final OpenAiHintsService _service = OpenAiHintsService();

  bool _loading = true;
  String? _hint;
  String? _errorMessage;
  bool _showFinalAnswer = false;

  @override
  void initState() {
    super.initState();
    _fetchHint();
  }

  Future<void> _fetchHint() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
      _hint = null;
    });
    try {
      final hint = await _service.generateHint(task: widget.task);
      if (!mounted) {
        return;
      }
      setState(() {
        _hint = hint;
        _loading = false;
      });
    } on OpenAiMissingKeyException {
      if (!mounted) {
        return;
      }
      setState(() {
        _loading = false;
        _errorMessage = 'Add your OPEN_AI_KEY to generate AI hints.';
      });
    } on OpenAiRequestException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _loading = false;
        _errorMessage = error.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _loading = false;
        _errorMessage = 'Unable to load a hint right now.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Hint'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_loading)
              const Center(child: CircularProgressIndicator())
            else if (_errorMessage != null)
              Text(
                _errorMessage!,
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: Colors.redAccent),
              )
            else if (_hint != null)
              SelectableText(_hint!),
            if (_showFinalAnswer) ...[
              const SizedBox(height: 16),
              Text(
                'Final answer',
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
              ),
              const SizedBox(height: 8),
              SelectableText(taskFinalAnswer(widget.task)),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _loading ? null : _fetchHint,
          child: const Text('Regenerate hint'),
        ),
        TextButton(
          onPressed: _showFinalAnswer
              ? null
              : () {
                  setState(() {
                    _showFinalAnswer = true;
                  });
                },
          child: const Text('Show final answer'),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Close'),
        ),
      ],
    );
  }
}

extension on Iterable<String> {
  String? get firstOrNull => isEmpty ? null : first;
}

Widget buildOrderingItem(String label) {
  return Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
      decoration: BoxDecoration(
        color: const Color(0xFFBFCBF9),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          const Icon(Icons.emoji_events, color: Color(0xFFFFD166)),
        ],
      ),
    ),
  );
}

Widget buildBlankInput(BuildContext context) {
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
    decoration: BoxDecoration(
      borderRadius: BorderRadius.circular(16),
      border: Border.all(color: Colors.blueGrey.shade200, width: 1.4),
    ),
    child: const Text(
      'Type your answer to reveal +10 XP',
      style: TextStyle(fontWeight: FontWeight.w600),
    ),
  );
}

Widget buildChoiceOption(String label, Color accentColor) {
  return Padding(
    padding: const EdgeInsets.symmetric(vertical: 8),
    child: Row(
      children: [
        Icon(Icons.radio_button_unchecked, color: accentColor),
        const SizedBox(width: 12),
        Expanded(
          child: Text(label, style: const TextStyle(fontSize: 16)),
        ),
      ],
    ),
  );
}
