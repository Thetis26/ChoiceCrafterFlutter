import 'activity.dart';

class Module {
  const Module({
    required this.id,
    required this.name,
    required this.summary,
    required this.activities,
  });

  final String id;
  final String name;
  final String summary;
  final List<Activity> activities;
}
