import '../models/activity.dart';
import '../models/course.dart';
import '../models/module.dart';
import '../models/user.dart';

class CourseRepository {
  CourseRepository();

  final List<Activity> _activities = const [
    Activity(
      id: 'activity-1',
      name: 'Getting started with Kotlin',
      description: 'A quick primer that mirrors the onboarding flow from Android.',
      type: 'article',
      content: 'Review nullable types, collections, and coroutines basics.',
      estimatedMinutes: 20,
    ),
    Activity(
      id: 'activity-2',
      name: 'Build your first API client',
      description: 'Hands-on exercise that matches the Android coding lab.',
      type: 'exercise',
      content: 'Use the provided REST endpoint and render results in the UI.',
      estimatedMinutes: 35,
    ),
    Activity(
      id: 'activity-3',
      name: 'Weekly reflection',
      description: 'Short journaling task that mirrors the reflection cards.',
      type: 'reflection',
      content: 'Capture blockers, wins, and the next action you will take.',
      estimatedMinutes: 10,
    ),
  ];

  late final List<Course> _courses = [
    Course(
      id: 'course-mobile',
      title: 'Mobile Development Foundations',
      instructor: 'Dr. C. Crafter',
      summary: 'Match the Android track with identical iOS milestones.',
      modules: [
        Module(
          id: 'module-1',
          name: 'Productivity basics',
          summary: 'Short lessons and practice prompts from the Android release.',
          activities: _activities,
        ),
        Module(
          id: 'module-2',
          name: 'User engagement',
          summary: 'Mirrors the colleagues activity and inbox experiences.',
          activities: [_activities[1], _activities[2]],
        ),
      ],
    ),
    Course(
      id: 'course-ai',
      title: 'AI Assisted Learning',
      instructor: 'Prof. A. Mentor',
      summary: 'Keep parity with the recommendation and statistics screens.',
      modules: [
        Module(
          id: 'module-3',
          name: 'Guided learning path',
          summary: 'Practice lessons that surface recommendations on demand.',
          activities: [_activities[0], _activities[2]],
        ),
      ],
    ),
  ];

  List<Course> getAllCourses() => _courses;

  List<Course> getEnrolledCourses(User user) {
    return _courses
        .where((course) => user.enrolledCourseIds.contains(course.id))
        .toList();
  }

  Course? getCourseById(String id) {
    try {
      return _courses.firstWhere((course) => course.id == id);
    } catch (_) {
      return null;
    }
  }

  Module? getModuleById(String moduleId) {
    for (final course in _courses) {
      for (final module in course.modules) {
        if (module.id == moduleId) {
          return module;
        }
      }
    }
    return null;
  }

  List<Activity> getAllActivities() {
    return _courses
        .expand((course) => course.modules)
        .expand((module) => module.activities)
        .toList();
  }
}
