import 'package:flutter/material.dart';

class NewsScreen extends StatelessWidget {
  const NewsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final items = [
      (
        'Platform parity update',
        'We matched the Android announcements with iOS friendly cards.',
      ),
      (
        'New recommendation flow',
        'Open the learning path to see the embedded recommendation mock.',
      ),
      (
        'Messaging refresh',
        'Thread previews and unread badges now align with the Android build.',
      ),
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final item = items[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            leading: const Icon(Icons.campaign),
            title: Text(item.$1),
            subtitle: Text(item.$2),
          ),
        );
      },
    );
  }
}
