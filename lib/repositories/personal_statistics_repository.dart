import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';

import '../models/enrollment_activity_progress.dart';

class PersonalStatisticsRepository {
  PersonalStatisticsRepository({FirebaseFirestore? firestore})
      : _firestore = firestore ?? FirebaseFirestore.instance;

  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';
  static const String _legacyEnrollmentsCollection = 'course_enrollment';

  final FirebaseFirestore _firestore;

  Stream<List<EnrollmentActivityProgress>> streamActivitySnapshotsForUser(
    String userKey,
  ) {
    if (userKey.isEmpty) {
      return Stream.value([]);
    }

    final controller =
        StreamController<List<EnrollmentActivityProgress>>.broadcast();
    QuerySnapshot<Map<String, dynamic>>? primarySnapshot;
    QuerySnapshot<Map<String, dynamic>>? legacySnapshot;
    StreamSubscription<QuerySnapshot<Map<String, dynamic>>>? primarySub;
    StreamSubscription<QuerySnapshot<Map<String, dynamic>>>? legacySub;

    void emit() {
      final snapshots = <QuerySnapshot<Map<String, dynamic>>>[];
      if (primarySnapshot != null) {
        snapshots.add(primarySnapshot!);
      }
      if (legacySnapshot != null) {
        snapshots.add(legacySnapshot!);
      }
      controller.add(_collectActivitySnapshots(snapshots));
    }

    primarySub = _firestore
        .collection(_enrollmentsCollection)
        .where('userId', isEqualTo: userKey)
        .snapshots()
        .listen(
      (snapshot) {
        primarySnapshot = snapshot;
        emit();
      },
      onError: controller.addError,
    );

    legacySub = _firestore
        .collection(_legacyEnrollmentsCollection)
        .where('userId', isEqualTo: userKey)
        .snapshots()
        .listen(
      (snapshot) {
        legacySnapshot = snapshot;
        emit();
      },
      onError: controller.addError,
    );

    controller.onCancel = () async {
      await primarySub?.cancel();
      await legacySub?.cancel();
      await controller.close();
    };

    return controller.stream;
  }

  Future<List<EnrollmentActivityProgress>> fetchActivitySnapshotsForUser(
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
    return _collectActivitySnapshots(snapshots);
  }

  List<EnrollmentActivityProgress> _collectActivitySnapshots(
    List<QuerySnapshot<Map<String, dynamic>>> snapshots,
  ) {
    final activitySnapshots = <EnrollmentActivityProgress>[];
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
          activitySnapshots.add(EnrollmentActivityProgress.fromMap(entryMap));
        }
      }
    }
    return activitySnapshots;
  }
}
