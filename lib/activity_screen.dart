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
    final style = _taskTypeStyle(task, context);
    if (task is OrderingTask) {
      return _buildOrderingTaskCard(context, task, style);
    }
    if (task is InfoCardTask) {
      return _buildInfoCardTask(context, task, style);
    }
    if (task is FillInTheBlankTask) {
      return _buildFillInTheBlankTask(context, task, style);
    }
    if (task is MultipleChoiceTask) {
      return _buildMultipleChoiceTask(context, task, style);
    }
    if (task is SpotTheErrorTask) {
      return _buildSpotTheErrorTask(context, task, style);
    }
    if (task is TrueFalseTask) {
      return _buildTrueFalseTaskCard(context, task, style);
    }
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
            ..._buildTaskDetails(task),
            if (task is! InfoCardTask) ...[
              const SizedBox(height: 16),
              _buildTaskActions(context),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildTrueFalseTaskCard(
    BuildContext context,
    TrueFalseTask task,
    _TaskTypeStyle style,
  ) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String titleText =
        task.statement.isNotEmpty ? task.statement : task.title;
    final String subtitleText = task.description.isNotEmpty
        ? task.description
        : 'Decide if the statement below is correct.';
    final String rewardText = task.status.isNotEmpty ? task.status : '+25 XP';

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFE9EDFF),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: const BoxDecoration(
                    color: Colors.white,
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
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: Colors.blueGrey.shade700,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.6,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      rewardText,
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: Colors.blueGrey.shade400),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 18),
            Text(
              titleText,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              subtitleText,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.blueGrey.shade600),
            ),
            const SizedBox(height: 16),
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                children: [
                  _buildTrueFalseOption('True', style.color),
                  const Divider(height: 1),
                  _buildTrueFalseOption('False', style.color),
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
            _buildTaskActions(context),
          ],
        ),
      ),
    );
  }

  Widget _buildOrderingTaskCard(
    BuildContext context,
    OrderingTask task,
    _TaskTypeStyle style,
  ) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = _taskRewardText(task, '+30 XP');

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
            _buildTaskHeader(style, rewardText, background: Colors.white),
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
              child: Column(
                children: task.items.isNotEmpty
                    ? task.items
                        .map((item) => _buildOrderingItem(item))
                        .toList()
                    : [
                        _buildOrderingItem('Step 1'),
                        _buildOrderingItem('Step 2'),
                        _buildOrderingItem('Step 3'),
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
            _buildTaskActions(context),
          ],
        ),
      ),
    );
  }

  Widget _buildOrderingItem(String label) {
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

  Widget _buildInfoCardTask(
    BuildContext context,
    InfoCardTask task,
    _TaskTypeStyle style,
  ) {
    final theme = Theme.of(context);
    final String rewardText = _taskRewardText(task, '+15 XP');
    final String contentText =
        task.contentText.isNotEmpty ? task.contentText : task.description;

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFECE6F7),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildTaskHeader(style, rewardText, background: Colors.white),
            const SizedBox(height: 18),
            Text(
              task.title.isNotEmpty ? task.title : 'Info boost',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              contentText,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.blueGrey.shade600, height: 1.4),
            ),
            const SizedBox(height: 20),
            Align(
              alignment: Alignment.centerRight,
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
                child: Text(task.actionText.isNotEmpty
                    ? task.actionText
                    : 'I got it!'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFillInTheBlankTask(
    BuildContext context,
    FillInTheBlankTask task,
    _TaskTypeStyle style,
  ) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = _taskRewardText(task, '+35 XP');
    final List<String> blanks = task.missingSegments.isNotEmpty
        ? task.missingSegments
        : ['Answer', 'Answer'];

    return Card(
      margin: EdgeInsets.zero,
      color: const Color(0xFFE3F2FC),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildTaskHeader(style, rewardText, background: Colors.white),
            const SizedBox(height: 18),
            Text(
              task.title.isNotEmpty ? task.title : 'Fill in the blanks',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Complete the tale without breaking your combo.',
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
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    task.text.isNotEmpty
                        ? task.text
                        : '____ <iostream>\nusing namespace ____;',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontFamily: 'Courier',
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 18),
                  ...blanks
                      .map((_) => Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: _buildBlankInput(),
                          ))
                      .toList(),
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
            _buildTaskActions(context),
          ],
        ),
      ),
    );
  }

  Widget _buildBlankInput() {
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

  Widget _buildMultipleChoiceTask(
    BuildContext context,
    MultipleChoiceTask task,
    _TaskTypeStyle style,
  ) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = _taskRewardText(task, '+40 XP');

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
            _buildTaskHeader(style, rewardText, background: Colors.white),
            const SizedBox(height: 18),
            Text(
              task.question.isNotEmpty ? task.question : task.title,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Choose the correct answer from the list.',
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
                children: (task.options.isNotEmpty
                        ? task.options
                        : ['Option A', 'Option B', 'Option C'])
                    .map((option) => _buildChoiceOption(option, style.color))
                    .toList(),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              helperText,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: Colors.blueGrey.shade500),
            ),
            const SizedBox(height: 16),
            _buildTaskActions(context),
          ],
        ),
      ),
    );
  }

  Widget _buildChoiceOption(String label, Color accentColor) {
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

  Widget _buildSpotTheErrorTask(
    BuildContext context,
    SpotTheErrorTask task,
    _TaskTypeStyle style,
  ) {
    final theme = Theme.of(context);
    final String helperText = (task.explanation != null &&
            task.explanation!.trim().isNotEmpty)
        ? task.explanation!.trim()
        : 'Using hints reduces your reward.';
    final String rewardText = _taskRewardText(task, '+45 XP');

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
            _buildTaskHeader(style, rewardText, background: Colors.white),
            const SizedBox(height: 18),
            Text(
              task.title.isNotEmpty ? task.title : 'Spot the error',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: Colors.blueGrey.shade900,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.description.isNotEmpty
                  ? task.description
                  : 'Choose the correct fix for the code snippet.',
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
              child: Text(
                task.codeSnippet.isNotEmpty
                    ? task.codeSnippet
                    : 'include <iostream>\nint main {\n  cout << \"Salut!\"\n}',
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontFamily: 'Courier',
                  height: 1.4,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Column(
                children: (task.options.isNotEmpty
                        ? task.options
                        : [
                            'Add # to include, add () to main, and add return 0;',
                            'Remove the cout line and keep the rest unchanged.',
                            'Replace cin with scanf for input.',
                          ])
                    .map((option) => _buildChoiceOption(option, style.color))
                    .toList(),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              helperText,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: Colors.blueGrey.shade500),
            ),
            const SizedBox(height: 16),
            _buildTaskActions(context),
          ],
        ),
      ),
    );
  }

  Widget _buildTaskHeader(
    _TaskTypeStyle style,
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

  String _taskRewardText(Task task, String fallback) {
    return task.status.isNotEmpty ? task.status : fallback;
  }

  Widget _buildTrueFalseOption(String label, Color accentColor) {
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

  Widget _buildTaskActions(BuildContext context) {
    final Color buttonColor = const Color(0xFF6E7BF2);
    return Row(
      children: [
        Expanded(
          child: ElevatedButton(
            onPressed: () {},
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
            onPressed: () {},
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
      icon: Icons.local_fire_department,
      color: Colors.indigo,
    );
  }
  if (task is FillInTheBlankTask) {
    return const _TaskTypeStyle(
      label: 'Fill in the Blank',
      icon: Icons.lightbulb_outline,
      color: Colors.blue,
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
      icon: Icons.checklist,
      color: Colors.indigo,
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
      icon: Icons.smart_display,
      color: Colors.deepOrange,
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
