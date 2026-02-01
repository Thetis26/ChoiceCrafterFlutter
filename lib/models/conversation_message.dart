class ConversationMessage {
  const ConversationMessage({
    required this.senderId,
    required this.text,
    required this.timestamp,
  });

  final String senderId;
  final String text;
  final DateTime timestamp;
}
