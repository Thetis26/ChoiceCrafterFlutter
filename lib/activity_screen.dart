// lib/activity_screen.dart

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import 'models/activity.dart';
import 'models/task.dart';
import 'models/task_stats.dart';
import 'models/user.dart';
import 'repositories/activity_progress_repository.dart';
import 'widgets/activity/task_cards/coding_challenge_task_card.dart';
import 'widgets/activity/task_cards/fill_in_the_blank_task_card.dart';
import 'widgets/activity/task_cards/generic_task_card.dart';
import 'widgets/activity/task_cards/info_card_task_card.dart';
import 'widgets/activity/task_cards/matching_pair_task_card.dart';
import 'widgets/activity/task_cards/multiple_choice_task_card.dart';
import 'widgets/activity/task_cards/ordering_task_card.dart';
import 'widgets/activity/task_cards/spot_the_error_task_card.dart';
import 'widgets/activity/task_cards/task_type_style.dart';
import 'widgets/activity/task_cards/true_false_task_card.dart';

class ActivityScreen extends StatefulWidget {
  const ActivityScreen({super.key});

  @override
  State<ActivityScreen> createState() => _ActivityScreenState();
}

class _ActivityScreenState extends State<ActivityScreen> {
  final PageController _controller = PageController(viewportFraction: 0.92);
  int _currentIndex = 0;
  final Map<int, bool> _taskCorrect = {};
  final Map<int, int> _taskAttempts = {};
  final ActivityProgressRepository _activityProgressRepository =
      ActivityProgressRepository();
  bool _progressInitialized = false;
  String? _userKey;
  String? _courseId;
  String? _activityId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _initializeProgress();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _initializeProgress() {
    if (_progressInitialized) {
      return;
    }
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is! Map) {
      _progressInitialized = true;
      return;
    }
    final activityArg = arguments['activity'];
    final courseId = arguments['courseId'] as String?;
    final userArg = arguments['user'];
    String? userKey;
    if (userArg is User) {
      userKey = userArg.email.isNotEmpty ? userArg.email : userArg.id;
    }
    if (activityArg is Activity) {
      final resolvedActivityId =
          activityArg.id.isNotEmpty ? activityArg.id : activityArg.name;
      _activityId = resolvedActivityId;
    }
    _courseId = courseId;
    _userKey = userKey;
    _progressInitialized = true;
    if (_userKey != null &&
        _userKey!.isNotEmpty &&
        _courseId != null &&
        _courseId!.isNotEmpty &&
        _activityId != null &&
        _activityId!.isNotEmpty) {
      _activityProgressRepository.startActivity(
        userId: _userKey!,
        courseId: _courseId!,
        activityId: _activityId!,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is! Map || arguments['activity'] is! Activity) {
      return const Scaffold(body: Center(child: Text('No activity data provided.')));
    }

    final Activity activity = arguments['activity'] as Activity;
    final String? courseId = arguments['courseId'] as String?;
    final userArg = arguments['user'];
    if (userArg is User) {
      _userKey = userArg.email.isNotEmpty ? userArg.email : userArg.id;
    }
    _courseId = courseId;
    _activityId = activity.id.isNotEmpty ? activity.id : activity.name;

    final List<Task> tasks = activity.tasks;
    final bool showRecommendations = _areTasksComplete(tasks);

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
                  : _buildTaskCarousel(context, tasks, showRecommendations),
            ),
            if (tasks.isNotEmpty) ...[
              const SizedBox(height: 16),
              _buildTaskNavigation(
                context,
                tasks,
                showRecommendations,
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildTaskCarousel(
    BuildContext context,
    List<Task> tasks,
    bool showRecommendations,
  ) {
    final itemCount = tasks.length + (showRecommendations ? 2 : 0);
    return PageView.builder(
      controller: _controller,
      itemCount: itemCount,
      onPageChanged: (index) => setState(() => _currentIndex = index),
      itemBuilder: (context, index) {
        if (index >= tasks.length) {
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: LayoutBuilder(
              builder: (context, constraints) {
                return SingleChildScrollView(
                  child: ConstrainedBox(
                    constraints:
                        BoxConstraints(minHeight: constraints.maxHeight),
                    child: index == tasks.length
                        ? _buildRecommendationsCard(context)
                        : _buildStatisticsCard(context, tasks),
                  ),
                );
              },
            ),
          );
        }
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: _buildTaskCard(
                    context,
                    tasks[index],
                    index,
                    tasks,
                  ),
                ),
              );
            },
          ),
        );
      },
    );
  }

  bool _areTasksComplete(List<Task> tasks) {
    if (tasks.isEmpty) {
      return false;
    }
    return tasks.asMap().entries.every(
          (entry) => _isTaskSatisfied(entry.value, entry.key),
        );
  }

  bool _isTaskMarkedComplete(Task task) {
    final normalized = task.status.trim().toLowerCase();
    return normalized == 'completed' ||
        normalized == 'complete' ||
        normalized == 'done' ||
        normalized == 'finished';
  }

  bool _isTaskSatisfied(Task task, int index) {
    if (_isTaskMarkedComplete(task)) {
      return true;
    }
    return _isTaskVerified(task, index);
  }

  Widget _buildRecommendationsCard(BuildContext context) {
    final recommendations = _recommendationMaterials();
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Recommended materials',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Explore these resources to keep building on what you finished.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            ...recommendations.map(
              (recommendation) => _RecommendationListTile(
                recommendation: recommendation,
                onTap: () => _openRecommendation(
                  context,
                  recommendation.url,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<_RecommendationMaterial> _recommendationMaterials() {
    return const [
      _RecommendationMaterial(
        title: 'Kotlin coroutines fundamentals',
        url: 'https://www.w3schools.com/cpp/',
        description: 'Website • Android Developers',
        type: _RecommendationType.website,
      ),
      _RecommendationMaterial(
        title: 'Async programming in Flutter',
        url: 'https://www.youtube.com/watch?v=CzRQ9mnmh44',
        description: 'YouTube • Flutter channel',
        type: _RecommendationType.youtube,
      ),
      _RecommendationMaterial(
        title: 'Mobile architecture checklist',
        url: 'https://upb.ro/wp-content/uploads/2025/05/Structura-an-universitar-2025-2026.pdf',
        description: 'PDF • Quick reference',
        type: _RecommendationType.pdf,
      ),
      _RecommendationMaterial(
        title: 'Reflection prompts worksheet',
        url: 'https://example.org/reflection-prompts.docx',
        description: 'Doc • Guided worksheet',
        type: _RecommendationType.doc,
      ),
    ];
  }

  Future<void> _openRecommendation(
    BuildContext context,
    String url,
  ) async {
    final uri = Uri.tryParse(url);
    if (uri == null) {
      _showLaunchError(context);
      return;
    }
    try {
      final canLaunch = await canLaunchUrl(uri);
      if (!canLaunch) {
        if (context.mounted) {
          _showLaunchError(context);
        }
        return;
      }
      final success = await launchUrl(
        uri,
        mode: LaunchMode.platformDefault,
      );
      if (!success && context.mounted) {
        _showLaunchError(context);
      }
    } on MissingPluginException {
      if (context.mounted) {
        _showLaunchError(context);
      }
    } catch (_) {
      _showLaunchError(context);
    }
  }

  void _showLaunchError(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Unable to open the resource link right now.'),
      ),
    );
  }

  Widget _buildTaskNavigation(
    BuildContext context,
    List<Task> tasks,
    bool showCompletionPages,
  ) {
    final int totalTasks = tasks.length;
    final int totalPages = totalTasks + (showCompletionPages ? 2 : 0);
    final bool hasPrevious = _currentIndex > 0;
    final bool hasNext = _currentIndex < totalPages - 1;
    final bool isTaskPage = _currentIndex < totalTasks;
    final bool isVerified = isTaskPage
        ? _isTaskVerified(
            tasks[_currentIndex.clamp(0, totalTasks - 1) as int],
            _currentIndex,
          )
        : true;
    final bool canMoveNext = hasNext && isVerified;
    final String pageLabel = _currentIndex < totalTasks
        ? 'Task ${_currentIndex + 1} of $totalTasks'
        : _currentIndex == totalTasks
            ? 'Recommendations'
            : 'Statistics';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: hasPrevious
                    ? () => _controller.previousPage(
                          duration: const Duration(milliseconds: 300),
                          curve: Curves.easeOut,
                        )
                    : null,
                child: const Text('Previous'),
              ),
            ),
            const SizedBox(width: 12),
            Text(pageLabel),
            const SizedBox(width: 12),
            Expanded(
              child: ElevatedButton(
                onPressed: canMoveNext
                    ? () => _controller.nextPage(
                          duration: const Duration(milliseconds: 300),
                          curve: Curves.easeOut,
                        )
                    : null,
                child: const Text('Next'),
              ),
            ),
          ],
          ),
        if (isTaskPage && !isVerified)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              'Answer correctly to unlock the next task.',
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: Colors.blueGrey.shade600),
            ),
          ),
      ],
    );
  }

  Widget _buildTaskCard(
    BuildContext context,
    Task task,
    int index,
    List<Task> tasks,
  ) {
    final int totalTasks = tasks.length;
    final style = taskTypeStyle(task, context);
    if (task is OrderingTask) {
      return OrderingTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    if (task is InfoCardTask) {
      final bool hasNext = index < totalTasks - 1;
      return InfoCardTaskCard(
        task: task,
        style: style,
        onActionPressed: hasNext
            ? () => _controller.nextPage(
                  duration: const Duration(milliseconds: 300),
                  curve: Curves.easeOut,
                )
            : null,
      );
    }
    if (task is FillInTheBlankTask) {
      return FillInTheBlankTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    if (task is MultipleChoiceTask) {
      return MultipleChoiceTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    if (task is SpotTheErrorTask) {
      return SpotTheErrorTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    if (task is TrueFalseTask) {
      return TrueFalseTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    if (task is MatchingPairTask) {
      return MatchingPairTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    if (task is CodingChallengeTask) {
      return CodingChallengeTaskCard(
        task: task,
        style: style,
        onAnswerChecked: (isCorrect) =>
            _markTaskCorrect(index, isCorrect, tasks),
      );
    }
    return GenericTaskCard(
      task: task,
      style: style,
      detailLines: _buildTaskDetails(task),
    );
  }

  void _markTaskCorrect(int index, bool isCorrect, List<Task> tasks) {
    final int totalTasks = tasks.length;
    final bool wasCorrect = _taskCorrect[index] ?? false;
    _taskAttempts[index] = (_taskAttempts[index] ?? 0) + 1;
    setState(() => _taskCorrect[index] = isCorrect);
    _recordTaskProgress(
      task: tasks[index],
      index: index,
      isCorrect: isCorrect,
      tasks: tasks,
    );
    final bool showCompletionPages = _areTasksComplete(tasks);
    final int totalPages = totalTasks + (showCompletionPages ? 2 : 0);
    if (isCorrect && !wasCorrect && index < totalPages - 1) {
      _controller.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    }
  }

  void _recordTaskProgress({
    required Task task,
    required int index,
    required bool isCorrect,
    required List<Task> tasks,
  }) {
    final userKey = _userKey;
    final courseId = _courseId;
    final activityId = _activityId;
    if (userKey == null ||
        userKey.isEmpty ||
        courseId == null ||
        courseId.isEmpty ||
        activityId == null ||
        activityId.isEmpty) {
      return;
    }
    final attemptCount = _taskAttempts[index] ?? 1;
    final stats = TaskStats(
      attemptDateTime: DateTime.now().toIso8601String(),
      retries: attemptCount > 0 ? attemptCount - 1 : 0,
      success: isCorrect,
      completionRatio: isCorrect ? 1.0 : 0.0,
      scoreRatio: isCorrect ? 1.0 : 0.0,
    );
    _activityProgressRepository.addTaskStats(
      userId: userKey,
      courseId: courseId,
      activityId: activityId,
      taskId: task.id,
      taskStats: stats,
    );
    if (_areTasksComplete(tasks)) {
      final score = _calculateActivityScore(tasks);
      _activityProgressRepository.updateHighestScoreIfGreater(
        userId: userKey,
        courseId: courseId,
        activityId: activityId,
        score: score,
      );
    }
  }

  int _calculateActivityScore(List<Task> tasks) {
    final requiredTasks = _requiredTaskCount(tasks);
    if (requiredTasks > 0) {
      final correctTasks = _correctTaskCount(tasks);
      return ((correctTasks / requiredTasks) * 100).round();
    }
    final totalTasks = tasks.length;
    if (totalTasks == 0) {
      return 0;
    }
    final completedTasks = _completedTaskCount(tasks);
    return ((completedTasks / totalTasks) * 100).round();
  }

  bool _isTaskVerified(Task task, int index) {
    if (!_requiresCorrectAnswer(task)) {
      return true;
    }
    return _taskCorrect[index] ?? _isTaskMarkedComplete(task);
  }

  bool _requiresCorrectAnswer(Task task) {
    return task is MultipleChoiceTask ||
        task is TrueFalseTask ||
        task is OrderingTask ||
        task is FillInTheBlankTask ||
        task is MatchingPairTask ||
        task is SpotTheErrorTask ||
        task is CodingChallengeTask;
  }

  int _completedTaskCount(List<Task> tasks) {
    return tasks.asMap().entries.where((entry) {
      final task = entry.value;
      final index = entry.key;
      return _isTaskSatisfied(task, index);
    }).length;
  }

  int _requiredTaskCount(List<Task> tasks) {
    return tasks.where(_requiresCorrectAnswer).length;
  }

  int _correctTaskCount(List<Task> tasks) {
    return tasks.asMap().entries.where((entry) {
      final task = entry.value;
      if (!_requiresCorrectAnswer(task)) {
        return false;
      }
      return _taskCorrect[entry.key] ?? _isTaskMarkedComplete(task);
    }).length;
  }

  Widget _buildStatisticsCard(BuildContext context, List<Task> tasks) {
    final int totalTasks = tasks.length;
    final int completedTasks = _completedTaskCount(tasks);
    final int requiredTasks = _requiredTaskCount(tasks);
    final int correctTasks = _correctTaskCount(tasks);
    final double completionRatio =
        totalTasks == 0 ? 0 : completedTasks / totalTasks;
    final double correctnessRatio =
        requiredTasks == 0 ? 0 : correctTasks / requiredTasks;
    final stats = [
      _ActivityStat(
        label: 'Completion rate',
        value: completionRatio,
        supportingText: '$completedTasks of $totalTasks tasks',
      ),
      _ActivityStat(
        label: 'Correct answers',
        value: correctnessRatio,
        supportingText: '$correctTasks of $requiredTasks checked tasks',
      ),
    ];
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Your statistics',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Here is a quick snapshot of your performance.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            ...stats.map(
              (stat) => Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      stat.label,
                      style: Theme.of(context).textTheme.bodyLarge,
                    ),
                    const SizedBox(height: 6),
                    LinearProgressIndicator(value: stat.value),
                    const SizedBox(height: 4),
                    Text(
                      '${(stat.value * 100).round()}% • ${stat.supportingText}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
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

class _ActivityStat {
  const _ActivityStat({
    required this.label,
    required this.value,
    required this.supportingText,
  });

  final String label;
  final double value;
  final String supportingText;
}

enum _RecommendationType { website, youtube, pdf, doc }

class _RecommendationMaterial {
  const _RecommendationMaterial({
    required this.title,
    required this.url,
    required this.description,
    required this.type,
  });

  final String title;
  final String url;
  final String description;
  final _RecommendationType type;
}

class _RecommendationListTile extends StatelessWidget {
  const _RecommendationListTile({
    required this.recommendation,
    required this.onTap,
  });

  final _RecommendationMaterial recommendation;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(_iconForType(recommendation.type)),
      title: Text(recommendation.title),
      subtitle: Text(recommendation.description),
      trailing: const Icon(Icons.open_in_new),
      onTap: onTap,
    );
  }

  IconData _iconForType(_RecommendationType type) {
    switch (type) {
      case _RecommendationType.website:
        return Icons.public;
      case _RecommendationType.youtube:
        return Icons.play_circle;
      case _RecommendationType.pdf:
        return Icons.picture_as_pdf;
      case _RecommendationType.doc:
        return Icons.description;
    }
  }
}
