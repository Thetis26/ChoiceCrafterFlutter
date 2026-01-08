import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class OrderingTaskCard extends StatefulWidget {
  const OrderingTaskCard({
    super.key,
    required this.task,
    required this.style,
  });

  final OrderingTask task;
  final TaskTypeStyle style;

  @override
  State<OrderingTaskCard> createState() => _OrderingTaskCardState();
}

class _OrderingTaskCardState extends State<OrderingTaskCard> {
  late List<String> _items;

  @override
  void initState() {
    super.initState();
    _items = widget.task.items.isNotEmpty
        ? List<String>.from(widget.task.items)
        : ['Step 1', 'Step 2', 'Step 3'];
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _checkAnswer() {
    final correctOrder = widget.task.correctOrder;
    if (correctOrder.isEmpty || correctOrder.length != widget.task.items.length) {
      _showSnack('No correct order configured yet.');
      return;
    }
    final correctItems = correctOrder
        .where((index) => index >= 0 && index < widget.task.items.length)
        .map((index) => widget.task.items[index])
        .toList();
    if (correctItems.length != _items.length) {
      _showSnack('The correct order is incomplete.');
      return;
    }
    final isCorrect = List.generate(_items.length, (index) {
      return _items[index] == correctItems[index];
    }).every((match) => match);
    _showSnack(isCorrect ? 'Nice! The order is correct.' : 'Not quite. Try a different order.');
  }

  void _showHint() {
    final hint = widget.task.explanation?.trim();
    _showSnack((hint != null && hint.isNotEmpty)
        ? hint
        : 'Start with the step that must happen first.');
  }

  @override
  Widget build(BuildContext context) {
    final style = widget.style;
    final theme = Theme.of(context);
    final String helperText = (widget.task.explanation != null &&
            widget.task.explanation!.trim().isNotEmpty)
        ? widget.task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = taskRewardText(widget.task, '+30 XP');

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFE3F4EE),
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
                  : 'Order the items',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.task.description.isNotEmpty
                  ? widget.task.description
                  : 'Arrange each step before the timer cools down.',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.blueGrey.shade600),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.06),
                    blurRadius: 14,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: Column(
                children: [
                  ReorderableListView(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    onReorder: (oldIndex, newIndex) {
                      setState(() {
                        if (newIndex > oldIndex) {
                          newIndex -= 1;
                        }
                        final item = _items.removeAt(oldIndex);
                        _items.insert(newIndex, item);
                      });
                    },
                    children: List.generate(_items.length, (index) {
                      final item = _items[index];
                      return Padding(
                        key: ValueKey('ordering_${index}_$item'),
                        padding: const EdgeInsets.only(bottom: 12),
                        child: buildOrderingItem(item),
                      );
                    }),
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
