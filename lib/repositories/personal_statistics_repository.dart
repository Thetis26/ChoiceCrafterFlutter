import 'package:cloud_firestore/cloud_firestore.dart';

import '../models/enrollment_activity_snapshot.dart';

class PersonalStatisticsRepository {
  PersonalStatisticsRepository({FirebaseFirestore? firestore})
      : _firestore = firestore ?? FirebaseFirestore.instance;

  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';
  static const String _legacyEnrollmentsCollection = 'course_enrollment';

  final FirebaseFirestore _firestore;

  Future<List<EnrollmentActivitySnapshot>> fetchActivitySnapshotsForUser(
    String userKey,
  ) async {
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

    final activitySnapshots = <EnrollmentActivitySnapshot>[];
    for (final snapshot in snapshots) {
      for (final doc in snapshot.docs) {
        final data = doc.data();
        final progressSummary =
            (data['progressSummary'] as Map?)?.cast<String, dynamic>() ??
                <String, dynamic>{};
        final rawSnapshots = progressSummary['activitySnapshots'];
        if (rawSnapshots is! List) {
          continue;
        }
        for (final entry in rawSnapshots) {
          if (entry is! Map) {
            continue;
          }
          final entryMap = Map<String, dynamic>.from(entry);
          if ((entryMap['courseId'] as String?)?.isEmpty ?? true) {
            final courseId = data['courseId'] as String?;
            if (courseId != null && courseId.isNotEmpty) {
              entryMap['courseId'] = courseId;
            }
          }
          activitySnapshots.add(EnrollmentActivitySnapshot.fromMap(entryMap));
        }
      }
    }
    return activitySnapshots;
  }
}
