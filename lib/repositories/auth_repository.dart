import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;

import '../models/user.dart';

class AuthRepository {
  AuthRepository({
    firebase_auth.FirebaseAuth? firebaseAuth,
    FirebaseFirestore? firestore,
  })  : _firebaseAuth = firebaseAuth ?? firebase_auth.FirebaseAuth.instance,
        _firestore = firestore ?? FirebaseFirestore.instance;

  final firebase_auth.FirebaseAuth _firebaseAuth;
  final FirebaseFirestore _firestore;

  static const String _usersCollection = 'users';
  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';
  static const String _legacyEnrollmentsCollection = 'course_enrollment';
  static const List<String> _defaultEnrolledCourseIds = ['course-mobile'];

  Future<User?> login(String email, String password) async {
    final credential = await _firebaseAuth.signInWithEmailAndPassword(
      email: email,
      password: password,
    );
    final firebaseUser = credential.user;
    if (firebaseUser == null) {
      return null;
    }
    return _userFromFirebase(firebaseUser);
  }

  Future<User> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    final credential = await _firebaseAuth.createUserWithEmailAndPassword(
      email: email,
      password: password,
    );
    final firebaseUser = credential.user;
    if (firebaseUser == null) {
      throw StateError('Registration failed. Please try again.');
    }

    final normalizedEmail = email.trim();
    await firebaseUser.updateDisplayName(fullName);
    final userDoc = await _resolveUserDoc(normalizedEmail, firebaseUser.uid);
    await userDoc.set(
      {
        'fullName': fullName,
        'name': fullName,
        'email': normalizedEmail,
        'enrolledCourseIds': _defaultEnrolledCourseIds,
      },
      SetOptions(merge: true),
    );

    final batch = _firestore.batch();
    final enrollmentUserId =
        normalizedEmail.isEmpty ? firebaseUser.uid : normalizedEmail;
    final enrollmentDate = _formatEnrollmentDate(DateTime.now());
    for (final courseId in _defaultEnrolledCourseIds) {
      final enrollmentDocId = '${enrollmentUserId}_$courseId';
      final enrollmentDoc =
          _firestore.collection(_enrollmentsCollection).doc(enrollmentDocId);
      batch.set(enrollmentDoc, {
        'userId': enrollmentUserId,
        'courseId': courseId,
        'enrollmentDate': enrollmentDate,
        'selfEnrolled': true,
        'enrolledBy': enrollmentUserId,
        'createdAt': FieldValue.serverTimestamp(),
      });
    }
    await batch.commit();

    return User(
      id: userDoc.id,
      fullName: fullName,
      email: normalizedEmail,
      enrolledCourseIds: _defaultEnrolledCourseIds,
    );
  }

  Future<User?> currentUser() async {
    final firebaseUser = _firebaseAuth.currentUser;
    if (firebaseUser == null) {
      return null;
    }

    return _userFromFirebase(firebaseUser);
  }

  Future<void> logout() async {
    await _firebaseAuth.signOut();
  }

  Future<User> _userFromFirebase(firebase_auth.User firebaseUser) async {
    final userEmail = firebaseUser.email ?? '';
    final docRef = await _resolveUserDoc(userEmail, firebaseUser.uid);
    final snapshot = await docRef.get();
    final data = snapshot.data();

    final fullName = data?['name'] as String? ??
        data?['fullName'] as String? ??
        firebaseUser.displayName ??
        'ChoiceCrafter Student';
    final email = data?['email'] as String? ?? userEmail;
    final legacyCourseIds = (data?['enrolledCourseIds'] as List?)
            ?.whereType<String>()
            .toList() ??
        <String>[];
    final enrollmentKey =
        userEmail.isNotEmpty ? userEmail : firebaseUser.uid;
    final enrolledCourseIds =
        await _loadEnrolledCourseIds(enrollmentKey, legacyCourseIds);

    if (!snapshot.exists) {
      await docRef.set({
        'fullName': fullName,
        'name': fullName,
        'email': email,
        if (legacyCourseIds.isNotEmpty) 'enrolledCourseIds': legacyCourseIds,
      });
    }

    return User(
      id: docRef.id,
      fullName: fullName,
      email: email,
      enrolledCourseIds: enrolledCourseIds,
    );
  }

  Future<List<String>> _loadEnrolledCourseIds(
    String enrollmentKey,
    List<String> fallbackIds,
  ) async {
    final courseIds = await _fetchEnrollmentCourseIds(enrollmentKey);

    if (courseIds.isNotEmpty) {
      return courseIds;
    }
    return fallbackIds;
  }

  Future<DocumentReference<Map<String, dynamic>>> _resolveUserDoc(
    String email,
    String fallbackUserId,
  ) async {
    if (email.isNotEmpty) {
      final userQuery = await _firestore
          .collection(_usersCollection)
          .where('email', isEqualTo: email)
          .limit(1)
          .get();
      if (userQuery.docs.isNotEmpty) {
        return userQuery.docs.first.reference;
      }
    }

    return _firestore.collection(_usersCollection).doc(fallbackUserId);
  }

  Future<List<String>> _fetchEnrollmentCourseIds(String userKey) async {
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

    final courseIds = <String>{};
    for (final snapshot in snapshots) {
      for (final doc in snapshot.docs) {
        final data = doc.data();
        final courseId = (data['courseId'] as String?) ??
            (data['course_id'] as String?) ??
            '';
        if (courseId.isNotEmpty) {
          courseIds.add(courseId);
        }
      }
    }
    return courseIds.toList();
  }

  String _formatEnrollmentDate(DateTime date) {
    final year = date.year.toString().padLeft(4, '0');
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }
}
