import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class LearningPathScreen extends StatelessWidget {
  const LearningPathScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    final courseId = arguments is Map ? arguments['courseId'] as String? : null;
    final moduleId = arguments is Map ? arguments['moduleId'] as String? : null;
    final activityId =
        arguments is Map ? arguments['activityId'] as String? : null;

    return FutureBuilder<_LearningPathData?>(
      future: _loadLearningPath(courseId, moduleId, activityId),
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          debugPrint(
            '[LearningPathScreen] failed to load learning path: ${snapshot.error}',
          );
          return const Scaffold(
            body: Center(child: Text('Unable to load learning path.')),
          );
        }
        if (!snapshot.hasData) {
          debugPrint('[LearningPathScreen] loading learning path data');
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }

        final learningPath = snapshot.data;
        if (learningPath == null) {
          debugPrint('[LearningPathScreen] no learning path available');
          return const Scaffold(
            body: Center(child: Text('No learning path available.')),
          );
        }

        final recommendation = learningPath.recommendation;
        debugPrint(
          '[LearningPathScreen] display learning path title=${learningPath.title} topics=${learningPath.topics.length} recommendation=${recommendation?.id ?? 'none'}',
        );

        return Scaffold(
          appBar: AppBar(
            title: Text(learningPath.title),
          ),
          body: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  learningPath.description,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  children: learningPath.topics
                      .map((topic) => Chip(label: Text(topic)))
                      .toList(),
                ),
                const SizedBox(height: 16),
                if (recommendation == null)
                  const Text('No recommendations available.')
                else
                  Card(
                    child: ListTile(
                      title: Text(recommendation.title),
                      subtitle: Text(recommendation.summary),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => Navigator.of(context).pushNamed(
                        '/recommendation',
                        arguments: {
                          'courseId': learningPath.courseId,
                          'moduleId': learningPath.moduleId,
                          'activityId': learningPath.activityId,
                          'recommendationId': recommendation.id,
                        },
                      ),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _LearningPathData {
  const _LearningPathData({
    required this.courseId,
    required this.moduleId,
    required this.activityId,
    required this.title,
    required this.description,
    required this.topics,
    required this.recommendation,
  });

  final String courseId;
  final String moduleId;
  final String activityId;
  final String title;
  final String description;
  final List<String> topics;
  final _RecommendationData? recommendation;
}

class _RecommendationData {
  const _RecommendationData({
    required this.id,
    required this.title,
    required this.url,
    required this.summary,
  });

  final String id;
  final String title;
  final String url;
  final String summary;
}

Future<_LearningPathData?> _loadLearningPath(
  String? courseId,
  String? moduleId,
  String? activityId,
) async {
  debugPrint(
    '[LearningPathScreen] loadLearningPath courseId=$courseId moduleId=$moduleId activityId=$activityId',
  );
  final firestore = FirebaseFirestore.instance;
  final courseDoc = await _loadCourseDocument(firestore, courseId);
  if (courseDoc == null) {
    debugPrint('[LearningPathScreen] course document not found');
    return null;
  }

  final courseData = courseDoc.data();
  if (courseData == null) {
    debugPrint('[LearningPathScreen] course document has no data');
    return null;
  }

  final modulesData = _listFromData(courseData['modules']);
  debugPrint('[LearningPathScreen] mapped modules=${modulesData.length}');
  final moduleMap = _findById(modulesData, moduleId);
  final moduleIdValue = (moduleMap?['id'] as String?) ?? '';
  final moduleTitle = (moduleMap?['title'] as String?)?.trim() ?? '';
  final activitiesData = _listFromData(moduleMap?['activities']);
  debugPrint('[LearningPathScreen] mapped activities=${activitiesData.length}');
  final activityMap = _findById(activitiesData, activityId);
  final activityIdValue = (activityMap?['id'] as String?) ?? '';
  final topics =
      _stringListFromData(moduleMap?['topics']).isNotEmpty
          ? _stringListFromData(moduleMap?['topics'])
          : _stringListFromData(
              activitiesData
                  .map((activity) => (activity['title'] as String?) ?? '')
                  .where((title) => title.trim().isNotEmpty)
                  .toList(),
            );

  final recommendations =
      _recommendationsFromData(activityMap?['recommendations']);
  final recommendation = recommendations.isNotEmpty
      ? recommendations.first
      : null;
  debugPrint(
    '[LearningPathScreen] mapped recommendations=${recommendations.length} selected=${recommendation?.id ?? 'none'}',
  );

  return _LearningPathData(
    courseId: courseDoc.id,
    moduleId: moduleIdValue.isNotEmpty ? moduleIdValue : moduleTitle,
    activityId: activityIdValue,
    title: (courseData['title'] as String?)?.trim() ?? 'Learning path',
    description:
        (courseData['summary'] as String?)?.trim() ?? 'No summary available.',
    topics: topics.isNotEmpty ? topics : ['No topics available'],
    recommendation: recommendation,
  );
}

Future<DocumentSnapshot<Map<String, dynamic>>?> _loadCourseDocument(
  FirebaseFirestore firestore,
  String? courseId,
) async {
  const primaryCollection = 'COURSES';
  const legacyCollection = 'courses';

  if (courseId != null && courseId.trim().isNotEmpty) {
    debugPrint(
      '[LearningPathScreen] fetching course document for courseId=$courseId',
    );
    final primaryDoc =
        await firestore.collection(primaryCollection).doc(courseId).get();
    if (primaryDoc.exists) {
      debugPrint('[LearningPathScreen] found primary course document');
      return primaryDoc;
    }
    final legacyDoc =
        await firestore.collection(legacyCollection).doc(courseId).get();
    if (legacyDoc.exists) {
      debugPrint('[LearningPathScreen] found legacy course document');
      return legacyDoc;
    }
  }

  debugPrint('[LearningPathScreen] fetching fallback course document');
  final primarySnapshot =
      await firestore.collection(primaryCollection).limit(1).get();
  if (primarySnapshot.docs.isNotEmpty) {
    debugPrint('[LearningPathScreen] found fallback primary document');
    return primarySnapshot.docs.first;
  }

  final legacySnapshot =
      await firestore.collection(legacyCollection).limit(1).get();
  if (legacySnapshot.docs.isNotEmpty) {
    debugPrint('[LearningPathScreen] found fallback legacy document');
    return legacySnapshot.docs.first;
  }

  debugPrint('[LearningPathScreen] no course documents available');
  return null;
}

List<Map<String, dynamic>> _listFromData(dynamic value) {
  if (value is! List) {
    return [];
  }
  return value
      .whereType<Map>()
      .map((item) => Map<String, dynamic>.from(item))
      .toList();
}

Map<String, dynamic>? _findById(
  List<Map<String, dynamic>> items,
  String? id,
) {
  if (items.isEmpty) {
    return null;
  }
  if (id != null && id.trim().isNotEmpty) {
    for (final item in items) {
      if ((item['id'] as String?) == id) {
        return item;
      }
    }
  }
  return items.first;
}

List<String> _stringListFromData(dynamic value) {
  if (value is List) {
    return value.map((item) => item.toString()).toList();
  }
  return [];
}

List<_RecommendationData> _recommendationsFromData(dynamic value) {
  if (value is! List) {
    return [];
  }
  return value.whereType<Map>().map((item) {
    final map = Map<String, dynamic>.from(item);
    return _RecommendationData(
      id: (map['id'] as String?) ?? '',
      title: (map['title'] as String?) ?? 'Recommendation',
      url: (map['url'] as String?) ?? '',
      summary: (map['summary'] as String?) ?? '',
    );
  }).toList();
}
