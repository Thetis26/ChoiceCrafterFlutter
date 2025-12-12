import 'package:flutter/material.dart';

import '../sample_data.dart';

class InboxScreen extends StatelessWidget {
  const InboxScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: SampleData.inbox.length,
      itemBuilder: (context, index) {
        final item = SampleData.inbox[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            leading: const Icon(Icons.notifications_active),
            title: Text(item.title),
            subtitle: Text(item.body),
            trailing: Text('${item.timestamp.hour.toString().padLeft(2, '0')}:${item.timestamp.minute.toString().padLeft(2, '0')}'),
          ),
        );
      },
    );
  }
}
