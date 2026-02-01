class Comment {
  const Comment({
    required this.userId,
    required this.text,
    required this.timestamp,
    this.anonymousAvatarName,
    this.anonymousAvatarImageUrl,
  });

  final String userId;
  final String text;
  final String timestamp;
  final String? anonymousAvatarName;
  final String? anonymousAvatarImageUrl;
}
