import 'avatar.dart';

class ColleagueActivity {
  ColleagueActivity({
    required this.colleagueName,
    required this.activityName,
    required this.activityDescription,
    required this.timestamp,
    this.anonymousAvatar,
  });

  final String colleagueName;
  final String activityName;
  final String activityDescription;
  final String timestamp;
  final Avatar? anonymousAvatar;

  String? get imageUrl => anonymousAvatar?.resolvedImageUrl();
  String? get anonymousName => anonymousAvatar?.name;
}
