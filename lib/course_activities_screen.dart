// lib/course_activities_screen.dart

import 'package:flutter/material.dart';

class CourseActivitiesScreen extends StatefulWidget {
  const CourseActivitiesScreen({super.key});

  @override
  State<CourseActivitiesScreen> createState() => _CourseActivitiesScreenState();
}

class _CourseActivitiesScreenState extends State<CourseActivitiesScreen> {
  String? courseId;
  String? highlightActivityId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Safely extract navigation arguments here.
    final arguments = ModalRoute.of(context)?.settings.arguments;
    if (arguments is Map) {
      setState(() {
        courseId = arguments['courseId'];
        highlightActivityId = arguments['highlightActivityId'];
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(courseId != null ? 'Course: $courseId' : 'Course Activities'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Displaying activities for course: ${courseId ?? "Unknown"}'),
            if (highlightActivityId != null)
              Padding(
                padding: const EdgeInsets.only(top: 16.0),
                child: Text('Highlighting activity: $highlightActivityId'),
              ),
            // TODO: Fetch and display the list of activities for the courseId.
            // You would typically use a ListView.builder here.
          ],
        ),
      ),
    );
  }
}
