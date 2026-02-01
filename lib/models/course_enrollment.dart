import 'course.dart';
import 'course_progress.dart';

class CourseEnrollment {
  CourseEnrollment({
    required this.id,
    required this.userId,
    required this.courseId,
    required this.enrollmentDate,
    required this.enrolledBy,
    required this.selfEnrolled,
    this.course,
    this.progress,
  });

  final String id;
  final String userId;
  final String courseId;
  final String enrollmentDate;
  final String enrolledBy;
  final bool selfEnrolled;
  final Course? course;
  final CourseProgress? progress;

  String get enrollmentSourceLabel {
    if (selfEnrolled) {
      return 'Self-enrolled';
    }
    return enrolledBy.isEmpty ? 'Teacher' : 'Teacher: $enrolledBy';
  }
}
