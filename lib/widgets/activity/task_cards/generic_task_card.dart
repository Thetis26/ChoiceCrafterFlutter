import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_card_shared.dart';
import 'task_type_style.dart';

class GenericTaskCard extends StatelessWidget {
  const GenericTaskCard({
    super.key,
    required this.task,
    required this.style,
    required this.detailLines,
  });

  final Task task;
  final TaskTypeStyle style;
  final List<Widget> detailLines;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 18,
                  backgroundColor: style.color.withOpacity(0.15),
                  child: Icon(style.icon, color: style.color, size: 18),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        style.label,
                        style: Theme.of(context).textTheme.labelMedium?.copyWith(
                              color: style.color,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        task.title.isEmpty ? 'Untitled task' : task.title,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            if (task.description.isNotEmpty) ...[
              const SizedBox(height: 4),
              Text(task.description),
            ],
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 4,
              children: [
                Chip(label: Text(task.type)),
                if (task.status.isNotEmpty) Chip(label: Text(task.status)),
              ],
            ),
            const SizedBox(height: 8),
            ...detailLines,
            if (task is! InfoCardTask) ...[
              const SizedBox(height: 16),
              buildTaskActions(
                context,
                onHint: () => showTaskHintDialog(context, task: task),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
