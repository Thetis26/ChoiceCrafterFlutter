import 'package:flutter/material.dart';

import '../../../models/task.dart';
import '../../../services/ai_hint_service.dart';
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
  bool showHintButton = true,
}) {
  final Color buttonColor = const Color(0xFF6E7BF2);
  if (!showHintButton) {
    return SizedBox(
      width: double.infinity,
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
    );
  }
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

Widget buildAiHelperBanner(BuildContext context, Task task) {
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
    decoration: BoxDecoration(
      color: Colors.white.withOpacity(0.9),
      borderRadius: BorderRadius.circular(14),
      border: Border.all(color: Colors.blueGrey.shade100),
    ),
    child: Row(
      children: [
        Icon(Icons.auto_awesome, color: Colors.deepPurple.shade400, size: 20),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            'AI Helper is ready with a hint if you need a nudge.',
            style: TextStyle(
              color: Colors.blueGrey.shade700,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        TextButton(
          onPressed: () => showAiHintDialog(context, task),
          style: TextButton.styleFrom(
            foregroundColor: Colors.deepPurple,
            textStyle: const TextStyle(fontWeight: FontWeight.w700),
          ),
          child: const Text('Hint'),
        ),
      ],
    ),
  );
}

Future<void> showAiHintDialog(BuildContext context, Task task) {
  return showDialog<void>(
    context: context,
    builder: (context) => _AiHintDialog(task: task),
  );
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

class _AiHintDialog extends StatefulWidget {
  const _AiHintDialog({required this.task});

  final Task task;

  @override
  State<_AiHintDialog> createState() => _AiHintDialogState();
}

class _AiHintDialogState extends State<_AiHintDialog> {
  final AiHintService _aiHintService = AiHintService();
  String? _hintText;
  String? _errorMessage;
  bool _isLoading = true;
  bool _showAnswer = false;

  @override
  void initState() {
    super.initState();
    _fetchHint();
  }

  Future<void> _fetchHint() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _hintText = null;
    });
    try {
      final hint =
          await _aiHintService.generateHint(_buildHintPrompt(widget.task));
      if (!mounted) {
        return;
      }
      setState(() {
        _hintText = hint;
        _isLoading = false;
      });
    } on AiHintException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _errorMessage = error.message;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _errorMessage = 'Unable to fetch a hint right now.';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final answerText = _buildAnswerText(widget.task);
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Stack(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'AI Hint',
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 12),
                if (_isLoading)
                  const Center(child: CircularProgressIndicator())
                else if (_errorMessage != null)
                  Text(
                    _errorMessage!,
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(color: Colors.redAccent),
                  )
                else if (_hintText != null)
                  Text(
                    _hintText!,
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(color: Colors.blueGrey.shade700),
                  ),
                if (_showAnswer)
                  Padding(
                    padding: const EdgeInsets.only(top: 16),
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.deepPurple.shade50,
                        borderRadius: BorderRadius.circular(12),
                        border:
                            Border.all(color: Colors.deepPurple.shade100),
                      ),
                      child: Text(
                        answerText.isNotEmpty
                            ? answerText
                            : 'No answer is available for this task.',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: Colors.deepPurple.shade700,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    TextButton(
                      onPressed: _isLoading ? null : _fetchHint,
                      child: const Text('Regenerate Hint'),
                    ),
                    const Spacer(),
                    ElevatedButton(
                      onPressed: _showAnswer
                          ? null
                          : () => setState(() => _showAnswer = true),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.deepPurple,
                        foregroundColor: Colors.white,
                      ),
                      child: const Text('Show Answer'),
                    ),
                  ],
                ),
              ],
            ),
          ),
          Positioned(
            right: 8,
            top: 8,
            child: IconButton(
              icon: const Icon(Icons.close),
              onPressed: () => Navigator.of(context).pop(),
            ),
          ),
        ],
      ),
    );
  }
}

String _buildHintPrompt(Task task) {
  final buffer = StringBuffer(
    'Provide a concise hint that helps solve the task without revealing the answer.\n',
  );
  if (task.title.isNotEmpty) {
    buffer.writeln('Title: ${task.title}');
  }
  if (task.description.isNotEmpty) {
    buffer.writeln('Description: ${task.description}');
  }
  if (task is MultipleChoiceTask) {
    if (task.question.isNotEmpty) {
      buffer.writeln('Question: ${task.question}');
    }
    if (task.options.isNotEmpty) {
      buffer.writeln('Options: ${task.options.join(', ')}');
    }
  } else if (task is TrueFalseTask) {
    if (task.statement.isNotEmpty) {
      buffer.writeln('Statement: ${task.statement}');
    }
  } else if (task is FillInTheBlankTask) {
    if (task.text.isNotEmpty) {
      buffer.writeln('Text: ${task.text}');
    }
    if (task.missingSegments.isNotEmpty) {
      buffer.writeln('Number of blanks: ${task.missingSegments.length}');
    }
  } else if (task is MatchingPairTask) {
    if (task.leftItems.isNotEmpty) {
      buffer.writeln('Left items: ${task.leftItems.join(', ')}');
    }
    if (task.rightItems.isNotEmpty) {
      buffer.writeln('Right items: ${task.rightItems.join(', ')}');
    }
  } else if (task is OrderingTask) {
    if (task.items.isNotEmpty) {
      buffer.writeln('Items to order: ${task.items.join(', ')}');
    }
  } else if (task is SpotTheErrorTask) {
    if (task.prompt.isNotEmpty) {
      buffer.writeln('Prompt: ${task.prompt}');
    }
    if (task.codeSnippet.isNotEmpty) {
      buffer.writeln('Snippet: ${task.codeSnippet}');
    }
    if (task.options.isNotEmpty) {
      buffer.writeln('Options: ${task.options.join(', ')}');
    }
  } else if (task is CodingChallengeTask) {
    if (task.problemDescription.isNotEmpty) {
      buffer.writeln('Problem: ${task.problemDescription}');
    }
    if (task.expectedOutputDescription.isNotEmpty) {
      buffer.writeln('Expected output: ${task.expectedOutputDescription}');
    }
    if (task.examples.isNotEmpty) {
      for (final example in task.examples) {
        buffer.writeln('Example input: ${example.input}');
        buffer.writeln('Example output: ${example.output}');
        if (example.explanation != null && example.explanation!.isNotEmpty) {
          buffer.writeln('Example explanation: ${example.explanation}');
        }
      }
    }
    if (task.starterCodeByLanguage.isNotEmpty) {
      final language = task.defaultLanguage.isNotEmpty
          ? task.defaultLanguage
          : task.starterCodeByLanguage.keys.first;
      buffer.writeln(
        'Starter code ($language): ${task.starterCodeByLanguage[language] ?? ''}',
      );
    }
  }
  return buffer.toString();
}

String _buildAnswerText(Task task) {
  if (task is MultipleChoiceTask) {
    if (task.correctAnswer != null &&
        task.correctAnswer! >= 0 &&
        task.correctAnswer! < task.options.length) {
      return task.options[task.correctAnswer!];
    }
    return 'Correct option index: ${task.correctAnswer ?? 'Not provided'}';
  }
  if (task is TrueFalseTask) {
    if (task.correctAnswer == null) {
      return 'Correct answer not provided.';
    }
    return task.correctAnswer! ? 'True' : 'False';
  }
  if (task is FillInTheBlankTask) {
    if (task.missingSegments.isNotEmpty) {
      return task.missingSegments.join(', ');
    }
    return 'Correct segments not provided.';
  }
  if (task is MatchingPairTask) {
    if (task.correctMatches.isNotEmpty) {
      return task.correctMatches.entries
          .map((entry) => '${entry.key} → ${entry.value}')
          .join('\n');
    }
    return 'Correct matches not provided.';
  }
  if (task is OrderingTask) {
    if (task.items.isNotEmpty && task.correctOrder.isNotEmpty) {
      final orderedItems = <String>[];
      for (final index in task.correctOrder) {
        if (index >= 0 && index < task.items.length) {
          orderedItems.add(task.items[index]);
        }
      }
      if (orderedItems.isNotEmpty) {
        return orderedItems.join(' → ');
      }
    }
    return task.correctOrder.isNotEmpty
        ? task.correctOrder.join(', ')
        : 'Correct order not provided.';
  }
  if (task is SpotTheErrorTask) {
    if (task.correctOptionIndex != null &&
        task.correctOptionIndex! >= 0 &&
        task.correctOptionIndex! < task.options.length) {
      return task.options[task.correctOptionIndex!];
    }
    return 'Correct option index: ${task.correctOptionIndex ?? 'Not provided'}';
  }
  if (task is CodingChallengeTask) {
    final String? language = task.defaultLanguage.isNotEmpty
        ? task.defaultLanguage
        : (task.solutionCodeByLanguage.keys.isNotEmpty
            ? task.solutionCodeByLanguage.keys.first
            : null);
    if (language != null && task.solutionCodeByLanguage[language] != null) {
      return 'Solution ($language):\n${task.solutionCodeByLanguage[language]!}';
    }
    if (task.solutionCodeByLanguage.isNotEmpty) {
      final entry = task.solutionCodeByLanguage.entries.first;
      return 'Solution (${entry.key}):\n${entry.value}';
    }
    return 'Solution not provided.';
  }
  return '';
}
