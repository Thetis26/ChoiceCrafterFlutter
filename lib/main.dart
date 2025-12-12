import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'activity_screen.dart';
import 'course_activities_screen.dart';

void main() {
  runApp(MyApp());
}

// In your main Flutter app widget (e.g., main.dart)
class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: MyHomePage(),
      // Define your routes here to handle navigation
      routes: {
        '/courseActivities': (context) => CourseActivitiesScreen(),
        '/activity': (context) => ActivityScreen(),
      },
    );
  }
}


class MyHomePage extends StatefulWidget {
  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const _navigationChannel = MethodChannel('com.choicecrafter.students/navigation');

  @override
  void initState() {
    super.initState();
    _navigationChannel.setMethodCallHandler(_handleNavigationCall);
  }

  Future<void> _handleNavigationCall(MethodCall call) async {
    if (call.method == 'navigateTo') {
      final args = call.arguments as Map;
      final route = args['route'] as String;
      // Use Navigator to go to the screen
      Navigator.of(context).pushNamed(route, arguments: args);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("ChoiceCrafter")),
      drawer: Drawer(
        // ... Your drawer items (Logout, etc.)
      ),
      body: Center(
        child: Text("Flutter UI"),
      ),
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          // FIX: Ensure you have at least two items here
          BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Settings',
          ),
          // You can add more items if needed
          // BottomNavigationBarItem(
          //   icon: Icon(Icons.person),
          //   label: 'Profile',
          // ),
        ],
      ),
    );
  }
}
