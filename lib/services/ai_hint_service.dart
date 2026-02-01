import 'dart:convert';
import 'dart:io';

class AiHintService {
  AiHintService({HttpClient? client, String? apiKeyOverride})
      : _client = client ?? HttpClient(),
        _apiKeyOverride = apiKeyOverride;

  static const String _endpoint =
      'https://api.openai.com/v1/chat/completions';
  static const String _model = 'gpt-3.5-turbo';
  static const String _defaultApiKey =
      String.fromEnvironment('OPENAI_API_KEY', defaultValue: '');

  final HttpClient _client;
  final String? _apiKeyOverride;

  Future<String> generateHint(String prompt) async {
    final apiKey = _resolveApiKey();
    if (apiKey.isEmpty) {
      throw AiHintException(
        'Missing OpenAI API key. Add OPENAI_API_KEY to your build settings.',
      );
    }

    final request = await _client.postUrl(Uri.parse(_endpoint));
    request.headers.set('Authorization', 'Bearer $apiKey');
    request.headers.contentType = ContentType.json;
    request.write(jsonEncode({
      'model': _model,
      'messages': [
        {
          'role': 'system',
          'content':
              'You are a helpful tutor. Provide concise hints that guide the learner without revealing the final answer.',
        },
        {
          'role': 'user',
          'content': prompt,
        },
      ],
      'temperature': 0.7,
      'max_tokens': 120,
    }));

    final response = await request.close();
    final responseBody = await response.transform(utf8.decoder).join();
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw AiHintException(_extractErrorMessage(responseBody));
    }

    final decoded = jsonDecode(responseBody) as Map<String, dynamic>;
    final choices = decoded['choices'];
    if (choices is List && choices.isNotEmpty) {
      final firstChoice = choices.first;
      if (firstChoice is Map<String, dynamic>) {
        final message = firstChoice['message'];
        if (message is Map<String, dynamic>) {
          final content = message['content'];
          if (content is String && content.trim().isNotEmpty) {
            return content.trim();
          }
        }
      }
    }

    throw AiHintException(
      'Unable to parse a hint from the OpenAI response.',
    );
  }

  String _resolveApiKey() {
    final overrideKey = _apiKeyOverride?.trim();
    if (overrideKey != null && overrideKey.isNotEmpty) {
      return overrideKey;
    }
    if (_defaultApiKey.trim().isNotEmpty) {
      return _defaultApiKey.trim();
    }
    final envKey = Platform.environment['OPENAI_API_KEY']?.trim();
    return envKey ?? '';
  }

  String _extractErrorMessage(String responseBody) {
    try {
      final decoded = jsonDecode(responseBody) as Map<String, dynamic>;
      final error = decoded['error'];
      if (error is Map<String, dynamic>) {
        final message = error['message'];
        if (message is String && message.trim().isNotEmpty) {
          return message.trim();
        }
      }
    } catch (_) {
      // Ignore parsing errors.
    }
    return 'Unable to fetch a hint right now. Please try again.';
  }
}

class AiHintException implements Exception {
  AiHintException(this.message);

  final String message;

  @override
  String toString() => message;
}
