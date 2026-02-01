class Conversation {
  const Conversation({
    required this.id,
    required this.title,
    required this.lastMessage,
    required this.timestamp,
    required this.unread,
    required this.participants,
    this.formerParticipants,
    this.readOnly = false,
    this.unreadBy,
    this.lastMessageSenderId,
  });

  final String id;
  final String title;
  final String lastMessage;
  final int timestamp;
  final bool unread;
  final List<String> participants;
  final List<String>? formerParticipants;
  final bool readOnly;
  final List<String>? unreadBy;
  final String? lastMessageSenderId;
}
