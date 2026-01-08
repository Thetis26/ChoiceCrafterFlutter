import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class MatchingPairTaskCard extends StatefulWidget {
  const MatchingPairTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final MatchingPairTask task;
  final TaskTypeStyle style;

  @override
  State<MatchingPairTaskCard> createState() => _MatchingPairTaskCardState();
}

class _MatchingPairTaskCardState extends State<MatchingPairTaskCard> {
  late final List<String> _leftItems;
  late final List<String> _rightItems;
  late final Map<String, String?> _selectedMatches;

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
              child: Column(
                children: _leftItems.map((leftItem) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Row(
                      children: [
                        Expanded(child: _buildMatchPill(leftItem)),
                        const SizedBox(width: 12),
                        Expanded(
                          child: DropdownButtonFormField<String>(
                            value: _selectedMatches[leftItem],
                            items: _rightItems
                                .map(
                                  (rightItem) => DropdownMenuItem(
                                    value: rightItem,
                                    child: Text(rightItem),
                                  ),
                                )
                                .toList(),
                            onChanged: (value) {
                              setState(() {
                                _selectedMatches[leftItem] = value;
                              });
                            },
                            decoration: InputDecoration(
                              hintText: 'Select match',
                              filled: true,
                              fillColor: const Color(0xFFFFF5ED),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(16),
                                borderSide: BorderSide(
                                  color: Colors.blueGrey.shade200,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
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

  Widget _buildMatchPill(String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE6D1),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Text(
        label,
        textAlign: TextAlign.center,
        style: const TextStyle(
          fontWeight: FontWeight.w600,
          color: Color(0xFF4A4A4A),
        ),
      ),
    );
  }
}
