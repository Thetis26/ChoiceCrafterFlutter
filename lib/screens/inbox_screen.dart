import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../sample_data.dart';

class InboxScreen extends StatelessWidget {
  const InboxScreen({super.key});

  static const String _notificationsCollection = 'NOTIFICATIONS';
  static const String _usersCollection = 'users';

  @override
  Widget build(BuildContext context) {
    final currentUser = firebase_auth.FirebaseAuth.instance.currentUser;
    if (currentUser == null) {
      return _buildLocalInbox(context);
    }

    return FutureBuilder<String?>(
      future: _resolveUserId(currentUser),
      builder: (context, userSnapshot) {
        if (userSnapshot.hasError) {
          return _buildScaffold(
            context,
            const Center(child: Text('Unable to load notifications.')),
          );
        }

        if (userSnapshot.connectionState == ConnectionState.waiting) {
          return _buildScaffold(
            context,
            const Center(child: CircularProgressIndicator()),
          );
        }

        final userId = userSnapshot.data;
        if (userId == null || userId.isEmpty) {
          return _buildLocalInbox(context);
        }

        final stream = FirebaseFirestore.instance
            .collection(_notificationsCollection)
            .where('userId', isEqualTo: userId)
            .orderBy('timestamp', descending: true)
            .snapshots();

        return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
          stream: stream,
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return _buildScaffold(
                context,
                const Center(child: Text('Unable to load notifications.')),
              );
            }

            if (snapshot.connectionState == ConnectionState.waiting) {
              return _buildScaffold(
                context,
                const Center(child: CircularProgressIndicator()),
              );
            }

            final docs = snapshot.data?.docs ?? [];
            if (docs.isEmpty) {
              return _buildLocalInbox(context);
            }

            return _buildScaffold(
              context,
              _buildNotificationList(
                context,
                docs
                    .map(
                      (doc) {
                        final data = doc.data();
                        final title = (data['type'] as String?)?.trim();
                        final details = (data['details'] as String?)?.trim();
                        return _InboxEntry(
                          title: title?.isNotEmpty == true
                              ? title!
                              : 'Notification',
                          details: details?.isNotEmpty == true
                              ? details!
                              : 'No details available.',
                          timestamp: _parseTimestamp(data['timestamp']),
                        );
                      },
                    )
                    .toList(),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildScaffold(BuildContext context, Widget child) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Inbox'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
      ),
      body: _wrapBody(context, child),
    );
  }

  Widget _wrapBody(BuildContext context, Widget child) {
    final theme = Theme.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(color: theme.colorScheme.surface),
      child: SafeArea(child: child),
    );
  }

  Widget _buildNotificationCard(
    BuildContext context, {
    required String title,
    required String details,
    required String timestampLabel,
  }) {
    final theme = Theme.of(context);
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            CircleAvatar(
              radius: 20,
              backgroundColor: theme.colorScheme.primary.withOpacity(0.1),
              child: Icon(
                Icons.notifications_active,
                color: theme.colorScheme.primary,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    details,
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Text(
              timestampLabel,
              style: theme.textTheme.labelSmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }

  DateTime? _parseTimestamp(dynamic value) {
    if (value is Timestamp) {
      return value.toDate();
    }
    if (value is DateTime) {
      return value;
    }
    if (value is int) {
      return DateTime.fromMillisecondsSinceEpoch(value);
    }
    if (value is String) {
      return DateTime.tryParse(value);
    }
    return null;
  }

  String _formatTimestamp(DateTime? dateTime) {
    if (dateTime == null) {
      return '';
    }
    final date =
        '${dateTime.year.toString().padLeft(4, '0')}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')}';
    final time =
        '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
    return '$date $time';
  }

  Future<String?> _resolveUserId(firebase_auth.User user) async {
    final email = user.email?.trim() ?? '';
    final usersCollection =
        FirebaseFirestore.instance.collection(_usersCollection);

    if (email.isNotEmpty) {
      final query = await usersCollection
          .where('email', isEqualTo: email)
          .limit(1)
          .get();
      if (query.docs.isNotEmpty) {
        return query.docs.first.id;
      }
    }

    final fallbackDoc = await usersCollection.doc(user.uid).get();
    if (fallbackDoc.exists) {
      return fallbackDoc.id;
    }

    return null;
  }

  Widget _buildLocalInbox(BuildContext context) {
    if (SampleData.inbox.isEmpty) {
      return _buildScaffold(
        context,
        const Center(child: Text('No notifications yet.')),
      );
    }

    return _buildScaffold(
      context,
      _buildNotificationList(
        context,
        SampleData.inbox
            .map(
              (item) => _InboxEntry(
                title: item.title,
                details: item.body,
                timestamp: item.timestamp,
              ),
            )
            .toList(),
      ),
    );
  }

  Widget _buildNotificationList(
    BuildContext context,
    List<_InboxEntry> entries,
  ) {
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: entries.length,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final entry = entries[index];
        final timestampLabel = _formatTimestamp(entry.timestamp);

        return _buildNotificationCard(
          context,
          title: entry.title,
          details: entry.details,
          timestampLabel: timestampLabel,
        );
      },
    );
  }
}

class _InboxEntry {
  const _InboxEntry({
    required this.title,
    required this.details,
    required this.timestamp,
  });

  final String title;
  final String details;
  final DateTime? timestamp;
}
