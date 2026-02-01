import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class MatchingPairTaskCard extends StatefulWidget {
  const MatchingPairTaskCard({
    super.key,
    required this.task,
    required this.style,
    this.onAnswerChecked,
  });

  final MatchingPairTask task;
  final TaskTypeStyle style;
  final ValueChanged<bool>? onAnswerChecked;

  @override
  State<MatchingPairTaskCard> createState() => _MatchingPairTaskCardState();
}

class _MatchingPairTaskCardState extends State<MatchingPairTaskCard> {
  late final List<String> _leftItems;
  late final List<String> _rightItems;
  late final Map<String, String?> _selectedMatches;
  String? _activeLeftItem;

  @override
  void initState() {
    super.initState();
    _leftItems = widget.task.leftItems.isNotEmpty
        ? widget.task.leftItems
        : ['Spring', 'React'];
    _rightItems = widget.task.rightItems.isNotEmpty
        ? widget.task.rightItems
        : ['Frontend', 'Backend'];
    _selectedMatches = {
      for (final item in _leftItems) item: null,
    };
  }

  Map<String, String> get _reverseMatches {
    final reverse = <String, String>{};
    for (final entry in _selectedMatches.entries) {
      final match = entry.value;
      if (match != null) {
        reverse[match] = entry.key;
      }
    }
    return reverse;
  }

  void _toggleLeftSelection(String leftItem) {
    setState(() {
      if (_activeLeftItem == leftItem) {
        _activeLeftItem = null;
      } else {
        _activeLeftItem = leftItem;
      }
    });
  }

  void _selectRightItem(String rightItem) {
    if (_activeLeftItem == null) {
      return;
    }

    setState(() {
      final previousLeft = _reverseMatches[rightItem];
      if (previousLeft != null && previousLeft != _activeLeftItem) {
        _selectedMatches[previousLeft] = null;
      }
      _selectedMatches[_activeLeftItem!] = rightItem;
      _activeLeftItem = null;
    });
  }

  void _checkAnswer() {
    if (widget.task.correctMatches.isEmpty) {
      showTaskFeedback(
        context,
        message: 'No correct matches provided for this task yet.',
        isCorrect: false,
      );
      return;
    }

    if (_selectedMatches.values.any((value) => value == null)) {
      showTaskFeedback(
        context,
        message: 'Match every item before checking.',
        isCorrect: false,
      );
      return;
    }

    final bool isCorrect = _selectedMatches.entries.every((entry) {
      final expected = widget.task.correctMatches[entry.key];
      return expected != null && expected == entry.value;
    });

    widget.onAnswerChecked?.call(isCorrect);
    showTaskFeedback(
      context,
      message: isCorrect ? 'All pairs match!' : 'Some pairs are incorrect.',
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
    final String rewardText = taskRewardText(task, '+45 XP');

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
              task.title.isNotEmpty ? task.title : 'Match the pairs',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Match partners to keep the synergy meter full.',
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
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      children: _leftItems.map((leftItem) {
                        final matchedRight = _selectedMatches[leftItem];
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _buildSelectableOption(
                            label: leftItem,
                            isActive: _activeLeftItem == leftItem,
                            isMatched: matchedRight != null,
                            matchedLabel: matchedRight,
                            onTap: () => _toggleLeftSelection(leftItem),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      children: _rightItems.map((rightItem) {
                        final matchedLeft = _reverseMatches[rightItem];
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _buildSelectableOption(
                            label: rightItem,
                            isActive: false,
                            isMatched: matchedLeft != null,
                            matchedLabel: matchedLeft,
                            onTap: () => _selectRightItem(rightItem),
                          ),
                        );
                      }).toList(),
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
              onHint: () => showTaskHintDialog(context, task: task),
              onCheckAnswer: _checkAnswer,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSelectableOption({
    required String label,
    required bool isActive,
    required bool isMatched,
    String? matchedLabel,
    required VoidCallback onTap,
  }) {
    final Color backgroundColor;
    if (isActive) {
      backgroundColor = const Color(0xFFFFD3B0);
    } else if (isMatched) {
      backgroundColor = const Color(0xFFFFE6D1);
    } else {
      backgroundColor = const Color(0xFFFFF5ED);
    }

    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
        decoration: BoxDecoration(
          color: backgroundColor,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: isActive ? const Color(0xFFED8B3D) : Colors.transparent,
            width: 1.5,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontWeight: FontWeight.w600,
                color: Color(0xFF4A4A4A),
              ),
            ),
            if (matchedLabel != null) ...[
              const SizedBox(height: 4),
              Text(
                'Matched: $matchedLabel',
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.blueGrey.shade700,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
