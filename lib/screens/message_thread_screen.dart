import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../models/conversation_message.dart';

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
  final List<String> _localParticipants = [];
  String? _resolvedUserId;
  bool _isResolvingUser = false;
  late String _localTitle;

  String get _currentUserId {
    if (widget.localMessages != null) {
      return widget.localUserId ?? 'You';
    }
    return _resolvedUserId ?? _auth.currentUser?.uid ?? _auth.currentUser?.email ?? '';
  }


  Future<String> _resolveCurrentUserId() async {
    final user = _auth.currentUser;
    if (user == null) {
      return '';
    }

    final email = user.email ?? '';
    if (email.isNotEmpty) {
      final query = await _firestore
          .collection('users')
          .where('email', isEqualTo: email)
          .limit(1)
          .get();
      if (query.docs.isNotEmpty) {
        return query.docs.first.id;
      }
    }

    final fallbackDoc = await _firestore.collection('users').doc(user.uid).get();
    if (fallbackDoc.exists) {
      return fallbackDoc.id;
    }

    return user.uid;
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

  String _displayName(Map<String, dynamic> data, String fallback) {
    final name = (data['name'] as String?)?.trim();
    if (name != null && name.isNotEmpty) {
      return name;
    }
    final fullName = (data['fullName'] as String?)?.trim();
    if (fullName != null && fullName.isNotEmpty) {
      return fullName;
    }
    final email = (data['email'] as String?)?.trim();
    if (email != null && email.isNotEmpty) {
      return email;
    }
    return fallback;
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
      _localMessages.add(
        ConversationMessage(
          senderId: 'Virtual Assistant',
          text: _assistantResponse(text),
          timestamp: DateTime.now(),
        ),
      );
    });
    _scrollToBottom();
  }

  String _assistantResponse(String prompt) {
    if (prompt.trim().isEmpty) {
      return 'Could you share a bit more detail so I can help?';
    }
    return 'Thanks for your question! I can help you break this down into steps or clarify the requirements.';
  }

  Future<void> _showEditChatNameDialog({
    required String currentTitle,
    required bool isLocal,
  }) async {
    final controller = TextEditingController(text: currentTitle);
    final updatedTitle = await showDialog<String>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Edit chat name'),
          content: TextField(
            controller: controller,
            textInputAction: TextInputAction.done,
            decoration: const InputDecoration(
              hintText: 'Enter a chat name',
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(context, controller.text.trim()),
              child: const Text('Save'),
            ),
          ],
        );
      },
    );
    if (updatedTitle == null || updatedTitle.isEmpty) {
      return;
    }
    if (isLocal) {
      setState(() {
        _localTitle = updatedTitle;
      });
    } else if (widget.conversationId != null) {
      await _firestore
          .collection('conversations')
          .doc(widget.conversationId!)
          .update({'title': updatedTitle});
    }
  }

  Future<void> _showAddParticipantDialog({
    required bool isLocal,
    required List<String> participants,
  }) async {
    final controller = TextEditingController();
    var searchQuery = '';
    final canSearchUsers = !isLocal;
    final newParticipant = await showDialog<String>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: const Text('Add new participant'),
              content: SizedBox(
                width: double.maxFinite,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(
                      controller: controller,
                      textInputAction: TextInputAction.done,
                      decoration: InputDecoration(
                        hintText: 'Enter email or user id',
                        prefixIcon: canSearchUsers
                            ? const Icon(Icons.search)
                            : null,
                        filled: canSearchUsers,
                        fillColor: canSearchUsers ? Colors.grey.shade100 : null,
                      ),
                      onChanged: canSearchUsers
                          ? (value) {
                              setState(() {
                                searchQuery = value.trim().toLowerCase();
                              });
                            }
                          : null,
                    ),
                    if (canSearchUsers) ...[
                      const SizedBox(height: 12),
                      SizedBox(
                        height: 220,
                        child: StreamBuilder<
                            QuerySnapshot<Map<String, dynamic>>>(
                          stream: _firestore.collection('users').snapshots(),
                          builder: (context, snapshot) {
                            if (snapshot.connectionState ==
                                ConnectionState.waiting) {
                              return const Center(
                                child: CircularProgressIndicator(),
                              );
                            }
                            if (snapshot.hasError) {
                              return const Center(
                                child: Text('Unable to load users.'),
                              );
                            }
                            final users = (snapshot.data?.docs ?? [])
                                .where((doc) =>
                                    !participants.contains(doc.id) &&
                                    doc.id != _currentUserId)
                                .map((doc) {
                                  final data = doc.data();
                                  final name = _displayName(data, doc.id);
                                  final email =
                                      (data['email'] as String?)?.trim() ?? '';
                                  return (
                                    id: doc.id,
                                    name: name,
                                    email: email
                                  );
                                })
                                .where((user) {
                                  if (searchQuery.isEmpty) {
                                    return true;
                                  }
                                  final name = user.name.toLowerCase();
                                  final email = user.email.toLowerCase();
                                  return name.contains(searchQuery) ||
                                      email.contains(searchQuery);
                                })
                                .toList()
                              ..sort(
                                (a, b) => a.name
                                    .toLowerCase()
                                    .compareTo(b.name.toLowerCase()),
                              );
                            if (users.isEmpty) {
                              return const Center(
                                child: Text('No matching users found.'),
                              );
                            }
                            return ListView.separated(
                              itemCount: users.length,
                              separatorBuilder: (_, __) =>
                                  const Divider(height: 1),
                              itemBuilder: (context, index) {
                                final user = users[index];
                                return ListTile(
                                  title: Text(user.name),
                                  subtitle: user.email.isNotEmpty
                                      ? Text(user.email)
                                      : null,
                                  onTap: () {
                                    final value = user.email.isNotEmpty
                                        ? user.email
                                        : user.id;
                                    setState(() {
                                      controller.text = value;
                                      controller.selection =
                                          TextSelection.collapsed(
                                        offset: value.length,
                                      );
                                      searchQuery =
                                          value.trim().toLowerCase();
                                    });
                                  },
                                );
                              },
                            );
                          },
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () =>
                      Navigator.pop(context, controller.text.trim()),
                  child: const Text('Add'),
                ),
              ],
            );
          },
        );
      },
    );

    if (newParticipant == null || newParticipant.isEmpty) {
      return;
    }

    if (isLocal) {
      if (_localParticipants.contains(newParticipant)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Participant already added.')),
        );
        return;
      }
      setState(() {
        _localParticipants.add(newParticipant);
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Added $newParticipant to the chat.')),
      );
      return;
    }

    if (widget.conversationId == null) {
      return;
    }

    String participantId = newParticipant;
    if (newParticipant.contains('@')) {
      final query = await _firestore
          .collection('users')
          .where('email', isEqualTo: newParticipant)
          .limit(1)
          .get();
      if (query.docs.isNotEmpty) {
        participantId = query.docs.first.id;
      }
    }

    if (participants.contains(participantId)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Participant already in this chat.')),
      );
      return;
    }

    await _firestore
        .collection('conversations')
        .doc(widget.conversationId!)
        .update({
      'participants': FieldValue.arrayUnion([participantId]),
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Added $newParticipant to the chat.')),
    );
  }

  PopupMenuButton<String> _buildThreadMenu({
    required bool isLocal,
    required List<String> participants,
    required String currentTitle,
  }) {
    return PopupMenuButton<String>(
      icon: const Icon(Icons.more_vert),
      onSelected: (value) {
        if (value == 'add_participant') {
          _showAddParticipantDialog(
            isLocal: isLocal,
            participants: participants,
          );
        } else if (value == 'edit_name') {
          _showEditChatNameDialog(
            currentTitle: currentTitle,
            isLocal: isLocal,
          );
        }
      },
      itemBuilder: (context) => [
        const PopupMenuItem(
          value: 'add_participant',
          child: Text('Add new participant'),
        ),
        const PopupMenuItem(
          value: 'edit_name',
          child: Text('Edit chat name'),
        ),
      ],
    );
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
    _localTitle = widget.initialTitle;
    final localUserId = widget.localUserId ?? 'You';
    _localParticipants.add(localUserId);
    if (widget.localMessages != null) {
      _localMessages.addAll(widget.localMessages!);
      for (final message in widget.localMessages!) {
        if (!_localParticipants.contains(message.senderId)) {
          _localParticipants.add(message.senderId);
        }
      }
    } else {
      _isResolvingUser = true;
      _resolveCurrentUserId().then((id) {
        if (!mounted) {
          return;
        }
        setState(() {
          _resolvedUserId = id;
          _isResolvingUser = false;
        });
      });
    }
  }

  Widget _buildMessageBubble({
    required String senderName,
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
                senderName,
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
        title: Text(_localTitle),
        backgroundColor: const Color(0xFF7E86F9),
        foregroundColor: Colors.white,
        actions: [
          _buildThreadMenu(
            isLocal: true,
            participants: _localParticipants,
            currentTitle: _localTitle,
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
                  senderName: message.senderId,
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

    if (_isResolvingUser) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (_currentUserId.isEmpty) {
      return const Scaffold(
        body: Center(child: Text('Sign in to view this conversation.')),
      );
    }

    return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
      stream: _firestore.collection('users').snapshots(),
      builder: (context, usersSnapshot) {
        final userNameById = <String, String>{};
        final userDocs = usersSnapshot.data?.docs ?? [];
        for (final doc in userDocs) {
          userNameById[doc.id] = _displayName(doc.data(), doc.id);
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
            final othersDisplay = others
                .map((id) => userNameById[id] ?? id)
                .where((name) => name.trim().isNotEmpty)
                .toList();
            final displayTitle = title != null && title.isNotEmpty
                ? title
                : (othersDisplay.isNotEmpty
                    ? othersDisplay.join(', ')
                    : widget.initialTitle);
            final unreadBy = _participants(data?['unreadBy']);
            _markRead(unreadBy);

            return Scaffold(
              backgroundColor: const Color(0xFFF4F6FB),
              appBar: AppBar(
                title: Text(displayTitle),
                backgroundColor: const Color(0xFF7E86F9),
                foregroundColor: Colors.white,
                actions: [
                  _buildThreadMenu(
                    isLocal: false,
                    participants: participants,
                    currentTitle: displayTitle,
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
                        if (snapshot.connectionState ==
                            ConnectionState.waiting) {
                          return const Center(
                            child: CircularProgressIndicator(),
                          );
                        }
                        if (snapshot.hasError) {
                          return const Center(
                            child: Text('Unable to load messages.'),
                          );
                        }
                        final docs = snapshot.data?.docs ?? [];
                        return ListView.builder(
                          controller: _scrollController,
                          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                          itemCount: docs.length,
                          itemBuilder: (context, index) {
                            final message = docs[index].data();
                            final senderId =
                                (message['senderId'] as String?) ?? '';
                            final isMe = senderId == _currentUserId;
                            final text = (message['text'] as String?) ?? '';
                            final timestamp =
                                _parseTimestamp(message['timestamp']);
                            final senderName =
                                userNameById[senderId] ?? senderId;
                            return _buildMessageBubble(
                              senderName: senderName,
                              isMe: isMe,
                              text: text,
                              timestamp: timestamp,
                            );
                          },
                        );
                      },
                    ),
                  ),
                  _buildMessageComposer(
                    onSend: () => _sendMessage(participants),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}
