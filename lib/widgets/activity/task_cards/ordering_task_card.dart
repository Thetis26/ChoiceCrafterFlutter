import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class OrderingTaskCard extends StatefulWidget {
  const OrderingTaskCard({
    super.key,
    required this.task,
    required this.style,
    this.onAnswerChecked,
  });

  final OrderingTask task;
  final TaskTypeStyle style;
  final ValueChanged<bool>? onAnswerChecked;

  @override
  State<OrderingTaskCard> createState() => _OrderingTaskCardState();
}

class _OrderingTaskCardState extends State<OrderingTaskCard> {
  late final List<String> _originalItems;
  late final List<String> _items;

  @override
  void initState() {
    super.initState();
    _originalItems = widget.task.items.isNotEmpty
        ? List<String>.from(widget.task.items)
        : ['Step 1', 'Step 2', 'Step 3'];
    _items = List<String>.from(_originalItems);
  }

  void _checkAnswer() {
    if (widget.task.correctOrder.isEmpty) {
      showTaskFeedback(
        context,
        message: 'No correct order provided for this task yet.',
        isCorrect: false,
      );
      return;
    }

    final currentOrder = _items
        .map((item) => _originalItems.indexOf(item))
        .toList();
    final bool isCorrect =
        currentOrder.length == widget.task.correctOrder.length &&
            List.generate(currentOrder.length,
                    (index) => currentOrder[index] == widget.task.correctOrder[index])
                .every((match) => match);

    widget.onAnswerChecked?.call(isCorrect);
    showTaskFeedback(
      context,
      message: isCorrect ? 'Perfect order!' : 'Order is off. Try again!',
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
    final String rewardText = taskRewardText(task, '+30 XP');

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
              task.title.isNotEmpty ? task.title : 'Order the items',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
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
              child: ReorderableListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _items.length,
                onReorder: (oldIndex, newIndex) {
                  setState(() {
                    if (newIndex > oldIndex) {
                      newIndex -= 1;
                    }
                    final item = _items.removeAt(oldIndex);
                    _items.insert(newIndex, item);
                  });
                },
                itemBuilder: (context, index) {
                  final item = _items[index];
                  return Padding(
                    key: ValueKey(item),
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 18,
                        vertical: 16,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFBFCBF9),
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              item,
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          const Icon(
                            Icons.drag_handle,
                            color: Color(0xFFFFD166),
                          ),
                        ],
                      ),
                    ),
                  );
                },
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
}
