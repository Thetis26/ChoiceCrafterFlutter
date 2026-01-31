class TaskStats {
  const TaskStats({
    this.attemptDateTime,
    this.timeSpent,
    this.retries,
    this.success,
    this.hintsUsed,
    this.completionRatio,
    this.scoreRatio,
  });

  final String? attemptDateTime;
  final String? timeSpent;
  final int? retries;
  final bool? success;
  final bool? hintsUsed;
  final double? completionRatio;
  final double? scoreRatio;

  factory TaskStats.fromMap(Map<String, dynamic> map) {
    return TaskStats(
      attemptDateTime: map['attemptDateTime'] as String?,
      timeSpent: map['timeSpent']?.toString(),
      retries: map['retries'] is num ? (map['retries'] as num).toInt() : null,
      success: map['success'] as bool?,
      hintsUsed: map['hintsUsed'] as bool?,
      completionRatio: _toDouble(map['completionRatio']),
      scoreRatio: _toDouble(map['scoreRatio']),
    );
  }

  Map<String, dynamic> toMap() {
    final map = <String, dynamic>{};
    if (attemptDateTime != null) {
      map['attemptDateTime'] = attemptDateTime;
    }
    if (timeSpent != null) {
      map['timeSpent'] = timeSpent;
    }
    if (retries != null) {
      map['retries'] = retries;
    }
    if (success != null) {
      map['success'] = success;
    }
    if (hintsUsed != null) {
      map['hintsUsed'] = hintsUsed;
    }
    if (completionRatio != null) {
      map['completionRatio'] = completionRatio;
    }
    if (scoreRatio != null) {
      map['scoreRatio'] = scoreRatio;
    }
    return map;
  }

  DateTime? attemptDateTimeParsed() {
    if (attemptDateTime == null || attemptDateTime!.isEmpty) {
      return null;
    }
    return DateTime.tryParse(attemptDateTime!);
  }

  double resolveCompletionRatio() {
    if (completionRatio != null) {
      return completionRatio!.clamp(0.0, 1.0);
    }
    if (success == true) {
      return 1.0;
    }
    return 0.0;
  }

  double resolveScoreRatio() {
    if (scoreRatio != null) {
      return scoreRatio!.clamp(0.0, 1.0);
    }
    return resolveCompletionRatio();
  }

  bool isCompleted() {
    if (success == true) {
      return true;
    }
    final completion = resolveCompletionRatio();
    if (completion >= 1.0) {
      return true;
    }
    if (completion > 0.0) {
      if (scoreRatio == null) {
        return true;
      }
      if (scoreRatio! > 0.0) {
        return true;
      }
    }
    if (scoreRatio != null) {
      return scoreRatio! > 0.0;
    }
    return false;
  }

  bool isFullyCorrect() {
    return resolveScoreRatio() >= 1.0;
  }
}

double? _toDouble(Object? value) {
  if (value is num) {
    return value.toDouble();
  }
  if (value is String) {
    return double.tryParse(value);
  }
  return null;
}
