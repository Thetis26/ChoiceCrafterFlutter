class Activity {
  const Activity({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.content,
    this.estimatedMinutes = 15,
  });

  final String id;
  final String name;
  final String description;
  final String type;
  final String content;
  final int estimatedMinutes;
}
