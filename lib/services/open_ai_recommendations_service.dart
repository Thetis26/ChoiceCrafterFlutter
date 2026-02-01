import 'dart:convert';
import 'dart:io';

import '../models/recommendation.dart';
import '../models/task.dart';

class OpenAiMissingKeyException implements Exception {
  const OpenAiMissingKeyException();
}

class OpenAiRequestException implements Exception {
  const OpenAiRequestException(this.message);

  final String message;
}

class OpenAiRecommendationsService {
  OpenAiRecommendationsService({HttpClient? httpClient})
      : _httpClient = httpClient ?? HttpClient();

  // TODO: Provide your key via --dart-define=OPEN_AI_KEY=your_key_here.
  static const String openAiApiKey =
      String.fromEnvironment('OPEN_AI_KEY', defaultValue: '');
  static const String _endpoint = 'https://api.openai.com/v1/chat/completions';
  static const String _model = 'gpt-3.5-turbo';

  final HttpClient _httpClient;

  Future<List<Recommendation>> generateRecommendations({
    required String activityTitle,
    required String activityDescription,
    required List<Task> tasks,
  }) async {
    final apiKey = openAiApiKey.trim();
    if (apiKey.isEmpty) {
      throw const OpenAiMissingKeyException();
    }

    final payload = _buildPayload(
      activityTitle: activityTitle,
      activityDescription: activityDescription,
      tasks: tasks,
    );
    final request = await _httpClient.postUrl(Uri.parse(_endpoint));
    request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $apiKey');
    request.headers.set(HttpHeaders.contentTypeHeader, 'application/json');
    request.write(jsonEncode(payload));

    final response = await request.close();
    final responseBody = await response.transform(utf8.decoder).join();

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw OpenAiRequestException(_extractErrorMessage(responseBody));
    }

    final content = _extractAssistantContent(responseBody);
    if (content == null || content.trim().isEmpty) {
      throw const OpenAiRequestException(
        'Unable to generate recommendations right now.',
      );
    }

    final parsed = _parseRecommendations(content);
    if (parsed.isEmpty) {
      throw const OpenAiRequestException(
        'The AI response did not include recommendations.',
      );
    }
    return parsed;
  }

  Map<String, dynamic> _buildPayload({
    required String activityTitle,
    required String activityDescription,
    required List<Task> tasks,
  }) {
    return {
      'model': _model,
      'temperature': 0.6,
      'max_tokens': 420,
      'messages': [
        {
          'role': 'system',
          'content': 'You are a helpful tutor. Provide curated learning'
              ' resources that reinforce the activity tasks. Respond only'
              ' with a JSON array of objects (no markdown) using fields:'
              ' title, url, description, type (website|youtube|pdf|doc).'
              ' Keep descriptions short (under 12 words).',
        },
        {
          'role': 'user',
          'content': _buildPrompt(
            activityTitle: activityTitle,
            activityDescription: activityDescription,
            tasks: tasks,
          ),
        },
      ],
    };
  }

  String _buildPrompt({
    required String activityTitle,
    required String activityDescription,
    required List<Task> tasks,
  }) {
    final buffer = StringBuffer()
      ..writeln('Activity: $activityTitle')
      ..writeln('Description: $activityDescription')
      ..writeln('Tasks:');
    for (final task in tasks) {
      buffer.writeln('- ${task.title} (${task.type}): ${task.description}');
    }
    buffer.writeln(
      'Provide 3-4 high-quality resources with real URLs.',
    );
    return buffer.toString();
  }

  String? _extractAssistantContent(String responseBody) {
    try {
      final decoded = jsonDecode(responseBody);
      final choices = decoded['choices'];
      if (choices is! List || choices.isEmpty) {
        return null;
      }
      final message = choices.first['message'];
      if (message is Map<String, dynamic>) {
        return message['content'] as String?;
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  String _extractErrorMessage(String responseBody) {
    try {
      final decoded = jsonDecode(responseBody);
      final error = decoded['error'];
      if (error is Map<String, dynamic>) {
        final message = error['message'];
        if (message is String && message.trim().isNotEmpty) {
          return message;
        }
      }
    } catch (_) {
      // ignore
    }
    return 'Unable to reach OpenAI right now.';
  }

  List<Recommendation> _parseRecommendations(String rawContent) {
    final jsonContent = _extractJsonArray(rawContent);
    if (jsonContent == null) {
      return [];
    }
    try {
      final decoded = jsonDecode(jsonContent);
      if (decoded is! List) {
        return [];
      }
      return decoded
          .whereType<Map>()
          .map((entry) => Recommendation.fromMap(
                Map<String, dynamic>.from(entry),
              ))
          .toList();
    } catch (_) {
      return [];
    }
  }

  String? _extractJsonArray(String content) {
    final fenced = RegExp(r'```(?:json)?(.*?)```', dotAll: true);
    final match = fenced.firstMatch(content);
    final candidate = match != null ? match.group(1) : content;
    if (candidate == null) {
      return null;
    }
    final start = candidate.indexOf('[');
    final end = candidate.lastIndexOf(']');
    if (start == -1 || end == -1 || end <= start) {
      return null;
    }
    return candidate.substring(start, end + 1);
  }
}
