class ActivityData {
  const ActivityData({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.content,
    this.estimatedMinutes = 15,
  });

  final String id;
  final String name;
  final String description;
  final String type;
  final String content;
  final int estimatedMinutes;
}

class ModuleData {
  const ModuleData({
    required this.id,
    required this.name,
    required this.summary,
    required this.activities,
  });

  final String id;
  final String name;
  final String summary;
  final List<ActivityData> activities;
}

class CourseData {
  const CourseData({
    required this.id,
    required this.title,
    required this.instructor,
    required this.summary,
    required this.modules,
  });

  final String id;
  final String title;
  final String instructor;
  final String summary;
  final List<ModuleData> modules;
}

class InboxItem {
  const InboxItem({
    required this.id,
    required this.title,
    required this.body,
    required this.timestamp,
  });

  final String id;
  final String title;
  final String body;
  final DateTime timestamp;
}

class MessageThread {
  const MessageThread({
    required this.id,
    required this.sender,
    required this.preview,
    required this.unreadCount,
    required this.timestamp,
  });

  final String id;
  final String sender;
  final String preview;
  final int unreadCount;
  final DateTime timestamp;
}

class LearningPathData {
  const LearningPathData({
    required this.id,
    required this.title,
    required this.description,
    required this.topics,
    required this.recommendation,
  });

  final String id;
  final String title;
  final String description;
  final List<String> topics;
  final RecommendationData recommendation;
}

class RecommendationData {
  const RecommendationData({
    required this.id,
    required this.title,
    required this.url,
    required this.summary,
  });

  final String id;
  final String title;
  final String url;
  final String summary;
}

class SampleData {
  static final List<ActivityData> activities = [
    const ActivityData(
      id: 'activity-1',
      name: 'Getting started with Kotlin',
      description: 'A quick primer that mirrors the onboarding flow from Android.',
      type: 'article',
      content: 'Review nullable types, collections, and coroutines basics.',
      estimatedMinutes: 20,
    ),
    const ActivityData(
      id: 'activity-2',
      name: 'Build your first API client',
      description: 'Hands-on exercise that matches the Android coding lab.',
      type: 'exercise',
      content: 'Use the provided REST endpoint and render results in the UI.',
      estimatedMinutes: 35,
    ),
    const ActivityData(
      id: 'activity-3',
      name: 'Weekly reflection',
      description: 'Short journaling task that mirrors the reflection cards.',
      type: 'reflection',
      content: 'Capture blockers, wins, and the next action you will take.',
      estimatedMinutes: 10,
    ),
  ];

  static final List<CourseData> courses = [
    CourseData(
      id: 'course-mobile',
      title: 'Mobile Development Foundations',
      instructor: 'Dr. C. Crafter',
      summary: 'Match the Android track with identical iOS milestones.',
      modules: [
        ModuleData(
          id: 'module-1',
          name: 'Productivity basics',
          summary: 'Short lessons and practice prompts from the Android release.',
          activities: activities,
        ),
        ModuleData(
          id: 'module-2',
          name: 'User engagement',
          summary: 'Mirrors the colleagues activity and inbox experiences.',
          activities: [activities[1], activities[2]],
        ),
      ],
    ),
    CourseData(
      id: 'course-ai',
      title: 'AI Assisted Learning',
      instructor: 'Prof. A. Mentor',
      summary: 'Keep parity with the recommendation and statistics screens.',
      modules: [
        ModuleData(
          id: 'module-3',
          name: 'Guided learning path',
          summary: 'Practice lessons that surface recommendations on demand.',
          activities: [activities[0], activities[2]],
        ),
      ],
    ),
  ];

  static final List<InboxItem> inbox = [
    InboxItem(
      id: 'inbox-1',
      title: 'New activity assigned',
      body: 'Your instructor added a reflection just like on Android.',
      timestamp: DateTime.now().subtract(const Duration(hours: 1)),
    ),
    InboxItem(
      id: 'inbox-2',
      title: 'Peer comment',
      body: 'Amira left feedback on your submission.',
      timestamp: DateTime.now().subtract(const Duration(hours: 4)),
    ),
  ];

  static final List<MessageThread> messages = [
    MessageThread(
      id: 'thread-1',
      sender: 'Learning Coach',
      preview: 'Remember to finish the exercise from the Android build.',
      unreadCount: 1,
      timestamp: DateTime.now().subtract(const Duration(minutes: 24)),
    ),
    MessageThread(
      id: 'thread-2',
      sender: 'Study buddy',
      preview: 'Shall we pair on the recommendation screen redesign?',
      unreadCount: 0,
      timestamp: DateTime.now().subtract(const Duration(hours: 6)),
    ),
  ];

  static final List<LearningPathData> learningPaths = [
    LearningPathData(
      id: 'lp-1',
      title: 'Engagement focus',
      description: 'Combines colleagues activity, inbox, and feedback patterns.',
      topics: ['Activity feed', 'Notifications', 'Messaging'],
      recommendation: RecommendationData(
        id: 'rec-1',
        title: 'How to mirror Android engagement on iOS',
        url: 'https://example.org/engagement-guide',
        summary: 'Implementation checklist to keep parity between platforms.',
      ),
    ),
    LearningPathData(
      id: 'lp-2',
      title: 'Progress and statistics',
      description: 'Match the Android statistics dashboard with Flutter widgets.',
      topics: ['Progress tracking', 'Badges', 'Time on task'],
      recommendation: RecommendationData(
        id: 'rec-2',
        title: 'Measuring completion in Flutter',
        url: 'https://example.org/statistics-guide',
        summary: 'Charts and KPIs that align with the native Android screen.',
      ),
    ),
  ];
}
