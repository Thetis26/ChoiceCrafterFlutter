import '../models/user.dart';

class AuthRepository {
  AuthRepository();

  final List<User> _users = [
    const User(
      id: 'user-1',
      fullName: 'Amira Student',
      email: 'amira@example.com',
      enrolledCourseIds: ['course-mobile', 'course-ai'],
      password: 'password123',
    ),
    const User(
      id: 'user-2',
      fullName: 'Jordan Learner',
      email: 'jordan@example.com',
      enrolledCourseIds: ['course-mobile'],
      password: 'letmein',
    ),
  ];

  User? login(String email, String password) {
    try {
      return _users.firstWhere(
        (user) => user.email.toLowerCase() == email.toLowerCase() && user.password == password,
      );
    } catch (_) {
      return null;
    }
  }

  User register({
    required String fullName,
    required String email,
    required String password,
  }) {
    final existingUser = _users.any((user) => user.email.toLowerCase() == email.toLowerCase());
    if (existingUser) {
      throw StateError('An account already exists for that email address.');
    }

    final newUser = User(
      id: 'user-${_users.length + 1}',
      fullName: fullName,
      email: email,
      enrolledCourseIds: const ['course-mobile'],
      password: password,
    );
    _users.add(newUser);
    return newUser;
  }
}
