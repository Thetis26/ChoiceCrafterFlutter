// lib/activity_screen.dart

import 'package:flutter/material.dart';

import 'models/activity.dart';
import 'models/task.dart';

class ActivityScreen extends StatelessWidget {
  const ActivityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is! Map || arguments['activity'] is! Activity) {
      return const Scaffold(body: Center(child: Text('No activity data provided.')));
    }

    final Activity activity = arguments['activity'] as Activity;
    final String? courseId = arguments['courseId'] as String?;

    final List<Task> tasks = activity.tasks;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(activity.name),
            if (courseId != null)
              Text(
                'Course: $courseId',
                style: Theme.of(context)
                    .textTheme
                    .labelMedium
                    ?.copyWith(color: Colors.white70),
              ),
          ],
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          Row(
            children: [
              Chip(label: Text(activity.type)),
              if (courseId != null) ...[
                const SizedBox(width: 8),
                Chip(label: Text('Course $courseId')),
              ],
            ],
          ),
          const SizedBox(height: 12),
          Text(activity.description, style: Theme.of(context).textTheme.bodyLarge),
          const SizedBox(height: 16),
          if (activity.content.isNotEmpty) Text(activity.content),
          const SizedBox(height: 24),
          Text('Tasks', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          if (tasks.isEmpty)
            const Text('No tasks available for this activity yet.')
          else
            _buildTaskCarousel(context, tasks),
          const SizedBox(height: 24),
          Align(
            alignment: Alignment.centerRight,
            child: ElevatedButton.icon(
              onPressed: () {
                Navigator.of(context).pushNamed(
                  '/courseActivities',
                  arguments: {
                    'courseId': courseId,
                    'highlightActivityId': activity.id,
                  },
                );
              },
              icon: const Icon(Icons.list),
              label: const Text('Back to activities'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTaskCarousel(BuildContext context, List<Task> tasks) {
    final controller = PageController(viewportFraction: 0.92);
    return SizedBox(
      height: 320,
      child: PageView.builder(
        controller: controller,
        itemCount: tasks.length,
        itemBuilder: (context, index) {
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: _buildTaskCard(context, tasks[index]),
          );
        },
      ),
    );
  }

  Widget _buildTaskCard(BuildContext context, Task task) {
    final style = _taskTypeStyle(task, context);
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
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
            ..._buildTaskDetails(task),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildTaskDetails(Task task) {
    if (task is MultipleChoiceTask) {
      return _detailLines([
        'Question: ${task.question}',
        if (task.options.isNotEmpty) 'Options: ${task.options.join(', ')}',
        if (task.supportingContent?.text != null)
          'Supporting text: ${task.supportingContent?.text}',
        if (task.supportingContent?.imageUrl != null)
          'Supporting image: ${task.supportingContent?.imageUrl}',
      ]);
    }
    if (task is FillInTheBlankTask) {
      return _detailLines([
        'Text: ${task.text}',
        if (task.missingSegments.isNotEmpty)
          'Missing segments: ${task.missingSegments.join(', ')}',
        if (task.supportingContent?.text != null)
          'Supporting text: ${task.supportingContent?.text}',
        if (task.supportingContent?.imageUrl != null)
          'Supporting image: ${task.supportingContent?.imageUrl}',
      ]);
    }
    if (task is MatchingPairTask) {
      return _detailLines([
        if (task.leftItems.isNotEmpty)
          'Left items: ${task.leftItems.join(', ')}',
        if (task.rightItems.isNotEmpty)
          'Right items: ${task.rightItems.join(', ')}',
        if (task.correctMatches.isNotEmpty)
          'Matches: ${task.correctMatches.entries.map((entry) => '${entry.key} → ${entry.value}').join(', ')}',
      ]);
    }
    if (task is OrderingTask) {
      return _detailLines([
        if (task.items.isNotEmpty) 'Items: ${task.items.join(', ')}',
        if (task.correctOrder.isNotEmpty)
          'Correct order: ${task.correctOrder.join(', ')}',
      ]);
    }
    if (task is TrueFalseTask) {
      return _detailLines([
        'Statement: ${task.statement}',
        if (task.correctAnswer != null)
          'Answer: ${task.correctAnswer == true ? 'True' : 'False'}',
      ]);
    }
    if (task is SpotTheErrorTask) {
      return _detailLines([
        'Prompt: ${task.prompt}',
        if (task.codeSnippet.isNotEmpty) 'Snippet: ${task.codeSnippet}',
        if (task.options.isNotEmpty) 'Options: ${task.options.join(', ')}',
      ]);
    }
    if (task is CodingChallengeTask) {
      return _detailLines([
        'Problem: ${task.problemDescription}',
        if (task.expectedOutputDescription.isNotEmpty)
          'Expected output: ${task.expectedOutputDescription}',
        if (task.examples.isNotEmpty)
          'Examples: ${task.examples.length}',
        if (task.starterCodeByLanguage.isNotEmpty)
          'Starter code: ${task.starterCodeByLanguage.keys.join(', ')}',
        if (task.solutionCodeByLanguage.isNotEmpty)
          'Solutions: ${task.solutionCodeByLanguage.keys.join(', ')}',
        if (task.defaultLanguage.isNotEmpty)
          'Default language: ${task.defaultLanguage}',
      ]);
    }
    if (task is InfoCardTask) {
      return _detailLines([
        if (task.contentType.isNotEmpty) 'Content type: ${task.contentType}',
        if (task.contentText.isNotEmpty) 'Content: ${task.contentText}',
        if (task.mediaUrl.isNotEmpty) 'Media: ${task.mediaUrl}',
        if (task.interactiveUrl.isNotEmpty)
          'Interactive: ${task.interactiveUrl}',
        if (task.actionText.isNotEmpty) 'Action text: ${task.actionText}',
      ]);
    }
    return const [];
  }

  List<Widget> _detailLines(List<String> lines) {
    return lines
        .where((line) => line.trim().isNotEmpty)
        .map((line) => Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Text(line),
            ))
        .toList();
  }
}

class _TaskTypeStyle {
  const _TaskTypeStyle({
    required this.label,
    required this.icon,
    required this.color,
  });

  final String label;
  final IconData icon;
  final Color color;
}

_TaskTypeStyle _taskTypeStyle(Task task, BuildContext context) {
  if (task is MultipleChoiceTask) {
    return const _TaskTypeStyle(
      label: 'Multiple Choice',
      icon: Icons.list_alt,
      color: Colors.indigo,
    );
  }
  if (task is FillInTheBlankTask) {
    return const _TaskTypeStyle(
      label: 'Fill in the Blank',
      icon: Icons.edit_note,
      color: Colors.orange,
    );
  }
  if (task is MatchingPairTask) {
    return const _TaskTypeStyle(
      label: 'Matching Pairs',
      icon: Icons.compare_arrows,
      color: Colors.green,
    );
  }
  if (task is OrderingTask) {
    return const _TaskTypeStyle(
      label: 'Ordering',
      icon: Icons.format_list_numbered,
      color: Colors.purple,
    );
  }
  if (task is TrueFalseTask) {
    return const _TaskTypeStyle(
      label: 'True / False',
      icon: Icons.rule,
      color: Colors.blueGrey,
    );
  }
  if (task is SpotTheErrorTask) {
    return const _TaskTypeStyle(
      label: 'Spot the Error',
      icon: Icons.bug_report,
      color: Colors.redAccent,
    );
  }
  if (task is CodingChallengeTask) {
    return const _TaskTypeStyle(
      label: 'Coding Challenge',
      icon: Icons.code,
      color: Colors.teal,
    );
  }
  if (task is InfoCardTask) {
    return const _TaskTypeStyle(
      label: 'Info Card',
      icon: Icons.info_outline,
      color: Colors.blue,
    );
  }
  return _TaskTypeStyle(
    label: task.type,
    icon: Icons.assignment,
    color: Theme.of(context).colorScheme.primary,
  );
}
