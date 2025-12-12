import '../models/user.dart';
import '../models/user_activity.dart';

class UserActivityRepository {
  List<UserActivity> activitiesFor(User user) {
    return [
      UserActivity(
        id: 'ua-1',
        activityName: 'Mobile quiz review',
        courseTitle: 'Mobile Development Foundations',
        status: 'In progress',
        progress: 0.45,
        estimatedMinutes: 20,
      ),
      UserActivity(
        id: 'ua-2',
        activityName: 'Peer feedback loop',
        courseTitle: 'AI Assisted Learning',
        status: 'Waiting for review',
        progress: 0.8,
        estimatedMinutes: 10,
      ),
      UserActivity(
        id: 'ua-3',
        activityName: 'Weekly reflection',
        courseTitle: 'Mobile Development Foundations',
        status: 'Due soon',
        progress: 0.2,
        estimatedMinutes: 15,
      ),
    ].where((activity) {
      return user.enrolledCourseIds
          .any((courseId) => activity.courseTitle.toLowerCase().contains(courseId.split('-').last));
    }).toList();
  }
}
