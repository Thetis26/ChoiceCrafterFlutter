import 'avatar.dart';

class User {
  const User({
    required this.id,
    required this.fullName,
    required this.email,
    required this.enrolledCourseIds,
    this.anonymousAvatarName,
    this.anonymousAvatarImageUrl,
    this.password,
    String? name,
    this.anonymousAvatar,
    this.online = false,
    this.totalScore = 0,
    this.streak = 0,
    this.learningPathPoints = 0,
    this.badges = const [],
    this.scores = const {},
  }) : name = name ?? fullName;

  final String id;
  final String fullName;
  final String name;
  final String email;
  final List<String> enrolledCourseIds;
  final String? anonymousAvatarName;
  final String? anonymousAvatarImageUrl;
  final String? password;
  final Avatar? anonymousAvatar;
  final bool online;
  final int totalScore;
  final int streak;
  final int learningPathPoints;
  final List<String> badges;
  final Map<String, int> scores;

  User copyWith({
    List<String>? enrolledCourseIds,
    String? anonymousAvatarName,
    String? anonymousAvatarImageUrl,
    String? password,
    String? name,
    Avatar? anonymousAvatar,
    bool? online,
    int? totalScore,
    int? streak,
    int? learningPathPoints,
    List<String>? badges,
    Map<String, int>? scores,
  }) {
    return User(
      id: id,
      fullName: fullName,
      email: email,
      enrolledCourseIds: enrolledCourseIds ?? this.enrolledCourseIds,
      anonymousAvatarName: anonymousAvatarName ?? this.anonymousAvatarName,
      anonymousAvatarImageUrl:
          anonymousAvatarImageUrl ?? this.anonymousAvatarImageUrl,
      password: password ?? this.password,
      name: name ?? this.name,
      anonymousAvatar: anonymousAvatar ?? this.anonymousAvatar,
      online: online ?? this.online,
      totalScore: totalScore ?? this.totalScore,
      streak: streak ?? this.streak,
      learningPathPoints: learningPathPoints ?? this.learningPathPoints,
      badges: badges ?? this.badges,
      scores: scores ?? this.scores,
    );
  }
}
