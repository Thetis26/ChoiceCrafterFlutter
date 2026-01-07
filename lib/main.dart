import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';

import 'activity_screen.dart';
import 'course_activities_screen.dart';
import 'models/course.dart';
import 'models/user.dart';
import 'repositories/auth_repository.dart';
import 'repositories/course_repository.dart';
import 'repositories/user_activity_repository.dart';
import 'screens/auth/login_screen.dart';
import 'screens/colleagues_activity_screen.dart';
import 'screens/feedback_screen.dart';
import 'screens/home_screen.dart';
import 'screens/inbox_screen.dart';
import 'screens/learning_path_screen.dart';
import 'screens/messages_screen.dart';
import 'screens/module_screen.dart';
import 'screens/news_screen.dart';
import 'screens/personal_activity_screen.dart';
import 'screens/recommendation_webview_screen.dart';
import 'screens/settings_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final AuthRepository _authRepository = AuthRepository();
  final CourseRepository _courseRepository = CourseRepository();
  final UserActivityRepository _userActivityRepository = UserActivityRepository();
  User? _currentUser;

  void _handleAuthenticated(User user) {
    setState(() => _currentUser = user);
  }

  void _handleLogout() {
    setState(() => _currentUser = null);
  }

  Route<dynamic>? _onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case '/courseActivities':
        final args = settings.arguments as Map?;
        final courseId = args?['courseId'] as String?;
        final course = args?['course'] as Course?;
        final highlightActivityId = args?['highlightActivityId'] as String?;
        return MaterialPageRoute(
          builder: (_) => CourseActivitiesLoader(
            courseRepository: _courseRepository,
            course: course,
            courseId: courseId,
            highlightActivityId: highlightActivityId,
          ),
        );
      case '/activity':
        return MaterialPageRoute(builder: (_) => const ActivityScreen());
      case '/module':
        return MaterialPageRoute(
          builder: (_) => ModuleScreen(courseRepository: _courseRepository),
        );
      default:
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ChoiceCrafter Students',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: _currentUser == null
          ? LoginScreen(
              authRepository: _authRepository,
              onAuthenticated: _handleAuthenticated,
            )
          : AuthenticatedShell(
              user: _currentUser!,
              courseRepository: _courseRepository,
              userActivityRepository: _userActivityRepository,
              onLogout: _handleLogout,
            ),
      routes: {
        '/learningPath': (context) => const LearningPathScreen(),
        '/recommendation': (context) => const RecommendationWebViewScreen(),
        '/inbox': (context) => const InboxScreen(),
        '/messages': (context) => const MessagesScreen(),
        '/settings': (context) => const SettingsScreen(),
        '/feedback': (context) => const FeedbackScreen(),
      },
      onGenerateRoute: _onGenerateRoute,
    );
  }
}

class AuthenticatedShell extends StatefulWidget {
  const AuthenticatedShell({
    super.key,
    required this.user,
    required this.courseRepository,
    required this.userActivityRepository,
    required this.onLogout,
  });

  final User user;
  final CourseRepository courseRepository;
  final UserActivityRepository userActivityRepository;
  final VoidCallback onLogout;

  @override
  State<AuthenticatedShell> createState() => _AuthenticatedShellState();
}

class _AuthenticatedShellState extends State<AuthenticatedShell> {
  int _selectedIndex = 0;

  void _onBottomNavTapped(int index) {
    setState(() => _selectedIndex = index);
  }

  void _openCourse(Course course) {
    final highlightActivityId = course.modules.isNotEmpty &&
            course.modules.first.activities.isNotEmpty
        ? course.modules.first.activities.first.id
        : null;
    Navigator.of(context).pushNamed(
      '/courseActivities',
      arguments: {
        'course': course,
        'courseId': course.id,
        'highlightActivityId': highlightActivityId,
      },
    );
  }

  Widget _buildBody() {
    switch (_selectedIndex) {
      case 0:
        return HomeScreen(
          user: widget.user,
          courseRepository: widget.courseRepository,
          onCourseSelected: _openCourse,
        );
      case 1:
        return ColleaguesActivityScreen(courseRepository: widget.courseRepository);
      case 2:
        return NewsScreen(
          courseRepository: widget.courseRepository,
          onCourseSelected: _openCourse,
        );
      case 3:
        return PersonalActivityScreen(
          user: widget.user,
          userActivityRepository: widget.userActivityRepository,
        );
      default:
        return const SizedBox.shrink();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ChoiceCrafter'),
        actions: [
          IconButton(
            onPressed: widget.onLogout,
            icon: const Icon(Icons.logout),
            tooltip: 'Sign out',
          ),
        ],
      ),
      drawer: Drawer(
        child: SafeArea(
          child: ListView(
            children: [
              UserAccountsDrawerHeader(
                accountName: Text(widget.user.fullName),
                accountEmail: Text(widget.user.email),
              ),
              ListTile(
                leading: const Icon(Icons.inbox),
                title: const Text('Inbox'),
                onTap: () => Navigator.of(context).pushNamed('/inbox'),
              ),
              ListTile(
                leading: const Icon(Icons.chat),
                title: const Text('Messages'),
                onTap: () => Navigator.of(context).pushNamed('/messages'),
              ),
              ListTile(
                leading: const Icon(Icons.settings),
                title: const Text('Settings'),
                onTap: () => Navigator.of(context).pushNamed('/settings'),
              ),
              ListTile(
                leading: const Icon(Icons.feedback),
                title: const Text('Feedback'),
                onTap: () => Navigator.of(context).pushNamed('/feedback'),
              ),
            ],
          ),
        ),
      ),
      body: _buildBody(),
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
            icon: Icon(Icons.person),
            label: 'Personal',
          ),
        ],
      ),
      floatingActionButton: FutureBuilder<List<Course>>(
        future: widget.courseRepository.getEnrolledCourses(widget.user),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const SizedBox.shrink();
          }
          if (snapshot.hasError) {
            return const SizedBox.shrink();
          }
          final courses = snapshot.data ?? [];
          if (courses.isEmpty) {
            return const SizedBox.shrink();
          }
          final course = courses.first;
          final highlightActivityId = course.modules.isNotEmpty &&
                  course.modules.first.activities.isNotEmpty
              ? course.modules.first.activities.first.id
              : null;
          return FloatingActionButton.extended(
            onPressed: () {
              Navigator.of(context).pushNamed(
                '/courseActivities',
                arguments: {
                  'course': course,
                  'courseId': course.id,
                  'highlightActivityId': highlightActivityId,
                },
              );
            },
            icon: const Icon(Icons.list),
            label: const Text('Activities'),
          );
        },
      ),
    );
  }
}
