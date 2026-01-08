import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../models/activity.dart';
import '../models/course.dart';
import '../repositories/course_repository.dart';

class ColleaguesActivityScreen extends StatefulWidget {
  const ColleaguesActivityScreen({
    super.key,
    required this.courseRepository,
  });

  final CourseRepository courseRepository;

  @override
  State<ColleaguesActivityScreen> createState() =>
      _ColleaguesActivityScreenState();
}

class _ColleaguesActivityScreenState extends State<ColleaguesActivityScreen> {
  static const int _maxDisplayedActivities = 20;
  static const String _enrollmentsCollection = 'COURSE_ENROLLMENTS';

  late final Future<List<Course>> _coursesFuture;
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final firebase_auth.FirebaseAuth _auth = firebase_auth.FirebaseAuth.instance;

  @override
  void initState() {
    super.initState();
    _coursesFuture = widget.courseRepository.getAllCourses();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Course>>(
      future: _coursesFuture,
      builder: (context, courseSnapshot) {
        if (courseSnapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (courseSnapshot.hasError) {
          return const Center(child: Text('Unable to load activity feed.'));
        }
        final courses = courseSnapshot.data ?? [];
        if (courses.isEmpty) {
          return const Center(child: Text('No activities yet.'));
        }

        final activityLookup = _buildActivityLookup(courses);
        final defaultCourseId = courses.first.id;
        return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
          stream: _firestore.collection('users').snapshots(),
          builder: (context, usersSnapshot) {
            if (usersSnapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (usersSnapshot.hasError) {
              return const Center(child: Text('Unable to load activity feed.'));
            }

            final users = _parseUsers(usersSnapshot.data);
            final usersByEmail = {
              for (final user in users)
                if (user.email.isNotEmpty) user.email: user,
              for (final user in users)
                if (user.email.isNotEmpty)
                  user.email.toLowerCase(): user,
            };
            users.sort((a, b) => b.totalScore.compareTo(a.totalScore));
            final rankedUsers = [
              for (final entry in users.asMap().entries)
                entry.value.withRank(entry.key + 1),
            ];

            final currentEmail = _auth.currentUser?.email ?? '';
            final currentUserIndex = rankedUsers.indexWhere(
              (user) => currentEmail.isNotEmpty && user.email == currentEmail,
            );
            final currentUser =
                currentUserIndex >= 0 ? rankedUsers[currentUserIndex] : null;

            return StreamBuilder<QuerySnapshot<Map<String, dynamic>>>(
              stream:
                  _firestore.collection(_enrollmentsCollection).snapshots(),
              builder: (context, enrollmentsSnapshot) {
                if (enrollmentsSnapshot.connectionState ==
                    ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (enrollmentsSnapshot.hasError) {
                  return const Center(
                    child: Text('Unable to load activity feed.'),
                  );
                }

                final activities = _parseColleagueActivities(
                  enrollmentsSnapshot.data,
                  usersByEmail,
                  activityLookup,
                  currentEmail,
                  defaultCourseId,
                );

                return ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    _LeaderboardCard(
                      users: rankedUsers,
                      currentUser: currentUser,
                    ),
                    const SizedBox(height: 16),
                    _MotivationalPromptCard(
                      prompt: _buildMotivationalPrompt(
                        users: rankedUsers,
                        currentUserIndex: currentUserIndex,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Latest activity',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 12),
                    if (activities.isEmpty)
                      const Text('No colleague activity yet.')
                    else
                      ...activities.map(
                        (activity) => _ColleagueActivityCard(
                          activity: activity,
                          onTap: activity.activity != null
                              ? () => Navigator.of(context).pushNamed(
                                    '/activity',
                                    arguments: {
                                      'activity': activity.activity,
                                      'courseId': activity.courseId,
                                    },
                                  )
                              : null,
                        ),
                      ),
                  ],
                );
              },
            );
          },
        );
      },
    );
  }

  Map<String, Activity> _buildActivityLookup(List<Course> courses) {
    final lookup = <String, Activity>{};
    for (final course in courses) {
      for (final module in course.modules) {
        for (final activity in module.activities) {
          lookup[activity.id] = activity;
          lookup[activity.name.toLowerCase()] = activity;
        }
      }
    }
    return lookup;
  }

  List<_ColleagueUser> _parseUsers(
    QuerySnapshot<Map<String, dynamic>>? snapshot,
  ) {
    if (snapshot == null) {
      return [];
    }
    return snapshot.docs.map((doc) {
      final data = doc.data();
      final email = (data['email'] as String?)?.trim() ?? '';
      final name = (data['name'] as String?) ??
          (data['fullName'] as String?) ??
          email;
      final anonymousAvatar = data['anonymousAvatar'];
      final avatarName = anonymousAvatar is Map
          ? anonymousAvatar['name'] as String?
          : null;
      final avatarUrl = anonymousAvatar is Map
          ? anonymousAvatar['imageUrl'] as String?
          : null;
      final totalScore = _extractTotalScore(data);
      return _ColleagueUser(
        name: name?.trim().isNotEmpty == true ? name! : 'Anonymous learner',
        email: email,
        avatarName: avatarName,
        avatarUrl: avatarUrl,
        totalScore: totalScore,
      );
    }).toList();
  }

  int _extractTotalScore(Map<String, dynamic> data) {
    final directScore = data['totalScore'];
    if (directScore is num) {
      return directScore.round();
    }
    final scores = data['scores'];
    if (scores is Map) {
      var total = 0;
      for (final value in scores.values) {
        if (value is num) {
          total += value.round();
        } else {
          final parsed = int.tryParse(value.toString());
          if (parsed != null) {
            total += parsed;
          }
        }
      }
      return total;
    }
    return 0;
  }

  List<_ColleagueActivity> _parseColleagueActivities(
    QuerySnapshot<Map<String, dynamic>>? snapshot,
    Map<String, _ColleagueUser> usersByEmail,
    Map<String, Activity> activityLookup,
    String currentEmail,
    String defaultCourseId,
  ) {
    if (snapshot == null) {
      return [];
    }
    final results = <_ColleagueActivity>[];
    for (final doc in snapshot.docs) {
      final data = doc.data();
      final userId = (data['userId'] as String?)?.trim() ?? '';
      final userKey = userId.toLowerCase();
      if (userId.isEmpty ||
          (currentEmail.isNotEmpty &&
              userKey == currentEmail.toLowerCase())) {
        continue;
      }

      final progressSummary = data['progressSummary'];
      if (progressSummary is! Map) {
        continue;
      }
      final snapshots = progressSummary['activitySnapshots'];
      if (snapshots is! List) {
        continue;
      }

      for (final entry in snapshots) {
        if (entry is! Map) {
          continue;
        }
        final snapshotMap = Map<String, dynamic>.from(entry as Map);
        final taskStats = snapshotMap['taskStats'];
        if (taskStats is! Map || taskStats.isEmpty) {
          continue;
        }

        final activityId = _extractActivityIdentifier(snapshotMap);
        if (activityId == null || activityId.isEmpty) {
          continue;
        }
        final activity =
            activityLookup[activityId] ?? activityLookup[activityId.toLowerCase()];
        final courseId = (snapshotMap['courseId'] as String?) ??
            (data['courseId'] as String?) ??
            defaultCourseId;
        final latestAttempt = _resolveLatestAttempt(taskStats);
        final user = usersByEmail[userId] ??
            usersByEmail[userKey] ??
            _ColleagueUser(
              name: userId,
              email: userId,
              avatarName: null,
              avatarUrl: null,
              totalScore: 0,
            );
        results.add(
          _ColleagueActivity(
            user: user,
            activityId: activityId,
            activity: activity,
            courseId: courseId,
            activityTitle: activity?.name ?? activityId,
            activityDescription:
                activity?.description ?? 'Recently worked on this activity.',
            timestamp: latestAttempt ?? DateTime.now(),
          ),
        );
      }
    }

    results.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    if (results.length > _maxDisplayedActivities) {
      return results.sublist(0, _maxDisplayedActivities);
    }
    return results;
  }

  String? _extractActivityIdentifier(Map<String, dynamic> snapshotMap) {
    final values = [
      snapshotMap['activityId'],
      snapshotMap['activityTitle'],
      snapshotMap['activityName'],
    ];
    for (final value in values) {
      if (value is String && value.trim().isNotEmpty) {
        return value.trim();
      }
    }
    return null;
  }

  DateTime? _resolveLatestAttempt(Map<dynamic, dynamic> taskStats) {
    DateTime? latest;
    for (final value in taskStats.values) {
      if (value is Map) {
        final attempt = value['attemptDateTime'] ?? value['attempt_time'];
        DateTime? parsed;
        if (attempt is Timestamp) {
          parsed = attempt.toDate();
        } else if (attempt is String) {
          parsed = DateTime.tryParse(attempt);
        }
        if (parsed != null) {
          if (latest == null || parsed.isAfter(latest)) {
            latest = parsed;
          }
        }
      }
    }
    return latest;
  }

  String _buildMotivationalPrompt({
    required List<_ColleagueUser> users,
    required int currentUserIndex,
  }) {
    if (users.isEmpty || currentUserIndex < 0) {
      return 'Connect with your peers and celebrate new activity milestones.';
    }
    if (currentUserIndex == 0) {
      final lead = users.length > 1
          ? users[currentUserIndex].totalScore - users[1].totalScore
          : users[currentUserIndex].totalScore;
      return 'You are leading by $lead points. Keep inspiring the group!';
    }
    final user = users[currentUserIndex];
    final ahead = users[currentUserIndex - 1];
    final gap = (ahead.totalScore - user.totalScore).abs();
    return 'Only $gap points to catch ${ahead.displayName}. You are close!';
  }
}

class _LeaderboardCard extends StatelessWidget {
  const _LeaderboardCard({
    required this.users,
    required this.currentUser,
  });

  final List<_ColleagueUser> users;
  final _ColleagueUser? currentUser;

  @override
  Widget build(BuildContext context) {
    final topUsers = users.take(3).toList();
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Top three', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            if (topUsers.isEmpty)
              const Text('Scores will appear once colleagues get started.')
            else
              ...topUsers.asMap().entries.map(
                    (entry) => _LeaderboardRow(
                      rank: entry.key + 1,
                      user: entry.value,
                      isCurrentUser: currentUser != null &&
                          currentUser!.email.isNotEmpty &&
                          entry.value.email == currentUser!.email,
                    ),
                  ),
            const SizedBox(height: 16),
            _CurrentUserScoreCard(user: currentUser, totalPeers: users.length),
          ],
        ),
      ),
    );
  }
}

class _LeaderboardRow extends StatelessWidget {
  const _LeaderboardRow({
    required this.rank,
    required this.user,
    required this.isCurrentUser,
  });

  final int rank;
  final _ColleagueUser user;
  final bool isCurrentUser;

  @override
  Widget build(BuildContext context) {
    final medalIcon = switch (rank) {
      1 => Icons.emoji_events,
      2 => Icons.military_tech,
      _ => Icons.workspace_premium,
    };
    final medalColor = switch (rank) {
      1 => const Color(0xFFFFC107),
      2 => const Color(0xFFB0BEC5),
      _ => const Color(0xFFCD7F32),
    };
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(medalIcon, color: medalColor, size: 28),
          const SizedBox(width: 12),
          CircleAvatar(
            radius: 18,
            backgroundColor: const Color(0xFFE3E7FF),
            child: Text(
              user.initials,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              isCurrentUser ? '${user.displayName} (You)' : user.displayName,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
          ),
          Text(
            '${user.totalScore}',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
        ],
      ),
    );
  }
}

class _CurrentUserScoreCard extends StatelessWidget {
  const _CurrentUserScoreCard({
    required this.user,
    required this.totalPeers,
  });

  final _ColleagueUser? user;
  final int totalPeers;

  @override
  Widget build(BuildContext context) {
    if (user == null) {
      return _ScoreCardContainer(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Your score', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            const Text('Sign in to see your ranking.'),
          ],
        ),
      );
    }
    final rankLabel = user!.rankPosition != null
        ? 'Your rank: #${user!.rankPosition} of $totalPeers peers'
        : 'Your rank is updating';
    return _ScoreCardContainer(
      child: Row(
        children: [
          CircleAvatar(
            radius: 26,
            backgroundColor: Colors.white.withOpacity(0.2),
            child: Text(
              user!.initials,
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Your score',
                  style: TextStyle(color: Colors.white70),
                ),
                const SizedBox(height: 6),
                Text(
                  '${user!.totalScore} points',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  rankLabel,
                  style: const TextStyle(color: Colors.white70),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ScoreCardContainer extends StatelessWidget {
  const _ScoreCardContainer({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF4B5CF0), Color(0xFF56B2FF)],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      padding: const EdgeInsets.all(16),
      child: child,
    );
  }
}

class _MotivationalPromptCard extends StatelessWidget {
  const _MotivationalPromptCard({required this.prompt});

  final String prompt;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.emoji_events, color: Color(0xFFFFC107)),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                prompt,
                style: Theme.of(context).textTheme.bodyLarge,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ColleagueActivityCard extends StatelessWidget {
  const _ColleagueActivityCard({
    required this.activity,
    this.onTap,
  });

  final _ColleagueActivity activity;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: const Color(0xFFE3E7FF),
          child: Text(
            activity.user.initials,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
        title: Text(activity.user.displayName),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              activity.activityTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            Text(activity.activityDescription),
            Text(
              activity.relativeTimestamp,
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: Colors.grey[600]),
            ),
          ],
        ),
        trailing: onTap != null ? const Icon(Icons.chevron_right) : null,
        onTap: onTap,
      ),
    );
  }
}

class _ColleagueUser {
  const _ColleagueUser({
    required this.name,
    required this.email,
    required this.avatarName,
    required this.avatarUrl,
    required this.totalScore,
    this.rankPosition,
  });

  final String name;
  final String email;
  final String? avatarName;
  final String? avatarUrl;
  final int totalScore;
  final int? rankPosition;

  _ColleagueUser withRank(int rank) => _ColleagueUser(
        name: name,
        email: email,
        avatarName: avatarName,
        avatarUrl: avatarUrl,
        totalScore: totalScore,
        rankPosition: rank,
      );

  String get displayName => avatarName?.isNotEmpty == true ? avatarName! : name;

  String get initials {
    final source = displayName.trim().isNotEmpty
        ? displayName.trim()
        : email.trim();
    if (source.isEmpty) {
      return 'C';
    }
    return source.substring(0, 1).toUpperCase();
  }
}

class _ColleagueActivity {
  const _ColleagueActivity({
    required this.user,
    required this.activityId,
    required this.activityTitle,
    required this.activityDescription,
    required this.timestamp,
    required this.courseId,
    this.activity,
  });

  final _ColleagueUser user;
  final String activityId;
  final String activityTitle;
  final String activityDescription;
  final DateTime timestamp;
  final String courseId;
  final Activity? activity;

  String get relativeTimestamp {
    final now = DateTime.now();
    final difference = now.difference(timestamp);
    if (difference.inMinutes < 60) {
      return '${difference.inMinutes} min ago';
    }
    if (difference.inHours < 24) {
      return '${difference.inHours} hours ago';
    }
    if (difference.inDays < 7) {
      return '${difference.inDays} days ago';
    }
    return '${timestamp.month}/${timestamp.day}/${timestamp.year}';
  }
}
