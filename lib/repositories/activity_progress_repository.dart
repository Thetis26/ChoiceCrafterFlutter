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
    int? activityIndex,
  }) async {
    final activityKey = _resolveActivityKey(courseId, activityId, activityIndex);
    debugPrint(
      '[ActivityProgressRepository] startActivity userId=$userId courseId=$courseId activityKey=$activityKey activityId=$activityId activityIndex=$activityIndex',
    );
    if (userId.isEmpty || courseId.isEmpty || activityKey.isEmpty) {
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
      activityKey,
      activityIndex,
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
    int? activityIndex,
    required String taskId,
    int? taskIndex,
    required TaskStats taskStats,
  }) async {
    final activityKey = _resolveActivityKey(courseId, activityId, activityIndex);
    final taskKey = _resolveTaskKey(taskId, taskIndex);
    debugPrint(
      '[ActivityProgressRepository] addTaskStats userId=$userId courseId=$courseId activityKey=$activityKey activityId=$activityId activityIndex=$activityIndex taskKey=$taskKey taskId=$taskId taskIndex=$taskIndex',
    );
    if (userId.isEmpty ||
        courseId.isEmpty ||
        activityKey.isEmpty ||
        taskKey.isEmpty) {
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
        activityKey,
        activityIndex,
      );
      debugPrint(
        '[ActivityProgressRepository] addTaskStats snapshot taskStats=${(snapshot['taskStats'] as Map?)?.length ?? 0}',
      );

      final taskStatsMap = _ensureTaskStatsMap(snapshot);
      debugPrint(
        '[ActivityProgressRepository] addTaskStats taskStatsMap before=${taskStatsMap.length}',
      );
      debugPrint(
        '[ActivityProgressRepository] addTaskStats taskStats details attemptDateTime=${taskStats.attemptDateTime} timeSpent=${taskStats.timeSpent} retries=${taskStats.retries} success=${taskStats.success} hintsUsed=${taskStats.hintsUsed} completionRatio=${taskStats.completionRatio} scoreRatio=${taskStats.scoreRatio}',
      );
      taskStatsMap[taskKey] = taskStats.toMap();
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
    int? activityIndex,
    required int score,
  }) async {
    final activityKey = _resolveActivityKey(courseId, activityId, activityIndex);
    debugPrint(
      '[ActivityProgressRepository] updateHighestScoreIfGreater userId=$userId courseId=$courseId activityKey=$activityKey activityId=$activityId activityIndex=$activityIndex score=$score',
    );
    if (userId.isEmpty || courseId.isEmpty || activityKey.isEmpty) {
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
      activityKey,
      activityIndex,
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
    String activityKey,
    int? activityIndex,
  ) {
    for (final snapshot in activitySnapshots) {
      final snapshotId = snapshot['activityId'];
      if (snapshotId != null && snapshotId.toString() == activityKey) {
        _ensureSnapshotIdentifiers(
          snapshot,
          userId,
          courseId,
          activityKey,
          activityIndex,
        );
        return snapshot;
      }
    }
    final snapshot = <String, dynamic>{
      'activityId': activityKey,
      'courseId': courseId,
      'userId': userId,
      'taskStats': <String, dynamic>{},
    };
    if (activityIndex != null) {
      snapshot['activityIndex'] = activityIndex;
    }
    activitySnapshots.add(snapshot);
    return snapshot;
  }

  void _ensureSnapshotIdentifiers(
    Map<String, dynamic> snapshot,
    String userId,
    String courseId,
    String activityKey,
    int? activityIndex,
  ) {
    snapshot['userId'] = userId;
    snapshot['courseId'] = courseId;
    snapshot['activityId'] = activityKey;
    if (activityIndex != null) {
      snapshot['activityIndex'] = activityIndex;
    }
    snapshot['taskStats'] = _normalizeTaskStats(snapshot['taskStats']);
  }

  Map<String, dynamic> _ensureTaskStatsMap(
    Map<String, dynamic> snapshot,
  ) {
    final normalized = _normalizeTaskStats(snapshot['taskStats']);
    snapshot['taskStats'] = normalized;
    return normalized;
  }

  Map<String, dynamic> _buildProgressUpdate(
    Map<String, dynamic> progressSummary,
  ) {
    return {'progressSummary': progressSummary};
  }

  String _resolveTaskKey(String taskId, int? taskIndex) {
    if (taskId.isNotEmpty) {
      return taskId;
    }
    if (taskIndex != null) {
      return taskIndex.toString();
    }
    return '';
  }

  String _resolveActivityKey(
    String courseId,
    String activityId,
    int? activityIndex,
  ) {
    if (activityId.isNotEmpty) {
      return activityId;
    }
    if (activityIndex != null) {
      if (courseId.isNotEmpty) {
        return '${courseId}_$activityIndex';
      }
      return activityIndex.toString();
    }
    return '';
  }

  Map<String, dynamic> _normalizeTaskStats(Object? stats) {
    if (stats is Map) {
      return Map<String, dynamic>.from(stats);
    }
    if (stats is List) {
      final normalized = <String, dynamic>{};
      for (var i = 0; i < stats.length; i += 1) {
        final entry = stats[i];
        if (entry is Map) {
          final entryMap = Map<String, dynamic>.from(entry);
          final key = entryMap['taskId']?.toString() ??
              entryMap['id']?.toString() ??
              i.toString();
          normalized[key] = entryMap;
        }
      }
      return normalized;
    }
    return <String, dynamic>{};
  }
}
