// lib/activity_screen.dart

import 'package:flutter/material.dart';

class ActivityScreen extends StatefulWidget {const ActivityScreen({super.key});

@override
State<ActivityScreen> createState() => _ActivityScreenState();
}

class _ActivityScreenState extends State<ActivityScreen> {
  Map<String, dynamic>? activityData;
  String? courseId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is Map) {
      setState(() {
        // The 'activity' argument will be a Map<Object?, Object?>
        // so we cast it to the expected type.
        activityData = Map<String, dynamic>.from(arguments['activity'] as Map);
        courseId = arguments['courseId'];
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    // You can access activity properties like this:
    final String activityName = activityData?['name'] ?? 'Unknown Activity';

    return Scaffold(
      appBar: AppBar(
        title: Text(activityName),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Details for activity: $activityName'),
            if (courseId != null)
              Padding(
                padding: const EdgeInsets.only(top: 16.0),
                child: Text('From course: $courseId'),
              ),
            // TODO: Display all other details from the 'activityData' map.
            // e.g., Text(activityData?['description'] ?? ''),
          ],
        ),
      ),
    );
  }
}
