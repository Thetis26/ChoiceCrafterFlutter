import 'dart:convert';
import 'dart:io';

import '../models/task.dart';

class OpenAiMissingKeyException implements Exception {
  const OpenAiMissingKeyException();
}

class OpenAiRequestException implements Exception {
  const OpenAiRequestException(this.message);

  final String message;
}

class OpenAiHintsService {
  OpenAiHintsService({HttpClient? httpClient})
      : _httpClient = httpClient ?? HttpClient();

  static const String openAiApiKey =
      String.fromEnvironment('OPEN_AI_KEY', defaultValue: '');
  static const String _endpoint = 'https://api.openai.com/v1/chat/completions';
  static const String _model = 'gpt-3.5-turbo';

  final HttpClient _httpClient;

  Future<String> generateHint({required Task task}) async {
    final apiKey = openAiApiKey.trim();
    if (apiKey.isEmpty) {
      throw const OpenAiMissingKeyException();
    }

    final payload = _buildPayload(task: task);
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
        'Unable to generate a hint right now.',
      );
    }
    return content.trim();
  }

  Map<String, dynamic> _buildPayload({required Task task}) {
    return {
      'model': _model,
      'temperature': 0.7,
      'max_tokens': 120,
      'messages': [
        {
          'role': 'system',
          'content': 'You are a helpful tutor. Provide one concise hint that'
              ' helps the learner solve the task without revealing the final'
              ' answer. Keep it under 2 sentences.',
        },
        {
          'role': 'user',
          'content': _buildPrompt(task),
        },
      ],
    };
  }

  String _buildPrompt(Task task) {
    final buffer = StringBuffer()
      ..writeln('Task type: ${task.type}')
      ..writeln('Title: ${task.title}')
      ..writeln('Description: ${task.description}');

    if (task is MultipleChoiceTask) {
      buffer
        ..writeln('Question: ${task.question}')
        ..writeln('Options: ${task.options.join(', ')}');
    } else if (task is FillInTheBlankTask) {
      buffer
        ..writeln('Text: ${task.text}')
        ..writeln('Missing segments: ${task.missingSegments.length}');
    } else if (task is MatchingPairTask) {
      buffer
        ..writeln('Left items: ${task.leftItems.join(', ')}')
        ..writeln('Right items: ${task.rightItems.join(', ')}');
    } else if (task is OrderingTask) {
      buffer.writeln('Items: ${task.items.join(', ')}');
    } else if (task is TrueFalseTask) {
      buffer.writeln('Statement: ${task.statement}');
    } else if (task is SpotTheErrorTask) {
      buffer
        ..writeln('Prompt: ${task.prompt}')
        ..writeln('Code snippet: ${task.codeSnippet}')
        ..writeln('Options: ${task.options.join(', ')}');
    } else if (task is CodingChallengeTask) {
      buffer
        ..writeln('Problem: ${task.problemDescription}')
        ..writeln('Expected output: ${task.expectedOutputDescription}')
        ..writeln('Examples:');
      for (final example in task.examples) {
        buffer
          ..writeln('- Input: ${example.input}')
          ..writeln('  Output: ${example.output}');
      }
    } else if (task is InfoCardTask) {
      buffer.writeln('Content: ${task.contentText}');
    }

    buffer.writeln('Provide a helpful hint without giving away the answer.');
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
}
