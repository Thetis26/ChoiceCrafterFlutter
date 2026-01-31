import 'package:cloud_firestore/cloud_firestore.dart';

import '../models/enrollment_activity_progress.dart';
import '../models/task_stats.dart';

class ActivityProgressRepository {
  ActivityProgressRepository({FirebaseFirestore? firestore})
      : _firestore = firestore ?? FirebaseFirestore.instance;

  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';

  final FirebaseFirestore _firestore;

  String _buildEnrollmentId(String userId, String courseId) {
    return '${userId}_$courseId';
  }

  Future<EnrollmentActivityProgress?> startActivity({
    required String userId,
    required String courseId,
    required String activityId,
  }) async {
    if (userId.isEmpty || courseId.isEmpty || activityId.isEmpty) {
      return null;
    }
    final documentReference = _firestore
        .collection(_enrollmentsCollection)
        .doc(_buildEnrollmentId(userId, courseId));
    final documentSnapshot = await documentReference.get();
    if (!documentSnapshot.exists) {
      return null;
    }

    final progressSummary = _progressSummaryFromDoc(documentSnapshot);
    final activitySnapshots = _activitySnapshotsFromSummary(progressSummary);
    final snapshot = _getOrCreateActivitySnapshot(
      activitySnapshots,
      userId,
      courseId,
      activityId,
    );

    await documentReference.set(
      _buildProgressUpdate(progressSummary),
      SetOptions(merge: true),
    );

    return EnrollmentActivityProgress.fromMap(snapshot);
  }

  Future<void> addTaskStats({
    required String userId,
    required String courseId,
    required String activityId,
    required String taskId,
    required TaskStats taskStats,
  }) async {
    if (userId.isEmpty ||
        courseId.isEmpty ||
        activityId.isEmpty ||
        taskId.isEmpty) {
      return;
    }
    final documentReference = _firestore
        .collection(_enrollmentsCollection)
        .doc(_buildEnrollmentId(userId, courseId));

    await _firestore.runTransaction((transaction) async {
      final documentSnapshot = await transaction.get(documentReference);
      if (!documentSnapshot.exists) {
        return;
      }

      final progressSummary = _progressSummaryFromDoc(documentSnapshot);
      final activitySnapshots = _activitySnapshotsFromSummary(progressSummary);
      final snapshot = _getOrCreateActivitySnapshot(
        activitySnapshots,
        userId,
        courseId,
        activityId,
      );

      final taskStatsMap = _ensureTaskStatsMap(snapshot);
      taskStatsMap[taskId] = taskStats.toMap();
      snapshot['taskStats'] = taskStatsMap;

      transaction.set(
        documentReference,
        _buildProgressUpdate(progressSummary),
        SetOptions(merge: true),
      );
    });
  }

  Future<void> updateHighestScoreIfGreater({
    required String userId,
    required String courseId,
    required String activityId,
    required int score,
  }) async {
    if (userId.isEmpty || courseId.isEmpty || activityId.isEmpty) {
      return;
    }
    final documentReference = _firestore
        .collection(_enrollmentsCollection)
        .doc(_buildEnrollmentId(userId, courseId));
    final documentSnapshot = await documentReference.get();
    if (!documentSnapshot.exists) {
      return;
    }

    final progressSummary = _progressSummaryFromDoc(documentSnapshot);
    final activitySnapshots = _activitySnapshotsFromSummary(progressSummary);
    final snapshot = _getOrCreateActivitySnapshot(
      activitySnapshots,
      userId,
      courseId,
      activityId,
    );
    final highestScore = snapshot['highestScore'];
    final currentScore =
        highestScore is num ? highestScore.toInt() : null;
    if (currentScore == null || score > currentScore) {
      snapshot['highestScore'] = score;
      await documentReference.set(
        _buildProgressUpdate(progressSummary),
        SetOptions(merge: true),
      );
    }
  }

  Map<String, dynamic> _progressSummaryFromDoc(
    DocumentSnapshot<Map<String, dynamic>> documentSnapshot,
  ) {
    final summary = documentSnapshot.data()?['progressSummary'];
    if (summary is Map) {
      return Map<String, dynamic>.from(summary);
    }
    return <String, dynamic>{};
  }

  List<Map<String, dynamic>> _activitySnapshotsFromSummary(
    Map<String, dynamic> progressSummary,
  ) {
    final snapshots = progressSummary['activitySnapshots'];
    if (snapshots is List) {
      final converted = <Map<String, dynamic>>[];
      for (final item in snapshots) {
        if (item is Map) {
          converted.add(Map<String, dynamic>.from(item));
        }
      }
      progressSummary['activitySnapshots'] = converted;
      return converted;
    }
    final converted = <Map<String, dynamic>>[];
    progressSummary['activitySnapshots'] = converted;
    return converted;
  }

  Map<String, dynamic> _getOrCreateActivitySnapshot(
    List<Map<String, dynamic>> activitySnapshots,
    String userId,
    String courseId,
    String activityId,
  ) {
    for (final snapshot in activitySnapshots) {
      final snapshotId = snapshot['activityId'];
      if (snapshotId != null && snapshotId.toString() == activityId) {
        _ensureSnapshotIdentifiers(
          snapshot,
          userId,
          courseId,
          activityId,
        );
        return snapshot;
      }
    }
    final snapshot = <String, dynamic>{
      'activityId': activityId,
      'courseId': courseId,
      'userId': userId,
      'taskStats': <String, dynamic>{},
    };
    activitySnapshots.add(snapshot);
    return snapshot;
  }

  void _ensureSnapshotIdentifiers(
    Map<String, dynamic> snapshot,
    String userId,
    String courseId,
    String activityId,
  ) {
    snapshot['userId'] = userId;
    snapshot['courseId'] = courseId;
    snapshot['activityId'] = activityId;
    if (snapshot['taskStats'] is! Map) {
      snapshot['taskStats'] = <String, dynamic>{};
    }
  }

  Map<String, dynamic> _ensureTaskStatsMap(
    Map<String, dynamic> snapshot,
  ) {
    final stats = snapshot['taskStats'];
    if (stats is Map) {
      return Map<String, dynamic>.from(stats);
    }
    final empty = <String, dynamic>{};
    snapshot['taskStats'] = empty;
    return empty;
  }

  Map<String, dynamic> _buildProgressUpdate(
    Map<String, dynamic> progressSummary,
  ) {
    return {'progressSummary': progressSummary};
  }
}
