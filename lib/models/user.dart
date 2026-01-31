class User {
  const User({
    required this.id,
    required this.fullName,
    required this.email,
    required this.enrolledCourseIds,
    this.anonymousAvatarName,
    this.anonymousAvatarImageUrl,
    this.password,
  });

  final String id;
  final String fullName;
  final String email;
  final List<String> enrolledCourseIds;
  final String? anonymousAvatarName;
  final String? anonymousAvatarImageUrl;
  final String? password;

  User copyWith({
    List<String>? enrolledCourseIds,
    String? anonymousAvatarName,
    String? anonymousAvatarImageUrl,
    String? password,
  }) {
    return User(
      id: id,
      fullName: fullName,
      email: email,
      enrolledCourseIds: enrolledCourseIds ?? this.enrolledCourseIds,
      anonymousAvatarName:
          anonymousAvatarName ?? this.anonymousAvatarName,
      anonymousAvatarImageUrl:
          anonymousAvatarImageUrl ?? this.anonymousAvatarImageUrl,
      password: password ?? this.password,
    );
  }
}
