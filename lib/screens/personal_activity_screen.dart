import 'package:flutter/material.dart';

import '../models/user.dart';
import '../models/user_activity.dart';
import '../repositories/user_activity_repository.dart';

class PersonalActivityScreen extends StatelessWidget {
  const PersonalActivityScreen({
    super.key,
    required this.user,
    required this.userActivityRepository,
  });

  final User user;
  final UserActivityRepository userActivityRepository;

  @override
  Widget build(BuildContext context) {
    final activities = userActivityRepository.activitiesFor(user);
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: activities.length,
      itemBuilder: (context, index) {
        final activity = activities[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            leading: SizedBox(
              height: 32,
              width: 32,
              child: CircularProgressIndicator(value: activity.progress),
            ),
            title: Text(activity.activityName),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(activity.courseTitle),
                Text(activity.status),
                if (activity.estimatedMinutes != null)
                  Text('${activity.estimatedMinutes} min remaining'),
              ],
            ),
          ),
        );
      },
    );
  }
}
