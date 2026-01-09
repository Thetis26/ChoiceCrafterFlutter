import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../sample_data.dart';

class InboxScreen extends StatelessWidget {
  const InboxScreen({super.key});

  static const String _notificationsCollection = 'NOTIFICATIONS';

  @override
  Widget build(BuildContext context) {
    final currentUser = firebase_auth.FirebaseAuth.instance.currentUser;
    if (currentUser == null) {
      return _buildScaffold(context, _buildInboxList(SampleData.inbox));
    }

    final stream = FirebaseFirestore.instance
        .collection(_notificationsCollection)
        .where('userId', isEqualTo: currentUser.uid)
        .orderBy('timestamp', descending: true)
        .snapshots();

    return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
      stream: stream,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return _buildScaffold(context, _buildInboxList(SampleData.inbox));
        }

        if (snapshot.connectionState == ConnectionState.waiting) {
          return _buildScaffold(
            context,
            const Center(child: CircularProgressIndicator()),
          );
        }

        final docs = snapshot.data?.docs ?? [];
        if (docs.isEmpty) {
          return _buildScaffold(
            context,
            const Center(child: Text('No notifications yet.')),
          );
        }

        return _buildScaffold(
          context,
          ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: docs.length,
            separatorBuilder: (_, __) => const SizedBox(height: 12),
            itemBuilder: (context, index) {
              final data = docs[index].data();
              final title = (data['type'] as String?)?.trim();
              final details = (data['details'] as String?)?.trim();
              final timestamp = _parseTimestamp(data['timestamp']);
              final timestampLabel = _formatTimestamp(timestamp);

              return _buildNotificationCard(
                context,
                title: title?.isNotEmpty == true ? title! : 'Notification',
                details: details?.isNotEmpty == true
                    ? details!
                    : 'No details available.',
                timestampLabel: timestampLabel,
              );
            },
          ),
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

  Widget _buildInboxList(List<InboxItem> items) {
    if (items.isEmpty) {
      return const Center(child: Text('No notifications yet.'));
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final item = items[index];
        final timestampLabel = _formatTimestamp(item.timestamp);

        return _buildNotificationCard(
          context,
          title: item.title,
          details: item.body,
          timestampLabel: timestampLabel,
        );
      },
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
}
