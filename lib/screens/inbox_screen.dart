import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../sample_data.dart';

class InboxScreen extends StatelessWidget {
  const InboxScreen({super.key});

  static const String _notificationsCollection = 'NOTIFICATION';

  @override
  Widget build(BuildContext context) {
    final currentUser = firebase_auth.FirebaseAuth.instance.currentUser;
    if (currentUser == null) {
      return _buildInboxList(SampleData.inbox);
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
          return _buildInboxList(SampleData.inbox);
        }

        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }

        final docs = snapshot.data?.docs ?? [];
        if (docs.isEmpty) {
          return const Center(child: Text('No notifications yet.'));
        }

        return ListView.separated(
          padding: const EdgeInsets.all(16),
          itemCount: docs.length,
          separatorBuilder: (_, __) => const SizedBox(height: 12),
          itemBuilder: (context, index) {
            final data = docs[index].data();
            final title = (data['type'] as String?)?.trim();
            final details = (data['details'] as String?)?.trim();
            final timestamp = _parseTimestamp(data['timestamp']);
            final timestampLabel = _formatTimestamp(timestamp);

            return Card(
              child: ListTile(
                leading: const Icon(Icons.notifications_active),
                title: Text(title?.isNotEmpty == true ? title! : 'Notification'),
                subtitle: Text(
                  details?.isNotEmpty == true
                      ? details!
                      : 'No details available.',
                ),
                trailing: Text(timestampLabel),
              ),
            );
          },
        );
      },
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

        return Card(
          child: ListTile(
            leading: const Icon(Icons.notifications_active),
            title: Text(item.title),
            subtitle: Text(item.body),
            trailing: Text(timestampLabel),
          ),
        );
      },
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
