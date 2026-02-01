import 'dart:async';
import 'dart:developer' as developer;

import 'package:cloud_firestore/cloud_firestore.dart';

import '../models/enrollment_activity_progress.dart';

class UserStatisticsScheduler {
  UserStatisticsScheduler({
    FirebaseFirestore? firestore,
    Duration interval = const Duration(seconds: 30),
  })  : _firestore = firestore ?? FirebaseFirestore.instance,
        _interval = interval;

  static const String _usersCollection = 'users';
  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';
  static const String _legacyEnrollmentsCollection = 'course_enrollment';

  final FirebaseFirestore _firestore;
  final Duration _interval;
  Timer? _timer;
  bool _isRunning = false;

  void start() {
    if (_timer != null) {
      return;
    }
    _timer = Timer.periodic(_interval, (_) => _run());
    _run();
  }

  void stop() {
    _timer?.cancel();
    _timer = null;
  }

  Future<void> _run() async {
    if (_isRunning) {
      return;
    }
    _isRunning = true;
    try {
      await _updateAllUserStats();
    } catch (error, stackTrace) {
      developer.log(
        'User statistics refresh failed: $error',
        name: 'userStatsScheduler',
        error: error,
        stackTrace: stackTrace,
      );
    } finally {
      _isRunning = false;
    }
  }

  Future<void> _updateAllUserStats() async {
    final snapshots = await Future.wait([
      _firestore.collection(_enrollmentsCollection).get(),
      _firestore.collection(_legacyEnrollmentsCollection).get(),
    ]);

    final userStats = <String, _UserStatAggregation>{};
    for (final snapshot in snapshots) {
      for (final doc in snapshot.docs) {
        final data = doc.data();
        final userId = (data['userId'] as String?)?.trim() ?? '';
        if (userId.isEmpty) {
          continue;
        }
        final progressSummary = data['progressSummary'];
        if (progressSummary is! Map) {
          continue;
        }
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
          final snapshotData = EnrollmentActivityProgress.fromMap(entryMap);
          if (snapshotData.courseId.isEmpty) {
            continue;
          }
          final points = _resolvePoints(snapshotData);
          if (points <= 0) {
            continue;
          }
          userStats
              .putIfAbsent(userId, _UserStatAggregation.new)
              .addSnapshot(snapshotData, points);
        }
      }
    }

    if (userStats.isEmpty) {
      return;
    }

    WriteBatch batch = _firestore.batch();
    var operationCount = 0;
    for (final entry in userStats.entries) {
      final userId = entry.key;
      final aggregation = entry.value;
      final docRef = await _resolveUserDoc(userId);
      if (docRef == null) {
        continue;
      }
      batch.set(
        docRef,
        {
          'scores': aggregation.scores,
          'streak': aggregation.streak,
          'totalScore': aggregation.totalScore,
        },
        SetOptions(merge: true),
      );
      operationCount += 1;
      if (operationCount >= 450) {
        await batch.commit();
        batch = _firestore.batch();
        operationCount = 0;
      }
    }
    if (operationCount > 0) {
      await batch.commit();
    }
  }

  Future<DocumentReference<Map<String, dynamic>>?> _resolveUserDoc(
    String userId,
  ) async {
    if (userId.contains('@')) {
      final userQuery = await _firestore
          .collection(_usersCollection)
          .where('email', isEqualTo: userId)
          .limit(1)
          .get();
      if (userQuery.docs.isNotEmpty) {
        return userQuery.docs.first.reference;
      }
    }
    if (userId.isNotEmpty) {
      return _firestore.collection(_usersCollection).doc(userId);
    }
    return null;
  }

  int _resolvePoints(EnrollmentActivityProgress snapshot) {
    final highestScore = snapshot.highestScore;
    if (highestScore != null) {
      return highestScore;
    }
    final scoreRatio = snapshot.averageScoreRatio();
    if (scoreRatio > 0) {
      return (scoreRatio * 100).round();
    }
    final completionRatio = snapshot.averageCompletionRatio();
    if (completionRatio > 0) {
      return (completionRatio * 100).round();
    }
    return 0;
  }
}

class _UserStatAggregation {
  final Map<String, int> scores = <String, int>{};
  final List<DateTime> _attemptDates = <DateTime>[];

  void addSnapshot(EnrollmentActivityProgress snapshot, int points) {
    scores.update(
      snapshot.courseId,
      (value) => value + points,
      ifAbsent: () => points,
    );
    final latestAttempt = snapshot.latestAttempt();
    if (latestAttempt != null) {
      _attemptDates.add(latestAttempt);
    }
  }

  int get totalScore {
    return scores.values.fold<int>(0, (sum, value) => sum + value);
  }

  int get streak {
    if (_attemptDates.isEmpty) {
      return 0;
    }
    final normalized = _attemptDates
        .map((date) => DateTime(date.year, date.month, date.day))
        .toSet();
    var streak = 0;
    var dayCursor = DateTime.now();
    while (true) {
      final dayKey = DateTime(dayCursor.year, dayCursor.month, dayCursor.day);
      if (!normalized.contains(dayKey)) {
        break;
      }
      streak += 1;
      dayCursor = dayCursor.subtract(const Duration(days: 1));
    }
    return streak;
  }
}
