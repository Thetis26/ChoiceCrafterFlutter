class ChatMessage {
  const ChatMessage({
    required this.senderId,
    required this.text,
    required this.timestamp,
  });

  final String senderId;
  final String text;
  final int timestamp;
}
