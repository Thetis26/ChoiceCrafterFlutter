import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import 'message_thread_screen.dart';

class MessagesScreen extends StatelessWidget {
  const MessagesScreen({super.key});

  String _currentUserId(firebase_auth.FirebaseAuth auth) {
    final user = auth.currentUser;
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

  String _formatDate(DateTime? date) {
    if (date == null) {
      return '';
    }
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    return '${months[date.month - 1]} ${date.day}, ${date.year}';
  }

  List<String> _participants(dynamic value) {
    if (value is Iterable) {
      return value.whereType<String>().toList();
    }
    return [];
  }

  @override
  Widget build(BuildContext context) {
    final firestore = FirebaseFirestore.instance;
    final auth = firebase_auth.FirebaseAuth.instance;
    final currentUserId = _currentUserId(auth);

    if (currentUserId.isEmpty) {
      return const Center(
        child: Text('Sign in to view your conversations.'),
      );
    }

    return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
      stream: firestore
          .collection('conversations')
          .where('participants', arrayContains: currentUserId)
          .orderBy('timestamp', descending: true)
          .snapshots(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }

        if (snapshot.hasError) {
          return const Center(child: Text('Unable to load messages.'));
        }

        final docs = snapshot.data?.docs ?? [];
        final contacts = <_ContactChip>[];

        for (final doc in docs) {
          final data = doc.data();
          final participants = _participants(data['participants']);
          final others = participants.where((id) => id != currentUserId).toList();
          final title = (data['title'] as String?)?.trim();
          final name = title != null && title.isNotEmpty
              ? title
              : (others.isNotEmpty ? others.first : 'Conversation');
          contacts.add(_ContactChip(name: name, id: others.isNotEmpty ? others.first : name));
        }

        return Scaffold(
          backgroundColor: const Color(0xFFF4F6FB),
          appBar: AppBar(
            title: const Text('Messages'),
            backgroundColor: const Color(0xFF7E86F9),
            foregroundColor: Colors.white,
            elevation: 4,
          ),
          floatingActionButton: FloatingActionButton(
            onPressed: () {},
            backgroundColor: const Color(0xFF7E86F9),
            child: const Icon(Icons.add, color: Colors.white),
          ),
          body: Column(
            children: [
              if (contacts.isNotEmpty)
                SizedBox(
                  height: 120,
                  child: ListView.separated(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    scrollDirection: Axis.horizontal,
                    itemCount: contacts.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 16),
                    itemBuilder: (context, index) {
                      final contact = contacts[index];
                      return Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          CircleAvatar(
                            radius: 28,
                            backgroundColor: const Color(0xFFE0E4FF),
                            child: Text(
                              contact.name.substring(0, 1).toUpperCase(),
                              style: const TextStyle(
                                color: Color(0xFF4D59FF),
                                fontWeight: FontWeight.bold,
                                fontSize: 20,
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          SizedBox(
                            width: 88,
                            child: Text(
                              contact.name,
                              overflow: TextOverflow.ellipsis,
                              textAlign: TextAlign.center,
                              style: const TextStyle(fontSize: 12),
                            ),
                          ),
                        ],
                      );
                    },
                  ),
                ),
              Expanded(
                child: ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                  itemCount: docs.length,
                  itemBuilder: (context, index) {
                    final doc = docs[index];
                    final data = doc.data();
                    final participants = _participants(data['participants']);
                    final others =
                        participants.where((id) => id != currentUserId).toList();
                    final title = (data['title'] as String?)?.trim();
                    final name = title != null && title.isNotEmpty
                        ? title
                        : (others.isNotEmpty ? others.first : 'Conversation');
                    final lastMessage = (data['lastMessage'] as String?) ?? '';
                    final timestamp = _parseTimestamp(data['timestamp']);
                    final unreadBy = _participants(data['unreadBy']);
                    final isUnread = unreadBy.contains(currentUserId) ||
                        (data['unread'] as bool? ?? false);

                    return Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(20),
                        onTap: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (_) => MessageThreadScreen(
                                conversationId: doc.id,
                                initialTitle: name,
                              ),
                            ),
                          );
                        },
                        child: Container(
                          decoration: BoxDecoration(
                            color: const Color(0xFFE8EDFF),
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withOpacity(0.05),
                                blurRadius: 6,
                                offset: const Offset(0, 3),
                              ),
                            ],
                          ),
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              CircleAvatar(
                                radius: 28,
                                backgroundColor: const Color(0xFFE0E4FF),
                                child: Text(
                                  name.substring(0, 1).toUpperCase(),
                                  style: const TextStyle(
                                    color: Color(0xFF4D59FF),
                                    fontWeight: FontWeight.bold,
                                    fontSize: 20,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      name,
                                      style: const TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    const SizedBox(height: 6),
                                    Text(
                                      lastMessage,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: Colors.black54,
                                        fontSize: 14,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    _formatDate(timestamp),
                                    style: const TextStyle(
                                      color: Colors.black45,
                                      fontSize: 12,
                                    ),
                                  ),
                                  const SizedBox(height: 12),
                                  if (isUnread)
                                    Container(
                                      width: 12,
                                      height: 12,
                                      decoration: const BoxDecoration(
                                        color: Color(0xFFD62828),
                                        shape: BoxShape.rectangle,
                                      ),
                                    ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _ContactChip {
  const _ContactChip({required this.name, required this.id});

  final String name;
  final String id;
}
