import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'activity_screen.dart';
import 'course_activities_screen.dart';
import 'sample_data.dart';
import 'screens/colleagues_activity_screen.dart';
import 'screens/feedback_screen.dart';
import 'screens/home_screen.dart';
import 'screens/inbox_screen.dart';
import 'screens/learning_path_screen.dart';
import 'screens/messages_screen.dart';
import 'screens/module_screen.dart';
import 'screens/news_screen.dart';
import 'screens/recommendation_webview_screen.dart';
import 'screens/settings_screen.dart';
import 'screens/statistics_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ChoiceCrafter Students',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(),
      routes: {
        '/courseActivities': (context) => const CourseActivitiesScreen(),
        '/activity': (context) => const ActivityScreen(),
        '/module': (context) => const ModuleScreen(),
        '/learningPath': (context) => const LearningPathScreen(),
        '/recommendation': (context) => const RecommendationWebViewScreen(),
        '/inbox': (context) => const InboxScreen(),
        '/messages': (context) => const MessagesScreen(),
        '/settings': (context) => const SettingsScreen(),
        '/feedback': (context) => const FeedbackScreen(),
      },
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const _navigationChannel =
      MethodChannel('com.choicecrafter.students/navigation');
  int _selectedIndex = 0;

  final List<Widget> _screens = const [
    HomeScreen(),
    ColleaguesActivityScreen(),
    NewsScreen(),
    StatisticsScreen(),
  ];

  @override
  void initState() {
    super.initState();
    _navigationChannel.setMethodCallHandler(_handleNavigationCall);
  }

  Future<void> _handleNavigationCall(MethodCall call) async {
    if (call.method == 'navigateTo') {
      final args = call.arguments as Map;
      final route = args['route'] as String;
      Navigator.of(context).pushNamed(route, arguments: args);
    }
  }

  void _onBottomNavTapped(int index) {
    setState(() => _selectedIndex = index);
  }

  void _openDrawerDestination(String route) {
    Navigator.of(context).pop();
    if (route == '/home') {
      setState(() => _selectedIndex = 0);
      return;
    }
    Navigator.of(context).pushNamed(route);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ChoiceCrafter')),
      drawer: Drawer(
        child: SafeArea(
          child: ListView(
            children: [
              const DrawerHeader(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('ChoiceCrafter'),
                    SizedBox(height: 8),
                    Text('Matches the Android navigation drawer.'),
                  ],
                ),
              ),
              ListTile(
                leading: const Icon(Icons.home),
                title: const Text('Home'),
                onTap: () => _openDrawerDestination('/home'),
              ),
              ListTile(
                leading: const Icon(Icons.notifications),
                title: const Text('Inbox'),
                onTap: () => _openDrawerDestination('/inbox'),
              ),
              ListTile(
                leading: const Icon(Icons.chat),
                title: const Text('Messages'),
                onTap: () => _openDrawerDestination('/messages'),
              ),
              ListTile(
                leading: const Icon(Icons.settings),
                title: const Text('Settings'),
                onTap: () => _openDrawerDestination('/settings'),
              ),
              ListTile(
                leading: const Icon(Icons.feedback),
                title: const Text('Feedback'),
                onTap: () => _openDrawerDestination('/feedback'),
              ),
            ],
          ),
        ),
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: _screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: _onBottomNavTapped,
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.people_alt),
            label: 'Colleagues',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.newspaper),
            label: 'News',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.bar_chart),
            label: 'Statistics',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          Navigator.of(context).pushNamed(
            '/courseActivities',
            arguments: {
              'courseId': SampleData.courses.first.id,
              'highlightActivityId': SampleData.courses.first.modules.first.activities.first.id,
            },
          );
        },
        icon: const Icon(Icons.list),
        label: const Text('Activities'),
      ),
    );
  }
}
