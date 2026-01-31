// dart
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import 'message_thread_screen.dart';
import 'dart:developer' as developer;

class MessagesScreen extends StatelessWidget {
  const MessagesScreen({super.key});

  Future<String> _resolveCurrentUserId({
    required FirebaseFirestore firestore,
    required firebase_auth.FirebaseAuth auth,
  }) async {
    developer.log(
      'Starting _resolveCurrentUserId',
      name: 'messages._resolveCurrentUserId',
    );
    final user = auth.currentUser;
    if (user == null) {
      developer.log(
        'No authenticated Firebase user',
        name: 'messages._resolveCurrentUserId',
      );
      return '';
    }

    final providers = user.providerData
        .map((p) => '${p.providerId}:${p.email ?? p.uid ?? ''}')
        .join(',');
    developer.log(
      'Auth user details: uid=${user.uid}, email=${user.email}, displayName=${user.displayName}, phone=${user.phoneNumber}, providers=$providers',
      name: 'messages._resolveCurrentUserId',
    );

    final email = user.email ?? '';
    if (email.isNotEmpty) {
      developer.log(
        'Querying users collection by email: $email',
        name: 'messages._resolveCurrentUserId',
      );
      final query = await firestore
          .collection('users')
          .where('email', isEqualTo: email)
          .limit(1)
          .get();
      developer.log(
        'Email query returned ${query.docs.length} documents',
        name: 'messages._resolveCurrentUserId',
      );
      if (query.docs.isNotEmpty) {
        final id = query.docs.first.id;
        developer.log(
          'Found user doc by email: $id',
          name: 'messages._resolveCurrentUserId',
        );
        return id;
      } else {
        developer.log(
          'No user doc found by email',
          name: 'messages._resolveCurrentUserId',
        );
      }
    } else {
      developer.log(
        'Auth user has no email, skipping email lookup',
        name: 'messages._resolveCurrentUserId',
      );
    }

    developer.log(
      'Attempting fallback lookup by auth uid: ${user.uid}',
      name: 'messages._resolveCurrentUserId',
    );
    final fallbackDoc = await firestore.collection('users').doc(user.uid).get();
    if (fallbackDoc.exists) {
      developer.log(
        'Found fallback user doc by uid: ${fallbackDoc.id}',
        name: 'messages._resolveCurrentUserId',
      );
      return fallbackDoc.id;
    }

    developer.log(
      'Falling back to returning auth uid: ${user.uid}',
      name: 'messages._resolveCurrentUserId',
    );
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

  String? _avatarUrl(Map<String, dynamic> data) {
    final anonymousAvatar = data['anonymousAvatar'];
    final anonymousAvatarUrl = anonymousAvatar is Map
        ? anonymousAvatar['imageUrl'] as String?
        : null;
    if (anonymousAvatarUrl != null && anonymousAvatarUrl.trim().isNotEmpty) {
      return anonymousAvatarUrl.trim();
    }
    final directUrl = (data['avatarUrl'] as String?)?.trim();
    if (directUrl != null && directUrl.isNotEmpty) {
      return directUrl;
    }
    return null;
  }

  String _initials(String name) {
    final trimmed = name.trim();
    if (trimmed.isEmpty) {
      return '?';
    }
    return trimmed.substring(0, 1).toUpperCase();
  }

  Widget _buildAvatarCircle(
    _AvatarToken token, {
    double radius = 28,
  }) {
    final imageUrl = token.imageUrl;
    return CircleAvatar(
      radius: radius,
      backgroundColor: const Color(0xFFE0E4FF),
      backgroundImage: imageUrl != null ? NetworkImage(imageUrl) : null,
      child: imageUrl == null
          ? Text(
              _initials(token.name),
              style: TextStyle(
                color: const Color(0xFF4D59FF),
                fontWeight: FontWeight.bold,
                fontSize: radius * 0.75,
              ),
            )
          : null,
    );
  }

  Widget _buildAvatarWithBorder(
    _AvatarToken token, {
    required double radius,
  }) {
    return CircleAvatar(
      radius: radius,
      backgroundColor: Colors.white,
      child: _buildAvatarCircle(token, radius: radius - 2),
    );
  }

  Widget _buildAvatarGroup(
    List<_AvatarToken> tokens, {
    double size = 56,
  }) {
    if (tokens.isEmpty) {
      return _buildAvatarCircle(
        const _AvatarToken(name: 'Conversation'),
        radius: size / 2,
      );
    }
    if (tokens.length == 1) {
      return _buildAvatarCircle(tokens.first, radius: size / 2);
    }
    final displayTokens = tokens.take(2).toList();
    final smallDiameter = size * 0.7;
    final smallRadius = smallDiameter / 2;
    return SizedBox(
      width: size,
      height: size,
      child: Stack(
        children: [
          Positioned(
            left: 0,
            top: 0,
            child: _buildAvatarWithBorder(
              displayTokens[0],
              radius: smallRadius,
            ),
          ),
          Positioned(
            right: 0,
            bottom: 0,
            child: _buildAvatarWithBorder(
              displayTokens[1],
              radius: smallRadius,
            ),
          ),
        ],
      ),
    );
  }

  Future<String?> _findExistingConversation({
    required FirebaseFirestore firestore,
    required String currentUserId,
    required String otherUserId,
  }) async {
    developer.log(
      'Looking for existing conversation: currentUser=$currentUserId, otherUser=$otherUserId',
      name: 'messages._findExistingConversation',
    );
    if (currentUserId.isEmpty || otherUserId.isEmpty) {
      developer.log(
        'One of the participant ids is empty, aborting search',
        name: 'messages._findExistingConversation',
      );
      return null;
    }
    final snapshot = await firestore
        .collection('conversations')
        .where('participants', arrayContains: currentUserId)
        .get();
    developer.log(
      'Found ${snapshot.docs.length} conversations',
      name: 'messages._findExistingConversation',
    );
    for (final doc in snapshot.docs) {
      final data = doc.data();
      final participants = _participants(data['participants']);
      developer.log(
        'Checking conversation ${doc.id} with participants $participants',
        name: 'messages._findExistingConversation',
      );
      if (participants.contains(otherUserId) && participants.length == 2) {
        developer.log(
          'Matched existing conversation ${doc.id}',
          name: 'messages._findExistingConversation',
        );
        return doc.id;
      }
    }
    developer.log(
      'No existing conversation found',
      name: 'messages._findExistingConversation',
    );
    return null;
  }

  Future<String> _createConversation({
    required FirebaseFirestore firestore,
    required List<String> participants,
    String? title,
  }) async {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    final conversationRef = firestore.collection('conversations').doc();
    developer.log(
      'Creating conversation ${conversationRef.id} with participants=$participants title=${title ?? ''}',
      name: 'messages._createConversation',
    );
    await conversationRef.set({
      'participants': participants,
      if (title != null && title.trim().isNotEmpty) 'title': title.trim(),
      'lastMessage': '',
      'lastMessageSenderId': '',
      'timestamp': timestamp,
      'unread': false,
      'unreadBy': <String>[],
    });
    developer.log(
      'Conversation created: ${conversationRef.id}',
      name: 'messages._createConversation',
    );
    return conversationRef.id;
  }

  Future<void> _openConversation({
    required BuildContext context,
    required String conversationId,
    required String title,
  }) async {
    developer.log(
      'Opening conversation: id=$conversationId, title=$title',
      name: 'messages._openConversation',
    );
    final destination = MessageThreadScreen(
      conversationId: conversationId,
      initialTitle: title,
    );
    await Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => destination));
    developer.log(
      'Returned from conversation: id=$conversationId',
      name: 'messages._openConversation',
    );
  }

  Future<void> _startConversationWith({
    required BuildContext context,
    required FirebaseFirestore firestore,
    required String currentUserId,
    required _ContactChip contact,
  }) async {
    developer.log(
      'Start conversation requested: currentUser=$currentUserId, contact=${contact.id}',
      name: 'messages._startConversationWith',
    );
    if (currentUserId.isEmpty) {
      developer.log(
        'Current user id is empty, aborting start conversation',
        name: 'messages._startConversationWith',
      );
      return;
    }
    final existingId = await _findExistingConversation(
      firestore: firestore,
      currentUserId: currentUserId,
      otherUserId: contact.id,
    );
    if (existingId != null) {
      developer.log(
        'Existing conversation found: $existingId - opening',
        name: 'messages._startConversationWith',
      );
      await _openConversation(
        context: context,
        conversationId: existingId,
        title: contact.name,
      );
      return;
    }
    developer.log(
      'No existing conversation, creating new',
      name: 'messages._startConversationWith',
    );
    final conversationId = await _createConversation(
      firestore: firestore,
      participants: [currentUserId, contact.id],
    );
    developer.log(
      'Created conversation $conversationId - opening',
      name: 'messages._startConversationWith',
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
    developer.log(
      'Showing new conversation picker for user $currentUserId',
      name: 'messages._showNewConversationPicker',
    );
    if (currentUserId.isEmpty) {
      developer.log(
        'Current user id empty - aborting showNewConversationPicker',
        name: 'messages._showNewConversationPicker',
      );
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
                          developer.log(
                            'Users stream state: ${snapshot.connectionState}',
                            name: 'messages._showNewConversationPicker',
                          );
                          if (snapshot.connectionState ==
                              ConnectionState.waiting) {
                            return const Center(
                              child: CircularProgressIndicator(),
                            );
                          }
                          if (snapshot.hasError) {
                            developer.log(
                              'Users stream error: ${snapshot.error}',
                              name: 'messages._showNewConversationPicker',
                            );
                            return const Center(
                              child: Text('Unable to load users.'),
                            );
                          }
                          final users =
                              (snapshot.data?.docs ?? [])
                                  .map((doc) {
                                    final data = doc.data();
                                    return _ContactChip(
                                      name: _displayName(data, doc.id),
                                      id: doc.id,
                                      avatarUrl: _avatarUrl(data),
                                    );
                                  })
                                  .where((user) => user.id != currentUserId)
                                  .where(
                                    (user) => searchQuery.isEmpty
                                        ? true
                                        : user.name.toLowerCase().contains(
                                            searchQuery,
                                          ),
                                  )
                                  .toList()
                                ..sort(
                                  (a, b) => a.name.toLowerCase().compareTo(
                                    b.name.toLowerCase(),
                                  ),
                                );

                          developer.log(
                            'Users available for picker: ${users.length}',
                            name: 'messages._showNewConversationPicker',
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
                                  backgroundImage: user.avatarUrl != null
                                      ? NetworkImage(user.avatarUrl!)
                                      : null,
                                  child: user.avatarUrl == null
                                      ? Text(
                                          _initials(user.name),
                                          style: const TextStyle(
                                            color: Color(0xFF4D59FF),
                                            fontWeight: FontWeight.bold,
                                          ),
                                        )
                                      : null,
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
                                developer.log(
                                  'Create conversation button pressed with selectedIds=$selectedIds',
                                  name: 'messages._showNewConversationPicker',
                                );
                                final snapshot = await firestore
                                    .collection('users')
                                    .where(
                                      FieldPath.documentId,
                                      whereIn: selectedIds.toList(),
                                    )
                                    .get();
                                final selectedUsers = snapshot.docs.map((doc) {
                                  final data = doc.data();
                                  return _ContactChip(
                                    name: _displayName(data, doc.id),
                                    id: doc.id,
                                    avatarUrl: _avatarUrl(data),
                                  );
                                }).toList();
                                developer.log(
                                  'Selected users from firestore: ${selectedUsers.map((u) => u.id).toList()}',
                                  name: 'messages._showNewConversationPicker',
                                );
                                if (selectedUsers.isEmpty) {
                                  developer.log(
                                    'No selected users found after fetch',
                                    name: 'messages._showNewConversationPicker',
                                  );
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
                                developer.log(
                                  'Creating group conversation with participants=$participantIds title=$title',
                                  name: 'messages._showNewConversationPicker',
                                );
                                final conversationId =
                                    await _createConversation(
                                      firestore: firestore,
                                      participants: participantIds,
                                      title: title,
                                    );
                                developer.log(
                                  'Group conversation created: $conversationId',
                                  name: 'messages._showNewConversationPicker',
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
    developer.log('Building MessagesScreen', name: 'messages.build');
    return FutureBuilder<String>(
      future: _resolveCurrentUserId(firestore: firestore, auth: auth),
      builder: (context, currentUserSnapshot) {
        developer.log(
          'Current user Future state: ${currentUserSnapshot.connectionState}',
          name: 'messages.build',
        );
        if (currentUserSnapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }

        final currentUserId = currentUserSnapshot.data ?? '';
        developer.log(
          'Resolved currentUserId=$currentUserId',
          name: 'messages.build',
        );

        return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
          stream: firestore.collection('users').snapshots(),
          builder: (context, usersSnapshot) {
            developer.log(
              'Users stream state: ${usersSnapshot.connectionState}',
              name: 'messages.build.usersStream',
            );
            final users = usersSnapshot.hasData && !usersSnapshot.hasError
                ? usersSnapshot.data?.docs ?? []
                : const <QueryDocumentSnapshot<Map<String, dynamic>>>[];
            developer.log(
              'Users snapshot contains ${users.length} docs',
              name: 'messages.build.usersStream',
            );
            final userNames = <String, String>{
              for (final doc in users) doc.id: _displayName(doc.data(), doc.id),
            };
            final userAvatarUrls = <String, String?>{
              for (final doc in users) doc.id: _avatarUrl(doc.data()),
            };
            final contacts = users
                .where((doc) => (doc.data()['online'] as bool?) ?? false)
                .map((doc) {
                  final data = doc.data();
                  return _ContactChip(
                    name: _displayName(data, doc.id),
                    id: doc.id,
                    avatarUrl: _avatarUrl(data),
                  );
                })
                .toList();
            developer.log(
              'Contacts count: ${contacts.length}',
              name: 'messages.build.usersStream',
            );

            if (currentUserId.isEmpty) {
              developer.log(
                'currentUserId empty - rendering scaffold without conversations',
                name: 'messages.build',
              );
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
                developer.log(
                  'Conversations stream state: ${snapshot.connectionState}',
                  name: 'messages.build.conversationsStream',
                );
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }

                if (snapshot.hasError) {
                  developer.log(
                    'Conversations stream error: ${snapshot.error}',
                    name: 'messages.build.conversationsStream',
                  );
                  return _buildMessagesScaffold(
                    context,
                    const [],
                    contacts: contacts,
                    currentUserId: currentUserId,
                    firestore: firestore,
                  );
                }

                final docs = snapshot.data?.docs ?? [];
                developer.log(
                  'Conversations snapshot contains ${docs.length} docs',
                  name: 'messages.build.conversationsStream',
                );
                final entries = docs.map((doc) {
                  final data = doc.data();
                  final participants = _participants(data['participants']);
                  final others = participants
                      .where((id) => id != currentUserId)
                      .toList();
                  final title = (data['title'] as String?)?.trim();
                  final otherNames = others
                      .map((id) => userNames[id] ?? id)
                      .where((name) => name.trim().isNotEmpty)
                      .toList();
                  final otherAvatars = others
                      .map(
                        (id) => _AvatarToken(
                          name: userNames[id] ?? id,
                          imageUrl: userAvatarUrls[id],
                        ),
                      )
                      .toList();
                  final name = title != null && title.isNotEmpty
                      ? title
                      : (otherNames.isNotEmpty
                            ? otherNames.join(', ')
                            : 'Conversation');
                  final lastMessage = (data['lastMessage'] as String?) ?? '';
                  final timestamp = _parseTimestamp(data['timestamp']);
                  final unreadBy = _participants(data['unreadBy']);
                  final isUnread =
                      unreadBy.contains(currentUserId) ||
                      (data['unread'] as bool? ?? false);

                  developer.log(
                    'Conversation ${doc.id}: title=$name, participants=$participants, isUnread=$isUnread',
                    name: 'messages.build.conversationsStream',
                  );

                  return _MessageEntry(
                    name: name,
                    lastMessage: lastMessage,
                    timestamp: timestamp,
                    isUnread: isUnread,
                    conversationId: doc.id,
                    avatars: otherAvatars,
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
    developer.log(
      'Rendering messages scaffold: entries=${entries.length}, contacts=${contacts.length}, currentUserId=$currentUserId',
      name: 'messages._buildMessagesScaffold',
    );
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      appBar: AppBar(
        title: const Text('Messages'),
        backgroundColor: const Color(0xFF7E86F9),
        foregroundColor: Colors.white,
        elevation: 4,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          developer.log(
            'FAB pressed to open new conversation picker',
            name: 'messages._buildMessagesScaffold',
          );
          _showNewConversationPicker(
            context: context,
            firestore: firestore,
            currentUserId: currentUserId,
          );
        },
        backgroundColor: const Color(0xFF7E86F9),
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: Column(
        children: [
          if (contacts.isNotEmpty)
            SizedBox(
              height: 120,
              child: ListView.separated(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                scrollDirection: Axis.horizontal,
                itemCount: contacts.length,
                separatorBuilder: (_, __) => const SizedBox(width: 16),
                itemBuilder: (context, index) {
                  final contact = contacts[index];
                  return InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () {
                      developer.log(
                        'Contact tapped: ${contact.id}',
                        name: 'messages._buildMessagesScaffold',
                      );
                      _startConversationWith(
                        context: context,
                        firestore: firestore,
                        currentUserId: currentUserId,
                        contact: contact,
                      );
                    },
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        _buildAvatarCircle(
                          _AvatarToken(
                            name: contact.name,
                            imageUrl: contact.avatarUrl,
                          ),
                          radius: 28,
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
                                  developer.log(
                                    'Conversation tapped: ${entry.conversationId}',
                                    name: 'messages._buildMessagesScaffold',
                                  );
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
                                _buildAvatarGroup(entry.avatars),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
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
  const _ContactChip({
    required this.name,
    required this.id,
    this.avatarUrl,
  });

  final String name;
  final String id;
  final String? avatarUrl;
}

class _MessageEntry {
  const _MessageEntry({
    required this.name,
    required this.lastMessage,
    required this.timestamp,
    required this.isUnread,
    required this.avatars,
    this.conversationId,
  });

  final String name;
  final String lastMessage;
  final DateTime? timestamp;
  final bool isUnread;
  final List<_AvatarToken> avatars;
  final String? conversationId;
}

class _AvatarToken {
  const _AvatarToken({required this.name, this.imageUrl});

  final String name;
  final String? imageUrl;
}
