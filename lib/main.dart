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
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  AuthRepository? _authRepository;
  late final Future<FirebaseApp> _firebaseInit;
  CourseRepository? _courseRepository;
  UserActivityRepository? _userActivityRepository;
  User? _currentUser;

  @override
  void initState() {
    super.initState();
    _firebaseInit = Firebase.initializeApp().then((app) async {
      _authRepository = AuthRepository();
      _currentUser = await _authRepository!.currentUser();
      return app;
    });
  }

  void _ensureRepositories() {
    _courseRepository ??= CourseRepository();
    _userActivityRepository ??= UserActivityRepository();
  }

  void _handleAuthenticated(User user) {
    setState(() => _currentUser = user);
  }

  Future<void> _handleLogout() async {
    await _authRepository!.logout();
    if (!mounted) {
      return;
    }
    setState(() => _currentUser = null);
  }

  Route<dynamic>? _onGenerateRoute(
    RouteSettings settings, {
    required CourseRepository courseRepository,
  }) {
    switch (settings.name) {
      case '/courseActivities':
        final args = settings.arguments as Map?;
        final courseId = args?['courseId'] as String?;
        final course = args?['course'] as Course?;
        final highlightActivityId = args?['highlightActivityId'] as String?;
        return MaterialPageRoute(
          builder: (_) => CourseActivitiesLoader(
            courseRepository: courseRepository,
            course: course,
            courseId: courseId,
            highlightActivityId: highlightActivityId,
          ),
        );
      case '/activity':
        return MaterialPageRoute(builder: (_) => const ActivityScreen());
      case '/module':
        return MaterialPageRoute(
          builder: (_) => ModuleScreen(courseRepository: courseRepository),
        );
      default:
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<FirebaseApp>(
      future: _firebaseInit,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return MaterialApp(
            title: 'ChoiceCrafter Students',
            theme: ThemeData(
              colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
              useMaterial3: true,
            ),
            home: const Scaffold(
              body: Center(child: CircularProgressIndicator()),
            ),
          );
        }

        if (snapshot.hasError) {
          return MaterialApp(
            title: 'ChoiceCrafter Students',
            theme: ThemeData(
              colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
              useMaterial3: true,
            ),
            home: const Scaffold(
              body: Center(
                child: Text(
                  'Firebase failed to initialize. Check your configuration and try again.',
                ),
              ),
            ),
          );
        }

        _ensureRepositories();
        final courseRepository = _courseRepository!;
        final userActivityRepository = _userActivityRepository!;
        final authRepository = _authRepository!;

        return MaterialApp(
          title: 'ChoiceCrafter Students',
          theme: ThemeData(
            colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
            useMaterial3: true,
          ),
          home: _currentUser == null
              ? LoginScreen(
                  authRepository: authRepository,
                  onAuthenticated: _handleAuthenticated,
                )
              : AuthenticatedShell(
                  user: _currentUser!,
                  courseRepository: courseRepository,
                  userActivityRepository: userActivityRepository,
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
          onGenerateRoute: (settings) =>
              _onGenerateRoute(settings, courseRepository: courseRepository),
        );
      },
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
  final Future<void> Function() onLogout;

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
            onPressed: () async {
              await widget.onLogout();
            },
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
                currentAccountPicture: CircleAvatar(
                  child: Text(widget.user.fullName.substring(0, 1)),
                ),
              ),
              ListTile(
                leading: const Icon(Icons.message_outlined),
                title: const Text('Messages'),
                onTap: () => Navigator.of(context).pushNamed('/messages'),
              ),
              ListTile(
                leading: const Icon(Icons.inbox_outlined),
                title: const Text('Inbox'),
                onTap: () => Navigator.of(context).pushNamed('/inbox'),
              ),
              ListTile(
                leading: const Icon(Icons.settings),
                title: const Text('Settings'),
                onTap: () => Navigator.of(context).pushNamed('/settings'),
              ),
              ListTile(
                leading: const Icon(Icons.feedback_outlined),
                title: const Text('Feedback'),
                onTap: () => Navigator.of(context).pushNamed('/feedback'),
              ),
            ],
          ),
        ),
      ),
      body: _buildBody(),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: _onBottomNavTapped,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.people_alt_outlined),
            label: 'Colleagues activity',
          ),
          NavigationDestination(
            icon: Icon(Icons.campaign_outlined),
            label: 'News',
          ),
          NavigationDestination(
            icon: Icon(Icons.bar_chart_outlined),
            label: 'Personal statistics',
          ),
        ],
      ),
    );
  }
}
