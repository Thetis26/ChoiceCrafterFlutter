import 'package:cloud_firestore/cloud_firestore.dart';

import '../models/activity.dart';
import '../models/comment.dart';
import '../models/recommendation.dart';
import '../models/course.dart';
import '../models/module.dart';
import '../models/task.dart';
import '../models/user.dart';

class CourseRepository {
  CourseRepository({FirebaseFirestore? firestore})
      : _firestore = firestore ?? FirebaseFirestore.instance;

  static const String _coursesCollection = 'COURSES';
  static const String _legacyCoursesCollection = 'courses';
  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';
  static const String _legacyEnrollmentsCollection = 'course_enrollment';
  static const String _newsAvailableCoursesCollection =
      'NEWS_AVAILABLE_COURSES';
  static const String _userAvailableCoursesCollection =
      'USER_AVAILABLE_COURSES';
  static const String _globalAvailableCoursesDoc = 'GLOBAL';

  final FirebaseFirestore _firestore;
  List<Course> _courses = [];

  Future<List<Course>> getAllCourses() async {
    if (_courses.isNotEmpty) {
      return _courses;
    }
    return _loadCourses();
  }

  Future<List<Course>> refreshCourses() async {
    return _loadCourses();
  }

  Future<List<Course>> _loadCourses() async {
    final snapshot = await _firestore.collection(_coursesCollection).get();
    if (snapshot.docs.isNotEmpty) {
      _courses = snapshot.docs.map(_courseFromDoc).toList();
      return _courses;
    }

    final legacySnapshot =
        await _firestore.collection(_legacyCoursesCollection).get();
    _courses = legacySnapshot.docs.map(_courseFromDoc).toList();
    return _courses;
  }

  Future<List<Course>> getEnrolledCourses(User user) async {
    final enrolledCourseIds = await _loadEnrolledCourseIds(user);
    if (enrolledCourseIds.isEmpty) {
      return [];
    }
    final courses = await getAllCourses();
    return courses
        .where((course) => enrolledCourseIds.contains(course.id))
        .toList();
  }

  Future<Course?> getCourseById(String id) async {
    if (_courses.isNotEmpty) {
      try {
        return _courses.firstWhere((course) => course.id == id);
      } catch (_) {
        return null;
      }
    }

    final doc = await _firestore.collection(_coursesCollection).doc(id).get();
    if (doc.exists) {
      return _courseFromDoc(doc);
    }

    final legacyDoc =
        await _firestore.collection(_legacyCoursesCollection).doc(id).get();
    if (!legacyDoc.exists) {
      return null;
    }
    return _courseFromDoc(legacyDoc);
  }

  Future<List<Course>> getAvailableCourses(User user) async {
    final availableCourseIds = await _loadAvailableCourseIds(user);
    if (availableCourseIds.isEmpty) {
      return [];
    }
    final courses = await getAllCourses();
    return courses
        .where((course) => availableCourseIds.contains(course.id))
        .toList();
  }

  Future<Module?> getModuleById(String moduleId) async {
    final courses = await getAllCourses();
    for (final course in courses) {
      for (final module in course.modules) {
        if (module.id == moduleId) {
          return module;
        }
      }
    }
    return null;
  }

  Future<List<Activity>> getAllActivities() async {
    final courses = await getAllCourses();
    return courses
        .expand((course) => course.modules)
        .expand((module) => module.activities)
        .toList();
  }

  Future<List<String>> _loadEnrolledCourseIds(User user) async {
    final userKey = user.email.isNotEmpty ? user.email : user.id;
    final courseIds = await _fetchEnrollmentCourseIds(userKey);

    if (courseIds.isNotEmpty) {
      return courseIds;
    }
    return user.enrolledCourseIds;
  }

  Future<List<String>> _loadAvailableCourseIds(User user) async {
    final userKey = user.email.isNotEmpty ? user.email : user.id;
    final globalDoc = await _firestore
        .collection(_newsAvailableCoursesCollection)
        .doc(_globalAvailableCoursesDoc)
        .get();
    final globalIds = _courseIdsFromDoc(globalDoc);

    if (userKey.isEmpty) {
      return globalIds;
    }

    final userDoc = await _firestore
        .collection(_userAvailableCoursesCollection)
        .doc(userKey)
        .get();
    final userIds = _courseIdsFromDoc(userDoc);

    return {...globalIds, ...userIds}.toList();
  }

  List<String> _courseIdsFromDoc(
    DocumentSnapshot<Map<String, dynamic>> doc,
  ) {
    final data = doc.data();
    final rawIds = data?['availableCourses'];
    if (rawIds is! List) {
      return [];
    }
    return rawIds.whereType<String>().toList();
  }

  Future<List<String>> _fetchEnrollmentCourseIds(String userKey) async {
    if (userKey.isEmpty) {
      return [];
    }
    final snapshots = await Future.wait([
      _firestore
          .collection(_enrollmentsCollection)
          .where('userId', isEqualTo: userKey)
          .get(),
      _firestore
          .collection(_legacyEnrollmentsCollection)
          .where('userId', isEqualTo: userKey)
          .get(),
    ]);

    final courseIds = <String>{};
    for (final snapshot in snapshots) {
      for (final doc in snapshot.docs) {
        final data = doc.data();
        final courseId = (data['courseId'] as String?) ??
            (data['course_id'] as String?) ??
            '';
        if (courseId.isNotEmpty) {
          courseIds.add(courseId);
        }
      }
    }
    return courseIds.toList();
  }

  Future<void> enrollInCourse(User user, Course course) async {
    final userKey = user.email.isNotEmpty ? user.email : user.id;
    if (userKey.isEmpty) {
      return;
    }
    final enrollmentDocId = '${userKey}_${course.id}';
    await _firestore
        .collection(_enrollmentsCollection)
        .doc(enrollmentDocId)
        .set({
      'userId': userKey,
      'courseId': course.id,
      'enrollmentDate': _formatEnrollmentDate(DateTime.now()),
      'selfEnrolled': true,
      'enrolledBy': userKey,
      'createdAt': FieldValue.serverTimestamp(),
    }, SetOptions(merge: true));
  }

  String _formatEnrollmentDate(DateTime date) {
    final year = date.year.toString().padLeft(4, '0');
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  Course _courseFromDoc(DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return Course(
      id: (data['id'] as String?) ?? doc.id,
      title: (data['title'] as String?) ?? 'Untitled course',
      instructor: (data['teacher'] as String?) ?? 'Unknown instructor',
      summary: (data['description'] as String?) ?? '',
      description: data['description'] as String?,
      teacher: data['teacher'] as String?,
      imageUrl: data['imageUrl'] as String?,
      activities: _activitiesFromData(data['activities']),
      modules: _modulesFromData(data['modules']),
    );
  }

  List<Module> _modulesFromData(dynamic modulesData) {
    if (modulesData is! List) {
      return [];
    }

    return modulesData.map((moduleData) {
      final moduleMap = moduleData is Map
          ? Map<String, dynamic>.from(moduleData as Map)
          : <String, dynamic>{};
      return Module(
        id: (moduleMap['id'] as String?) ?? '',
        name: (moduleMap['title'] as String?) ?? 'Untitled module',
        summary: (moduleMap['description'] as String?) ?? '',
        activities: _activitiesFromData(moduleMap['activities']),
        title: moduleMap['title'] as String?,
        description: moduleMap['description'] as String?,
        completedPercentage: (moduleMap['completedPercentage'] as num?)?.toInt() ??
            (moduleMap['completionPercentage'] as num?)?.toInt() ??
            0,
        courseId: moduleMap['courseId'] as String?,
      );
    }).toList();
  }

  List<Activity> _activitiesFromData(dynamic activitiesData) {
    if (activitiesData is! List) {
      return [];
    }

    return activitiesData.map((activityData) {
      final activityMap = activityData is Map
          ? Map<String, dynamic>.from(activityData as Map)
          : <String, dynamic>{};
      final estimatedMinutes = activityMap['estimatedMinutes'];
      return Activity(
        id: (activityMap['id'] as String?) ?? '',
        name: (activityMap['title'] as String?) ?? 'Untitled activity',
        title: activityMap['title'] as String?,
        description: (activityMap['description'] as String?) ?? '',
        type: (activityMap['type'] as String?) ?? 'activity',
        content: (activityMap['content'] as String?) ?? '',
        date: activityMap['date'] as String?,
        time: activityMap['time'] as String?,
        status: _activityStatusFrom(activityMap['status']),
        reminders: _stringList(activityMap['reminders']),
        estimatedMinutes: estimatedMinutes is num ? estimatedMinutes.round() : 15,
        tasks: _tasksFromData(activityMap['tasks']),
        recommendations: _recommendationsFromData(
          activityMap['recommendations'],
        ),
        reactions: _reactionsFromData(activityMap['reactions']),
        reactionCounts: _reactionCountsFromData(
          activityMap['reactionCounts'] ?? activityMap['reactions'],
        ),
        comments: _commentsFromData(activityMap['comments']),
      );
    }).toList();
  }

  List<Recommendation> _recommendationsFromData(dynamic recommendationsData) {
    if (recommendationsData is! List) {
      return [];
    }
    return recommendationsData
        .map((entry) {
          if (entry is Map) {
            return Recommendation.fromMap(
              Map<String, dynamic>.from(entry),
            );
          }
          return null;
        })
        .whereType<Recommendation>()
        .toList();
  }

  List<ActivityReaction> _reactionsFromData(dynamic reactionsData) {
    if (reactionsData is! List) {
      return [];
    }
    return reactionsData.map((reactionData) {
      if (reactionData is Map) {
        final data = Map<String, dynamic>.from(reactionData);
        final countValue = data['count'] ?? data['total'];
        return ActivityReaction(
          type: (data['type'] as String?) ?? 'like',
          count: countValue is num ? countValue.round() : 0,
        );
      }
      if (reactionData is String) {
        return ActivityReaction(type: reactionData, count: 1);
      }
      return const ActivityReaction(type: 'like', count: 0);
    }).toList();
  }

  Status _activityStatusFrom(dynamic statusData) {
    final raw = statusData?.toString().toLowerCase();
    switch (raw) {
      case 'started':
        return Status.started;
      case 'ended':
        return Status.ended;
      case 'created':
      default:
        return Status.created;
    }
  }

  List<String> _stringList(dynamic raw) {
    if (raw is List) {
      return raw
          .where((entry) => entry != null)
          .map((entry) => entry.toString())
          .toList();
    }
    return [];
  }

  Map<String, int> _reactionCountsFromData(dynamic raw) {
    if (raw is Map) {
      return raw.map((key, value) => MapEntry(
            key.toString(),
            value is num ? value.toInt() : int.tryParse(value.toString()) ?? 0,
          ));
    }
    if (raw is List) {
      final counts = <String, int>{};
      for (final entry in raw) {
        if (entry is Map) {
          final type = entry['type']?.toString();
          if (type == null) {
            continue;
          }
          final countValue = entry['count'] ?? entry['total'];
          counts[type] =
              countValue is num ? countValue.toInt() : int.tryParse('$countValue') ?? 0;
        } else if (entry is String) {
          counts[entry] = (counts[entry] ?? 0) + 1;
        }
      }
      return counts;
    }
    return {};
  }

  List<Comment> _commentsFromData(dynamic commentsData) {
    if (commentsData is! List) {
      return [];
    }
    return commentsData.map((commentData) {
      if (commentData is Map) {
        final data = Map<String, dynamic>.from(commentData);
        final message = (data['message'] as String?) ??
            (data['comment'] as String?) ??
            '';
        if (message.isEmpty) {
          return null;
        }
        return Comment(
          userId: (data['author'] as String?) ??
              (data['user'] as String?) ??
              'Anonymous',
          text: message,
          timestamp: _parseCommentTimestamp(
            data['timestamp'] ?? data['createdAt'],
          ),
        );
      }
      return null;
    }).whereType<Comment>().toList();
  }

  String _parseCommentTimestamp(dynamic rawTimestamp) {
    if (rawTimestamp is Timestamp) {
      return rawTimestamp.toDate().toIso8601String();
    }
    if (rawTimestamp is DateTime) {
      return rawTimestamp.toIso8601String();
    }
    if (rawTimestamp is String) {
      return rawTimestamp;
    }
    if (rawTimestamp is num) {
      return DateTime.fromMillisecondsSinceEpoch(rawTimestamp.round())
          .toIso8601String();
    }
    return DateTime.now().toIso8601String();
  }

  List<Task> _tasksFromData(dynamic tasksData) {
    if (tasksData is! List) {
      return [];
    }

    return tasksData.map(_taskFromData).whereType<Task>().toList();
  }

  Task? _taskFromData(dynamic taskData) {
    if (taskData is Map) {
      return Task.fromMap(Map<String, dynamic>.from(taskData));
    }
    if (taskData is DocumentSnapshot) {
      final data = taskData.data();
      if (data is Map) {
        return Task.fromMap(Map<String, dynamic>.from(data));
      }
    }
    return null;
  }
}
