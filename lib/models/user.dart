class User {
  const User({
    required this.id,
    required this.fullName,
    required this.email,
    required this.enrolledCourseIds,
    this.password,
  });

  final String id;
  final String fullName;
  final String email;
  final List<String> enrolledCourseIds;
  final String? password;

  User copyWith({
    List<String>? enrolledCourseIds,
    String? password,
  }) {
    return User(
      id: id,
      fullName: fullName,
      email: email,
      enrolledCourseIds: enrolledCourseIds ?? this.enrolledCourseIds,
      password: password ?? this.password,
    );
  }
}
