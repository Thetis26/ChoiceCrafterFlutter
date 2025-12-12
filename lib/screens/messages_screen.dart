import 'package:flutter/material.dart';

import '../sample_data.dart';

class MessagesScreen extends StatelessWidget {
  const MessagesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: SampleData.messages.length,
      itemBuilder: (context, index) {
        final thread = SampleData.messages[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            leading: CircleAvatar(
              child: Text(thread.sender.substring(0, 1)),
            ),
            title: Text(thread.sender),
            subtitle: Text(thread.preview),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (thread.unreadCount > 0)
                  CircleAvatar(
                    radius: 10,
                    backgroundColor: Colors.deepPurple,
                    child: Text(
                      thread.unreadCount.toString(),
                      style: const TextStyle(color: Colors.white, fontSize: 12),
                    ),
                  ),
                const SizedBox(height: 4),
                Text('${thread.timestamp.hour.toString().padLeft(2, '0')}:${thread.timestamp.minute.toString().padLeft(2, '0')}'),
              ],
            ),
          ),
        );
      },
    );
  }
}
