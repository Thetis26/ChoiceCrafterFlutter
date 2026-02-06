import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class RecommendationWebViewScreen extends StatelessWidget {
  const RecommendationWebViewScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final arguments = ModalRoute.of(context)?.settings.arguments;
    final courseId = arguments is Map ? arguments['courseId'] as String? : null;
    final moduleId = arguments is Map ? arguments['moduleId'] as String? : null;
    final activityId =
        arguments is Map ? arguments['activityId'] as String? : null;
    final recommendationId =
        arguments is Map ? arguments['recommendationId'] as String? : null;

    return FutureBuilder<_RecommendationData?>(
      future: _loadRecommendation(
        courseId,
        moduleId,
        activityId,
        recommendationId,
      ),
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          debugPrint(
            '[RecommendationWebViewScreen] failed to load recommendation: ${snapshot.error}',
          );
          return const Scaffold(
            body: Center(child: Text('Unable to load recommendation.')),
          );
        }
        if (!snapshot.hasData) {
          debugPrint(
            '[RecommendationWebViewScreen] loading recommendation data',
          );
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }

        final recommendation = snapshot.data;
        if (recommendation == null) {
          debugPrint('[RecommendationWebViewScreen] no recommendation available');
          return const Scaffold(
            body: Center(child: Text('No recommendation available.')),
          );
        }
        debugPrint(
          '[RecommendationWebViewScreen] display recommendation id=${recommendation.id} title=${recommendation.title}',
        );

        return Scaffold(
          appBar: AppBar(
            title: Text(recommendation.title),
          ),
          body: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  recommendation.summary,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: 12),
                const Text(
                  'Open the link in your browser to mirror the Android web view.',
                ),
                const SizedBox(height: 8),
                SelectableText(recommendation.url),
                const SizedBox(height: 16),
                ElevatedButton.icon(
                  onPressed: () async {
                    await Clipboard.setData(
                      ClipboardData(text: recommendation.url),
                    );
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Link copied to clipboard.')),
                    );
                  },
                  icon: const Icon(Icons.copy),
                  label: const Text('Copy link'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
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

Future<_RecommendationData?> _loadRecommendation(
  String? courseId,
  String? moduleId,
  String? activityId,
  String? recommendationId,
) async {
  debugPrint(
    '[RecommendationWebViewScreen] loadRecommendation courseId=$courseId moduleId=$moduleId activityId=$activityId recommendationId=$recommendationId',
  );
  final firestore = FirebaseFirestore.instance;
  final courseDoc = await _loadCourseDocument(firestore, courseId);
  if (courseDoc == null) {
    debugPrint('[RecommendationWebViewScreen] course document not found');
    return null;
  }
  final data = courseDoc.data();
  if (data == null) {
    debugPrint('[RecommendationWebViewScreen] course document has no data');
    return null;
  }
  final modules = _listFromData(data['modules']);
  debugPrint('[RecommendationWebViewScreen] mapped modules=${modules.length}');
  final moduleMap = _findById(modules, moduleId);
  final activities = _listFromData(moduleMap?['activities']);
  debugPrint(
    '[RecommendationWebViewScreen] mapped activities=${activities.length}',
  );
  final activityMap = _findById(activities, activityId);
  final recommendations =
      _recommendationsFromData(activityMap?['recommendations']);
  debugPrint(
    '[RecommendationWebViewScreen] mapped recommendations=${recommendations.length}',
  );
  if (recommendations.isEmpty) {
    return null;
  }
  if (recommendationId != null && recommendationId.trim().isNotEmpty) {
    for (final rec in recommendations) {
      if (rec.id == recommendationId) {
        debugPrint(
          '[RecommendationWebViewScreen] found requested recommendation id=${rec.id}',
        );
        return rec;
      }
    }
  }
  debugPrint(
    '[RecommendationWebViewScreen] using first recommendation id=${recommendations.first.id}',
  );
  return recommendations.first;
}

Future<DocumentSnapshot<Map<String, dynamic>>?> _loadCourseDocument(
  FirebaseFirestore firestore,
  String? courseId,
) async {
  const primaryCollection = 'COURSES';
  const legacyCollection = 'courses';

  if (courseId != null && courseId.trim().isNotEmpty) {
    debugPrint(
      '[RecommendationWebViewScreen] fetching course document for courseId=$courseId',
    );
    final primaryDoc =
        await firestore.collection(primaryCollection).doc(courseId).get();
    if (primaryDoc.exists) {
      debugPrint('[RecommendationWebViewScreen] found primary course document');
      return primaryDoc;
    }
    final legacyDoc =
        await firestore.collection(legacyCollection).doc(courseId).get();
    if (legacyDoc.exists) {
      debugPrint('[RecommendationWebViewScreen] found legacy course document');
      return legacyDoc;
    }
  }

  debugPrint('[RecommendationWebViewScreen] fetching fallback course document');
  final primarySnapshot =
      await firestore.collection(primaryCollection).limit(1).get();
  if (primarySnapshot.docs.isNotEmpty) {
    debugPrint(
      '[RecommendationWebViewScreen] found fallback primary document',
    );
    return primarySnapshot.docs.first;
  }

  final legacySnapshot =
      await firestore.collection(legacyCollection).limit(1).get();
  if (legacySnapshot.docs.isNotEmpty) {
    debugPrint('[RecommendationWebViewScreen] found fallback legacy document');
    return legacySnapshot.docs.first;
  }

  debugPrint('[RecommendationWebViewScreen] no course documents available');
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
