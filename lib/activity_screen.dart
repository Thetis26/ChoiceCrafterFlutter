// lib/activity_screen.dart

import 'package:flutter/material.dart';

import 'models/activity.dart';
import 'models/task.dart';
import 'widgets/activity/task_cards/fill_in_the_blank_task_card.dart';
import 'widgets/activity/task_cards/generic_task_card.dart';
import 'widgets/activity/task_cards/info_card_task_card.dart';
import 'widgets/activity/task_cards/multiple_choice_task_card.dart';
import 'widgets/activity/task_cards/ordering_task_card.dart';
import 'widgets/activity/task_cards/spot_the_error_task_card.dart';
import 'widgets/activity/task_cards/task_type_style.dart';
import 'widgets/activity/task_cards/true_false_task_card.dart';

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
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (activity.content.isNotEmpty) Text(activity.content),
            const SizedBox(height: 16),
            Expanded(
              child: tasks.isEmpty
                  ? const Center(
                      child: Text('No tasks available for this activity yet.'),
                    )
                  : _buildTaskCarousel(context, tasks),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTaskCarousel(BuildContext context, List<Task> tasks) {
    final controller = PageController(viewportFraction: 0.92);
    return PageView.builder(
      controller: controller,
      itemCount: tasks.length,
      itemBuilder: (context, index) {
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: _buildTaskCard(context, tasks[index]),
                ),
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildTaskCard(BuildContext context, Task task) {
    final style = taskTypeStyle(task, context);
    if (task is OrderingTask) {
      return OrderingTaskCard(task: task, style: style);
    }
    if (task is InfoCardTask) {
      return InfoCardTaskCard(task: task, style: style);
    }
    if (task is FillInTheBlankTask) {
      return FillInTheBlankTaskCard(task: task, style: style);
    }
    if (task is MultipleChoiceTask) {
      return MultipleChoiceTaskCard(task: task, style: style);
    }
    if (task is SpotTheErrorTask) {
      return SpotTheErrorTaskCard(task: task, style: style);
    }
    if (task is TrueFalseTask) {
      return TrueFalseTaskCard(task: task, style: style);
    }
    return GenericTaskCard(
      task: task,
      style: style,
      detailLines: _buildTaskDetails(task),
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
