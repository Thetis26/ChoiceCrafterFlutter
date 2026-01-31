import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'activity_screen.dart';
import 'course_activities_screen.dart';
import 'models/course.dart';
import 'models/user.dart';
import 'repositories/auth_repository.dart';
import 'repositories/course_repository.dart';
import 'repositories/personal_statistics_repository.dart';
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
  PersonalStatisticsRepository? _personalStatisticsRepository;
  User? _currentUser;
  ThemeMode _themeMode = ThemeMode.light;
  Locale _locale = const Locale('en');
  String _languageLabel = 'English';

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
    _personalStatisticsRepository ??= PersonalStatisticsRepository();
  }

  void _handleAuthenticated(User user) {
    setState(() => _currentUser = user);
  }

  void _handleThemeModeChanged(ThemeMode themeMode) {
    setState(() => _themeMode = themeMode);
  }

  void _handleLanguageChanged(String language) {
    final locale = language == 'Romanian' ? const Locale('ro') : const Locale('en');
    setState(() {
      _languageLabel = language;
      _locale = locale;
    });
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
          settings: settings,
          builder: (_) => CourseActivitiesLoader(
            courseRepository: courseRepository,
            course: course,
            courseId: courseId,
            highlightActivityId: highlightActivityId,
          ),
        );
      case '/activity':
        return MaterialPageRoute(
          settings: settings,
          builder: (_) => const ActivityScreen(),
        );
      case '/module':
        return MaterialPageRoute(
          settings: settings,
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
            themeMode: _themeMode,
            theme: ThemeData(
              colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
              useMaterial3: true,
            ),
            darkTheme: ThemeData(
              colorScheme: ColorScheme.fromSeed(
                seedColor: Colors.deepPurple,
                brightness: Brightness.dark,
              ),
              useMaterial3: true,
            ),
          locale: _locale,
          supportedLocales: const [
            Locale('en'),
            Locale('ro'),
          ],
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          home: const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          ),
        );
        }

        if (snapshot.hasError) {
          return MaterialApp(
            title: 'ChoiceCrafter Students',
            themeMode: _themeMode,
            theme: ThemeData(
              colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
              useMaterial3: true,
            ),
            darkTheme: ThemeData(
              colorScheme: ColorScheme.fromSeed(
                seedColor: Colors.deepPurple,
                brightness: Brightness.dark,
              ),
              useMaterial3: true,
            ),
          locale: _locale,
          supportedLocales: const [
            Locale('en'),
            Locale('ro'),
          ],
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
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
        final personalStatisticsRepository = _personalStatisticsRepository!;
        final authRepository = _authRepository!;

        return MaterialApp(
          title: 'ChoiceCrafter Students',
          themeMode: _themeMode,
          theme: ThemeData(
            colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
            useMaterial3: true,
          ),
          darkTheme: ThemeData(
            colorScheme: ColorScheme.fromSeed(
              seedColor: Colors.deepPurple,
              brightness: Brightness.dark,
            ),
            useMaterial3: true,
          ),
          locale: _locale,
          supportedLocales: const [
            Locale('en'),
            Locale('ro'),
          ],
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          home: _currentUser == null
              ? LoginScreen(
                  authRepository: authRepository,
                  onAuthenticated: _handleAuthenticated,
                )
              : AuthenticatedShell(
                  user: _currentUser!,
                  courseRepository: courseRepository,
                  personalStatisticsRepository: personalStatisticsRepository,
                  onLogout: _handleLogout,
                ),
          routes: {
            '/learningPath': (context) => const LearningPathScreen(),
            '/recommendation': (context) => const RecommendationWebViewScreen(),
            '/inbox': (context) => const InboxScreen(),
            '/messages': (context) => const MessagesScreen(),
            '/settings': (context) => SettingsScreen(
                  language: _languageLabel,
                  themeMode: _themeMode,
                  onLanguageChanged: _handleLanguageChanged,
                  onThemeModeChanged: _handleThemeModeChanged,
                ),
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
    required this.personalStatisticsRepository,
    required this.onLogout,
  });

  final User user;
  final CourseRepository courseRepository;
  final PersonalStatisticsRepository personalStatisticsRepository;
  final Future<void> Function() onLogout;

  @override
  State<AuthenticatedShell> createState() => _AuthenticatedShellState();
}

class _AuthenticatedShellState extends State<AuthenticatedShell> {
  int _selectedIndex = 0;
  String? _highlightedCourseId;
  Timer? _highlightTimer;

  void _onBottomNavTapped(int index) {
    setState(() => _selectedIndex = index);
  }

  void _handleCourseEnrolled(Course course) {
    _highlightTimer?.cancel();
    setState(() {
      _selectedIndex = 0;
      _highlightedCourseId = course.id;
    });
    _highlightTimer = Timer(const Duration(seconds: 5), () {
      if (!mounted) {
        return;
      }
      setState(() => _highlightedCourseId = null);
    });
  }

  @override
  void dispose() {
    _highlightTimer?.cancel();
    super.dispose();
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
          highlightCourseId: _highlightedCourseId,
        );
      case 1:
        return ColleaguesActivityScreen(courseRepository: widget.courseRepository);
      case 2:
        return NewsScreen(
          user: widget.user,
          courseRepository: widget.courseRepository,
          onCourseEnrolled: _handleCourseEnrolled,
        );
      case 3:
        return PersonalActivityScreen(
          user: widget.user,
          courseRepository: widget.courseRepository,
          personalStatisticsRepository: widget.personalStatisticsRepository,
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
              DrawerHeader(
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primaryContainer,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Builder(
                      builder: (context) {
                        final trimmedName = widget.user.fullName.trim();
                        final avatarName = trimmedName.isEmpty
                            ? '?'
                            : trimmedName
                                .split(RegExp(r'\\s+'))
                                .where((part) => part.isNotEmpty)
                                .take(2)
                                .map((part) => part[0].toUpperCase())
                                .join();
                        return CircleAvatar(
                          radius: 28,
                          child: Text(
                            avatarName,
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                        );
                      },
                    ),
                    const SizedBox(height: 12),
                    Builder(
                      builder: (context) {
                        final trimmedName = widget.user.fullName.trim();
                        final avatarName = trimmedName.isEmpty
                            ? '?'
                            : trimmedName
                                .split(RegExp(r'\\s+'))
                                .where((part) => part.isNotEmpty)
                                .take(2)
                                .map((part) => part[0].toUpperCase())
                                .join();
                        return Row(
                          children: [
                            Expanded(
                              child: Text(
                                widget.user.fullName,
                                style: Theme.of(context).textTheme.titleMedium,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Text(
                              avatarName,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ],
                        );
                      },
                    ),
                    const SizedBox(height: 4),
                    Text(
                      widget.user.email,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
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
            label: 'Peers\' activity',
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
