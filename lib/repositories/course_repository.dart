import 'package:cloud_firestore/cloud_firestore.dart';

import '../models/activity.dart';
import '../models/course.dart';
import '../models/module.dart';
import '../models/user.dart';

class CourseRepository {
  CourseRepository({FirebaseFirestore? firestore})
      : _firestore = firestore ?? FirebaseFirestore.instance;

  static const String _coursesCollection = 'COURSES';
  static const String _legacyCoursesCollection = 'courses';
  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';
  static const String _legacyEnrollmentsCollection = 'course_enrollment';

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

  Course _courseFromDoc(DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return Course(
      id: (data['id'] as String?) ?? doc.id,
      title: (data['title'] as String?) ?? 'Untitled course',
      instructor: (data['instructor'] as String?) ?? 'Unknown instructor',
      summary: (data['summary'] as String?) ?? '',
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
        name: (moduleMap['name'] as String?) ?? 'Untitled module',
        summary: (moduleMap['summary'] as String?) ?? '',
        activities: _activitiesFromData(moduleMap['activities']),
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
        name: (activityMap['name'] as String?) ?? 'Untitled activity',
        description: (activityMap['description'] as String?) ?? '',
        type: (activityMap['type'] as String?) ?? 'activity',
        content: (activityMap['content'] as String?) ?? '',
        estimatedMinutes: estimatedMinutes is num ? estimatedMinutes.round() : 15,
      );
    }).toList();
  }
}
