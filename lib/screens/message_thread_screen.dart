import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../sample_data.dart';

class MessageThreadScreen extends StatefulWidget {
  const MessageThreadScreen({
    super.key,
    required this.conversationId,
    required this.initialTitle,
  })  : localMessages = null,
        localUserId = null;

  const MessageThreadScreen.local({
    super.key,
    required this.initialTitle,
    required List<ConversationMessage> messages,
    String localUserId = 'You',
  })  : conversationId = null,
        localMessages = messages,
        localUserId = localUserId;

  final String? conversationId;
  final String initialTitle;
  final List<ConversationMessage>? localMessages;
  final String? localUserId;

  @override
  State<MessageThreadScreen> createState() => _MessageThreadScreenState();
}

class _MessageThreadScreenState extends State<MessageThreadScreen> {
  final _controller = TextEditingController();
  final _scrollController = ScrollController();
  final _firestore = FirebaseFirestore.instance;
  final _auth = firebase_auth.FirebaseAuth.instance;
  final List<ConversationMessage> _localMessages = [];

  String get _currentUserId {
    if (widget.localMessages != null) {
      return widget.localUserId ?? 'You';
    }
    final user = _auth.currentUser;
    return user?.email ?? user?.uid ?? '';
  }

  DateTime? _parseTimestamp(dynamic value) {
    if (value is Timestamp) {
      return value.toDate();
    }
    if (value is int) {
      return DateTime.fromMillisecondsSinceEpoch(value);
    }
    return null;
  }

  String _formatTime(DateTime? date) {
    if (date == null) {
      return '';
    }
    final hours = date.hour.toString().padLeft(2, '0');
    final minutes = date.minute.toString().padLeft(2, '0');
    return '$hours:$minutes';
  }

  List<String> _participants(dynamic value) {
    if (value is Iterable) {
      return value.whereType<String>().toList();
    }
    return [];
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    }
  }

  void _sendLocalMessage() {
    final text = _controller.text.trim();
    if (text.isEmpty) {
      return;
    }
    _controller.clear();
    setState(() {
      _localMessages.add(
        ConversationMessage(
          senderId: _currentUserId,
          text: text,
          timestamp: DateTime.now(),
        ),
      );
    });
    _scrollToBottom();
  }

  Future<void> _sendMessage(List<String> participants) async {
    if (widget.conversationId == null) {
      _sendLocalMessage();
      return;
    }
    final text = _controller.text.trim();
    if (text.isEmpty || _currentUserId.isEmpty) {
      return;
    }
    _controller.clear();
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    final conversationRef =
        _firestore.collection('conversations').doc(widget.conversationId!);
    final messageRef = conversationRef.collection('messages').doc();

    await _firestore.runTransaction((transaction) async {
      transaction.set(messageRef, {
        'senderId': _currentUserId,
        'text': text,
        'timestamp': timestamp,
      });
      final otherParticipants =
          participants.where((id) => id != _currentUserId).toList();
      transaction.update(conversationRef, {
        'lastMessage': text,
        'lastMessageSenderId': _currentUserId,
        'timestamp': timestamp,
        'unread': true,
        'unreadBy': otherParticipants,
      });
    });

    _scrollToBottom();
  }

  Future<void> _markRead(List<String> unreadBy) async {
    if (widget.conversationId == null ||
        _currentUserId.isEmpty ||
        !unreadBy.contains(_currentUserId)) {
      return;
    }
    await _firestore
        .collection('conversations')
        .doc(widget.conversationId!)
        .update({
      'unreadBy': FieldValue.arrayRemove([_currentUserId]),
      'unread': false,
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    if (widget.localMessages != null) {
      _localMessages.addAll(widget.localMessages!);
    }
  }

  Widget _buildMessageBubble({
    required String senderId,
    required bool isMe,
    required String text,
    required DateTime? timestamp,
  }) {
    return Align(
      alignment: isMe ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.all(14),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.7,
        ),
        decoration: BoxDecoration(
          color: isMe ? const Color(0xFF7E86F9) : Colors.white,
          borderRadius: BorderRadius.circular(18),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 6,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!isMe)
              Text(
                senderId,
                style: const TextStyle(
                  color: Colors.black54,
                  fontSize: 11,
                ),
              ),
            if (!isMe) const SizedBox(height: 4),
            Text(
              text,
              style: TextStyle(
                fontSize: 16,
                color: isMe ? Colors.white : Colors.black87,
                fontWeight: isMe ? FontWeight.w600 : FontWeight.w500,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              _formatTime(timestamp),
              style: TextStyle(
                color: isMe ? Colors.white70 : Colors.black38,
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageComposer({required VoidCallback onSend}) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _controller,
              decoration: InputDecoration(
                hintText: 'Type a message',
                filled: true,
                fillColor: Colors.white,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 14,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(30),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          CircleAvatar(
            radius: 26,
            backgroundColor: const Color(0xFF7E86F9),
            child: IconButton(
              onPressed: onSend,
              icon: const Icon(Icons.send, color: Colors.white),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLocalThread() {
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      appBar: AppBar(
        title: Text(widget.initialTitle),
        backgroundColor: const Color(0xFF7E86F9),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            onPressed: () {},
            icon: const Icon(Icons.more_vert),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
              itemCount: _localMessages.length,
              itemBuilder: (context, index) {
                final message = _localMessages[index];
                final isMe = message.senderId == _currentUserId;
                return _buildMessageBubble(
                  senderId: message.senderId,
                  isMe: isMe,
                  text: message.text,
                  timestamp: message.timestamp,
                );
              },
            ),
          ),
          _buildMessageComposer(onSend: _sendLocalMessage),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (widget.localMessages != null) {
      return _buildLocalThread();
    }

    if (_currentUserId.isEmpty) {
      return const Scaffold(
        body: Center(child: Text('Sign in to view this conversation.')),
      );
    }

    return StreamBuilder<DocumentSnapshot<Map<String, dynamic>>>(
      stream: _firestore
          .collection('conversations')
          .doc(widget.conversationId!)
          .snapshots(),
      builder: (context, conversationSnapshot) {
        final data = conversationSnapshot.data?.data();
        final participants = _participants(data?['participants']);
        final title = (data?['title'] as String?)?.trim();
        final others =
            participants.where((id) => id != _currentUserId).toList();
        final displayTitle = title != null && title.isNotEmpty
            ? title
            : (others.isNotEmpty ? others.first : widget.initialTitle);
        final unreadBy = _participants(data?['unreadBy']);
        _markRead(unreadBy);

        return Scaffold(
          backgroundColor: const Color(0xFFF4F6FB),
          appBar: AppBar(
            title: Text(displayTitle),
            backgroundColor: const Color(0xFF7E86F9),
            foregroundColor: Colors.white,
            actions: [
              IconButton(
                onPressed: () {},
                icon: const Icon(Icons.more_vert),
              ),
            ],
          ),
          body: Column(
            children: [
              Expanded(
                child: StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
                  stream: _firestore
                      .collection('conversations')
                      .doc(widget.conversationId!)
                      .collection('messages')
                      .orderBy('timestamp')
                      .snapshots(),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator());
                    }
                    if (snapshot.hasError) {
                      return const Center(child: Text('Unable to load messages.'));
                    }
                    final docs = snapshot.data?.docs ?? [];
                    return ListView.builder(
                      controller: _scrollController,
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                      itemCount: docs.length,
                      itemBuilder: (context, index) {
                        final message = docs[index].data();
                        final senderId = (message['senderId'] as String?) ?? '';
                        final isMe = senderId == _currentUserId;
                        final text = (message['text'] as String?) ?? '';
                        final timestamp = _parseTimestamp(message['timestamp']);
                        return _buildMessageBubble(
                          senderId: senderId,
                          isMe: isMe,
                          text: text,
                          timestamp: timestamp,
                        );
                      },
                    );
                  },
                ),
              ),
              _buildMessageComposer(onSend: () => _sendMessage(participants)),
            ],
          ),
        );
      },
    );
  }
}
