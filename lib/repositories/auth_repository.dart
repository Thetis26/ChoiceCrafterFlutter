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

    await firebaseUser.updateDisplayName(fullName);
    await _firestore.collection('users').doc(firebaseUser.uid).set({
      'fullName': fullName,
      'email': email,
      'enrolledCourseIds': _defaultEnrolledCourseIds,
    });

    return User(
      id: firebaseUser.uid,
      fullName: fullName,
      email: email,
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
    final docRef = _firestore.collection('users').doc(firebaseUser.uid);
    final snapshot = await docRef.get();
    final data = snapshot.data();

    final fullName = data?['fullName'] as String? ??
        firebaseUser.displayName ??
        'ChoiceCrafter Student';
    final email = data?['email'] as String? ?? firebaseUser.email ?? '';
    final enrolledCourseIds = (data?['enrolledCourseIds'] as List?)
            ?.whereType<String>()
            .toList() ??
        _defaultEnrolledCourseIds;

    if (!snapshot.exists) {
      await docRef.set({
        'fullName': fullName,
        'email': email,
        'enrolledCourseIds': enrolledCourseIds,
      });
    }

    return User(
      id: firebaseUser.uid,
      fullName: fullName,
      email: email,
      enrolledCourseIds: enrolledCourseIds,
    );
  }
}
