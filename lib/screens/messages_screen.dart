import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import 'message_thread_screen.dart';

class MessagesScreen extends StatelessWidget {
  const MessagesScreen({super.key});

  Future<String> _resolveCurrentUserId({
    required FirebaseFirestore firestore,
    required firebase_auth.FirebaseAuth auth,
  }) async {
    final user = auth.currentUser;
    if (user == null) {
      return '';
    }

    final email = user.email ?? '';
    if (email.isNotEmpty) {
      final query = await firestore
          .collection('users')
          .where('email', isEqualTo: email)
          .limit(1)
          .get();
      if (query.docs.isNotEmpty) {
        return query.docs.first.id;
      }
    }

    final fallbackDoc = await firestore.collection('users').doc(user.uid).get();
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

  Future<String?> _findExistingConversation({
    required FirebaseFirestore firestore,
    required String currentUserId,
    required String otherUserId,
  }) async {
    if (currentUserId.isEmpty || otherUserId.isEmpty) {
      return null;
    }
    final snapshot = await firestore
        .collection('conversations')
        .where('participants', arrayContains: currentUserId)
        .get();
    for (final doc in snapshot.docs) {
      final data = doc.data();
      final participants = _participants(data['participants']);
      if (participants.contains(otherUserId) && participants.length == 2) {
        return doc.id;
      }
    }
    return null;
  }

  Future<String> _createConversation({
    required FirebaseFirestore firestore,
    required List<String> participants,
    String? title,
  }) async {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    final conversationRef = firestore.collection('conversations').doc();
    await conversationRef.set({
      'participants': participants,
      if (title != null && title.trim().isNotEmpty) 'title': title.trim(),
      'lastMessage': '',
      'lastMessageSenderId': '',
      'timestamp': timestamp,
      'unread': false,
      'unreadBy': <String>[],
    });
    return conversationRef.id;
  }

  Future<void> _openConversation({
    required BuildContext context,
    required String conversationId,
    required String title,
  }) async {
    final destination = MessageThreadScreen(
      conversationId: conversationId,
      initialTitle: title,
    );
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => destination),
    );
  }

  Future<void> _startConversationWith({
    required BuildContext context,
    required FirebaseFirestore firestore,
    required String currentUserId,
    required _ContactChip contact,
  }) async {
    if (currentUserId.isEmpty) {
      return;
    }
    final existingId = await _findExistingConversation(
      firestore: firestore,
      currentUserId: currentUserId,
      otherUserId: contact.id,
    );
    if (existingId != null) {
      await _openConversation(
        context: context,
        conversationId: existingId,
        title: contact.name,
      );
      return;
    }
    final conversationId = await _createConversation(
      firestore: firestore,
      participants: [currentUserId, contact.id],
    );
    await _openConversation(
      context: context,
      conversationId: conversationId,
      title: contact.name,
    );
  }

  Future<void> _showNewConversationPicker({
    required BuildContext context,
    required FirebaseFirestore firestore,
    required String currentUserId,
  }) async {
    if (currentUserId.isEmpty) {
      return;
    }
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFFF4F6FB),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) {
        final selectedIds = <String>{};
        var searchQuery = '';
        return StatefulBuilder(
          builder: (context, setState) {
            return SafeArea(
              child: Padding(
                padding: EdgeInsets.only(
                  left: 20,
                  right: 20,
                  top: 16,
                  bottom: MediaQuery.of(context).viewInsets.bottom + 20,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 40,
                      height: 4,
                      margin: const EdgeInsets.only(bottom: 16),
                      decoration: BoxDecoration(
                        color: Colors.black26,
                        borderRadius: BorderRadius.circular(999),
                      ),
                    ),
                    const Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        'New conversation',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      decoration: InputDecoration(
                        hintText: 'Search users',
                        prefixIcon: const Icon(Icons.search),
                        filled: true,
                        fillColor: Colors.white,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16),
                          borderSide: BorderSide.none,
                        ),
                      ),
                      onChanged: (value) {
                        setState(() {
                          searchQuery = value.trim().toLowerCase();
                        });
                      },
                    ),
                    const SizedBox(height: 12),
                    Flexible(
                      child: StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
                        stream: firestore.collection('users').snapshots(),
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
                              .map((doc) {
                                final data = doc.data();
                                return _ContactChip(
                                  name: _displayName(data, doc.id),
                                  id: doc.id,
                                );
                              })
                              .where((user) => user.id != currentUserId)
                              .where((user) => searchQuery.isEmpty
                                  ? true
                                  : user.name
                                      .toLowerCase()
                                      .contains(searchQuery))
                              .toList()
                            ..sort(
                              (a, b) => a.name.toLowerCase().compareTo(
                                    b.name.toLowerCase(),
                                  ),
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
                              final isSelected = selectedIds.contains(user.id);
                              return CheckboxListTile(
                                value: isSelected,
                                onChanged: (value) {
                                  setState(() {
                                    if (value ?? false) {
                                      selectedIds.add(user.id);
                                    } else {
                                      selectedIds.remove(user.id);
                                    }
                                  });
                                },
                                title: Text(user.name),
                                secondary: CircleAvatar(
                                  backgroundColor: const Color(0xFFE0E4FF),
                                  child: Text(
                                    user.name.substring(0, 1).toUpperCase(),
                                    style: const TextStyle(
                                      color: Color(0xFF4D59FF),
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                                controlAffinity:
                                    ListTileControlAffinity.trailing,
                              );
                            },
                          );
                        },
                      ),
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: selectedIds.isEmpty
                            ? null
                            : () async {
                                final snapshot = await firestore
                                    .collection('users')
                                    .where(FieldPath.documentId,
                                        whereIn: selectedIds.toList())
                                    .get();
                                final selectedUsers = snapshot.docs.map((doc) {
                                  final data = doc.data();
                                  return _ContactChip(
                                    name: _displayName(data, doc.id),
                                    id: doc.id,
                                  );
                                }).toList();
                                if (selectedUsers.isEmpty) {
                                  return;
                                }
                                Navigator.of(context).pop();
                                if (selectedUsers.length == 1) {
                                  await _startConversationWith(
                                    context: context,
                                    firestore: firestore,
                                    currentUserId: currentUserId,
                                    contact: selectedUsers.first,
                                  );
                                  return;
                                }
                                final participantIds = [
                                  currentUserId,
                                  ...selectedUsers.map((user) => user.id),
                                ];
                                final title = selectedUsers
                                    .map((user) => user.name)
                                    .join(', ');
                                final conversationId =
                                    await _createConversation(
                                  firestore: firestore,
                                  participants: participantIds,
                                  title: title,
                                );
                                await _openConversation(
                                  context: context,
                                  conversationId: conversationId,
                                  title: title,
                                );
                              },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF7E86F9),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                          ),
                        ),
                        child: const Text('Start conversation'),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final firestore = FirebaseFirestore.instance;
    final auth = firebase_auth.FirebaseAuth.instance;
    return FutureBuilder<String>(
      future: _resolveCurrentUserId(firestore: firestore, auth: auth),
      builder: (context, currentUserSnapshot) {
        if (currentUserSnapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }

        final currentUserId = currentUserSnapshot.data ?? '';

        return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
          stream: firestore.collection('users').snapshots(),
          builder: (context, usersSnapshot) {
            final users = usersSnapshot.hasData && !usersSnapshot.hasError
                ? usersSnapshot.data?.docs ?? []
                : const <QueryDocumentSnapshot<Map<String, dynamic>>>[];
            final userNames = <String, String>{
              for (final doc in users) doc.id: _displayName(doc.data(), doc.id),
            };
            final contacts = users
                .where((doc) => (doc.data()['online'] as bool?) ?? false)
                .map((doc) {
                  final data = doc.data();
                  return _ContactChip(
                    name: _displayName(data, doc.id),
                    id: doc.id,
                  );
                })
                .toList();

            if (currentUserId.isEmpty) {
              return _buildMessagesScaffold(
                context,
                const [],
                contacts: contacts,
                currentUserId: currentUserId,
                firestore: firestore,
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
                  return _buildMessagesScaffold(
                    context,
                    const [],
                    contacts: contacts,
                    currentUserId: currentUserId,
                    firestore: firestore,
                  );
                }

                final docs = snapshot.data?.docs ?? [];
                final entries = docs.map((doc) {
                  final data = doc.data();
                  final participants = _participants(data['participants']);
                  final others =
                      participants.where((id) => id != currentUserId).toList();
                  final title = (data['title'] as String?)?.trim();
                  final otherNames = others
                      .map((id) => userNames[id] ?? id)
                      .where((name) => name.trim().isNotEmpty)
                      .toList();
                  final name = title != null && title.isNotEmpty
                      ? title
                      : (otherNames.isNotEmpty
                          ? otherNames.join(', ')
                          : 'Conversation');
                  final lastMessage = (data['lastMessage'] as String?) ?? '';
                  final timestamp = _parseTimestamp(data['timestamp']);
                  final unreadBy = _participants(data['unreadBy']);
                  final isUnread = unreadBy.contains(currentUserId) ||
                      (data['unread'] as bool? ?? false);

                  return _MessageEntry(
                    name: name,
                    lastMessage: lastMessage,
                    timestamp: timestamp,
                    isUnread: isUnread,
                    conversationId: doc.id,
                  );
                }).toList();

                return _buildMessagesScaffold(
                  context,
                  entries,
                  contacts: contacts,
                  currentUserId: currentUserId,
                  firestore: firestore,
                );
              },
            );
          },
        );
      },
    );
  }

  Widget _buildMessagesScaffold(
    BuildContext context,
    List<_MessageEntry> entries, {
    required List<_ContactChip> contacts,
    required String currentUserId,
    required FirebaseFirestore firestore,
  }) {
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      appBar: AppBar(
        title: const Text('Messages'),
        backgroundColor: const Color(0xFF7E86F9),
        foregroundColor: Colors.white,
        elevation: 4,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showNewConversationPicker(
          context: context,
          firestore: firestore,
          currentUserId: currentUserId,
        ),
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
                  return InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => _startConversationWith(
                      context: context,
                      firestore: firestore,
                      currentUserId: currentUserId,
                      contact: contact,
                    ),
                    child: Column(
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
                    ),
                  );
                },
              ),
            ),
          Expanded(
            child: entries.isEmpty
                ? const Center(child: Text('No conversations yet.'))
                : ListView.builder(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    itemCount: entries.length,
                    itemBuilder: (context, index) {
                      final entry = entries[index];

                      return Padding(
                        padding: const EdgeInsets.only(bottom: 16),
                        child: InkWell(
                          borderRadius: BorderRadius.circular(20),
                          onTap: entry.conversationId == null
                              ? null
                              : () {
                                  final destination = MessageThreadScreen(
                                    conversationId: entry.conversationId!,
                                    initialTitle: entry.name,
                                  );
                                  Navigator.of(context).push(
                                    MaterialPageRoute(
                                      builder: (_) => destination,
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
                                    entry.name.substring(0, 1).toUpperCase(),
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
                                        entry.name,
                                        style: const TextStyle(
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        entry.lastMessage,
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
                                      _formatDate(entry.timestamp),
                                      style: const TextStyle(
                                        color: Colors.black45,
                                        fontSize: 12,
                                      ),
                                    ),
                                    const SizedBox(height: 12),
                                    if (entry.isUnread)
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
  }
}

class _ContactChip {
  const _ContactChip({required this.name, required this.id});

  final String name;
  final String id;
}

class _MessageEntry {
  const _MessageEntry({
    required this.name,
    required this.lastMessage,
    required this.timestamp,
    required this.isUnread,
    this.conversationId,
  });

  final String name;
  final String lastMessage;
  final DateTime? timestamp;
  final bool isUnread;
  final String? conversationId;
}
