import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart';

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
    debugPrint(
      '[ActivityProgressRepository] startActivity userId=$userId courseId=$courseId activityId=$activityId',
    );
    if (userId.isEmpty || courseId.isEmpty || activityId.isEmpty) {
      debugPrint(
        '[ActivityProgressRepository] startActivity aborted: missing identifiers.',
      );
      return null;
    }
    final documentReference = _firestore
        .collection(_enrollmentsCollection)
        .doc(_buildEnrollmentId(userId, courseId));
    final documentSnapshot = await documentReference.get();
    if (!documentSnapshot.exists) {
      debugPrint(
        '[ActivityProgressRepository] startActivity aborted: enrollment doc not found.',
      );
      return null;
    }

    final progressSummary = _progressSummaryFromDoc(documentSnapshot);
    debugPrint(
      '[ActivityProgressRepository] startActivity progressSummary keys=${progressSummary.keys}',
    );
    final activitySnapshots = _activitySnapshotsFromSummary(progressSummary);
    debugPrint(
      '[ActivityProgressRepository] startActivity activitySnapshots=${activitySnapshots.length}',
    );
    final snapshot = _getOrCreateActivitySnapshot(
      activitySnapshots,
      userId,
      courseId,
      activityId,
    );
    debugPrint(
      '[ActivityProgressRepository] startActivity snapshot taskStats=${(snapshot['taskStats'] as Map?)?.length ?? 0}',
    );

    await documentReference.set(
      _buildProgressUpdate(progressSummary),
      SetOptions(merge: true),
    );
    debugPrint('[ActivityProgressRepository] startActivity saved.');

    return EnrollmentActivityProgress.fromMap(snapshot);
  }

  Future<void> addTaskStats({
    required String userId,
    required String courseId,
    required String activityId,
    required String taskId,
    required TaskStats taskStats,
  }) async {
    debugPrint(
      '[ActivityProgressRepository] addTaskStats userId=$userId courseId=$courseId activityId=$activityId taskId=$taskId',
    );
    if (userId.isEmpty ||
        courseId.isEmpty ||
        activityId.isEmpty ||
        taskId.isEmpty) {
      debugPrint(
        '[ActivityProgressRepository] addTaskStats aborted: missing identifiers.',
      );
      return;
    }
    final documentReference = _firestore
        .collection(_enrollmentsCollection)
        .doc(_buildEnrollmentId(userId, courseId));

    await _firestore.runTransaction((transaction) async {
      debugPrint(
        '[ActivityProgressRepository] addTaskStats transaction started.',
      );
      final documentSnapshot = await transaction.get(documentReference);
      if (!documentSnapshot.exists) {
        debugPrint(
          '[ActivityProgressRepository] addTaskStats aborted: enrollment doc not found.',
        );
        return;
      }

      final progressSummary = _progressSummaryFromDoc(documentSnapshot);
      debugPrint(
        '[ActivityProgressRepository] addTaskStats progressSummary keys=${progressSummary.keys}',
      );
      final activitySnapshots = _activitySnapshotsFromSummary(progressSummary);
      debugPrint(
        '[ActivityProgressRepository] addTaskStats activitySnapshots=${activitySnapshots.length}',
      );
      final snapshot = _getOrCreateActivitySnapshot(
        activitySnapshots,
        userId,
        courseId,
        activityId,
      );
      debugPrint(
        '[ActivityProgressRepository] addTaskStats snapshot taskStats=${(snapshot['taskStats'] as Map?)?.length ?? 0}',
      );

      final taskStatsMap = _ensureTaskStatsMap(snapshot);
      debugPrint(
        '[ActivityProgressRepository] addTaskStats taskStatsMap before=${taskStatsMap.length}',
      );
      taskStatsMap[taskId] = taskStats.toMap();
      snapshot['taskStats'] = taskStatsMap;
      debugPrint(
        '[ActivityProgressRepository] addTaskStats taskStatsMap after=${taskStatsMap.length}',
      );

      transaction.set(
        documentReference,
        _buildProgressUpdate(progressSummary),
        SetOptions(merge: true),
      );
      debugPrint(
        '[ActivityProgressRepository] addTaskStats transaction write queued.',
      );
    });
    debugPrint('[ActivityProgressRepository] addTaskStats completed.');
  }

  Future<void> updateHighestScoreIfGreater({
    required String userId,
    required String courseId,
    required String activityId,
    required int score,
  }) async {
    debugPrint(
      '[ActivityProgressRepository] updateHighestScoreIfGreater userId=$userId courseId=$courseId activityId=$activityId score=$score',
    );
    if (userId.isEmpty || courseId.isEmpty || activityId.isEmpty) {
      debugPrint(
        '[ActivityProgressRepository] updateHighestScoreIfGreater aborted: missing identifiers.',
      );
      return;
    }
    final documentReference = _firestore
        .collection(_enrollmentsCollection)
        .doc(_buildEnrollmentId(userId, courseId));
    final documentSnapshot = await documentReference.get();
    if (!documentSnapshot.exists) {
      debugPrint(
        '[ActivityProgressRepository] updateHighestScoreIfGreater aborted: enrollment doc not found.',
      );
      return;
    }

    final progressSummary = _progressSummaryFromDoc(documentSnapshot);
    debugPrint(
      '[ActivityProgressRepository] updateHighestScoreIfGreater progressSummary keys=${progressSummary.keys}',
    );
    final activitySnapshots = _activitySnapshotsFromSummary(progressSummary);
    debugPrint(
      '[ActivityProgressRepository] updateHighestScoreIfGreater activitySnapshots=${activitySnapshots.length}',
    );
    final snapshot = _getOrCreateActivitySnapshot(
      activitySnapshots,
      userId,
      courseId,
      activityId,
    );
    final highestScore = snapshot['highestScore'];
    final currentScore =
        highestScore is num ? highestScore.toInt() : null;
    debugPrint(
      '[ActivityProgressRepository] updateHighestScoreIfGreater currentScore=$currentScore',
    );
    if (currentScore == null || score > currentScore) {
      snapshot['highestScore'] = score;
      debugPrint(
        '[ActivityProgressRepository] updateHighestScoreIfGreater updating highestScore to $score',
      );
      await documentReference.set(
        _buildProgressUpdate(progressSummary),
        SetOptions(merge: true),
      );
      debugPrint(
        '[ActivityProgressRepository] updateHighestScoreIfGreater saved.',
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
